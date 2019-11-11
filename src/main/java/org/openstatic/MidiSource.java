package org.openstatic;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class MidiSource
{
    private String name;
    private MidiDevice device;
    private Transmitter transmitter;
    private boolean opened;

    public MidiSource(MidiDevice device)
    {
        this.device = device;
        this.name = device.getDeviceInfo().getName();
    }

    public void open()
    {
        try
        {
            if (this.transmitter == null)
            {
                this.transmitter = device.getTransmitter();
                this.device.open();
                this.opened = true;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public boolean isOpened()
    {
        return this.opened;
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
            if (this.transmitter != null)
            {
                this.transmitter.close();
                this.device.close();
                this.opened = false;
                this.transmitter = null;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public Transmitter getTransmitter()
    {
        this.open();
        return this.transmitter;
    }

    public void setReceiver(Receiver r)
    {
        try
        {
            this.getTransmitter().setReceiver(r);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public boolean equals(MidiSource source)
    {
        return this.name.equals(source.getName());
    }
}
