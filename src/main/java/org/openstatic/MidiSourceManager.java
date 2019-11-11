package org.openstatic;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;

public class MidiSourceManager
{
    private static LinkedHashMap<MidiDevice.Info, MidiSource> localDevices = new LinkedHashMap<MidiDevice.Info, MidiSource>();
    private static Vector<MidiSource> sources = new Vector<MidiSource>();
    
    private static Vector<MidiSourceListener> listeners = new Vector<MidiSourceListener>();
    private static long lastSourceFetch = 0;

    public static void refresh()
    {
        refreshLocalDevices();
        Vector<MidiSource> updatedSources = new Vector<MidiSource>();
        updatedSources.addAll(MidiSourceManager.localDevices.values());

         // Check for new sources added
        for(Iterator<MidiSource> updatedSourcesIterator = updatedSources.iterator(); updatedSourcesIterator.hasNext();)
        {
            MidiSource t = updatedSourcesIterator.next();
            if (!MidiSourceManager.sources.contains(t))
            {
                MidiSourceManager.sources.add(t);
                int idx = MidiSourceManager.sources.indexOf(t);
                //System.err.println("Source Added: " + String.valueOf(idx) + " - " + t.getName());
                fireSourceAdded(idx, t);
            }
        }

        // check for sources removed
        for(Iterator<MidiSource> oldSourcesIterator = MidiSourceManager.sources.iterator(); oldSourcesIterator.hasNext();)
        {
            MidiSource t = oldSourcesIterator.next();
            if (!updatedSources.contains(t))
            {
                int idx = MidiSourceManager.sources.indexOf(t);
                //System.err.println("Source Removed: " + String.valueOf(idx) + " - " + t.getName());
                fireSourceRemoved(idx, t);
            }
        }
        MidiSourceManager.sources = updatedSources;
        MidiSourceManager.lastSourceFetch = System.currentTimeMillis();
    }

    private static void refreshLocalDevices()
    {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        Vector<MidiDevice.Info> newLocalDevices = new Vector<MidiDevice.Info>(Arrays.asList(infos));

        // Check for new devices added
        for(Iterator<MidiDevice.Info> newLocalDevicesIterator = newLocalDevices.iterator(); newLocalDevicesIterator.hasNext();)
        {
            MidiDevice.Info di = newLocalDevicesIterator.next();
            //System.out.println("Local Device Found: " + di.toString());

            if (!MidiSourceManager.localDevices.containsKey(di))
            {
                try
                {
                    MidiDevice device = MidiSystem.getMidiDevice(di);
                    if (device.getMaxTransmitters() != 0)
                    {
                        MidiSource ms = new MidiSource(device);
                        //System.err.println("MidiSource Created: " + ms.getName());
                        MidiSourceManager.localDevices.put(di, ms);
                    }
                } catch (MidiUnavailableException e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        }

        // check for devices removed
        for(Iterator<MidiDevice.Info> oldDevicesIterator = MidiSourceManager.localDevices.keySet().iterator(); oldDevicesIterator.hasNext();)
        {
            MidiDevice.Info di = oldDevicesIterator.next();
            if (!newLocalDevices.contains(di))
            {
                MidiSourceManager.localDevices.remove(di);
            }
        }
    }

    private static void fireSourceAdded(int idx, MidiSource source)
    {
        for (Enumeration<MidiSourceListener> msle = ((Vector<MidiSourceListener>) MidiSourceManager.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            try
            {
                MidiSourceListener msl = msle.nextElement();
                msl.sourceAdded(idx, source);
            } catch (Exception mlex) {
            }
        }
    }

    private static void fireSourceRemoved(int idx, MidiSource source)
    {
        for (Enumeration<MidiSourceListener> msle = ((Vector<MidiSourceListener>) MidiSourceManager.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            try
            {
                MidiSourceListener msl = msle.nextElement();
                msl.sourceRemoved(idx, source);
            } catch (Exception mlex) {
            }
        }
    }

    public static void addMidiSourceListener(MidiSourceListener msl)
    {
        if (!MidiSourceManager.listeners.contains(msl))
        {
            MidiSourceManager.listeners.add(msl);
        }
    }

    public static void removeMidiSourceListener(MidiSourceListener msl)
    {
        if (MidiSourceManager.listeners.contains(msl))
        {
            MidiSourceManager.listeners.remove(msl);
        }
    }

    public static Collection<MidiSource> getSources()
    {
        if ((System.currentTimeMillis() - MidiSourceManager.lastSourceFetch) > 20000)
        {
            MidiSourceManager.refresh();
        }
        return MidiSourceManager.sources;
    }
}
