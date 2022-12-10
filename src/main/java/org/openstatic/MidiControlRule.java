package org.openstatic;

import org.openstatic.midi.*;

import org.json.*;
import javax.sound.midi.*;
import java.util.concurrent.TimeUnit;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.*;

public class MidiControlRule implements MidiControlListener
{
    private String nickname;
    private int action_type;
    private String ruleId;
    private String ruleGroup;
    private String canvasName;
    private String action_value;
    private int event_mode;
    private MidiControl control;
    private boolean enabled;
    private SoundFile sound;
    private long lastTriggered;
    private long lastFailed;
    private boolean valueInverted;
    private boolean valueSettled;
    
    public static final int ACTION_URL = 0;
    public static final int ACTION_PROC = 1;
    public static final int ACTION_SOUND = 2;
    public static final int ACTION_TRANSMIT = 3;
    public static final int ACTION_PLUGIN = 4;
    public static final int LOGGER_A_MESSAGE = 5;
    public static final int LOGGER_B_MESSAGE = 6;
    public static final int ACTION_ENABLE_RULE_GROUP = 7;
    public static final int ACTION_DISABLE_RULE_GROUP = 8;
    public static final int ACTION_TOGGLE_RULE_GROUP = 9;
    public static final int ACTION_EFFECT_IMAGE = 10;
    public static final int ACTION_ENABLE_MAPPING = 11;
    public static final int ACTION_DISABLE_MAPPING = 12;
    public static final int ACTION_TOGGLE_MAPPING = 13;
    
    public static final int EVENT_CHANGE = 0;
    public static final int EVENT_INCREASE = 1;
    public static final int EVENT_DECREASE = 2;
    public static final int EVENT_HIGH = 3;
    public static final int EVENT_LOW = 4;
    public static final int EVENT_ENTERED_HIGH = 5;
    public static final int EVENT_ENTERED_LOW = 6;
    public static final int EVENT_ENTERED_HIGH_LOW = 7;
    public static final int EVENT_BOTTOM_THIRD = 8;
    public static final int EVENT_MIDDLE_THIRD = 9;
    public static final int EVENT_TOP_THIRD = 10;
    public static final int EVENT_ENTERED_BOTTOM_THIRD = 11;
    public static final int EVENT_ENTERED_MIDDLE_THIRD = 12;
    public static final int EVENT_ENTERED_TOP_THIRD = 13;
    
    
    public MidiControlRule(JSONObject jo)
    {
        System.err.println("READING RULE: " + jo.toString());
        this.ruleId = jo.optString("ruleId", MidiPortManager.generateBigAlphaKey(24));
        this.ruleGroup = jo.optString("ruleGroup", "all");
        this.event_mode = jo.optInt("eventMode", 0);
        if (this.event_mode > 13) this.event_mode = 0;
        this.action_type = jo.optInt("actionType", 0);
        if (this.action_type > 14) this.action_type = 5;
        this.action_value = jo.optString("actionValue", null);
        this.nickname = jo.optString("nickname", null);
        this.canvasName = jo.optString("canvas", "(ALL)");
        this.enabled = jo.optBoolean("enabled", true);
        this.lastTriggered = jo.optLong("lastTriggered", 0l);
        this.lastFailed = jo.optLong("lastFailed", 0l);
        this.valueInverted = jo.optBoolean("valueInverted", false);
        this.valueSettled = jo.optBoolean("valueSettled", false);

        if (jo.has("control"))
        {
            JSONObject ctrl = jo.getJSONObject("control");
            int ccNum = ctrl.optInt("cc", -1);
            MidiControl mc = null;
            if (ccNum >= 0)
            {
                mc = MidiTools.getMidiControlByChannelCC(ctrl.optInt("channel", 0), ccNum);
                if (mc == null)
                {
                    mc = MidiTools.createMidiControlFromJSON(ctrl);
                }
            } else {
                mc = MidiTools.getMidiControlByChannelNote(ctrl.optInt("channel", 0), ctrl.optInt("note", -1));
                if (mc == null)
                {
                    mc = MidiTools.createMidiControlFromJSON(ctrl);
                }
            }
            this.control = mc;
            this.updateRule();
        } else {
            // will fire with update rule, only do it when no control
            this.actionValueChanged();
        }
    }
    
