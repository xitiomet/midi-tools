package org.openstatic.midi.ports;

import org.openstatic.midi.*;


import io.github.leovr.rtipmidi.*;
import io.github.leovr.rtipmidi.session.AppleMidiSession;
//import io.github.leovr.rtipmidi.model.MidiMessage;

import java.net.InetAddress;
import java.net.NetworkInterface;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Enumeration;

public class RTPMidiPort implements MidiPort
{
    private String name;
    private String rtp_name;
    private int port;
    private InetAddress hostname;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private AppleMidiServer appleMidiServer;
    private AppleMidiSession session;
    private JmDNS jmdns;
    private long lastRxAt;
    private long lastTxAt;

    public RTPMidiPort(String name, String rtp_name, InetAddress hostname, int port)
    {
        this.name = name;
        this.rtp_name = rtp_name;
        this.port = port;
        this.hostname = hostname;
        this.session = new AppleMidiSession()
        {
            protected void onMidiMessage(final io.github.leovr.rtipmidi.model.MidiMessage message, final long timestamp)
            {
                if (message != null && RTPMidiPort.this.opened)
                {
                    if (message instanceof io.github.leovr.rtipmidi.model.ShortMessage)
                    {
                        byte[] msgData = message.getData();
                        if (msgData.length == 3)
                        {
                            try
                            {
                                ShortMessage sm = new ShortMessage(msgData[0], msgData[1], msgData[2]);
                                RTPMidiPort.this.lastRxAt = System.currentTimeMillis();
                                RTPMidiPort.this.receivers.forEach((r) -> {
                                    r.send(sm, timestamp);
                                });
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        }
                    }
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
          public void run()
          {
            RTPMidiPort.this.shutDownMDNS();
          }
        });
    }

    public void shutDownMDNS()
    {
        if (this.jmdns != null)
        {
            System.err.println("Please Wait for mDNS to unregister....");
            try
            {
                this.jmdns.unregisterAllServices();
                this.jmdns.close();
                this.jmdns = null;
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    public long getMicrosecondPosition()
    {
        if (this.session != null)
        {
            return this.session.getCurrentTimestamp();
        } else {
            return System.currentTimeMillis() * 1000l;
        }
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
            Thread t = new Thread(() -> {
                try
                {
                    this.jmdns = JmDNS.create(this.hostname);
                    ServiceInfo serviceInfo = ServiceInfo.create("_apple-midi._udp.local.", this.rtp_name, this.port, "MidiTools RTP Port " + this.name);
                    jmdns.registerService(serviceInfo);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                this.appleMidiServer = new AppleMidiServer(this.hostname.getHostName(), this.port);
                this.appleMidiServer.addAppleMidiSession(session);
                this.appleMidiServer.start();
            });
            t.start();
            MidiPortManager.firePortOpened(this);
        }
    }
    
    public boolean isAvailable()
    {
        return true;
    }

    public boolean isOpened()
    {
        return this.opened;
    }

    public String getName()
    {
        return this.name;
    }

    public void close()
    {
        if (this.isOpened())
        {
            MidiPortManager.firePortClosed(this);
            try
            {
                Thread t = new Thread(() -> {
                    shutDownMDNS();
                });
                t.start();
                if (this.appleMidiServer != null)
                {
                    this.appleMidiServer.removeAppleMidiSession(this.session);
                    this.appleMidiServer.stop();
                    this.appleMidiServer = null;
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            this.opened = false;
        }
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
        this.lastTxAt = System.currentTimeMillis();
        if (this.session != null && message instanceof ShortMessage && this.opened)
        {
            byte[] msgData = message.getMessage();
            io.github.leovr.rtipmidi.model.ShortMessage rtpSM = new io.github.leovr.rtipmidi.model.ShortMessage(msgData[0], msgData[1], msgData[2]);
            this.session.sendMidiMessage(rtpSM, timeStamp);
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
        return this.name.equals(port.getName());
    }
    
    public long getLastRxAt()
    {
        return this.lastRxAt;
    }

    public long getLastTxAt()
    {
        return this.lastTxAt;
    }

    public String toString()
    {
        return this.name;
    }
    
}
