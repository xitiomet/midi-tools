package org.openstatic.midi;

import javax.sound.midi.*;

import org.json.*;
import java.util.Vector;
import java.util.Enumeration;

public class MidiControl
{
    private int channel;
    private int cc;
    private int note;
    private int value;
    private int settled_value;
    private String nickname;
    private boolean settled;
    private long lastChangeAt;
    private MidiPort lastReceivedFrom;
    private Vector<MidiControlListener> listeners = new Vector<MidiControlListener>();

    public MidiControl(JSONObject jo)
    {
        this.channel = jo.optInt("channel", 0);
        this.cc = jo.optInt("cc", -1);
        this.note = jo.optInt("note", -1);
        this.nickname = jo.optString("nickname", nameControl(channel, cc));
        this.settled = true;
        this.lastChangeAt = System.currentTimeMillis();
        this.value = jo.optInt("value", 0);
        this.settled_value = jo.optInt("settledValue", 0);
    }

    public MidiControl(int channel, int cc)
    {
        this.channel = channel;
        this.cc = cc;
        this.note = -1;
        this.value = 0;
        this.nickname = nameControl(channel, cc);
        this.settled = true;
        this.lastChangeAt = System.currentTimeMillis();
    }

    public MidiControl(int channel, int note, boolean isNote)
    {
        this.channel = channel;
        this.cc = -1;
        this.note = note;
        this.value = 0;
        this.nickname = "Note - " + MidiPortManager.nameNote(note);
        this.settled = true;
        this.lastChangeAt = System.currentTimeMillis();
    }
    
    private static String nameControl(int channel, int cc)
    {
        MidiPort sourcePort = MidiPortManager.findTransmittingPortByChannelCC(channel, cc);
        if (sourcePort != null)
        {
            String name = sourcePort.getCCName(channel, cc);
            System.err.println("name Control found Source port for " + String.valueOf(channel) + " "  + String.valueOf(cc));
            if (name != null)
            {
                System.err.println("Name Found " + name);
                return name;
            }
        }
        return MidiPortManager.nameCC(cc);
    }

    public String getNoteName()
    {
        return MidiPortManager.nameNote(this.note);
    }

    public int getNoteNumber()
    {
        return this.note;
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
        if ( ((msg.getChannel()+1) == this.channel || this.channel == 0))
        {
            if (msg.getCommand() == ShortMessage.CONTROL_CHANGE)
            {
                if (msg.getData1() == this.cc)
                {
                    return true;
                }
            } else if (msg.getCommand() == ShortMessage.NOTE_ON) {
                int incomingNote = msg.getData1() % 12;
                if (incomingNote == this.note)
                {
                    return true;
                }
            } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
                int incomingNote = msg.getData1() % 12;
                if (incomingNote == this.note)
                {
                    return true;
                }
            }
        }
        return false;
    }

    // Last port to send a message to this control
    public MidiPort getLastReceivedFromMidiPort()
    {
        return this.lastReceivedFrom;
    }

    public void processMessage(ShortMessage msg)
    {
        if (this.cc >= 0)
        {
            manualAdjust(msg.getData2());
            this.lastReceivedFrom = MidiPortManager.findTransmittingPortByChannelCC(this.getChannel(), this.getControlNumber());
        } else if (this.note >= 0) {
            int incomingNote = msg.getData1() % 12;
            if (incomingNote == this.note)
            {
                if (msg.getCommand() == ShortMessage.NOTE_ON) {
                    manualAdjust(msg.getData2());
                } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
                    manualAdjust(0);
                }
            }
        }
    }
    
    public synchronized void manualAdjust(final int new_value)
    {
        if (new_value <= 127 && new_value >= 0)
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
                    MidiPortManager.addTask(() -> {
                        mcl.controlValueChanged(MidiControl.this, old_value, new_value);
                    });
                }
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

    public long getLastChangeAt()
    {
        return this.lastChangeAt;
    }
    
    public void settle()
    {
        if ((System.currentTimeMillis() - this.lastChangeAt) > 250 && this.settled == false)
        {
            this.settled = true;
            final int old_value = this.settled_value;
            this.settled_value = this.value;
            final int final_value = this.value;
            //System.err.println(MidiControl.this.toString() + " settled at " + String.valueOf(final_value));
            for (Enumeration<MidiControlListener> mcle = ((Vector<MidiControlListener>) MidiControl.this.listeners.clone()).elements(); mcle.hasMoreElements();)
            {
                final MidiControlListener mcl = mcle.nextElement();
                MidiPortManager.addTask(() -> {
                    mcl.controlValueSettled(MidiControl.this, old_value, final_value);
                });
            }
        }
    }
    
    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        if (this.cc >= 0)
            jo.put("cc", this.cc);
        if (this.note >= 0)
           jo.put("note", this.note);
        jo.put("channel", this.channel);
        jo.put("nickname", this.nickname);
        jo.put("value", this.value);
        jo.put("settledValue", this.settled_value);
        jo.put("lastChangeAt", this.lastChangeAt);
        return jo;
    }

    public JSONObject toSavableJSONObject()
    {
        JSONObject jo = new JSONObject();
        if (this.cc >= 0)
            jo.put("cc", this.cc);
        if (this.note >= 0)
            jo.put("note", this.note);
        jo.put("channel", this.channel);
        jo.put("nickname", this.nickname);
        //jo.put("value", this.value);
        //jo.put("settledValue", this.settled_value);
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
            if (this.cc >= 0)
                return "Control " + String.valueOf(cc) + " (CH-" + String.valueOf(this.channel) + ")";
            else if (this.note >= 0)
                return MidiPortManager.nameNote(this.note) + " (CH-" + String.valueOf(this.channel) + ")";
            else
                return "";
        }
    }
}
