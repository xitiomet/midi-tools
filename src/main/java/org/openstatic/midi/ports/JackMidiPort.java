package org.openstatic.midi.ports;

import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.openstatic.midi.JackMidiMessage;
import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortManager;
import org.openstatic.midi.providers.JackMidiPortProvider;

public class JackMidiPort implements MidiPort
{
    public static final int INPUT_PORT = 1;
    public static final int OUTPUT_PORT = 2;

    private JackMidiPortProvider provider;
    private JackClient client;
    private String name;
    private JackPort jackPort;
    private long lastRxAt;
    private long lastTxAt;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private ConcurrentLinkedQueue<JackMidiMessage> jackOutputQueue;
    private boolean opened;
    private int direction;

    public JackMidiPort(JackMidiPortProvider provider, String portName, int direction) throws JackException
    {
        this.client = provider.getJackClient();
        this.opened = false;
        this.name = portName;
        this.provider = provider;
        this.direction = direction;
        this.jackOutputQueue = new ConcurrentLinkedQueue<JackMidiMessage>();
        if (direction == INPUT_PORT)
        {
            this.jackPort = client.registerPort(this.name, JackPortType.MIDI, JackPortFlags.JackPortIsInput);
        } else if (direction == OUTPUT_PORT) {
            this.jackPort = client.registerPort(this.name, JackPortType.MIDI, JackPortFlags.JackPortIsOutput);
        }
    }

    public JackPort getJackPort()
    {
        return this.jackPort;
    }

    @Override
    public void open() {
        this.opened = true;
        MidiPortManager.firePortOpened(this);

    }

    @Override
    public void close() {
        this.opened = false;
        MidiPortManager.firePortClosed(this);
    }

    @Override
    public boolean isOpened() {
        return this.opened;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "JACK " + this.name;
    }

    public String toString()
    {
        return "JACK " + this.name;
    }

    @Override
    public long getMicrosecondPosition()
    {
        try
        {
            long jackFrameTime = client.getFrameTime();
            long frameTime = Math.floorDiv(jackFrameTime, 10l);
            return frameTime;
        } catch (Exception e) {
            return 0l;
        }
    }

    @Override
    public boolean equals(MidiPort port)
    {
        return this.name.equals(port.getName());
    }

    @Override
    public boolean canTransmitMessages() {
        return this.direction == INPUT_PORT;
    }

    public void handleJackData(MidiMessage midiMessage, long timestamp)
    {
        if (this.opened)
        {
            try
            {
                JackMidiPort.this.lastRxAt = System.currentTimeMillis();
                JackMidiPort.this.receivers.forEach((r) -> {
                    r.send(midiMessage, timestamp);
                });
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public ConcurrentLinkedQueue<JackMidiMessage> getJackOutputQueue()
    {
        return this.jackOutputQueue;
    }

    public void addReceiver(Receiver r)
    {
        if (!this.receivers.contains(r))
        {
            this.receivers.add(r);
        }
    }
    
    public void removeReceiver(Receiver r)
    {
        if (this.receivers.contains(r))
        {
            this.receivers.remove(r);
        }
    }

    public Collection<Receiver> getReceivers()
    {
        return this.receivers;
    }

    public boolean hasReceiver(Receiver r)
    {
        return this.receivers.contains(r);
    }

    @Override
    public boolean canReceiveMessages() {
        return this.direction == OUTPUT_PORT;
    }

    @Override
    public void send(MidiMessage message, long timeStamp)
    {
        if (this.opened)
        {
            JackMidiPort.this.lastTxAt = System.currentTimeMillis();
            int ts = (int) (this.getMicrosecondPosition() % this.provider.getBufferSize());
            this.jackOutputQueue.add(new JackMidiMessage(message.getMessage(), ts));
        }
    }

    public long getLastRxAt()
    {
        return this.lastRxAt;
    }

    public long getLastTxAt()
    {
        return this.lastTxAt;
    }

    public String getCCName(int channel, int cc)
    {
        return null;
    }
}
