package org.openstatic.midi;

import java.util.Random;
import javax.sound.midi.*;
import org.json.*;

public class MidiPortMapping
{
    private String mappingId;
    private String nickname;
    private MidiPort source;
    private MidiPort destination;
    private String sourceName;
    private String destinationName;
    private Receiver receiver;
    private int messageCounter;
    private boolean opened;
    private long lastActiveAt;
    
    private static synchronized String generateBigAlphaKey(int key_length)
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
        //System.err.println("Generated Rule ID: " + randKey);
        return randKey;
    }
    
    public MidiPortMapping(JSONObject jo)
    {
        this.mappingId = jo.optString("mappingId", generateBigAlphaKey(24));
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
                MidiPortMapping.this.lastActiveAt = System.currentTimeMillis();
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
        this.mappingId = generateBigAlphaKey(24);
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
                    MidiPortMapping.this.lastActiveAt = System.currentTimeMillis();
                }
            }
            
            public void close()
            {
                
            }
        };
        this.open();
    }
    
    public String getMappingId()
    {
        return this.mappingId;
    }
    
    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }
    
    public long getLastActiveAt()
    {
        return this.lastActiveAt;
    }

    public void close()
    {
        if (this.isOpened())
        {
            try
            {
                this.opened = false;
                source.removeReceiver(this.receiver);
                MidiPortManager.fireMappingClosed(this);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
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
            MidiPortManager.fireMappingOpened(this);
        }
    }

    public void setOpen(boolean v)
    {
        if (v)
        {
            if (!this.isOpened())
                this.open();
        } else {
            if (this.isOpened())
                this.close();
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
            return this.sourceName + " to " + this.destinationName;
        else
            return this.nickname;
    }

    public JSONObject toSavableJSONObject()
    {
        return this.toJSONObject();
    }
    
    public JSONObject toJSONObject()
    {
        JSONObject jo = new JSONObject();
        jo.put("mappingId", this.mappingId);
        if (this.nickname != null)
            jo.put("nickname", this.nickname);
        else
            jo.put("nickname", this.sourceName + " to " + this.destinationName);
        jo.put("source", this.sourceName);
        jo.put("destination", this.destinationName);
        jo.put("opened", this.opened);
        jo.put("messageCounter", this.messageCounter);
        return jo;
    }
}
