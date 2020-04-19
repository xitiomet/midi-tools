package org.openstatic.midi;


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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Random;
import org.json.*;

public class RTPMidiPort implements MidiPort
{
    private String name;
    private String hostname;
    private Receiver outputReceiver;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private AppleMidiServer appleMidiServer;
    private AppleMidiSession session;
    private JmDNS jmdns;

    public RTPMidiPort(String name, int port)
    {
        this.name = name;
        InetAddress localHost = this.getLocalHost();
        this.hostname = localHost.getHostName();
        
        try
        {
            this.jmdns = JmDNS.create(localHost);
            ServiceInfo serviceInfo = ServiceInfo.create("_apple-midi._udp.local.", name, port, "MidiTools RTP Port " + this.name);
            jmdns.registerService(serviceInfo);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        
        this.appleMidiServer = new AppleMidiServer(hostname, port);
        this.session = new AppleMidiSession()
        {
            protected void onMidiMessage(final io.github.leovr.rtipmidi.model.MidiMessage message, final long timestamp)
            {
                if (message != null && RTPMidiPort.this.opened)
                {
                    if (message instanceof io.github.leovr.rtipmidi.model.ShortMessage)
                    {
                        byte[] msgData = message.getData();
                        try
                        {
                            ShortMessage sm = new ShortMessage(msgData[0], msgData[1], msgData[2]);
                            for (Enumeration<Receiver> re = ((Vector<Receiver>) RTPMidiPort.this.receivers.clone()).elements(); re.hasMoreElements();)
                            {
                                Receiver r = re.nextElement();
                                r.send(sm, timestamp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }
        };
        this.appleMidiServer.addAppleMidiSession(session);
        this.appleMidiServer.start();
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
        try
        {
            if (this.isOpened())
            {
                MidiPortManager.firePortClosed(this);
                this.opened = false;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
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
    
    public String toString()
    {
        return this.name;
    }
    
    // Figure out the local host ignoring any loopback interfaces.
    private static InetAddress getLocalHost()
    {
        InetAddress ra = null;
        try
        {
            for(Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces(); n.hasMoreElements();)
            {
                NetworkInterface ni = n.nextElement();
                for(Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();)
                {
                    InetAddress ia = e.nextElement();
                    if (!ia.isLoopbackAddress() && ia.isSiteLocalAddress())
                    {
                        System.err.println("Possible Local Address:" + ia.toString());
                        ra = ia;
                    }
                }
            }

        } catch (Exception e) {}
        return ra;
    }
}
