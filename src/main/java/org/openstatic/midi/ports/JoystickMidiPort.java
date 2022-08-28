package org.openstatic.midi.ports;

import org.openstatic.midi.*;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import net.java.games.input.*;

public class JoystickMidiPort implements MidiPort, Runnable
{
    private String name;
    private Controller controller;
    private Thread myThread;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private ArrayList<Component> controls;

    public JoystickMidiPort(Controller controller)
    {
        this.controller = controller;
        this.controls = new ArrayList<Component>(Arrays.asList(controller.getComponents()));
        this.name = controller.getName();
        this.opened = false;
    }

    public void run()
    {
        System.err.println("Controller thread started");
        while (this.opened)
        {
            try
            {
                boolean poll = this.controller.poll();
                if (poll)
                {
                    /* Get the controllers event queue */
                    EventQueue queue = controller.getEventQueue();

                    /* Create an event object for the underlying plugin to populate */
                    Event event = new Event();

                    /* For each object in the queue */
                    while (queue.getNextEvent(event)) {

                        /*
                            * Create a string buffer and put in it, the controller name,
                            * the time stamp of the event, the name of the component
                            * that changed and the new value.
                            * 
                            * Note that the timestamp is a relative thing, not
                            * absolute, we can tell what order events happened in
                            * across controllers this way. We can not use it to tell
                            * exactly *when* an event happened just the order.
                            */
                        Component comp = event.getComponent();
                        float value = event.getValue();

                        /*
                            * Check the type of the component and display an
                            * appropriate value
                            */
                        int data2 = 0;
                        int cc = controls.indexOf(comp)+1;
                        //System.err.println("CC = " + String.valueOf(cc));
                        if (comp.isAnalog()) 
                        {
                            if (comp.getName().toLowerCase().contains("y"))
                            {
                                //System.err.println("Y invert");
                                value = -(value);
                            }
                            //System.err.println("value = " + String.valueOf(value));
                            data2 = (int) ((value + 1f) * 64);
                            if (data2 > 127) data2 = 127;
                            if (data2 < 0) data2 = 0;
                        } else {
                            if (value == 1.0f) {
                                data2 = 127;
                            } else {
                                data2 = 0;
                            }
                        }
                        long timeStamp = this.getMicrosecondPosition();
                        try
                        {
                            final ShortMessage sm = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, cc, data2);
                            JoystickMidiPort.this.receivers.forEach((r) -> {
                                r.send(sm, timeStamp);
                            });
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                    }
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    public long getMicrosecondPosition()
    {
        return System.currentTimeMillis() * 1000l;
    }
    
    // Does this device transmit MIDI messages
    public boolean canTransmitMessages()
    {
        return true;
    }

    // Does This device Receive MIDI messages
    public boolean canReceiveMessages()
    {
        return false;
    }

    public void open()
    {
        try
        {
            if (!this.isOpened())
            {
                this.opened = true;
                this.myThread = new Thread(this);
                this.myThread.start();
                MidiPortManager.firePortOpened(this);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
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

    public Controller getController()
    {
        return this.controller;
    }

    public void close()
    {
        try
        {
            if (this.isOpened())
            {
                this.opened = false;
                MidiPortManager.firePortClosed(this);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
        
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
}
