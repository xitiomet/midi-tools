package org.openstatic;

import java.util.Enumeration;

import java.util.Iterator;

import org.openstatic.midi.*;
import org.openstatic.routeput.*;
import org.openstatic.routeput.client.*;

public class RoutePutSessionManager implements RoutePutRemoteSessionListener
{
    private RoutePutClient client;
    public RoutePutSessionManager(RoutePutClient client)
    {
        this.client = client;
        this.client.addSessionListener(this);
    }

    @Override
    public void onClose(RoutePutRemoteSession session)
    {
        
    }

    @Override
    public void onConnect(RoutePutRemoteSession session)
    {
        final String hostname = session.getConnectionId().substring(0,5);
        session.addMessageListener(new RoutePutMessageListener(){
            @Override
            public void onMessage(RoutePutMessage j)
            {
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
                        transmitStatus(session, j.optString("__sourceId", null));
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

    public void transmitStatus(RoutePutSession session, String targetId)
    {
        for (Enumeration<MidiControl> cenum = MidiTools.instance.controls.elements(); cenum.hasMoreElements();)
        {
            MidiControl mc = cenum.nextElement();
            RoutePutMessage event = new RoutePutMessage();
            event.put("event", "controlAdded");
            event.put("control", mc.toJSONObject());
            if (targetId != null)
                event.setTargetId(targetId);
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
            if (targetId != null)
                event.put("__targetId", targetId);
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
            if (targetId != null)
                event.put("__targetId", targetId);
            session.send(event);
            idx++;
        }
    }
}