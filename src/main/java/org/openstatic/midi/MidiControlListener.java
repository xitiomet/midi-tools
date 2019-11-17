package org.openstatic.midi;

public interface MidiControlListener
{
    public void controlValueChanged(MidiControl control, int old_value, int new_value);
    public void controlValueSettled(MidiControl control, int old_value, int new_value);
}
