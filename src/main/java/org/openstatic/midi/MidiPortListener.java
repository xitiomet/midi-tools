package org.openstatic.midi;

public interface MidiPortListener
{
    public void portAdded(int idx, MidiPort port);
    public void portRemoved(int idx, MidiPort port);
    public void portOpened(MidiPort port);
    public void portClosed(MidiPort port);
    public void mappingAdded(int idx, MidiPortMapping mapping);
    public void mappingRemoved(int idx, MidiPortMapping mapping);
}
