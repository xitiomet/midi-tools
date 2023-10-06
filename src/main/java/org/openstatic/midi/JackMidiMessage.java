package org.openstatic.midi;

import javax.sound.midi.MidiMessage;

public class JackMidiMessage extends MidiMessage
{
    private int tick;

    public JackMidiMessage(final byte[] bytes, int tick)
    {
        super(bytes);
        this.tick = tick;
    }

    public int getTick()
    {
        return this.tick;
    }

    @Override
    public Object clone()
    {
        return new JackMidiMessage(this.getMessage(), this.tick);
    }
}
