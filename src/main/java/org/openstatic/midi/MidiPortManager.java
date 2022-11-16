package org.openstatic.midi;

import javax.sound.midi.*;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Enumeration;
import java.util.stream.Collectors;

public class MidiPortManager
{
    private static Vector<MidiPort> ports = new Vector<MidiPort>();
    private static Vector<MidiPortProvider> providers = new Vector<MidiPortProvider>();
    private static Vector<MidiPortListener> listeners = new Vector<MidiPortListener>();
    private static Vector<MidiPortMapping> mappings = new Vector<MidiPortMapping>();
    private static long lastPortFetch = 0;
    private static boolean keepRunning;
    private static Thread taskThread;
    
    public static synchronized String generateBigAlphaKey(int key_length)
    {
        try
        {
            // make sure we never get the same millis!
            Thread.sleep(1);
        } catch (Exception e) {}
        Random n = new Random(System.currentTimeMillis());
        String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer return_key = new StringBuffer();
        for (int i = 0; i < key_length; i++)
        {
            return_key.append(alpha.charAt(n.nextInt(alpha.length())));
        }
        String randKey = return_key.toString();
        //System.err.println("Generated Rule ID: " + randKey);
        return randKey;
    }

    public static void init()
    {
        MidiPortManager.keepRunning = true;
        if (MidiPortManager.taskThread == null)
        {
            //refresh();
            Runtime.getRuntime().addShutdownHook(new Thread() 
            { 
                public void run() 
                { 
                    MidiPortManager.shutdown();
                    System.out.println("Shutdown MidiPortManager!"); 
                } 
            }); 
            MidiPortManager.taskThread = new Thread()
            {
                public void run()
                {
                    //System.err.println("task loop");
                    while(MidiPortManager.keepRunning)
                    {
                        if ((System.currentTimeMillis() - MidiPortManager.lastPortFetch) > 2000)
                        {
                            Thread x = new Thread()
                            {
                                public void run()
                                {
                                    MidiPortManager.refresh();
                                }
                            };
                            x.start();
                        }
                    }
                }
            };
            MidiPortManager.taskThread.setDaemon(true);
            MidiPortManager.taskThread.start();
        }
    }

    public static void shutdown()
    {
        MidiPortManager.keepRunning = false;
    }

    public static boolean isRunning()
    {
        return MidiPortManager.keepRunning;
    }

    private static synchronized void refresh()
    {   
        Vector<MidiPort> updatedSources = new Vector<MidiPort>();
        for (MidiPortProvider midiPortProvider : MidiPortManager.providers) 
        {
            Collection<? extends MidiPort> ports = midiPortProvider.getMidiPorts();
            if (ports != null)
            {
                updatedSources.addAll(ports.stream().filter((p) -> p.isAvailable()).collect(Collectors.toList()));
            }
        }
        
         // Check for new sources added
        for(Iterator<MidiPort> updatedPortsIterator = updatedSources.iterator(); updatedPortsIterator.hasNext();)
        {
            MidiPort t = updatedPortsIterator.next();
            if (!MidiPortManager.ports.contains(t))
            {
                MidiPortManager.ports.add(t);
                int idx = MidiPortManager.ports.indexOf(t);
                //System.err.println("Source Added: " + String.valueOf(idx) + " - " + t.getName());
                firePortAdded(idx, t);
            }
        }

        // check for sources removed
        for(Iterator<MidiPort> oldPortsIterator = MidiPortManager.ports.iterator(); oldPortsIterator.hasNext();)
        {
            MidiPort t = oldPortsIterator.next();
            if (!updatedSources.contains(t))
            {
                int idx = MidiPortManager.ports.indexOf(t);
                //System.err.println("Source Removed: " + String.valueOf(idx) + " - " + t.getName());
                firePortRemoved(idx, t);
            }
        }
        MidiPortManager.ports = updatedSources;
        MidiPortManager.lastPortFetch = System.currentTimeMillis();
    }

    

