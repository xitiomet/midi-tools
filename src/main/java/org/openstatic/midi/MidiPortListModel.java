package org.openstatic.midi;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.SwingUtilities;
import java.util.Enumeration;
import java.util.Vector;

public class MidiPortListModel implements ListModel<MidiPort>, MidiPortListener
{
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();

    public MidiPortListModel()
    {
        MidiPortManager.addMidiPortListener(this);
    }
    
    public void mappingAdded(int idx, MidiPortMapping mapping) {}
    public void mappingRemoved(int idx, MidiPortMapping mapping) {}
    public void mappingOpened(MidiPortMapping mapping) {}
    public void mappingClosed(MidiPortMapping mapping) {}
    public void portOpened(MidiPort port) {}
    public void portClosed(MidiPort port) {}

    public void portAdded(int idx, MidiPort port)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                final ListDataListener ldl = ldle.nextElement();
                final ListDataEvent lde = new ListDataEvent(port, ListDataEvent.INTERVAL_ADDED, idx, idx);
                SwingUtilities.invokeAndWait(() -> {
                    ldl.intervalAdded(lde);
                });
            } catch (Exception mlex) {
            }
        }
    }

    public void portRemoved(int idx, MidiPort port)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                final ListDataListener ldl = ldle.nextElement();
                final ListDataEvent lde = new ListDataEvent(port, ListDataEvent.INTERVAL_REMOVED, idx, idx);
                SwingUtilities.invokeAndWait(() -> {
                    ldl.intervalRemoved(lde);
                });
            } catch (Exception mlex) {
            }
        }
    }

    public int getSize()
    {
        try
        {
            return MidiPortManager.getPorts().size();
        } catch (Exception e) {
            return 0;
        }
    }

    public MidiPort getElementAt(int index)
    {
        try
        {
            MidiPort[] sources = MidiPortManager.getPorts().toArray(new MidiPort[0]);
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
