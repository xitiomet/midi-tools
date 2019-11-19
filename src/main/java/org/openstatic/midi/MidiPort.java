package org.openstatic.midi;

import javax.sound.midi.*;

public interface MidiPort extends Receiver
{
    public void open();
    public void close();
    public boolean isOpened();
    public String getName();
    public long getMicrosecondPosition();
    public boolean equals(MidiPort port);
    
    // does the midi port have an output?
    public boolean canTransmitMessages();
    
    // add a receiver for the device to transmit to, canTransmitMessages should be true
    public void addReceiver(Receiver r);
    
    
    // does the midi port have an input?
    public boolean canReceiveMessages();
    
    // transmit to this device. canReceiveMessages should be true.
    public void send(MidiMessage message, long timeStamp);
}
