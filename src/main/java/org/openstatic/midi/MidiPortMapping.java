package org.openstatic.midi;

import javax.sound.midi.*;
import org.json.*;

public class MidiPortMapping
{
    private String nickname;
    private MidiPort source;
    private MidiPort destination;
    private String sourceName;
    private String destinationName;
    private Receiver receiver;
    private int messageCounter;
    private boolean opened;
    
    public MidiPortMapping(JSONObject jo)
    {
        this.nickname = jo.optString("nickname",null);
        this.sourceName = jo.optString("source", null);
        this.destinationName = jo.optString("destination", null);
        this.source = MidiPortManager.findTransmittingPortByName(this.sourceName);
        this.destination = MidiPortManager.findReceivingPortByName(this.destinationName);
        this.receiver = new Receiver()
        {
            public void send(MidiMessage message, long timeStamp)
            {
                MidiPortMapping.this.destination.send(message, timeStamp);
                MidiPortMapping.this.messageCounter++;
            }
            
            public void close()
            {
                
            }
        };
        if (jo.optBoolean("opened", false))
            this.open();
    }
    
    public MidiPortMapping(MidiPort source, MidiPort destination)
    {
        this.nickname = null;
        this.source = source;
        this.destination = destination;
        this.sourceName = this.source.getName();
        this.destinationName = this.destination.getName();
        this.receiver = new Receiver()
        {
            public void send(MidiMessage message, long timeStamp)
            {
                if (MidiPortMapping.this.destination.isOpened())
                {
                    MidiPortMapping.this.destination.send(message, timeStamp);
                    MidiPortMapping.this.messageCounter++;
                }
            }
            
            public void close()
            {
                
            }
        };
        this.open();
    }
    
    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }
    
    public void close()
    {
        this.opened = false;
        source.removeReceiver(this.receiver);
    }
    
    public void open()
    {
        if (this.source == null)
            this.source = MidiPortManager.findTransmittingPortByName(this.sourceName);
        if (this.destination == null)
            this.destination = MidiPortManager.findReceivingPortByName(this.destinationName);
        if (this.source != null && this.destination != null)
        {
            if (!this.source.isOpened())
                this.source.open();
            if (!this.destination.isOpened())
                this.destination.open();
            this.opened = true;
            source.addReceiver(this.receiver);
        }
    }
    
    public void toggle()
    {
        if (this.isOpened())
            this.close();
        else
            this.open();
    }
    
    public boolean isOpened()
    {
        return this.opened;
    }
    
    public int getMessageCount()
    {
        return this.messageCounter;
    }
    
    public String toString()
    {
        if (this.nickname == null)
            return this.sourceName + " >> " + this.destinationName;
        else
            return this.nickname + ": " + this.sourceName + " >> " + this.destinationName;
    }
    
    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        if (this.nickname != null)
            jo.put("nickname", this.nickname);
        jo.put("source", this.sourceName);
        jo.put("destination", this.destinationName);
        jo.put("opened", this.opened);
        jo.put("messageCounter", this.messageCounter);
        return jo;
    }
}
