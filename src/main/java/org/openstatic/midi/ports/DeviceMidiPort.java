package org.openstatic.midi.ports;

import org.openstatic.midi.*;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Enumeration;

public class DeviceMidiPort implements MidiPort
{
    private String name;
    private MidiDevice device;
    
    // Actual Transmitter/Receiver for the MidiDevice
    private Receiver deviceReceiver;
    private Transmitter deviceTransmitter;

    // Our internal receiver for the output of the device's transmitter
    private Receiver outputReceiver;
    
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private long lastRxAt;
    private long lastTxAt;

    public DeviceMidiPort(MidiDevice device)
    {
        this.device = device;
        this.name = device.getDeviceInfo().getName();
        this.outputReceiver = new Receiver()
        {
            public void send(MidiMessage message, long timeStamp)
            {
                DeviceMidiPort.this.lastRxAt = System.currentTimeMillis();
                for (Enumeration<Receiver> re = ((Vector<Receiver>) DeviceMidiPort.this.receivers.clone()).elements(); re.hasMoreElements();)
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
                    this.deviceTransmitter = device.getTransmitter();
                    this.deviceTransmitter.setReceiver(this.outputReceiver);
                }
                if (this.canReceiveMessages())
                {
                    this.deviceReceiver = device.getReceiver();
                }
                this.device.open();
                MidiPortManager.firePortOpened(this);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (this.deviceTransmitter != null)
                this.deviceTransmitter.close();
            if (this.deviceReceiver != null)
                this.deviceReceiver.close();
        }
    }
    
    public boolean isAvailable()
    {
        return true;
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
                MidiPortManager.firePortClosed(this);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
        this.lastTxAt = System.currentTimeMillis();
        if (this.deviceReceiver != null)
        {
            this.deviceReceiver.send(message, timeStamp);
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

    public long getLastRxAt()
    {
        return this.lastRxAt;
    }

    public long getLastTxAt()
    {
        return this.lastTxAt;
    }
}
