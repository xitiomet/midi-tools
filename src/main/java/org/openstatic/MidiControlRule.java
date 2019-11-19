package org.openstatic;

import org.openstatic.midi.*;
import org.json.*;
import javax.sound.midi.*;
import java.util.concurrent.TimeUnit;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Random;

public class MidiControlRule implements MidiControlListener
{
    private String nickname;
    private int action_type;
    private String ruleId;
    private String ruleGroup;
    private String action_value;
    private int event_mode;
    private MidiControl control;
    private boolean enabled;
    private SoundFile sound;
    private long lastTriggered;
    
    public static final int ACTION_URL = 0;
    public static final int ACTION_PROC = 1;
    public static final int ACTION_SOUND = 2;
    public static final int ACTION_TRANSMIT = 3;
    public static final int ACTION_ENABLE_RULE_GROUP = 4;
    public static final int ACTION_DISABLE_RULE_GROUP = 5;
    public static final int ACTION_TOGGLE_RULE_GROUP = 6;
    
    public static final int EVENT_CHANGE = 0;
    public static final int EVENT_SETTLE = 1;
    public static final int EVENT_HIGH = 2;
    public static final int EVENT_LOW = 3;
    public static final int EVENT_INCREASE = 4;
    public static final int EVENT_DECREASE = 5;
    public static final int EVENT_SETTLED_INCREASE = 6;
    public static final int EVENT_SETTLED_DECREASE = 7;
    
    public static synchronized String generateBigAlphaKey(int key_length)
    {
        try
        {
            // make sure we never get the same millis!
            Thread.sleep(1);
        } catch (Exception e) {}
        Random n = new Random(System.currentTimeMillis());
        String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer return_key = new StringBuffer();
        for (int i = 0; i < key_length; i++)
        {
            return_key.append(alpha.charAt(n.nextInt(alpha.length())));
        }
        String randKey = return_key.toString();
        System.err.println("Generated Rule ID: " + randKey);
        return randKey;
    }
    
    public MidiControlRule(JSONObject jo)
    {
        this.ruleId = jo.optString("ruleId", generateBigAlphaKey(24));
        this.ruleGroup = jo.optString("ruleGroup", "all");
        this.event_mode = jo.optInt("eventMode", 0);
        this.action_type = jo.optInt("actionType", 0);
        this.action_value = jo.optString("actionValue", null);
        this.nickname = jo.optString("nickname", null);
        this.enabled = jo.optBoolean("enabled", true);
        this.lastTriggered = jo.optLong("lastTriggered", 0l);
        if (jo.has("control"))
        {
            JSONObject ctrl = jo.getJSONObject("control");
            
            MidiControl mc = MidiTools.getMidiControlByChannelCC(ctrl.optInt("channel", 0), ctrl.optInt("cc", 0));
            if (mc == null)
            {
                mc = MidiTools.createMidiControlFromJSON(ctrl);
            }
            this.control = mc;
            this.updateRule();
        }
    }
    
    public MidiControlRule(MidiControl control, int event_mode, int action_type, String action_value)
    {
        this.ruleId = generateBigAlphaKey(24);
        this.ruleGroup = "all";
        this.control = control;
        this.event_mode = event_mode;
        this.action_type = action_type;
        this.action_value = action_value;
        this.enabled = true;
    }
    
