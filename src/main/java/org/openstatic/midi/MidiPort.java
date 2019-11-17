package org.openstatic.midi;

import javax.sound.midi.*;

public interface MidiPort extends Receiver
{
    public void open();
    public void close();
    public boolean isOpened();
    public String getName();
    public long getMicrosecondPosition();
    
    // does the midi port have an input?
    public boolean canReceiveMessages();
    
    // does the midi port have an output?
    public boolean canTransmitMessages();
    
    // add a receiver for the device to transmit to
    public void addReceiver(Receiver r);
    
    // transmit to this device.
    public void send(MidiMessage message, long timeStamp);
    
    public boolean equals(MidiPort port);
}
