package org.openstatic.midi.ports;

import org.openstatic.MidiTools;
import org.openstatic.midi.*;

import io.github.leovr.rtipmidi.*;
import io.github.leovr.rtipmidi.messages.AppleMidiInvitationAccepted;
import io.github.leovr.rtipmidi.messages.AppleMidiInvitationDeclined;
import io.github.leovr.rtipmidi.messages.AppleMidiInvitationRequest;
import io.github.leovr.rtipmidi.model.AppleMidiServerAddress;
import io.github.leovr.rtipmidi.session.AppleMidiSession;
import io.github.leovr.rtipmidi.session.AppleMidiSessionClient;

import java.net.Inet4Address;

//import io.github.leovr.rtipmidi.model.MidiMessage;

import java.net.InetAddress;

import javax.annotation.Nonnull;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import javax.sound.midi.*;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import java.util.Vector;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class RTPMidiPort implements MidiPort, ServiceListener, ListModel<AppleMidiSessionClient>
{
    private String name;
    private String rtp_name;
    private int port;
    private InetAddress hostname;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private LinkedHashMap<String, AppleMidiSessionClient> remoteServers = new LinkedHashMap<String, AppleMidiSessionClient>();
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();
    private AppleMidiServer appleMidiServer;
    private AppleMidiSession session;
    private JmDNS jmdns;
    private long lastRxAt;
    private long lastTxAt;
    private long txCount;
    private long rxCount;

    public RTPMidiPort(String name, String rtp_name, InetAddress hostname, int port)
    {
        this.txCount = 0;
        this.rxCount = 0;
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
                                RTPMidiPort.this.rxCount++;
                                RTPMidiPort.this.receivers.forEach((r) -> {
                                    r.send(sm, timestamp);
                                });
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        } else if (msgData.length == 2) {
                            try
                            {
                                ShortMessage sm = new ShortMessage(msgData[0], msgData[1], 0);
                                RTPMidiPort.this.lastRxAt = System.currentTimeMillis();
                                RTPMidiPort.this.rxCount++;
                                RTPMidiPort.this.receivers.forEach((r) -> {
                                    r.send(sm, timestamp);
                                });
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            }
                        } else if (msgData.length == 1) {
                            try
                            {
                                ShortMessage sm = new ShortMessage(msgData[0] & 0xFF);
                                RTPMidiPort.this.lastRxAt = System.currentTimeMillis();
                                RTPMidiPort.this.rxCount++;
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

            @Override
            public void onMidiInvitation(@Nonnull final AppleMidiInvitationRequest invitation, @Nonnull final AppleMidiServerAddress appleMidiServer)
            {
                MidiTools.logIt("RTP Invitation from " + invitation.getName());
            }

            @Override
            public void onMidiInvitationAccepted(@Nonnull AppleMidiInvitationAccepted arg0,
                    @Nonnull io.github.leovr.rtipmidi.model.AppleMidiServerAddress arg1) {
                MidiTools.logIt("RTP Invitation accepted by " + arg0.getName());

                
            }

            @Override
            public void onMidiInvitationDeclined(@Nonnull AppleMidiInvitationDeclined arg0,
                    @Nonnull io.github.leovr.rtipmidi.model.AppleMidiServerAddress arg1) {
                MidiTools.logIt("RTP Invitation declined by " + arg0.getName());

                
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

    public AppleMidiServer getAppleMidiServer()
    {
        return this.appleMidiServer;
    }

    public Collection<AppleMidiSessionClient> getRemoteServers()
    {
        return this.remoteServers.values();
    }

    public AppleMidiSessionClient addSessionClient(String name, String ipAddress, int port)
    {
        try
        {
            AppleMidiSessionClient client = new AppleMidiSessionClient(name, InetAddress.getByName(ipAddress), port, this.rtp_name);
            client.setAppleMidiSession(this.session);
            this.remoteServers.put(name, client);
            return client;
        } catch (Exception e) {
            MidiTools.instance.midi_logger_b.printException(e);
        }
        return null;
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
                    jmdns.addServiceListener("_apple-midi._udp.local.", RTPMidiPort.this);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                try
                {
                    String strHostname = this.hostname.getHostName();
                    System.err.println("Launching RTP on " + strHostname + ":" + String.valueOf(this.port));
                    this.appleMidiServer = new AppleMidiServer(this.hostname, this.rtp_name, this.port);
                    this.appleMidiServer.setAppleMidiSession(this.session);
                    this.appleMidiServer.start();
                } catch (Exception e2) {
                    Thread t2 = new Thread(() -> {
                        shutDownMDNS();
                    });
                    t2.start();
                    this.opened = false;
                    MidiTools.instance.midi_logger_b.printException(e2);
                    MidiPortManager.firePortClosed(this);
                    
                }
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
                    this.appleMidiServer.setAppleMidiSession(null);
                    this.appleMidiServer.stop();
                    this.appleMidiServer = null;
                }
                this.remoteServers.values().forEach((remoteServer) -> 
                {
                    remoteServer.stopClient();
                });
                this.remoteServers.clear();
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
            this.txCount++;
            byte[] msgData = message.getMessage();
            if (msgData.length == 3)
            {
                io.github.leovr.rtipmidi.model.ShortMessage rtpSM = new io.github.leovr.rtipmidi.model.ShortMessage(msgData[0], msgData[1], msgData[2]);
                this.session.sendMidiMessage(rtpSM, timeStamp);
            } else if (msgData.length == 2) {
                io.github.leovr.rtipmidi.model.ShortMessage rtpSM = new io.github.leovr.rtipmidi.model.ShortMessage(msgData[0], msgData[1]);
                this.session.sendMidiMessage(rtpSM, timeStamp);
            } else if (msgData.length == 1) {
                io.github.leovr.rtipmidi.model.ShortMessage rtpSM = new io.github.leovr.rtipmidi.model.ShortMessage(msgData[0]);
                this.session.sendMidiMessage(rtpSM, timeStamp);
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

    @Override
    public void serviceAdded(ServiceEvent event) {
        
    }

    private void addRemoteServer(String name, AppleMidiSessionClient server)
    {
        if (!this.remoteServers.containsKey(name))
        {
            this.remoteServers.put(name, server);
            int location = this.remoteServers.size();
            for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
            {
                try
                {
                    final ListDataListener ldl = ldle.nextElement();
                    final ListDataEvent lde = new ListDataEvent(server, ListDataEvent.INTERVAL_ADDED, location, location);
                    SwingUtilities.invokeAndWait(() -> {
                        System.err.println("Interval Added "+ String.valueOf(location));
                        ldl.intervalAdded(lde);
                    });
                } catch (Exception mlex) {
                }
            }
        }
    }

    private void removeRemoteServer(String name)
    {
        if (this.remoteServers.containsKey(name))
        {
            int location = 0;
            Iterator<String> kIterator = this.remoteServers.keySet().iterator();
            while(kIterator.hasNext())
            {
                String key = kIterator.next();
                if (key.equals(name))
                    break;
                location++;
            }
            final int fLocation = location;
            AppleMidiSessionClient server = this.remoteServers.remove(name);
            for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
            {
                try
                {
                    final ListDataListener ldl = ldle.nextElement();
                    final ListDataEvent lde = new ListDataEvent(server, ListDataEvent.INTERVAL_REMOVED, location, location);
                    SwingUtilities.invokeAndWait(() -> {
                        System.err.println("Interval Removed "+ String.valueOf(fLocation));
                        ldl.intervalRemoved(lde);
                    });
                } catch (Exception mlex) {
                }
            }
        }
    }

    @Override
    public void serviceRemoved(ServiceEvent event) 
    {
        ServiceInfo serviceInfo = event.getInfo();
        String serviceName = serviceInfo.getName();
        removeRemoteServer(serviceName);
    }

    @Override
    public void serviceResolved(ServiceEvent event) 
    {
        ServiceInfo serviceInfo = event.getInfo();
        int port = serviceInfo.getPort();
        String serviceName = serviceInfo.getName();
        if (!remoteServers.containsKey(serviceName))
        {
            System.err.println(this.getName() + " resovled " + serviceName);
            Inet4Address[] addresses = serviceInfo.getInet4Addresses();
            AppleMidiSessionClient remoteServer = new AppleMidiSessionClient(serviceName, addresses[0], port, this.rtp_name);
            remoteServer.setAppleMidiSession(this.session);
            addRemoteServer(serviceName, remoteServer);
        }
    }

    @Override
    public int getSize() {
        return this.remoteServers.size();
    }

    @Override
    public AppleMidiSessionClient getElementAt(int index) 
    {
        return remoteServers.values().toArray(new AppleMidiSessionClient[remoteServers.size()])[index];
    }

    @Override
    public void addListDataListener(ListDataListener l)
    {
        //System.err.println("RTPMidi port addListDataListener");
        if (!this.listeners.contains(l))
            this.listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
        try
        {
            this.listeners.remove(l);
        } catch (Exception e) {}
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
