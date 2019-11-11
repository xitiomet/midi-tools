package org.openstatic;

import org.json.*;
import javax.sound.midi.*;
import java.util.concurrent.TimeUnit;
import java.util.StringTokenizer;
import java.util.Vector;

public class MidiControlRule implements MidiControlListener
{
    private String nickname;
    private int action_type;
    private String action_value;
    private int event_mode;
    private MidiControl control;
    
    private SoundFile sound;
    
    private MidiDevice transmit_device;
    private Receiver receiver;

    public static final int ACTION_URL = 0;
    public static final int ACTION_PROC = 1;
    public static final int ACTION_SOUND = 2;
    public static final int ACTION_TRANSMIT = 3;
    
    public static final int EVENT_CHANGE = 0;
    public static final int EVENT_SETTLE = 1;
    public static final int EVENT_HIGH = 2;
    public static final int EVENT_LOW = 3;
    public static final int EVENT_INCREASE = 4;
    public static final int EVENT_DECREASE = 5;
    public static final int EVENT_SETTLED_INCREASE = 6;
    public static final int EVENT_SETTLED_DECREASE = 7;
    
    
    public MidiControlRule(JSONObject jo)
    {
        this.event_mode = jo.optInt("event_mode", 0);
        this.action_type = jo.optInt("action_type", 0);
        this.action_value = jo.optString("action_value", null);
        this.nickname = jo.optString("nickname", null);
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
        this.control = control;
        this.event_mode = event_mode;
        this.action_type = action_type;
        this.action_value = action_value;
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
                    System.err.println("Transmitting ShortMessage [" + String.valueOf(channel) + "," + String.valueOf(cc) + "," + String.valueOf(v) + "]");
                    this.receiver.send(sm, this.transmit_device.getMicrosecondPosition());
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }
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
        }
        return "";
    }
    
    public void updateRule()
    {
        if (this.getActionType() != MidiControlRule.ACTION_TRANSMIT)
        {
            // Make sure we clean up old transmit devices
            if (this.receiver != null)
            {
                this.receiver.close();
                this.receiver = null;
            }
            if (this.transmit_device != null)
            {
                this.transmit_device.close();
                this.transmit_device = null;
            }
        }
        
        if (this.getActionType() == MidiControlRule.ACTION_SOUND && this.action_value != null)
        {
            if (this.sound == null)
            {
                this.sound = new SoundFile(this.action_value);
            } else if (!this.sound.getSoundUrl().equals(this.action_value)) {
                this.sound = new SoundFile(this.action_value);
            }
        } else if (this.getActionType() == MidiControlRule.ACTION_TRANSMIT && this.action_value != null) {
            StringTokenizer st = new StringTokenizer(this.action_value, ",");
            if (st.countTokens() == 4)
            {
                String devName = st.nextToken();
                boolean rebuild_device = false;
                if (this.transmit_device != null)
                {
                    if (!devName.equals(this.transmit_device.getDeviceInfo().getName()))
                    {
                        try
                        {
                            this.receiver.close();
                            this.transmit_device.close();
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                        rebuild_device = true;
                    }
                } else {
                    rebuild_device = true;
                }
                
                if (rebuild_device)
                {
                    try
                    {
                        MidiDevice device = MidiTools.findMidiDeviceReceiverByName(devName);
                        if (device != null)
                        {
                            if (device.getMaxReceivers() != 0)
                            {
                                System.err.println("Device setup for xmit");
                                this.transmit_device = device;
                                this.receiver = device.getReceiver();
                                if (!transmit_device.isOpen())
                                    transmit_device.open();
                            } else {
                                System.err.println("Device has no receivers");
                            }
                        } else {
                            System.err.println("Couldn't find a device named " + devName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
        
                }
            }

        }
        MidiTools.removeListenerFromControls(this);
        if (this.control != null)
        {
            this.control.addMidiControlListener(this);
        }
    }

    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        if (this.control != null)
            jo.put("control", this.control.toJSONObject());
        jo.put("action_type", this.action_type);
        jo.put("event_mode", this.event_mode);
        jo.put("action_value", this.action_value);
        jo.put("nickname", this.nickname);
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
        if (this.nickname == null)
        {
            return controlText + " [" + eventModeText + "] >> " + actionText + " " + targetText;
        } else {
            return this.nickname + " " + controlText + " [" + eventModeText + "] >> " + actionText + " " + targetText;
        }
    }
}