    public void controlValueChanged(MidiControl control, int old_value, int new_value)
    {
        if (this.event_mode == MidiControlRule.EVENT_CHANGE)
        {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_HIGH && new_value >= 64 && old_value <= 63) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_LOW && new_value <= 63 && old_value >= 64) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_INCREASE && new_value > old_value) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_DECREASE && new_value < old_value) {
            executeAction(control, old_value, new_value);
        }
    }
    
    public void controlValueSettled(MidiControl control, int old_value, int new_value)
    {
        if (this.event_mode == MidiControlRule.EVENT_SETTLE)
        {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_SETTLED_INCREASE && new_value > old_value) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_SETTLED_DECREASE && new_value < old_value) {
            executeAction(control, old_value, new_value);

        }
    }
    
    public void executeAction(MidiControl control, int old_value, int new_value)
    {
        if (this.enabled)
        {
            this.lastTriggered = System.currentTimeMillis();
            //System.err.println(this.toString() + " Recieved From " + control.toString());
            final String avparsed = this.action_value
                              .replaceAll("\\{\\{value\\}\\}", String.valueOf(new_value))
                              .replaceAll("\\{\\{value.inv\\}\\}", String.valueOf((127-new_value)))
                              .replaceAll("\\{\\{value.old\\}\\}", String.valueOf(old_value))
                              .replaceAll("\\{\\{value.old.inv\\}\\}", String.valueOf((127-old_value)))
                              .replaceAll("\\{\\{value.change\\}\\}", String.valueOf((new_value-old_value)))
                              .replaceAll("\\{\\{cc\\}\\}", String.valueOf(control.getControlNumber()))
                              .replaceAll("\\{\\{channel\\}\\}", String.valueOf(control.getChannel()));
            if (this.getActionType() == MidiControlRule.ACTION_URL)
            {
                PendingURLFetch puf = new PendingURLFetch(avparsed);
                MidiTools.addTask(puf);
            } else if (this.getActionType() == MidiControlRule.ACTION_PROC) {
                Runnable r = () -> {
                    try
                    {
                        Process process = new ProcessBuilder(avparsed).start();
                        if (!process.waitFor(10, TimeUnit.SECONDS))
                        {
                            process.destroyForcibly();
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                };
                MidiTools.addTask(r);
            } else if (this.getActionType() == MidiControlRule.ACTION_SOUND) {
                Runnable r = () -> {
                    if (MidiControlRule.this.sound != null)
                        MidiControlRule.this.sound.play();
                };
                MidiTools.addTask(r);
            } else if (this.getActionType() == MidiControlRule.ACTION_TRANSMIT) {
                StringTokenizer st = new StringTokenizer(avparsed, ",");
                if (st.countTokens() == 4)
                {
                    try
                    {
                        String devName = st.nextToken();
                        int channel = Integer.valueOf(st.nextToken()).intValue()-1;
                        int cc = Integer.valueOf(st.nextToken()).intValue();
                        int v = Integer.valueOf(st.nextToken()).intValue();
                        ShortMessage sm = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, cc, v);
                        MidiPort output = MidiPortManager.findReceivingPortByName(devName);
                        if (output != null)
                        {
                            if (output.isOpened())
                            {
                                //System.err.println("Transmitting ShortMessage " + MidiPortManager.shortMessageToString(sm) + " to " + output.getName());
                                output.send(sm, output.getMicrosecondPosition());
                            } else {
                                //System.err.println("Output device is closed.." + output.getName());
                            }
                        } else {
                            //System.err.println("Couldn't find output device " + devName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            } else if (this.getActionType() == MidiControlRule.ACTION_ENABLE_RULE_GROUP) {
                MidiTools.setRuleGroupEnabled(avparsed, true);
            } else if (this.getActionType() == MidiControlRule.ACTION_DISABLE_RULE_GROUP) {
                MidiTools.setRuleGroupEnabled(avparsed, false);
            } else if (this.getActionType() == MidiControlRule.ACTION_TOGGLE_RULE_GROUP) {
                MidiTools.toggleRuleGroupEnabled(avparsed);
            }
        }
    }
    
    public boolean isEnabled()
    {
        return this.enabled;
    }
    
    public void setEnabled(boolean b)
    {
        this.enabled = b;
        MidiTools.repaintRules();
    }
    
    public String getRuleGroup()
    {
        return this.ruleGroup;
    }
    
    public void setRuleGroup(String group)
    {
        this.ruleGroup = group;
    }
    
    public void toggleEnabled()
    {
        this.setEnabled(!this.isEnabled());
    }
    
    public void setMidiControl(MidiControl mc)
    {
        this.control = mc;
    }
    
    public MidiControl getMidiControl()
    {
        return this.control;
    }
    
    public void setEventMode(int event_mode)
    {
        this.event_mode = event_mode;
    }
    
    public int getEventMode()
    {
        return this.event_mode;
    }
    
    public void setActionType(int action_type)
    {
        this.action_type = action_type;
    }
    
    public void setActionValue(String action_value)
    {
        this.action_value = action_value;
    }
    
    public int getActionType()
    {
        return this.action_type;
    }
    
    public String getActionValue()
    {
        return this.action_value;
    }
    
    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getNickname()
    {
        return this.nickname;
    }
    
    public static String eventModeToString(int n)
    {
        if (n == MidiControlRule.EVENT_CHANGE)
        {
            return "onChanged";
        } else if (n == MidiControlRule.EVENT_SETTLE) {
            return "onSettled";
        } else if (n == MidiControlRule.EVENT_HIGH) {
            return "onHigh (64+)";
        } else if (n == MidiControlRule.EVENT_LOW) {
            return "onLow (63-)";
        } else if (n == MidiControlRule.EVENT_INCREASE) {
            return "onIncrease";
        } else if (n == MidiControlRule.EVENT_DECREASE) {
            return "onDecrease";
        } else if (n == MidiControlRule.EVENT_SETTLED_INCREASE) {
            return "onSettledIncrease";
        } else if (n == MidiControlRule.EVENT_SETTLED_DECREASE) {
            return "onSettledDecrease";
        }
        return "";
    }
    
    public static String actionNumberToString(int n)
    {
        if (n == MidiControlRule.ACTION_URL)
        {
            return "CALL URL";
        } else if (n == MidiControlRule.ACTION_PROC) {
            return "RUN PROGRAM";
        } else if (n == MidiControlRule.ACTION_SOUND) {
            return "PLAY SOUND";
        } else if (n == MidiControlRule.ACTION_TRANSMIT) {
            return "TRANSMIT MIDI";
        } else if (n == MidiControlRule.ACTION_ENABLE_RULE_GROUP) {
            return "ENABLE RULE GROUP";
        } else if (n == MidiControlRule.ACTION_DISABLE_RULE_GROUP) {
            return "DISABLE RULE GROUP";
        } else if (n == MidiControlRule.ACTION_TOGGLE_RULE_GROUP) {
            return "TOGGLE RULE GROUP";
        }
        return "";
    }
    
    public void updateRule()
    {
        if (this.getActionType() == MidiControlRule.ACTION_SOUND && this.action_value != null)
        {
            if (this.sound == null)
            {
                this.sound = new SoundFile(this.action_value);
            } else if (!this.sound.getSoundUrl().equals(this.action_value)) {
                this.sound = new SoundFile(this.action_value);
            }
        }
        MidiTools.removeListenerFromControls(this);
        if (this.control != null)
        {
            this.control.addMidiControlListener(this);
        }
    }

    public String getRuleId()
    {
        return this.ruleId;
    }

    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        jo.put("ruleId", this.ruleId);
        jo.put("ruleGroup", this.ruleGroup);
        if (this.control != null)
            jo.put("control", this.control.toJSONObject());
        jo.put("actionType", this.action_type);
        jo.put("eventMode", this.event_mode);
        jo.put("actionValue", this.action_value);
        jo.put("nickname", this.nickname);
        jo.put("enabled", this.enabled);
        jo.put("lastTriggered", this.lastTriggered);
        return jo;
    }
    
    public String toString()
    {
        String controlText = "(No Control Selected)";
        if (this.control != null)
            controlText = this.control.toString();
        String actionText = actionNumberToString(this.getActionType());
        String eventModeText = eventModeToString(this.getEventMode());
        String targetText = this.getActionValue();
        if (this.getActionType() >= 4 && this.getActionType() <= 6)
        {
            MidiControlRule mcr = MidiTools.getMidiControlRuleById(this.getActionValue());
            if (mcr != null)
                targetText = mcr.toString();
        }
        if (this.nickname == null)
        {
            return controlText + " [" + eventModeText + "] >> " + actionText + " " + targetText;
        } else {
            return this.nickname + " " + controlText + " [" + eventModeText + "] >> " + actionText + " " + targetText;
        }
    }
}