    private static void firePortAdded(final int idx, final MidiPort port)
    {
        for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            try
            {
                final MidiPortListener msl = msle.nextElement();
                (new Thread(() -> msl.portAdded(idx, port))).start();
            } catch (Exception mlex) {
            }
        }
    }

    private static void firePortRemoved(final int idx, final MidiPort port)
    {
        for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            try
            {
                final MidiPortListener msl = msle.nextElement();
                (new Thread(() -> msl.portRemoved(idx, port))).start();
            } catch (Exception mlex) {
            }
        }
    }
    
    public static void firePortOpened(final MidiPort port)
    {
        if (MidiPortManager.ports.contains(port))
        {
            for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
            {
                try
                {
                    final MidiPortListener msl = msle.nextElement();
                    (new Thread(() -> msl.portOpened(port))).start();
                } catch (Exception mlex) {
                }
            }
        }
    }

    public static void firePortClosed(final MidiPort port)
    {
        if (MidiPortManager.ports.contains(port))
        {
            for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
            {
                try
                {
                    final MidiPortListener msl = msle.nextElement();
                    (new Thread(() -> msl.portClosed(port))).start();
                } catch (Exception mlex) {
                }
            }
        }
    }
    
    private static void fireMappingAdded(final int idx, final MidiPortMapping mapping)
    {
        for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            final MidiPortListener msl = msle.nextElement();
            (new Thread(() -> msl.mappingAdded(idx, mapping))).start();
        }
    }

    private static void fireMappingRemoved(final int idx, final MidiPortMapping mapping)
    {
        for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
        {
            final MidiPortListener msl = msle.nextElement();
            (new Thread(() -> msl.mappingRemoved(idx, mapping))).start();
        }
    }
    
    public static void fireMappingOpened(final MidiPortMapping mapping)
    {
        if (MidiPortManager.mappings.contains(mapping))
        {
            for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
            {
                try
                {
                    final MidiPortListener msl = msle.nextElement();
                    (new Thread(() -> msl.mappingOpened(mapping))).start();
                } catch (Exception mlex) {
                }
            }
        }
    }

    public static void fireMappingClosed(final MidiPortMapping mapping)
    {
        if (MidiPortManager.mappings.contains(mapping))
        {
            for (Enumeration<MidiPortListener> msle = ((Vector<MidiPortListener>) MidiPortManager.listeners.clone()).elements(); msle.hasMoreElements();)
            {
                try
                {
                    final MidiPortListener msl = msle.nextElement();
                    (new Thread(() -> msl.mappingClosed(mapping))).start();
                } catch (Exception mlex) {
                }
            }
        }
    }

    public static void addProvider(MidiPortProvider mpp)
    {
        if (!MidiPortManager.providers.contains(mpp))
        {
            MidiPortManager.providers.add(mpp);
        }
    }

    public static void removeProvider(MidiPortProvider mpp)
    {
        if (MidiPortManager.providers.contains(mpp))
        {
            MidiPortManager.providers.remove(mpp);
        }
    }

    public static void addMidiPortListener(MidiPortListener msl)
    {
        if (!MidiPortManager.listeners.contains(msl))
        {
            MidiPortManager.listeners.add(msl);
        }
    }

    public static void removeMidiPortListener(MidiPortListener msl)
    {
        if (MidiPortManager.listeners.contains(msl))
        {
            MidiPortManager.listeners.remove(msl);
        }
    }

    public static Collection<MidiPort> getPorts()
    {
        return MidiPortManager.ports;
    }
    
    public static MidiPortMapping createMidiPortMapping(MidiPort source, MidiPort dest)
    {
        MidiPortMapping mpm = new MidiPortMapping(source, dest);
        MidiPortManager.addMidiPortMapping(mpm);
        return mpm;
    }
    
    public static void addMidiPortMapping(MidiPortMapping mpm)
    {
        MidiPortManager.mappings.add(mpm);
        MidiPortManager.fireMappingAdded(MidiPortManager.mappings.indexOf(mpm), mpm);
    }
    
    public static void removeMidiPortMapping(MidiPortMapping mpm)
    {
        if (mpm != null)
        {
            mpm.close();
            if (MidiPortManager.mappings.contains(mpm))
            {
                int idx = MidiPortManager.mappings.indexOf(mpm);
                MidiPortManager.mappings.remove(mpm);
                MidiPortManager.fireMappingRemoved(idx, mpm);
            }
        }
    }

    public static void deleteAllMidiPortMappings()
    {
        Collection<MidiPortMapping> mappings = new Vector<MidiPortMapping>(MidiPortManager.getMidiPortMappings());
        if (mappings != null)
        {
            mappings.forEach((mapping) -> {
                MidiPortManager.removeMidiPortMapping(mapping);
            });
        }
    }
    
    public static Collection<MidiPortMapping> getMidiPortMappings()
    {
        return MidiPortManager.mappings;
    }
    
    public static Collection<MidiPort> getReceivingPorts()
    {
        Vector<MidiPort> inputs = new Vector<MidiPort>();
        for(Iterator<MidiPort> portsIterator = MidiPortManager.ports.iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if (t.canReceiveMessages())
            {
                inputs.add(t);
            }
        }
        return inputs;
    }
    
    public static Collection<MidiPort> getTransmittingPorts()
    {
        Vector<MidiPort> outputs = new Vector<MidiPort>();
        for(Iterator<MidiPort> portsIterator = MidiPortManager.ports.iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if (t.canTransmitMessages())
            {
                outputs.add(t);
            }
        }
        return outputs;
    }
    
    public static MidiPortMapping findMidiPortMappingByName(String mappingName)
    {
        for(Iterator<MidiPortMapping> mappingsIterator = MidiPortManager.mappings.iterator(); mappingsIterator.hasNext();)
        {
            MidiPortMapping t = mappingsIterator.next();
            if (t.toString().equals(mappingName))
            {
                return t;
            }
        }
        return null;
    }

    public static MidiPortMapping findMidiPortMappingById(String mappingId)
    {
        for(Iterator<MidiPortMapping> mappingsIterator = MidiPortManager.mappings.iterator(); mappingsIterator.hasNext();)
        {
            MidiPortMapping t = mappingsIterator.next();
            if (t.getMappingId().equals(mappingId))
            {
                return t;
            }
        }
        return null;
    }
    
    public static MidiPort findReceivingPortByName(String name)
    {
        for(Iterator<MidiPort> portsIterator = MidiPortManager.ports.iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if (t.canReceiveMessages() && t.getName().toLowerCase().startsWith(name.toLowerCase()))
            {
                return t;
            }
        }
        return null;
    }
    
    public static MidiPort findTransmittingPortByName(String name)
    {
        for(Iterator<MidiPort> portsIterator = MidiPortManager.ports.iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if (t.canTransmitMessages() && t.getName().toLowerCase().startsWith(name.toLowerCase()))
            {
                return t;
            }
        }
        return null;
    }
    
    public static MidiPort findBidirectionalPortByName(String name)
    {
        for(Iterator<MidiPort> portsIterator = MidiPortManager.ports.iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if ((t.canTransmitMessages() && t.canReceiveMessages()) && t.getName().toLowerCase().startsWith(name.toLowerCase()))
            {
                return t;
            }
        }
        return null;
    }
    
    public static String noteNumberToString(int i)
    {
        String[] noteString = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return noteString[i%12] + String.valueOf( ((int)Math.floor(((float)i)/12f)) - 2);
    }
    
    public static String shortMessageToString(ShortMessage msg)
    {
        String channelText = "CH=" + String.valueOf(msg.getChannel()+1);
        String commandText = "";
        String data1Name = "?";
        String data1Value = "?";
        
        if (msg.getCommand() == ShortMessage.CONTROL_CHANGE)
        {
            data1Name = "CC";
            data1Value = String.valueOf(msg.getData1());
            commandText = "CONTROL CHANGE";
        } else if (msg.getCommand() == ShortMessage.NOTE_ON) {
            data1Name = "NOTE";
            data1Value = MidiPortManager.noteNumberToString(msg.getData1());
            commandText = "NOTE ON";
        } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
            data1Name = "NOTE";
            data1Value = MidiPortManager.noteNumberToString(msg.getData1());
            commandText = "NOTE OFF";
        }
        String data1Text = data1Name + "=" + data1Value;
        return "[ " + commandText + " " + channelText + " " + data1Text + " v=" + String.valueOf(msg.getData2()) + " ]";
    }
}
