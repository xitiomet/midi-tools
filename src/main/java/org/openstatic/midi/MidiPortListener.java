package org.openstatic.midi;

public interface MidiPortListener
{
    public void portAdded(int idx, MidiPort port);
    public void portRemoved(int idx, MidiPort port);
}
