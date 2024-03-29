package org.openstatic;

import org.openstatic.midi.*;
import javax.sound.midi.*;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Collection;
import org.json.*;

public class MidiAPIPort implements MidiPort
{
    public static final int TYPE_BIDIRECTIONAL = 0;
    public static final int TYPE_INPUT = 1;
    public static final int TYPE_OUTPUT = 2;
    
    private String name;
    private String deviceId;
    private int type;
    private WebSocketSession session;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private long lastRxAt;
    private long lastTxAt;
    private long txCount;
    private long rxCount;

    public MidiAPIPort(String name, String deviceId, WebSocketSession session, int type)
    {
        this.name = name;
        this.session = session;
        this.deviceId = deviceId;
        this.type = type;
        this.txCount = 0;
        this.rxCount = 0;
    }
    
    public WebSocketSession getWebSocketSession()
    {
        return this.session;
    }
    
    public void setWebSocketSession(WebSocketSession s)
    {
        this.session = s;
    }
    
    public void open()
    {
        if (!this.opened)
        {
            try
            {
                JSONObject mm = new JSONObject();
                mm.put("event", "openMidiDevice");
                mm.put("device", this.getDeviceId());
                MidiPortManager.firePortOpened(this);
                this.session.getRemote().sendStringByFuture(mm.toString());
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            this.opened = true;
        }
    }
    
    public void close()
    {
        if (this.opened)
        {
            try
            {
                JSONObject mm = new JSONObject();
                mm.put("event", "closeMidiDevice");
                mm.put("device", this.getDeviceId());
                MidiPortManager.firePortClosed(this);
                this.session.getRemote().sendStringByFuture(mm.toString());
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            this.opened = false;
        }
    }
    
    public boolean isOpened()
    {
        return this.opened;
    }
    
    public boolean isAvailable()
    {
        if (this.session == null)
        {
            return false;
        } else if (this.session.isOpen()) {
            return true;
        }
        return false;
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public String toString()
    {
        return this.name;
    }
    
    public String getDeviceId()
    {
        return this.deviceId;
    }
    
    public long getMicrosecondPosition()
    {
        return System.currentTimeMillis() * 1000l;
    }
    
    public void handleWebSocketEvent(JSONObject j)
    {
        if (this.opened)
        {
            try
            {
                if (j.has("do"))
                {
                    String doCmd = j.optString("do","");
                    if (doCmd.equals("midiShortMessage"))
                    {
                        JSONArray data = j.getJSONArray("data");
                        final long timeStamp = j.optLong("timeStamp", getMicrosecondPosition());
                        int data0 = data.optInt(0, 0);
                        int data1 = data.optInt(1, 0);
                        int data2 = data.optInt(2, 0);
                        int command = data0 & 0xF0;
                        int channel = data0 & 0x0F;
                        final ShortMessage sm = new ShortMessage(command, channel, data1, data2);
                        this.lastRxAt = System.currentTimeMillis();
                        this.rxCount++;
                        for (Enumeration<Receiver> re = ((Vector<Receiver>) MidiAPIPort.this.receivers.clone()).elements(); re.hasMoreElements();)
                        {
                            try
                            {
                                Receiver r = re.nextElement();
                                r.send(sm, timeStamp);
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }
                    } else if (doCmd.equals("beatClock")) {
                        final long timeStamp = j.optLong("timeStamp", 0);
                        final ShortMessage sm = new ShortMessage(ShortMessage.TIMING_CLOCK);
                        for (Enumeration<Receiver> re = ((Vector<Receiver>) MidiAPIPort.this.receivers.clone()).elements(); re.hasMoreElements();)
                        {
                            try
                            {
                                Receiver r = re.nextElement();
                                r.send(sm, timeStamp);
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    public boolean equals(MidiPort port)
    {
        if (port instanceof MidiAPIPort)
        {
            MidiAPIPort maport = (MidiAPIPort) port;
            return this.deviceId.equals(maport.getDeviceId()) && this.session.equals(maport.getWebSocketSession());
        } else {
            return false;
        }
    }
    
    // does the midi port have an output?
    public boolean canTransmitMessages()
    {
        if (this.type == 0 || this.type == 1)
        {
            return true;
        } else {
            return false;
        }
    }
    
    // add a receiver for the device to transmit to, canTransmitMessages should be true
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
    
    public boolean hasReceiver(Receiver r)
    {
        return this.receivers.contains(r);
    }
    
    public Collection<Receiver> getReceivers()
    {
        return this.receivers;
    }
    
    // does the midi port have an input?
    public boolean canReceiveMessages()
    {
        if (this.type == 0 || this.type == 2)
        {
            return true;
        } else {
            return false;
        }
    }
    
    // transmit to this device. canReceiveMessages should be true.
    public void send(MidiMessage message, long timeStamp)
    {
        this.lastTxAt = System.currentTimeMillis();
        if(message instanceof ShortMessage && this.opened)
        {
            this.txCount++;
            final ShortMessage sm = (ShortMessage) message;
            int smStatus = sm.getStatus();
            /*
            if (sm.getData1() > 0)
                System.err.println("Recieved Short Message " + MidiPortManager.shortMessageToString(sm));
                */
            if (smStatus == ShortMessage.TIMING_CLOCK)
            {
                try
                {
                    JSONObject mm = new JSONObject();
                    mm.put("event", "beatClock");
                    mm.put("device", this.getDeviceId());
                    mm.put("timeStamp", timeStamp);
                    this.session.getRemote().sendStringByFuture(mm.toString());
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    this.close();
                }
            } else {
                try
                {
                    JSONObject mm = new JSONObject();
                    mm.put("event", "midiShortMessage");
                    mm.put("device", this.getDeviceId());
                    JSONArray dArray = new JSONArray();
                    dArray.put(sm.getStatus());
                    dArray.put(sm.getData1());
                    dArray.put(sm.getData2());
                    mm.put("data", dArray);
                    mm.put("timeStamp", timeStamp);
                    this.session.getRemote().sendStringByFuture(mm.toString());
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    this.close();
                }
            }
        }
    }

    public long getLastTxAt()
    {
        return this.lastTxAt;
    }
    
    public long getLastRxAt()
    {
        return this.lastRxAt;
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
