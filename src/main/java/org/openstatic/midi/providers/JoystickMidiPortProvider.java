package org.openstatic.midi.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.ResourceBundle.Control;

import org.openstatic.midi.ports.JoystickMidiPort;
import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortProvider;

import java.util.Iterator;

import net.java.games.input.*;
import net.java.games.input.Controller.PortType;

public class JoystickMidiPortProvider implements MidiPortProvider 
{
    private LinkedHashMap<Controller, JoystickMidiPort> localDevices;

    public JoystickMidiPortProvider()
    {
        this.localDevices = new LinkedHashMap<Controller, JoystickMidiPort>();
    }

    @Override
    public Collection<? extends MidiPort> getMidiPorts()
    {
        ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
        Controller[] ca = ce.getControllers();
        Vector<Controller> newLocalDevices = new Vector<Controller>(Arrays.asList(ca));
        

        // Check for new devices added
        for(Iterator<Controller> newLocalDevicesIterator = newLocalDevices.iterator(); newLocalDevicesIterator.hasNext();)
        {
            Controller di = newLocalDevicesIterator.next();
            int componentCount = di.getComponents().length;
            String devName = di.getName().toLowerCase();
            //System.out.println("Local Device Found: " +  + " " + String.valueOf(componentCount));

            if (!this.localDevices.containsKey(di) && componentCount > 0 && !devName.contains("mouse") && !devName.contains("keyboard"))
            {
                JoystickMidiPort ms = new JoystickMidiPort(di);
                this.localDevices.put(di, ms);
            } else {
                //System.out.println("Local Device already exists " + di.getName());
            }
        }

        // check for devices removed
        for(Iterator<Controller> oldDevicesIterator = ((LinkedHashMap<Controller, MidiPort>) this.localDevices.clone()).keySet().iterator(); oldDevicesIterator.hasNext();)
        {
            Controller di = oldDevicesIterator.next();
            if (!newLocalDevices.contains(di))
            {
                this.localDevices.remove(di);
            }
        }
        return this.localDevices.values();
    }

}