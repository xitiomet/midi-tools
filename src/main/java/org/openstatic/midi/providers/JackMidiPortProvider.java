package org.openstatic.midi.providers;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackShutdownCallback;
import org.jaudiolibs.jnajack.JackStatus;
import org.openstatic.MidiTools;
import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortManager;
import org.openstatic.midi.JackMidiMessage;
import org.openstatic.midi.MidiPortProvider;
import org.openstatic.midi.ports.JackMidiPort;

public class JackMidiPortProvider implements MidiPortProvider, JackProcessCallback, JackShutdownCallback, Runnable
{
    private final Jack jack;
    private JackClient client;
    private String jackName;
    private LinkedHashMap<String, JackMidiPort> localDevices;
    private final JackMidi.Event midiEvent;
    private int bufferSize;
    private Thread reconnectThread;
    private long lastProcessAt;

    public JackMidiPortProvider() throws JackException
    {
        this.localDevices = new LinkedHashMap<String, JackMidiPort>();
        this.jackName = MidiTools.instance.getLocalHostname() + " MidiTools";
        this.jack = Jack.getInstance();
        this.midiEvent = new JackMidi.Event();
        this.reconnectThread = new Thread(this);
        this.reconnectThread.start();
    }

    public void connect() throws JackException
    {
        EnumSet<JackStatus> status = EnumSet.noneOf(JackStatus.class);
        this.client = jack.openClient(jackName, EnumSet.of(JackOptions.JackNoStartServer), status);
        this.client.setProcessCallback(this);
        this.client.onShutdown(this);
        this.bufferSize = client.getBufferSize();
        this.localDevices.put("IN1", new JackMidiPort(this, "IN1", JackMidiPort.INPUT_PORT));
        this.localDevices.put("OUT1", new JackMidiPort(this, "OUT1", JackMidiPort.OUTPUT_PORT));
        this.localDevices.put("IN2", new JackMidiPort(this, "IN2", JackMidiPort.INPUT_PORT));
        this.localDevices.put("OUT2", new JackMidiPort(this, "OUT2", JackMidiPort.OUTPUT_PORT));
        client.activate();
    }

    public int getBufferSize()
    {
        return this.bufferSize;
    }

    public JackClient getJackClient()
    {
        return this.client;
    }

    private ShortMessage shortMessageFromBytes(byte[] msgData) throws InvalidMidiDataException
    {
        ShortMessage returnMessage = new javax.sound.midi.ShortMessage();
        if (msgData.length == 3)
        {
            returnMessage = new ShortMessage(msgData[0] & 0xFF, msgData[1], msgData[2]);               
        } else if (msgData.length == 2) {
            returnMessage = new ShortMessage(msgData[0] & 0xFF, msgData[1], 0);
        } else if (msgData.length == 1) {
            returnMessage = new ShortMessage(msgData[0] & 0xFF);
        }
        return returnMessage;
    }

    @Override
    public Collection<? extends MidiPort> getMidiPorts() {
        // TODO Auto-generated method stub
        return this.localDevices.values();
    }

    @Override
    public void clientShutdown(JackClient client) {
        this.localDevices.clear();
        this.client = null;
    }

    private void processPortInput(JackMidiPort jmp)
    {
        try
        {
            byte[] data = null;
            JackPort inputPort = jmp.getJackPort();
            // Read Events From jack and send to apple midi
            int eventCount = JackMidi.getEventCount(inputPort);
            for (int i = 0; i < eventCount; ++i)
            {
                JackMidi.eventGet(midiEvent, inputPort, i);
                int size = midiEvent.size();
                if (data == null || data.length < size)
                {
                    data = new byte[size];
                }
                midiEvent.read(data);
                long jackFrameTime = client.getLastFrameTime();
                final int tickTs = (int) Math.floorDiv(jackFrameTime, 10l) % this.bufferSize;
                ShortMessage m = shortMessageFromBytes(data);
                jmp.handleJackData(m, tickTs);
                
            }
        } catch (Exception e) {
            System.err.println("Jack Read Error");
            e.printStackTrace(System.err);
        }
    }

    private void processPortOutput(JackMidiPort jmp) throws Exception
    {
        // Read Events from AppleMidi Queue and write to jack with frame time
        JackPort outputPort = jmp.getJackPort();
        ConcurrentLinkedQueue<JackMidiMessage> jackOutputQueue = jmp.getJackOutputQueue();
        JackMidi.clearBuffer(outputPort);
        while(jackOutputQueue.peek() != null)
        {
            JackMidiMessage msg = jackOutputQueue.poll();
            int ts = (int) msg.getTick();
            JackMidi.eventWrite(outputPort, ts, msg.getMessage(), msg.getLength());
        }
    }

    @Override
    public boolean process(JackClient client, int nframes) {
        try
        {
            for(JackMidiPort jackPort : this.localDevices.values())
            {
                if (jackPort.canTransmitMessages())
                {
                    processPortInput(jackPort);
                }
                if (jackPort.canReceiveMessages())
                {
                    processPortOutput(jackPort);
                }
            }
            this.lastProcessAt = System.currentTimeMillis();
            return true;
        } catch (Exception ex) {
            System.out.println("ERROR : " + ex);
            ex.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public void run() {
        while(MidiPortManager.isRunning())
        {
            if ((System.currentTimeMillis() - this.lastProcessAt) > 10000)
            {
                System.err.println("Jack Reconnect Test");
                try
                {
                    this.lastProcessAt = System.currentTimeMillis();
                    this.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