    public MidiControlRule(MidiControl control, int event_mode, int action_type, String action_value)
    {
        this.ruleId = MidiPortManager.generateBigAlphaKey(24);
        this.ruleGroup = "all";
        this.control = control;
        this.event_mode = event_mode;
        this.action_type = action_type;
        this.action_value = action_value;
        this.canvasName = "(ALL)";
        this.enabled = true;
        this.valueInverted = false;
        this.valueSettled = false;
        this.actionValueChanged();
    }

    public void controlValueAction(MidiControl control, int old_value, int new_value)
    {
        if (this.event_mode == MidiControlRule.EVENT_CHANGE)
        {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_ENTERED_HIGH && new_value >= 64 && old_value <= 63) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_ENTERED_LOW && new_value <= 63 && old_value >= 64) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_INCREASE && new_value > old_value) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_DECREASE && new_value < old_value) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_ENTERED_HIGH_LOW && ((new_value >= 64 && old_value <= 63) || (new_value <= 63 && old_value >= 64))) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_ENTERED_BOTTOM_THIRD && (new_value <= 42 && old_value >= 43)) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_ENTERED_MIDDLE_THIRD && new_value >= 43 && new_value <= 85 && (old_value <= 42 || old_value >= 86)) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_ENTERED_TOP_THIRD && new_value >= 86 && old_value <= 85) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_BOTTOM_THIRD && new_value <= 42) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_MIDDLE_THIRD && new_value >= 43 && new_value <= 85) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_TOP_THIRD && new_value >= 86) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_HIGH && new_value >= 64) {
            executeAction(control, old_value, new_value);
        } else if (this.event_mode == MidiControlRule.EVENT_LOW && new_value <= 63) {
            executeAction(control, old_value, new_value);
        }
    }
    
    public void controlValueChanged(MidiControl control, int old_value, int new_value)
    {
        if (!this.valueSettled)
        {
            if (this.valueInverted)
            {
                new_value = (127-new_value);
                old_value = (127-old_value);
            }
            controlValueAction(control, old_value, new_value);
        }
    }
    
    public void controlValueSettled(MidiControl control, int old_value, int new_value)
    {
        if (this.valueSettled)
        {
            if (this.valueInverted)
            {
                new_value = (127-new_value);
                old_value = (127-old_value);
            }
            controlValueAction(control, old_value, new_value);
        }
    }
    
    public boolean isValueInverted()
    {
        return this.valueInverted;
    }

    public void setValueInverted(boolean v)
    {
        this.valueInverted = v;
    }

    public void setValueSettled(boolean v)
    {
        this.valueSettled = v;
    }

    public boolean shouldValueSettle()
    {
        return this.valueSettled;
    }

    public static String mapReplace(String source, int value)
    {
        Pattern p = Pattern.compile("\\{\\{value.map\\((\\d+)\\,(\\d+)\\)\\}\\}");
        Matcher m = p.matcher(source);
        StringBuffer s = new StringBuffer();
        while (m.find())
        {
            int out_min = Integer.valueOf(m.group(1)).intValue();
            int out_max = Integer.valueOf(m.group(2)).intValue();
            int new_value = value * (out_max - out_min) / 127 + out_min;
            m.appendReplacement(s, String.valueOf(new_value));
        }
        m.appendTail(s);
        return s.toString();
    }

    private static String[] mergeArrays(String[] first, String[] second) {
        List<String> both = new ArrayList<String>(first.length + second.length);
        Collections.addAll(both, first);
        Collections.addAll(both, second);
        return both.toArray(new String[both.size()]);
    }

    private static String[] prependArray(String first, String[] second) {
        List<String> both = new ArrayList<String>(1 + second.length);
        both.add(first);
        Collections.addAll(both, second);
        return both.toArray(new String[both.size()]);
    }
    
    public void executeAction(MidiControl control, int old_value, int new_value)
    {
        if (this.enabled)
        {
            //MidiTools.instance.midi_logger_a.println("Rule Triggered - " + this.toShortString());
            boolean success = false;
            //System.err.println(this.toString() + " Recieved From " + control.toString());
            try
            {
                if (this.action_value != null)
                {
                    final String avparsed = mapReplace(this.action_value, new_value)
                                    .replaceAll("\\{\\{value\\}\\}", String.valueOf(new_value))
                                    .replaceAll("\\{\\{value.inv\\}\\}", String.valueOf((127-new_value)))
                                    .replaceAll("\\{\\{value.old\\}\\}", String.valueOf(old_value))
                                    .replaceAll("\\{\\{value.old.inv\\}\\}", String.valueOf((127-old_value)))
                                    .replaceAll("\\{\\{value.change\\}\\}", String.valueOf((new_value-old_value)))
                                    .replaceAll("\\{\\{cc\\}\\}", String.valueOf(control.getControlNumber()))
                                    .replaceAll("\\{\\{note\\}\\}", String.valueOf(control.getNoteNumber()))
                                    .replaceAll("\\{\\{note.name\\}\\}", control.getNoteName())
                                    .replaceAll("\\{\\{channel\\}\\}", String.valueOf(control.getChannel()));
                    if (this.getActionType() == MidiControlRule.ACTION_URL)
                    {
                        PendingURLFetch puf = new PendingURLFetch(avparsed);
                        puf.run();
                        //MidiTools.instance.midi_logger_a.println(this.toShortString() + " returned: " + puf.getResponse());
                        success = true;
                    } else if (this.getActionType() == MidiControlRule.ACTION_PROC) {
                        try
                        {
                            String[] avparsed2 = avparsed.split(",");
                            if (avparsed2[0].endsWith(".cmd") || avparsed2[0].endsWith(".bat"))
                            {
                                avparsed2 = prependArray(System.getenv("windir") + "\\system32\\cmd.exe", avparsed2);
                            } else if (avparsed2[0].endsWith(".sh") || avparsed2[0].endsWith(".bash")) {
                                avparsed2 = prependArray("/bin/bash", avparsed2);
                            }
                            ProcessBuilder pb = new ProcessBuilder(avparsed2);
                            pb.directory(MidiTools.getAssetFolder());
                            Process process = pb.start();
                            if (!process.waitFor(10, TimeUnit.SECONDS))
                            {
                                process.destroyForcibly();
                            }
                            success = true;
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                    } else if (this.getActionType() == MidiControlRule.ACTION_SOUND) {
                        if (!"(NONE)".equals(this.canvasName) && canvasName != null)
                        {
                            JSONObject canvasEvent = new JSONObject();
                            canvasEvent.put("sound", this.action_value);
                            canvasEvent.put("canvas", this.canvasName);
                            canvasEvent.put("volume", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 1f));
                            success = MidiTools.instance.apiServer.broadcastCanvasJSONObject(canvasEvent);
                        } else {
                            if (MidiControlRule.this.sound != null)
                            {
                                float volume = mapFloat(Float.valueOf(new_value).floatValue(), 0f, 127f, -40f, 0f);
                                MidiControlRule.this.sound.setVolume(volume);
                                MidiControlRule.this.sound.play();
                                success = true;
                            }
                        }
                    } else if (this.getActionType() == MidiControlRule.ACTION_EFFECT_IMAGE) {
                        if (!"(NONE)".equals(this.canvasName) && canvasName != null)
                        {
                            StringTokenizer st = new StringTokenizer(avparsed, ",");
                            String filename = st.nextToken();
                            String mode = "opacity";
                            if (st.hasMoreTokens())
                                mode = st.nextToken();
                            JSONObject canvasEvent = new JSONObject();
                            canvasEvent.put("image", filename);
                            if (mode.contains("solo"))
                                canvasEvent.put("solo", true);
                            if (mode.contains("fill-x"))
                                canvasEvent.put("fill", "x");
                            else if (mode.contains("fill-y"))
                                canvasEvent.put("fill", "y");
                            if (mode.contains("none"))
                                canvasEvent.put("none", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 1f));
                            if (mode.contains("opacity"))
                                canvasEvent.put("opacity", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 1f));
                            if (mode.contains("curtain"))
                                canvasEvent.put("curtain", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 1f));
                            if (mode.contains("riser"))
                                canvasEvent.put("riser", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 1f));
                            if (mode.contains("scale"))
                                canvasEvent.put("scale", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 1f));
                            if (mode.contains("rotate"))
                                canvasEvent.put("rotate", mapFloat(Float.valueOf(new_value), 0f, 127f, 0f, 360f));
                            canvasEvent.put("canvas", this.canvasName);
                            success = MidiTools.instance.apiServer.broadcastCanvasJSONObject(canvasEvent);
                        }
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
                                        success = true;
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
                        success = true;
                    } else if (this.getActionType() == MidiControlRule.ACTION_DISABLE_RULE_GROUP) {
                        MidiTools.setRuleGroupEnabled(avparsed, false);
                        success = true;
                    } else if (this.getActionType() == MidiControlRule.ACTION_TOGGLE_RULE_GROUP) {
                        if (new_value >= 64)
                        {
                            MidiTools.setRuleGroupEnabled(avparsed, true);
                        } else {
                            MidiTools.setRuleGroupEnabled(avparsed, false);
                        }
                        success = true;
                    } else if (this.getActionType() == MidiControlRule.LOGGER_A_MESSAGE) {
                        MidiTools.instance.midi_logger_a.println(avparsed);
                        success = true;
                    } else if (this.getActionType() == MidiControlRule.LOGGER_B_MESSAGE) {
                        MidiTools.instance.midi_logger_b.println(avparsed);
                        success = true;
                    } else if (this.getActionType() == MidiControlRule.ACTION_PLUGIN) {
                        if (!this.action_value.equals("") && this.action_value != null)
                        {
                            String[] avparsed2 = this.action_value.split(",");
                            if (avparsed2.length > 1)
                                success = MidiTools.instance.plugins.get(avparsed2[0]).onRule(this, avparsed2[1], old_value, new_value);
                            else
                                success = MidiTools.instance.plugins.get(avparsed2[0]).onRule(this, null, old_value, new_value);
                        }
                    } else if (this.getActionType() == MidiControlRule.ACTION_ENABLE_MAPPING) {
                        MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(avparsed);
                        if (mapping != null)
                        {
                            mapping.setOpen(true);
                            success = mapping.isOpened();
                        }
                    } else if (this.getActionType() == MidiControlRule.ACTION_DISABLE_MAPPING) {
                        MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(avparsed);
                        if (mapping != null)
                        {
                            mapping.setOpen(false);
                            success = !mapping.isOpened();
                        }
                    } else if (this.getActionType() == MidiControlRule.ACTION_TOGGLE_MAPPING) {
                        MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(avparsed);
                        if (mapping != null)
                        {
                            boolean changeTo = (new_value >= 64);
                            mapping.setOpen(changeTo);
                            success = (mapping.isOpened() == changeTo);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            if (success)
                this.lastTriggered = System.currentTimeMillis();
            else
                this.lastFailed = System.currentTimeMillis();
            MidiTools.repaintRules();
        }
    }

    public static float mapFloat(float x, float in_min, float in_max, float out_min, float out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static int mapInt(int x, int in_min, int in_max, int out_min, int out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public MidiToolsPlugin getSelectedPlugin()
    {
        if (this.getActionType() == MidiControlRule.ACTION_PLUGIN)
        {
            String[] avparsed2 = this.action_value.split(",");
            return MidiTools.instance.plugins.get(avparsed2[0]);
        } else {
            return null;
        }
    }
    
    public boolean isEnabled()
    {
        return this.enabled;
    }
    
    public void setEnabled(boolean b)
    {
        if (b != this.enabled)
        {
            if (b)
                MidiTools.instance.midi_logger_b.println("Rule Enabled - " + this.toShortString());
            else
                MidiTools.instance.midi_logger_b.println("Rule Disabled - " + this.toShortString());
            this.enabled = b;
            MidiTools.repaintRules();
        }
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
        this.updateRule();
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
        this.actionValueChanged();
    }
    
    public int getActionType()
    {
        return this.action_type;
    }

    public void setCanvasName(String val)
    {
        this.canvasName = val;
    }

    public String getCanvasName()
    {
        return this.canvasName;
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
        } else if (n == MidiControlRule.EVENT_ENTERED_HIGH) {
            return "onEnteredHigh (64+)";
        } else if (n == MidiControlRule.EVENT_ENTERED_LOW) {
            return "onEnteredLow (63-)";
        } else if (n == MidiControlRule.EVENT_INCREASE) {
            return "onChangedIncrease";
        } else if (n == MidiControlRule.EVENT_DECREASE) {
            return "onChangedDecrease";
        } else if (n == MidiControlRule.EVENT_ENTERED_HIGH_LOW) {
            return "onEnteredHighOrLow";
        } else if (n == MidiControlRule.EVENT_BOTTOM_THIRD) {
            return "onChangedBottomThird (0-42)";
        } else if (n == MidiControlRule.EVENT_MIDDLE_THIRD) {
            return "onChangedMiddleThird (43-85)";
        } else if (n == MidiControlRule.EVENT_TOP_THIRD) {
            return "onChangedTopThird (86-127)";
        } else if (n == MidiControlRule.EVENT_ENTERED_BOTTOM_THIRD) {
            return "onEnteredBottomThird (0-42)";
        } else if (n == MidiControlRule.EVENT_ENTERED_MIDDLE_THIRD) {
            return "onEnteredMiddleThird (43-85)";
        } else if (n == MidiControlRule.EVENT_ENTERED_TOP_THIRD) {
            return "onEnteredTopThird (86-127)";
        } else if (n == MidiControlRule.EVENT_HIGH) {
            return "onChangedHigh (64+)";
        } else if (n == MidiControlRule.EVENT_LOW) {
            return "onChangedLow (63-)";
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
            return "TRANSMIT CONTROL CHANGE";
        } else if (n == MidiControlRule.ACTION_ENABLE_RULE_GROUP) {
            return "ENABLE RULE GROUP";
        } else if (n == MidiControlRule.ACTION_DISABLE_RULE_GROUP) {
            return "DISABLE RULE GROUP";
        } else if (n == MidiControlRule.ACTION_TOGGLE_RULE_GROUP) {
            return "TOGGLE RULE GROUP";
        } else if (n == MidiControlRule.LOGGER_A_MESSAGE) {
            return "LOGGER A MESSAGE";
        } else if (n == MidiControlRule.LOGGER_B_MESSAGE) {
            return "LOGGER B MESSAGE";
        } else if (n == MidiControlRule.ACTION_PLUGIN) {
            return "PLUGIN";
        } else if (n == MidiControlRule.ACTION_ENABLE_MAPPING) {
            return "MAPPING ENABLE";
        } else if (n == MidiControlRule.ACTION_DISABLE_MAPPING) {
            return "MAPPING DISABLE";
        } else if (n == MidiControlRule.ACTION_TOGGLE_MAPPING) {
            return "MAPPING TOGGLE";
        } else if (n == MidiControlRule.ACTION_EFFECT_IMAGE) {
            return "EFFECT IMAGE";
        }
        return "";
    }

    private void actionValueChanged()
    {
        if (this.getActionType() == MidiControlRule.ACTION_SOUND && this.action_value != null)
        {
            if (this.sound != null)
            {
                if (!this.sound.wasCreatedWith(this.action_value))
                {
                    this.sound.close();
                    this.sound = new SoundFile(this.action_value);
                }
            } else {
                this.sound = new SoundFile(this.action_value);
            }
        } else {
            this.sound = null;
        }
    }
    
    public void updateRule()
    {
        this.actionValueChanged();
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
        jo.put("canvas", this.canvasName);
        jo.put("enabled", this.enabled);
        jo.put("lastTriggered", this.lastTriggered);
        jo.put("valueInverted", this.valueInverted);
        jo.put("valueSettled", this.valueSettled);
        return jo;
    }

    public JSONObject toSavableJSONObject()
    {
        JSONObject jo = new JSONObject();
        jo.put("ruleId", this.ruleId);
        jo.put("ruleGroup", this.ruleGroup);
        if (this.control != null)
            jo.put("control", this.control.toSavableJSONObject());
        jo.put("actionType", this.action_type);
        jo.put("eventMode", this.event_mode);
        jo.put("actionValue", this.action_value);
        jo.put("nickname", this.nickname);
        jo.put("canvas", this.canvasName);
        jo.put("enabled", this.enabled);
        jo.put("valueInverted", this.valueInverted);
        jo.put("valueSettled", this.valueSettled);
        return jo;
    }

    public long getLastTriggered()
    {
        return this.lastTriggered;
    }

    public long getLastFailed()
    {
        return this.lastFailed;
    }

    private static String addWordBeforeSpace(String sentence, String word)
    {
        if (sentence.contains(" "))
        {
            StringTokenizer st = new StringTokenizer(sentence, " ");
            StringBuffer sb = new StringBuffer();
            sb.append(st.nextToken());
            sb.append(word);
            while(st.hasMoreTokens())
            {
                sb.append(" ");
                sb.append(st.nextToken());
            }
            return sb.toString();
        } else {
            return sentence + word;
        }
    }
    
    public String toShortString()
    {
        String controlText = "(No Control Selected)";
        if (this.control != null)
            controlText = this.control.toString();
        if (this.valueInverted)
            controlText = "Inverted " + controlText;
        String eventModeText = eventModeToString(this.getEventMode());
        if (this.shouldValueSettle())
            eventModeText = addWordBeforeSpace(eventModeText, "AndSettled");
        if (this.nickname == null)
        {
            return controlText + " [" + eventModeText + "]";
        } else {
            return this.nickname + " - " + controlText + " [" + eventModeText + "]";
        }
    }

    public String toString()
    {
        String returnText = "";
        String controlText = "(No Control Selected)";
        if (this.control != null)
            controlText = this.control.toString();
        if (this.valueInverted)
            controlText = "Inverted " + controlText;
        String actionText = actionNumberToString(this.getActionType());
        String eventModeText = eventModeToString(this.getEventMode());
        if (this.shouldValueSettle())
            eventModeText = addWordBeforeSpace(eventModeText, "AndSettled");
        String targetText = this.getActionValue();
        if (this.getActionType() == MidiControlRule.ACTION_PLUGIN)
        {
            String[] avparsed2 = this.action_value.split(",");
            if (avparsed2.length > 1)
                returnText = controlText + " [" + eventModeText + "] >> " + avparsed2[0] + " - " + avparsed2[1];
            else
                returnText = controlText + " [" + eventModeText + "] >> " + avparsed2[0];
        } else if (this.getActionType() == MidiControlRule.ACTION_DISABLE_MAPPING || this.getActionType() == MidiControlRule.ACTION_ENABLE_MAPPING || this.getActionType() == MidiControlRule.ACTION_TOGGLE_MAPPING) {
            MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(targetText);
            if (mapping != null)
                targetText = mapping.toString();
            returnText = controlText + " [" + eventModeText + "] >> " + actionText + " " + targetText;
        } else {
            returnText = controlText + " [" + eventModeText + "] >> " + actionText + " " + targetText;
        }
        if (!"(ALL)".equals(this.getCanvasName()) && !"(NONE)".equals(this.getCanvasName()) && !"".equals(this.getCanvasName()) && this.getCanvasName() != null)
        {
            returnText = returnText + " @" + this.getCanvasName();
        }

        if (this.getRuleGroup() != null)
        {
            if (!this.getRuleGroup().equals("all"))
            {
                return "#"+this.getRuleGroup() + " " + returnText;
            } else {
                return returnText;
            }
        } else {
            return returnText;
        }
    }
}
