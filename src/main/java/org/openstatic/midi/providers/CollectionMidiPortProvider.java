package org.openstatic.midi.providers;

import java.util.ArrayList;
import java.util.Collection;

import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortProvider;

public class CollectionMidiPortProvider extends ArrayList<MidiPort> implements MidiPortProvider
{
    public CollectionMidiPortProvider()
    {
        super();
    }

    @Override
    public Collection<? extends MidiPort> getMidiPorts()
    {
        // TODO Auto-generated method stub
        return this;
    }
}