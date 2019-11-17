package org.openstatic.midi;

import javax.sound.midi.*;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import java.util.Enumeration;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.io.*;

public class MidiPortListModel implements ListModel<MidiPort>, MidiPortListener
{
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();

    public MidiPortListModel()
    {
    }

    public void portAdded(int idx, MidiPort port)
    {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(port, ListDataEvent.INTERVAL_ADDED, idx, idx);
                ldl.intervalAdded(lde);
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
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(port, ListDataEvent.INTERVAL_REMOVED, idx, idx);
                ldl.intervalRemoved(lde);
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
