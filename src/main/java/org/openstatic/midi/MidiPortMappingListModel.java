package org.openstatic.midi;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import java.util.Enumeration;
import java.util.Vector;

public class MidiPortMappingListModel implements ListModel<MidiPortMapping>, MidiPortListener
{
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();

    public MidiPortMappingListModel()
    {
        MidiPortManager.addMidiPortListener(this);
    }

    public void portAdded(int idx, MidiPort port) {}
    public void portRemoved(int idx, MidiPort port) {}
    public void portOpened(MidiPort port) {}
    public void portClosed(MidiPort port) {}
    public void mappingOpened(MidiPortMapping mapping) {}
    public void mappingClosed(MidiPortMapping mapping) {}
    
    public void mappingAdded(int idx, MidiPortMapping mapping)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(mapping, ListDataEvent.INTERVAL_ADDED, idx, idx);
                ldl.intervalAdded(lde);
            } catch (Exception mlex) {
            }
        }
    }

    public void mappingRemoved(int idx, MidiPortMapping mapping)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(mapping, ListDataEvent.INTERVAL_REMOVED, idx, idx);
                ldl.intervalRemoved(lde);
            } catch (Exception mlex) {
            }
        }
    }

    public int getSize()
    {
        try
        {
            return MidiPortManager.getMidiPortMappings().size();
        } catch (Exception e) {
            return 0;
        }
    }

    public MidiPortMapping getElementAt(int index)
    {
        try
        {
            MidiPortMapping[] sources = MidiPortManager.getMidiPortMappings().toArray(new MidiPortMapping[0]);
            return sources[index];
        } catch (Exception e) {
            return null;
        }
    }

    public void addListDataListener(ListDataListener l)
    {
        if (!this.listeners.contains(l))
            this.listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l)
    {
        try
        {
            this.listeners.remove(l);
        } catch (Exception e) {}
    }
}
