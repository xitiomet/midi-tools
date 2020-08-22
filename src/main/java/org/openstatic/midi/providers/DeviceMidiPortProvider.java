package org.openstatic.midi.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.sound.midi.*;

import org.openstatic.midi.ports.DeviceMidiPort;
import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortProvider;

import java.util.Iterator;

public class DeviceMidiPortProvider implements MidiPortProvider 
{
    private LinkedHashMap<MidiDevice.Info, MidiPort> localDevices;

    public DeviceMidiPortProvider()
    {
        this.localDevices = new LinkedHashMap<MidiDevice.Info, MidiPort>();
    }

    @Override
    public Collection<MidiPort> getMidiPorts()
    {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        Vector<MidiDevice.Info> newLocalDevices = new Vector<MidiDevice.Info>(Arrays.asList(infos));

        // Check for new devices added
        for(Iterator<MidiDevice.Info> newLocalDevicesIterator = newLocalDevices.iterator(); newLocalDevicesIterator.hasNext();)
        {
            MidiDevice.Info di = newLocalDevicesIterator.next();
            //System.out.println("Local Device Found: " + di.toString());

            if (!this.localDevices.containsKey(di))
            {
                try
                {
                    MidiDevice device = MidiSystem.getMidiDevice(di);
                    MidiPort ms = new DeviceMidiPort(device);
                    this.localDevices.put(di, ms);
                } catch (MidiUnavailableException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace(System.err);
                }
            } else {
                //System.out.println("Local Device already exists " + di.getName());
            }
        }

        // check for devices removed
        for(Iterator<MidiDevice.Info> oldDevicesIterator = ((LinkedHashMap<MidiDevice.Info, MidiPort>) this.localDevices.clone()).keySet().iterator(); oldDevicesIterator.hasNext();)
        {
            MidiDevice.Info di = oldDevicesIterator.next();
            if (!newLocalDevices.contains(di))
            {
                this.localDevices.remove(di);
            }
        }
        return this.localDevices.values();
    }

}