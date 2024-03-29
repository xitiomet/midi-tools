package org.openstatic.midi.ports;

import org.openstatic.midi.*;

import org.openstatic.routeput.*;
import org.openstatic.routeput.client.*;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import org.json.*;

public class RoutePutClientMidiPort implements MidiPort, RoutePutMessageListener
{
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private RoutePutClient upstreamClient;
    private int beatPulse;
    private long lastRxAt;
    private long lastTxAt;
    private long txCount;
    private long rxCount;

    public RoutePutClientMidiPort(RoutePutChannel channel, String websocketUri)
    {
        this.upstreamClient = new RoutePutClient(channel, websocketUri);
        this.upstreamClient.setProperty("description", "Midi Control Change Tool");
        this.upstreamClient.addMessageListener(this);
        this.beatPulse = 1;
        this.txCount = 0;
        this.rxCount = 0;
    }

    public RoutePutClientMidiPort(RoutePutClient client)
    {
        this.upstreamClient = client;
        this.upstreamClient.setProperty("description", "Midi Control Change Tool");
        this.upstreamClient.addMessageListener(this);
        this.beatPulse = 1;
        this.txCount = 0;
        this.rxCount = 0;
    }

    public RoutePutClient getRoutePutClient()
    {
        return this.upstreamClient;
    }

    @Override
    public void onMessage(RoutePutSession session, RoutePutMessage j) 
    {
        try
        {
            if (j.isType(RoutePutMessage.TYPE_MIDI))
            {
                this.lastRxAt = System.currentTimeMillis();
                this.rxCount++;
                JSONArray data = j.getRoutePutMeta().getJSONArray("data");
                final long timeStamp = j.getRoutePutMeta().optLong("ts", getMicrosecondPosition());
                int data0 = data.optInt(0, 0);
                int data1 = data.optInt(1, 0);
                int data2 = data.optInt(2, 0);
                int command = data0 & 0xF0;
                int channel = data0 & 0x0F;
                final ShortMessage sm = new ShortMessage(command, channel, data1, data2);
                this.receivers.forEach((r) -> {
                    r.send(sm, timeStamp);
                });
            } else if (j.isType(RoutePutMessage.TYPE_PULSE)) {
                final long timeStamp = j.getRoutePutMeta().optLong("ts", 0);
                final ShortMessage sm = new ShortMessage(ShortMessage.TIMING_CLOCK);
                this.receivers.forEach((r) -> {
                    r.send(sm, timeStamp);
                });
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public long getMicrosecondPosition()
    {
        return System.currentTimeMillis() * 1000l;
    }
    
    // Does this device transmit MIDI messages
    public boolean canTransmitMessages()
    {
        return true;
    }

    // Does This device Receive MIDI messages
    public boolean canReceiveMessages()
    {
        return true;
    }

    public void open()
    {
        if (!this.isOpened())
        {
            this.opened = true;
            this.upstreamClient.connect();
            MidiPortManager.firePortOpened(this);
        }
    }
    
    public boolean isAvailable()
    {
        return true;
    }

    public boolean isOpened()
    {
        return this.opened && this.upstreamClient.isConnected();
    }

    public String getName()
    {
        return "#" + this.upstreamClient.getDefaultChannel().getName();
    }

    public void close()
    {
        if (this.isOpened())
        {
            MidiPortManager.firePortClosed(this);
            if (this.upstreamClient != null)
            {
                try 
                {
                    this.upstreamClient.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.opened = false;
        }
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
        this.lastTxAt = System.currentTimeMillis();
        if(message instanceof ShortMessage && this.opened && this.upstreamClient.isConnected())
        {
            this.txCount++;
            final ShortMessage sm = (ShortMessage) message;
            int smStatus = sm.getStatus();
            if (smStatus == ShortMessage.TIMING_CLOCK)
            {
                RoutePutMessage mm = new RoutePutMessage();
                mm.setType(RoutePutMessage.TYPE_PULSE);
                mm.setMetaField("ts", timeStamp);
                mm.setMetaField("pulse", this.beatPulse);
                if (this.beatPulse >= 24)
                {
                    this.beatPulse = 0;
                }
                this.beatPulse++;
                mm.setChannel(this.upstreamClient.getDefaultChannel());
                this.upstreamClient.send(mm);
            } else {
                RoutePutMessage mm = new RoutePutMessage();
                mm.setType(RoutePutMessage.TYPE_MIDI);
                JSONArray dArray = new JSONArray();
                dArray.put(sm.getStatus());
                dArray.put(sm.getData1());
                dArray.put(sm.getData2());
                mm.setMetaField("data", dArray);
                mm.setMetaField("ts", timeStamp);
                mm.setChannel(this.upstreamClient.getDefaultChannel());
                this.upstreamClient.send(mm);
            }
        }
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

    public long getLastRxAt()
    {
        return this.lastRxAt;
    }

    public long getLastTxAt()
    {
        return this.lastTxAt;
    }

    public Collection<Receiver> getReceivers()
    {
        return this.receivers;
    }

    public boolean hasReceiver(Receiver r)
    {
        return this.receivers.contains(r);
    }

    public boolean equals(MidiPort port)
    {
        return this.upstreamClient.getDefaultChannel().equals(port.getName());
    }
    
    public String toString()
    {
        return this.getName();
    }

    public String getCCName(int channel, int cc)
    {
        return null;
    }

    @Override
    public long getRxCount() 
    {
        return this.rxCount;
    }

    @Override
    public long getTxCount()
    {
        return this.txCount;
    }
}
