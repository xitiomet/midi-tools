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
import java.util.Random;
import org.json.*;

public class MidiRandomizerPort implements MidiPort, Runnable
{
    private String name;
    private Receiver outputReceiver;
    private Thread myThread;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private Vector<JSONObject> randomRules = new Vector<JSONObject>();

    public MidiRandomizerPort(String name)
    {
        this.name = name;
    }
    
    public static JSONObject defaultRuleJSONObject()
    {
        JSONObject newRule = new JSONObject();
        newRule.put("channel", 1);
        newRule.put("cc", 1);
        newRule.put("min", 0);
        newRule.put("max", 127);
        newRule.put("smooth", true);
        return newRule;
    }
    
    public void addRandomRule(int channel, int cc, int min, int max)
    {
        JSONObject newRule = defaultRuleJSONObject();
        newRule.put("channel", channel);
        newRule.put("cc", cc);
        newRule.put("min", min);
        newRule.put("max", max);
        this.addRandomRule(newRule);
    }
    
    public void addRandomRule(JSONObject newRule)
    {
        this.randomRules.add(newRule);
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
    
    private static int getRandomNumberInRange(int min, int max)
    {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
    
    public void run()
    {
        while (this.opened)
        {
            try
            {
                long timeStamp = this.getMicrosecondPosition();
                for (Enumeration<JSONObject> rr = ((Vector<JSONObject>) MidiRandomizerPort.this.randomRules.clone()).elements(); rr.hasMoreElements();)
                {
                    JSONObject randRule = rr.nextElement();
                    int min = randRule.optInt("min", 0);
                    int max = randRule.optInt("max", 0);
                    int channel = randRule.optInt("channel", 1);
                    int cc = randRule.optInt("cc", 0);
                    int target = randRule.optInt("target", 0);
                    int value = randRule.optInt("value", 0);
                    
                    int data2 = value;
                    if (target == value)
                    {
                        target = getRandomNumberInRange(min, max);
                        randRule.put("target", target);
                    } else {
                        if (target > value)
                        {
                            data2++;
                        } else if (target < value) {
                            data2--;
                        }
                        randRule.put("value", data2);
                        final ShortMessage sm = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, cc, data2);
                        for (Enumeration<Receiver> re = ((Vector<Receiver>) MidiRandomizerPort.this.receivers.clone()).elements(); re.hasMoreElements();)
                        {
                            Receiver r = re.nextElement();
                            r.send(sm, timeStamp);
                        }
                    }
                }
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void open()
    {
        if (!this.isOpened())
        {
            this.opened = true;
            this.myThread = new Thread(this);
            this.myThread.start();
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
        this.opened = false;
        try
        {
            if (this.isOpened())
            {
                MidiPortManager.firePortClosed(this);
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
