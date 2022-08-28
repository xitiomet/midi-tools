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
    private LinkedHashMap<String, JoystickMidiPort> localDevices;

    public JoystickMidiPortProvider()
    {
        this.localDevices = new LinkedHashMap<String, JoystickMidiPort>();
    }

    @Override
    public Collection<? extends MidiPort> getMidiPorts()
    {
        ControllerEnvironment ce = new DirectAndRawInputEnvironmentPlugin();
        if(!ce.isSupported())
        {
            //System.err.println("Fallback to default controller environment");
            ce = ControllerEnvironment.getDefaultEnvironment();
        }
        Controller[] ca = ce.getControllers();
        Vector<Controller> newLocalDevices = new Vector<Controller>(Arrays.asList(ca));
        Vector<String> controllerNames = new Vector<String>();

        // Check for new devices added
        for(Iterator<Controller> newLocalDevicesIterator = newLocalDevices.iterator(); newLocalDevicesIterator.hasNext();)
        {
            Controller di = newLocalDevicesIterator.next();
            int componentCount = di.getComponents().length;
            String devName = di.getName().toLowerCase();
            controllerNames.add(devName);
            //System.out.println("Local Device Found: " +  + " " + String.valueOf(componentCount));

            if (!this.localDevices.containsKey(devName) && componentCount > 0 && !devName.contains("mouse") && !devName.contains("keyboard"))
            {
                JoystickMidiPort ms = new JoystickMidiPort(di);
                this.localDevices.put(devName, ms);
                System.err.println("Gamepad Added: " + devName);
            } else {
                //System.out.println("Local Device already exists " + di.getName());
            }
        }
        Iterator<String> keys = this.localDevices.keySet().iterator();
        while(keys.hasNext())
        {
            String key = keys.next();
            if (!controllerNames.contains(key))
            {
                System.err.println("Gamepad Removed: " + key);
                JoystickMidiPort removedGamepad = this.localDevices.remove(key);
                removedGamepad.close();
            }
        }
        return this.localDevices.values();
    }

}