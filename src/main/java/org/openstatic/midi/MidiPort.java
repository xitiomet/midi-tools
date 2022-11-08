package org.openstatic.midi;

import javax.sound.midi.*;
import java.util.Collection;

public interface MidiPort extends Receiver
{
    public void open();
    public void close();
    public boolean isOpened();
    
    // if a port is not available we shouldnt use it.
    public boolean isAvailable();
    
    public String getName();
    public long getMicrosecondPosition();
    public boolean equals(MidiPort port);
    
    // does the midi port have an output?
    public boolean canTransmitMessages();
    
    // add a receiver for the device to transmit to, canTransmitMessages should be true
    public void addReceiver(Receiver r);
    
    // removes a receiver from the device
    public void removeReceiver(Receiver r);
    
    // check if this port has a receiver
    public boolean hasReceiver(Receiver r);
    
    public Collection<Receiver> getReceivers();
    
    // does the midi port have an input?
    public boolean canReceiveMessages();
    
    // transmit to this device. canReceiveMessages should be true.
    public void send(MidiMessage message, long timeStamp);

    // Last time the port recieved data
    public long getLastRxAt();
// Last time the port recieved data
    public long getLastTxAt();
}
