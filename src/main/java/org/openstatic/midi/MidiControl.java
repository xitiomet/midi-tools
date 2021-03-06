package org.openstatic.midi;

import javax.sound.midi.*;
import org.json.*;
import java.util.Vector;
import java.util.Enumeration;

public class MidiControl
{
    private int channel;
    private int cc;
    private int value;
    private int settled_value;
    private String nickname;
    private boolean settled;
    private long lastChangeAt;
    private Vector<MidiControlListener> listeners = new Vector<MidiControlListener>();

    public MidiControl(JSONObject jo)
    {
        this.channel = jo.optInt("channel", 0);
        this.cc = jo.optInt("cc", 0);
        this.nickname = jo.optString("nickname", nameControl(cc));
        this.settled = true;
        this.lastChangeAt = System.currentTimeMillis();
        this.value = jo.optInt("value", 0);
        this.settled_value = jo.optInt("settledValue", 0);
    }

    public MidiControl(int channel, int cc)
    {
        this.channel = channel;
        this.cc = cc;
        this.value = 0;
        this.nickname = nameControl(cc);
        this.settled = true;
        this.lastChangeAt = System.currentTimeMillis();
    }
    
    private static String nameControl(int cc)
    {
        switch(cc)
        {
            case 0:
                return "Bank Select";
            case 1:
                return "Mod Wheel";
            case 2:
                return "Breath Controller";
            case 4:
                return "Foot Controller";
            case 5:
                return "Portamento Time";
            case 7:
                return "Volume";
            case 8:
                return "Balance";
            case 10:
                return "Pan";
            case 11:
                return "Expression";
            case 12:
                return "Effect Controller 1";
            case 13:
                return "Effect Controller 2";
            case 64:
                return "Sustain Pedal";
            case 65:
                return "Portamento Switch";
            case 66:
                return "Sostenuto Switch";
            case 91:
                return "Effect 1 Depth";
            case 92:
                return "Effect 2 Depth";
            case 93:
                return "Effect 3 Depth";
            case 94:
                return "Effect 4 Depth";
            case 95:
                return "Effect 5 Depth";
            default:
                return "Control " + String.valueOf(cc);
        }
    }
    
    public void addMidiControlListener(MidiControlListener mcl)
    {
        if (!listeners.contains(mcl))
        {
            //System.err.println ("Added MidiControlListener - " + mcl.toString());
            listeners.add(mcl);
        }
    }

    public void removeMidiControlListener(MidiControlListener mcl)
    {
        if (listeners.contains(mcl))
        {
            //System.err.println ("Removed MidiControlListener - " + mcl.toString());
            listeners.remove(mcl);
        }
    }
    
    public void removeAllListeners()
    {
        this.listeners.clear();
    }
    
    public boolean messageMatches(ShortMessage msg)
    {
        if ( ((msg.getChannel()+1) == this.channel || this.channel == 0) && msg.getCommand() == ShortMessage.CONTROL_CHANGE)
        {
            if (msg.getData1() == this.cc || this.cc == 0)
            {
                return true;
            }
        }
        return false;
    }

    public void processMessage(ShortMessage msg)
    {
        manualAdjust(msg.getData2());
    }
    
    public void manualAdjust(final int new_value)
    {
        final int old_value = this.value;
        if (old_value != new_value)
        {
            this.value = new_value;
            this.lastChangeAt = System.currentTimeMillis();
            this.settled = false;
            for (Enumeration<MidiControlListener> mcle = ((Vector<MidiControlListener>) MidiControl.this.listeners.clone()).elements(); mcle.hasMoreElements();)
            {
                final MidiControlListener mcl = mcle.nextElement();
                (new Thread(() -> {
                    mcl.controlValueChanged(MidiControl.this, old_value, new_value);
                })).start();
            }
        }
    }

    public int getChannel()
    {
        return this.channel;
    }

    public int getControlNumber()
    {
        return this.cc;
    }
    
    public int getValue()
    {
        return this.value;
    }
    
    public String getNickname()
    {
        return this.nickname;
    }

    public void setValue(int value)
    {
        this.value = value;
    }
    
    public boolean isSettled()
    {
        return this.settled;
    }
    
    public void settle()
    {
        if ((System.currentTimeMillis() - this.lastChangeAt) > 500)
        {
            this.settled = true;
            final int old_value = this.settled_value;
            this.settled_value = this.value;
            final int final_value = this.value;
            //System.err.println(MidiControl.this.toString() + " settled at " + String.valueOf(final_value));
            for (Enumeration<MidiControlListener> mcle = ((Vector<MidiControlListener>) MidiControl.this.listeners.clone()).elements(); mcle.hasMoreElements();)
            {
                final MidiControlListener mcl = mcle.nextElement();
                (new Thread(() -> {
                    mcl.controlValueSettled(MidiControl.this, old_value, final_value);
                })).start();
            }
        }
    }
    
    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        jo.put("cc", this.cc);
        jo.put("channel", this.channel);
        jo.put("nickname", this.nickname);
        jo.put("value", this.value);
        jo.put("settledValue", this.settled_value);
        jo.put("lastChangeAt", this.lastChangeAt);
        return jo;
    }
    
    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }
    
    public String toString()
    {
        if (this.nickname != null)
        {
            return this.nickname;
        } else {
            return "Control " + String.valueOf(cc) + " (CH-" + String.valueOf(this.channel) + ")";
        }
    }
}
