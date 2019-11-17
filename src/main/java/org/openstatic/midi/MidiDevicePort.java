package org.openstatic.midi;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class MidiDevicePort implements MidiPort
{
    private String name;
    private MidiDevice device;
    
    private Receiver deviceReceiver;
    private Receiver outputReceiver;
    
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();

    public MidiDevicePort(MidiDevice device)
    {
        this.device = device;
        this.name = device.getDeviceInfo().getName();
        this.deviceReceiver = new Receiver()
        {
            public void send(MidiMessage message, long timeStamp)
            {
                for (Enumeration<Receiver> re = ((Vector<Receiver>) MidiDevicePort.this.receivers.clone()).elements(); re.hasMoreElements();)
                {
                    try
                    {
                        Receiver r = re.nextElement();
                        r.send(message, timeStamp);
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
            
            public void close()
            {
                
            }
        };
    }
    
    public long getMicrosecondPosition()
    {
        return this.device.getMicrosecondPosition();
    }
    
    // Does this device transmit MIDI messages
    public boolean canTransmitMessages()
    {
        int mt = this.device.getMaxTransmitters();
        return (mt > 0) || (mt == -1);
    }

    // Does This device Receive MIDI messages
    public boolean canReceiveMessages()
    {
        int mr = this.device.getMaxReceivers();
        return (mr > 0) || (mr == -1);
    }

    public void open()
    {
        try
        {
            if (!this.isOpened())
            {
                if (this.canTransmitMessages())
                {
                    device.getTransmitter().setReceiver(this.deviceReceiver);
                }
                if (this.canReceiveMessages())
                {
                    this.outputReceiver = device.getReceiver();
                }
                this.device.open();
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public boolean isOpened()
    {
        return this.device.isOpen();
    }

    public String getName()
    {
        return this.name;
    }

    public MidiDevice getDevice()
    {
        return this.device;
    }

    public void close()
    {
        try
        {
            if (this.isOpened())
            {
                this.device.close();
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
        if (this.outputReceiver != null)
        {
            this.outputReceiver.send(message, timeStamp);
        }
    }

    public void addReceiver(Receiver r)
    {
        if (!this.receivers.contains(r))
        {
            this.receivers.add(r);
        }
    }

    public boolean equals(MidiPort port)
    {
        return this.name.equals(port.getName());
    }
}
