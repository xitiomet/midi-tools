package org.openstatic.midi;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import org.json.JSONObject;
import org.openstatic.midi.ports.MidiRandomizerPort;

import javax.swing.event.ListDataEvent;
import java.util.Enumeration;
import java.util.Vector;

public class RandomizerListModel implements ListModel<JSONObject>, RandomizerRulesListener
{
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();
    private MidiRandomizerPort randomizerPort;

    public RandomizerListModel(MidiRandomizerPort randomizerPort)
    {
        this.randomizerPort = randomizerPort;
        this.randomizerPort.addRandomizerRulesListener(this);
    }

    public int getSize()
    {
        try
        {
            return this.randomizerPort.getAllRules().length();
        } catch (Exception e) {
            return 0;
        }
    }

    public JSONObject getElementAt(int index)
    {
        try
        {
            return this.randomizerPort.getAllRules().getJSONObject(index);
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

    @Override
    public void ruleAdded(int idx, JSONObject rule) {
        System.err.println("Rule Added("+String.valueOf(idx)+"): " + rule.toString());

        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(rule, ListDataEvent.INTERVAL_ADDED, idx, idx);
                ldl.intervalAdded(lde);
            } catch (Exception mlex) {
            }
        }
        
    }

    @Override
    public void ruleRemoved(int idx, JSONObject rule) {
        for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
        {
            try
            {
                ListDataListener ldl = ldle.nextElement();
                ListDataEvent lde = new ListDataEvent(rule, ListDataEvent.INTERVAL_REMOVED, idx, idx);
                ldl.intervalRemoved(lde);
            } catch (Exception mlex) {
            }
        }
    }
}
