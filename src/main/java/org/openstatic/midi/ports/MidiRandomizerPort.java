package org.openstatic.midi.ports;

import org.openstatic.midi.*;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Random;
import org.json.*;

public class MidiRandomizerPort implements MidiPort, Runnable
{
    private String name;
    private Thread myThread;
    private boolean opened;
    private Vector<Receiver> receivers = new Vector<Receiver>();
    private JSONArray randomRules = new JSONArray();
    private Vector<RandomizerRulesListener> listeners;
    private long lastRxAt;
    private long lastTxAt;

    public MidiRandomizerPort(String name)
    {
        this(name, new JSONArray());
    }
    
    public MidiRandomizerPort(String name, JSONArray rules)
    {
        this.name = name;
        this.randomRules = rules;
        this.listeners = new Vector<RandomizerRulesListener>();
    }

    public void addRandomizerRulesListener(RandomizerRulesListener rrl)
    {
        if (!this.listeners.contains(rrl))
            this.listeners.add(rrl);
    }

    public void removeRandomizerRulesListner(RandomizerRulesListener rrl)
    {
        if (this.listeners.contains(rrl))
            this.listeners.remove(rrl);
    }
    
    public JSONArray getAllRules()
    {
        return randomRules;
    }
    
    public void setAllRules(JSONArray rules)
    {
        this.randomRules = new JSONArray();
        for (int i = 0; i < rules.length(); i++)
        {
            JSONObject randRule = rules.getJSONObject(i);
            long changeDelay = randRule.optLong("changeDelay",5000);
            if (changeDelay < 100)
                randRule.put("changeDelay", changeDelay * 1000l);
            addRandomRule(randRule);
        }
    }
    
    public void clearAllRules()
    {
        int rrl = this.randomRules.length();
        for (int i = 0; i <= rrl; i++)
        {
            this.removeRandomRule(0);
        }
    }

    public static JSONObject defaultRuleJSONObject()
    {
        JSONObject newRule = new JSONObject();
        newRule.put("channel", 1);
        newRule.put("cc", 1);
        newRule.put("min", 0);
        newRule.put("max", 127);
        newRule.put("smooth", true);
        newRule.put("changeDelay", 5000);
        newRule.put("enabled", true);
        return newRule;
    }
    
    public void addRandomRule(int channel, int cc, int min, int max)
    {
        JSONObject newRule = defaultRuleJSONObject();
        newRule.put("channel", channel);
        newRule.put("cc", cc);
        newRule.put("min", min);
        newRule.put("max", max);
        newRule.put("enabled", true);
        this.addRandomRule(newRule);
    }
    
    public int ruleIndex(JSONObject rule)
    {
        for (int i = 0; i < this.randomRules.length(); i++)
        {
            JSONObject randRule = this.randomRules.getJSONObject(i);
            if (randRule.optInt("channel", -1) == rule.optInt("channel", -1) && randRule.optInt("cc", -1) == rule.optInt("cc", -1))
            {
                return i;
            }
        }
        return -1;
    }

    public void toggleRandomRule(int idx)
    {
        final JSONObject rule = this.randomRules.optJSONObject(idx);
        if (rule != null)
        {
            rule.put("enabled", !rule.optBoolean("enabled", true));
            this.randomRules.put(idx, rule);
        }
    }
    
    public void addRandomRule(JSONObject newRule)
    {
        int ri = ruleIndex(newRule);
        if (ri == -1)
        {
            this.randomRules.put(newRule);
        } else {
            this.randomRules.put(ri, newRule);
        }
        final int rif = ruleIndex(newRule);
        this.listeners.forEach((listener) -> {
            listener.ruleAdded(rif, newRule);
        });
    }

    public void removeRandomRule(JSONObject rule)
    {
        int ri = ruleIndex(rule);
        if (ri >= 0)
        {
            this.removeRandomRule(ri);
        }
    }
    public void removeRandomRule(int ri)
    {
        final JSONObject rule = this.randomRules.optJSONObject(ri);
        if (rule != null)
        {
            this.randomRules.remove(ri);
            this.listeners.forEach((listener) -> {
                listener.ruleRemoved(ri, rule);
            });
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
                long currentMillis = System.currentTimeMillis();
                long timeStamp = this.getMicrosecondPosition();
                for (int i = 0; i < this.randomRules.length(); i++)
                {
                    JSONObject randRule = this.randomRules.getJSONObject(i);
                    boolean enabled = randRule.optBoolean("enabled", true);
                    if (enabled)
                    {
                        int min = randRule.optInt("min", 0);
                        int max = randRule.optInt("max", 0);
                        int channel = randRule.optInt("channel", 1);
                        int cc = randRule.optInt("cc", 0);
                        boolean smooth = randRule.optBoolean("smooth", true);
                        long changeDelayMillis = randRule.optLong("changeDelay", 5000);
                        int target = randRule.optInt("_target", 0);
                        int value = randRule.optInt("_value", 0);
                        int targetSpeed = randRule.optInt("_targetSpeed", 1);
                        
                        long lastRandomMillis = randRule.optLong("_lastRandomMillis", 0l);
                        long lastChangeMillis = randRule.optLong("_lastChangeMillis", 0l);
                        long elapsed = (currentMillis - lastRandomMillis);
                        long lastChangeElapsed = currentMillis - lastChangeMillis;
                        
                        int data2 = value;
                        if (elapsed >= changeDelayMillis || lastRandomMillis == 0l)
                        {
                            target = getRandomNumberInRange(min, max);
                            randRule.put("_target", target);
                            randRule.put("_lastRandomMillis", System.currentTimeMillis());
                            int diff = Math.abs(value - target);
                            if (diff == 0) diff = 1;
                            randRule.put("_targetSpeed", (changeDelayMillis / diff)); 
                        } else if (value != target) {
                            boolean valueChanged = false;
                            if (smooth)
                            {
                                if (lastChangeElapsed > targetSpeed)
                                {
                                    if (target > value)
                                    {
                                        data2++;
                                    } else if (target < value) {
                                        data2--;
                                    }
                                    valueChanged = true;
                                    //System.err.println("Elapsed: " + String.valueOf(elapsed) + " Target: " + String.valueOf(target) + " Value: " + String.valueOf(value));
                                }
                            } else {
                                data2 = target;
                                valueChanged = true;
                            }
                            if (valueChanged)
                            {
                                randRule.put("_value", data2);
                                randRule.put("_lastChangeMillis", currentMillis);
                                final ShortMessage sm = new ShortMessage(ShortMessage.CONTROL_CHANGE, (channel-1), cc, data2);
                                this.lastRxAt = System.currentTimeMillis();
                                MidiRandomizerPort.this.receivers.forEach((r) -> {
                                    r.send(sm, timeStamp);
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            try
            {
                Thread.sleep(10);
            } catch (Exception sleep_e) {
                
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
        try
        {
            if (this.isOpened())
            {
                MidiPortManager.firePortClosed(this);
                this.opened = false;
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

    public long getLastRxAt()
    {
        return this.lastRxAt;
    }

    public long getLastTxAt()
    {
        return 0;
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

    public String getCCName(int channel, int cc)
    {
        return null;
    }
}
