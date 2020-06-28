package org.openstatic;

import java.util.Enumeration;

import java.util.Iterator;

import org.openstatic.midi.*;
import org.openstatic.routeput.*;
import org.openstatic.routeput.client.*;

public class RoutePutSessionManager implements RoutePutSessionListener, MidiControlListener, MidiPortListener
{
    private RoutePutClient client;

    public RoutePutSessionManager(RoutePutClient client)
    {
        this.client = client;
        this.client.addSessionListener(this);
        MidiPortManager.addMidiPortListener(this);
    }

    public void portAdded(int idx, MidiPort port)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "deviceAdded");
        event.put("id", idx);
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }
    
    public void portRemoved(int idx, MidiPort port)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "deviceRemoved");
        event.put("id", idx);
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }
    
    public void portOpened(MidiPort port)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "deviceOpened");
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }
    
    public void portClosed(MidiPort port)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "deviceClosed");
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }
    
    public void mappingOpened(MidiPortMapping mapping)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "mappingOpened");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }
    
    public void mappingClosed(MidiPortMapping mapping)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "mappingClosed");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }
    
    public void mappingAdded(int idx, MidiPortMapping mapping)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "mappingAdded");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }
    
    public void mappingRemoved(int idx, MidiPortMapping mapping)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "mappingAdded");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }
    
    public void controlValueChanged(MidiControl control, int old_value, int new_value)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "controlValueChanged");
        event.put("control", control.toJSONObject());
        event.put("oldValue", old_value);
        event.put("newValue", new_value);
        broadcastJSONObject(event);
    }
    
    public void controlValueSettled(MidiControl control, int old_value, int new_value)
    {
        RoutePutMessage event = new RoutePutMessage();
        event.put("event", "controlValueSettled");
        event.put("control", control.toJSONObject());
        event.put("oldValue", old_value);
        event.put("newValue", new_value);
        broadcastJSONObject(event);
    }

    public void broadcastJSONObject(RoutePutMessage jo)
    {
        this.client.send(jo);
    }

    @Override
    public void onClose(RoutePutSession session, boolean local)
    {
        
    }

    @Override
    public void onConnect(RoutePutSession session, boolean local)
    {
        if (!local)
        {
            final String hostname = session.getProperty("identity", session.getConnectionId().substring(0,5));
            session.addMessageListener(new RoutePutMessageListener(){
                @Override
                public void onMessage(RoutePutMessage j)
                {
                    System.err.println("RPMR " + j.toString());
                    if (j.has("do"))
                    {
                        String doCmd = j.optString("do","");
                        if (doCmd.equals("registerMidiDevice"))
                        {
                            
                            String deviceName = j.optString("name", "unknown");
                            String deviceId = j.optString("device", "unknown");
                            
                            String deviceFullName = deviceName + " (" + hostname + ")";
                            String deviceFullId = deviceId + "." + hostname;
                            
                            int type = MidiAPIPort.TYPE_BIDIRECTIONAL;
                            String sType = j.optString("type","both");
                            if (sType.equals("input"))
                                type = MidiAPIPort.TYPE_INPUT;
                            else if (sType.equals("output"))
                                type = MidiAPIPort.TYPE_OUTPUT;
                            
                            RoutePutSessionMidiPort apiPort = new RoutePutSessionMidiPort(deviceFullName, deviceId, session, type);
                            MidiPort existingPort = MidiPortManager.findVirtualPort(deviceFullId);
                            if (existingPort == null)
                            {
                                MidiPortManager.registerVirtualPort(deviceFullId, apiPort);
                            } else if (existingPort instanceof RoutePutSessionMidiPort) {
                                RoutePutSessionMidiPort existingAPIPort = (RoutePutSessionMidiPort) existingPort;
                                existingAPIPort.setRoutePutSession(session);
                            }
                        } else if (doCmd.equals("fetchStatus")) {
                            transmitStatus(session);
                        } else if (doCmd.equals("removeMidiDevice")) {
                            String deviceId = j.optString("device", "unknown");
                            String deviceFullId = deviceId + "." + hostname;
                            MidiPortManager.removeVirtualPort(deviceFullId);
                        } else if (doCmd.equals("midiShortMessage") || doCmd.equals("beatClock")) {
                            String deviceId = j.optString("device", "unknown");
                            String deviceFullId = deviceId + "." + hostname;
                                        
                            MidiPort p = MidiPortManager.findVirtualPort(deviceFullId);
                            if (p instanceof MidiAPIPort)
                            {
                                MidiAPIPort port = (MidiAPIPort) p;
                                port.handleWebSocketEvent(j);
                            }
                        } else if (doCmd.equals("openDevice")) {
                            String deviceId = j.optString("device", "unknown");
                            String deviceType = j.optString("type", "both");
                            if ("input".equals(deviceType))
                            {
                                MidiPort p = MidiPortManager.findReceivingPortByName(deviceId);
                                p.open();
                            } else if ("output".equals(deviceType)) {
                                MidiPort p = MidiPortManager.findTransmittingPortByName(deviceId);
                                p.open();
                            } else {
                                MidiPort p = MidiPortManager.findBidirectionalPortByName(deviceId);
                                p.open();
                            }
                        } else if (doCmd.equals("closeDevice")) {
                            String deviceId = j.optString("device", "unknown");
                            String deviceType = j.optString("type", "both");
                            if ("input".equals(deviceType))
                            {
                                MidiPort p = MidiPortManager.findReceivingPortByName(deviceId);
                                p.close();
                            } else if ("output".equals(deviceType)) {
                                MidiPort p = MidiPortManager.findTransmittingPortByName(deviceId);
                                p.close();
                            } else {
                                MidiPort p = MidiPortManager.findBidirectionalPortByName(deviceId);
                                p.close();
                            }
                        } else if (doCmd.equals("openMapping")) {
                            String mappingId = j.optString("mappingId", null);
                            MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(mappingId);
                            if (mapping != null)
                            {
                                mapping.open();
                            }
                        } else if (doCmd.equals("closeMapping")) {
                            String mappingId = j.optString("mappingId", null);
                            MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(mappingId);
                            if (mapping != null)
                            {
                                mapping.close();
                            }
                        } else if (doCmd.equals("changeControlValue")) {
                            MidiControl mc = MidiTools.getMidiControlByChannelCC(j.optInt("channel", 0), j.optInt("cc", 0));
                            if (mc != null)
                            {
                                mc.manualAdjust(j.optInt("value",0));
                                MidiTools.repaintControls();
                            }
                        }
                    }
                }
            });
        }
    }

    public void transmitStatus(RoutePutSession session)
    {
        for (Enumeration<MidiControl> cenum = MidiTools.instance.controls.elements(); cenum.hasMoreElements();)
        {
            MidiControl mc = cenum.nextElement();
            RoutePutMessage event = new RoutePutMessage();
            event.put("event", "controlAdded");
            event.put("control", mc.toJSONObject());
            session.send(event);
        }
        int idx = 0;
        for (Iterator<MidiPort> pi = MidiPortManager.getPorts().iterator(); pi.hasNext();)
        {
            MidiPort mp = pi.next();
            RoutePutMessage event = new RoutePutMessage();
            event.put("event", "deviceAdded");
            event.put("id", idx);
            event.put("device", MidiTools.MidiPortToJSONObject(mp));
            session.send(event);
            idx++;
        }
        idx = 0;
        for (Iterator<MidiPortMapping> pi = MidiPortManager.getMidiPortMappings().iterator(); pi.hasNext();)
        {
            MidiPortMapping mp = pi.next();
            RoutePutMessage event = new RoutePutMessage();
            event.put("event", "mappingAdded");
            event.put("id", idx);
            event.put("mapping", mp.toJSONObject());
            session.send(event);
            idx++;
        }
    }
}