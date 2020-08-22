package org.openstatic.midi;

import java.util.Collection;

public interface MidiPortProvider 
{
    public Collection<? extends MidiPort> getMidiPorts();
}