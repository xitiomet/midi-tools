package org.openstatic.midi.ports;

import org.openstatic.midi.*;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import net.java.games.input.*;

public class JoystickMidiPort implements MidiPort, Runnable
{
    private String name;
    private Controller controller;
    private Thread myThread;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private ArrayList<Component> controls;
    private long lastRxAt;
    private long lastTxAt;
    private HashMap<Integer, String> ccNames;
    private Rumbler[] rumblers;
    private int channel;

    public JoystickMidiPort(Controller controller, int channel)
    {
        this.ccNames = new HashMap<Integer, String>();
        this.channel = channel;
        this.controller = controller;
        this.rumblers = controller.getRumblers();
        this.controls = new ArrayList<Component>(Arrays.asList(controller.getComponents()));
        this.name = controller.getName();
        for (Component control : this.controls) {
            int cc = controls.indexOf(control)+1;
            if (!this.ccNames.containsKey(cc))
                this.ccNames.put(cc, "Gamepad " + control.getName().toUpperCase());
        }
        this.opened = false;
    }

    private boolean isDigital(Component control)
    {
        if (control.getClass().getName().endsWith("Button"))
            return true;
        return !control.isAnalog();
    }

    // Analog but only with 0.0f - 1.0f no negative numbers
    private boolean isHalfAnalag(Component control)
    {
        if (control.getClass().getName().endsWith("Trigger"))
            return true;
        return false;
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
                        String compName = comp.getName();
                        String compNameLC = compName.toLowerCase();
                        /*
                            * Check the type of the component and display an
                            * appropriate value
                            */
                        int data2 = 0;
                        int cc = controls.indexOf(comp)+1;
                        //System.err.println("CC = " + String.valueOf(cc));
                        
                        if (isDigital(comp)) 
                        {
                            if (value == 1.0f) {
                                data2 = 127;
                            } else {
                                data2 = 0;
                            }
                        } else {
                            if (compNameLC.contains("y"))
                            {
                                //System.err.println("Y invert");
                                value = -(value);
                            }
                            //System.err.println(compName + " value = " + String.valueOf(value));
                            if (isHalfAnalag(comp))
                               data2 = (int) (value * 127f);
                            else
                               data2 = (int) ((value + 1f) * 64);
                            if (data2 > 127) data2 = 127;
                            if (data2 < 0) data2 = 0;
                        }
                        long timeStamp = this.getMicrosecondPosition();
                        try
                        {
                            final ShortMessage sm = new ShortMessage(ShortMessage.CONTROL_CHANGE, this.channel-1, cc, data2);
                            this.lastRxAt = System.currentTimeMillis();
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
        return rumblers.length > 0;
    }

    public void open()
    {
        try
        {
            if (!this.isOpened())
            {
                this.opened = true;
                this.myThread = new Thread(this);
                this.myThread.setName("Controller Thread " + this.name);
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

    public static float mapFloat(float x, float in_min, float in_max, float out_min, float out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
    
    public void send(MidiMessage message, long timeStamp)
    {
        this.lastTxAt = System.currentTimeMillis();
        if (rumblers.length > 0)
        {
            if (message instanceof ShortMessage && this.opened)
            {
                ShortMessage smsg = (ShortMessage) message;
                if (smsg.getCommand() == ShortMessage.CONTROL_CHANGE)
                {
                    int idx = smsg.getData1() -1;
                    float rumbleValue = mapFloat(smsg.getData2(), 0, 127, 0, 1);
                    //System.err.println("Rumbling " + String.valueOf(idx) + " = " + String.valueOf(rumbleValue));
                    if (idx < rumblers.length)
                    {
                        rumblers[idx].rumble(rumbleValue);
                    }
                }
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

    public String getCCName(int channel, int cc)
    {
        return this.ccNames.get(cc);
    }
}
