package org.openstatic;

import org.openstatic.midi.*;

import org.json.*;

import java.io.IOException;
import java.io.BufferedReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class APIWebServer implements MidiControlListener, MidiPortListener, MidiPortProvider
{
    private Server httpServer;
    protected ArrayList<WebSocketSession> wsSessions;
    protected static APIWebServer instance;
    private String staticRoot;
    private LinkedHashMap<String, MidiAPIPort> virtualPorts = new LinkedHashMap<String, MidiAPIPort>();

    public APIWebServer() {
        APIWebServer.instance = this;
        this.wsSessions = new ArrayList<WebSocketSession>();
        httpServer = new Server(6123);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(ApiServlet.class, "/api/*");
        context.addServlet(EventsWebSocketServlet.class, "/events/*");
        try {
            URL url = MidiTools.class.getResource("/midi-tools-res/index.html");
            this.staticRoot = url.toString().replaceAll("index.html", "");
            DefaultServlet defaultServlet = new DefaultServlet();
            ServletHolder holderPwd = new ServletHolder("default", defaultServlet);
            holderPwd.setInitParameter("resourceBase", this.staticRoot);
            context.addServlet(holderPwd, "/*");

            final HttpConfiguration httpConfiguration = new HttpConfiguration();
            httpConfiguration.setSecureScheme("https");
            httpConfiguration.setSecurePort(6124);
            final SslContextFactory sslContextFactory = new SslContextFactory(this.staticRoot + "midi-tools.jks");
            sslContextFactory.setKeyStorePassword("miditools");
            final HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
            final ServerConnector httpsConnector = new ServerConnector(httpServer,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfiguration));
            httpsConnector.setPort(6124);
            httpServer.addConnector(httpsConnector);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        httpServer.setHandler(context);
        MidiPortManager.addMidiPortListener(this);
    }

    public void handleWebSocketEvent(JSONObject j, WebSocketSession session) {
        if (j.has("do")) {
            String doCmd = j.optString("do", "");
            if (doCmd.equals("registerMidiDevice")) {
                String hostname = session.getRemoteAddress().getHostName();
                String deviceName = j.optString("name", "unknown");
                String deviceId = j.optString("device", "unknown");

                String deviceFullName = deviceName + " (" + hostname + ")";
                String deviceFullId = deviceId + "." + hostname;

                int type = MidiAPIPort.TYPE_BIDIRECTIONAL;
                String sType = j.optString("type", "both");
                if (sType.equals("input"))
                    type = MidiAPIPort.TYPE_INPUT;
                else if (sType.equals("output"))
                    type = MidiAPIPort.TYPE_OUTPUT;

                if (!this.virtualPorts.containsKey(deviceFullId)) {
                    MidiAPIPort apiPort = new MidiAPIPort(deviceFullName, deviceId, session, type);
                    this.virtualPorts.put(deviceFullId, apiPort);
                } else {
                    this.virtualPorts.get(deviceFullId).setWebSocketSession(session);
                }
            } else if (doCmd.equals("fetchStatus")) {
                transmitStatus(session, j.optString("__sourceId", null));
            } else if (doCmd.equals("removeMidiDevice")) {
                String hostname = session.getRemoteAddress().getHostName();
                String deviceId = j.optString("device", "unknown");
                String deviceFullId = deviceId + "." + hostname;
                if (this.virtualPorts.containsKey(deviceFullId))
                    this.virtualPorts.remove(deviceFullId);
            } else if (doCmd.equals("midiShortMessage") || doCmd.equals("beatClock")) {
                String hostname = session.getRemoteAddress().getHostName();
                String deviceId = j.optString("device", "unknown");
                String deviceFullId = deviceId + "." + hostname;

                MidiAPIPort port = this.virtualPorts.get(deviceFullId);
                if (port != null) {
                    port.handleWebSocketEvent(j);
                }
            } else if (doCmd.equals("openDevice")) {
                String deviceId = j.optString("device", "unknown");
                String deviceType = j.optString("type", "both");
                if ("input".equals(deviceType)) {
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
                if ("input".equals(deviceType)) {
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
                if (mapping != null) {
                    mapping.open();
                }
            } else if (doCmd.equals("closeMapping")) {
                String mappingId = j.optString("mappingId", null);
                MidiPortMapping mapping = MidiPortManager.findMidiPortMappingById(mappingId);
                if (mapping != null) {
                    mapping.close();
                }
            } else if (doCmd.equals("changeControlValue")) {
                MidiControl mc = MidiTools.getMidiControlByChannelCC(j.optInt("channel", 0), j.optInt("cc", 0));
                if (mc != null) {
                    mc.manualAdjust(j.optInt("value", 0));
                    MidiTools.repaintControls();
                }
            }
        }
    }

    public void setState(boolean b) {
        if (b) {
            try {
                httpServer.start();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        } else {
            try {
                httpServer.stop();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void portAdded(int idx, MidiPort port) {
        JSONObject event = new JSONObject();
        event.put("event", "deviceAdded");
        event.put("id", idx);
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }

    public void portRemoved(int idx, MidiPort port) {
        JSONObject event = new JSONObject();
        event.put("event", "deviceRemoved");
        event.put("id", idx);
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }

    public void portOpened(MidiPort port) {
        JSONObject event = new JSONObject();
        event.put("event", "deviceOpened");
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }

    public void portClosed(MidiPort port) {
        JSONObject event = new JSONObject();
        event.put("event", "deviceClosed");
        event.put("device", MidiTools.MidiPortToJSONObject(port));
        broadcastJSONObject(event);
    }

    public void mappingOpened(MidiPortMapping mapping) {
        JSONObject event = new JSONObject();
        event.put("event", "mappingOpened");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }

    public void mappingClosed(MidiPortMapping mapping) {
        JSONObject event = new JSONObject();
        event.put("event", "mappingClosed");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }

    public void mappingAdded(int idx, MidiPortMapping mapping) {
        JSONObject event = new JSONObject();
        event.put("event", "mappingAdded");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }

    public void mappingRemoved(int idx, MidiPortMapping mapping) {
        JSONObject event = new JSONObject();
        event.put("event", "mappingAdded");
        event.put("mapping", mapping.toJSONObject());
        broadcastJSONObject(event);
    }

    public void controlValueChanged(MidiControl control, int old_value, int new_value) {
        JSONObject event = new JSONObject();
        event.put("event", "controlValueChanged");
        event.put("control", control.toJSONObject());
        event.put("oldValue", old_value);
        event.put("newValue", new_value);
        broadcastJSONObject(event);
    }

    public void controlValueSettled(MidiControl control, int old_value, int new_value) {
        JSONObject event = new JSONObject();
        event.put("event", "controlValueSettled");
        event.put("control", control.toJSONObject());
        event.put("oldValue", old_value);
        event.put("newValue", new_value);
        broadcastJSONObject(event);
    }

    public void broadcastJSONObject(JSONObject jo) {
        String message = jo.toString();
        for (Session s : this.wsSessions) {
            try {
                s.getRemote().sendStringByFuture(message);
            } catch (Exception e) {

            }
        }
    }

    public static class EventsWebSocketServlet extends WebSocketServlet {
        @Override
        public void configure(WebSocketServletFactory factory) {
            // factory.getPolicy().setIdleTimeout(10000);
            factory.register(EventsWebSocket.class);
        }
    }

    public void transmitStatus(WebSocketSession wssession, String targetId) {
        for (Enumeration<MidiControl> cenum = MidiTools.instance.midiControlsPanel.getControlsEnumeration(); cenum.hasMoreElements();) {
            MidiControl mc = cenum.nextElement();
            JSONObject event = new JSONObject();
            event.put("event", "controlAdded");
            event.put("control", mc.toJSONObject());
            if (targetId != null)
                event.put("__targetId", targetId);
            wssession.getRemote().sendStringByFuture(event.toString());
        }
        int idx = 0;
        for (Iterator<MidiPort> pi = MidiPortManager.getPorts().iterator(); pi.hasNext();) {
            MidiPort mp = pi.next();
            JSONObject event = new JSONObject();
            event.put("event", "deviceAdded");
            event.put("id", idx);
            event.put("device", MidiTools.MidiPortToJSONObject(mp));
            if (targetId != null)
                event.put("__targetId", targetId);
            wssession.getRemote().sendStringByFuture(event.toString());
            idx++;
        }
        idx = 0;
        for (Iterator<MidiPortMapping> pi = MidiPortManager.getMidiPortMappings().iterator(); pi.hasNext();) {
            MidiPortMapping mp = pi.next();
            JSONObject event = new JSONObject();
            event.put("event", "mappingAdded");
            event.put("id", idx);
            event.put("mapping", mp.toJSONObject());
            if (targetId != null)
                event.put("__targetId", targetId);
            wssession.getRemote().sendStringByFuture(event.toString());
            idx++;
        }
    }

    @WebSocket
    public static class EventsWebSocket {

        @OnWebSocketMessage
        public void onText(Session session, String message) throws IOException {
            try {
                JSONObject jo = new JSONObject(message);
                if (session instanceof WebSocketSession) {
                    WebSocketSession wssession = (WebSocketSession) session;
                    APIWebServer.instance.handleWebSocketEvent(jo, wssession);
                } else {
                    System.err.println("not instance of WebSocketSession");
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        @OnWebSocketConnect
        public void onConnect(Session session) throws IOException {
            System.err.println("@OnWebSocketConnect");
            if (session instanceof WebSocketSession) {
                WebSocketSession wssession = (WebSocketSession) session;
                System.out.println(wssession.getRemoteAddress().getHostString() + " connected!");
                APIWebServer.instance.wsSessions.add(wssession);

            } else {
                System.err.println("Not an instance of WebSocketSession");
            }
        }

        @OnWebSocketClose
        public void onClose(Session session, int status, String reason) {
            if (session instanceof WebSocketSession) {
                WebSocketSession wssession = (WebSocketSession) session;
                APIWebServer.instance.wsSessions.remove(wssession);
                APIWebServer.instance.virtualPorts.entrySet().removeIf((entry) -> {
                    if (entry.getValue().getWebSocketSession() == wssession)
                    {
                        System.err.println("Removed Session onClose " + entry.getKey());
                        return true;
                    }
                    return false;
                });
            }
        }

    }

    public static class ApiServlet extends HttpServlet {
        public JSONObject readJSONObjectPOST(HttpServletRequest request) {
            StringBuffer jb = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null) {
                    jb.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            try {
                JSONObject jsonObject = new JSONObject(jb.toString());
                return jsonObject;
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                return new JSONObject();
            }
        }

        public boolean isNumber(String v) {
            try {
                Integer.parseInt(v);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse)
                throws ServletException, IOException {
            httpServletResponse.setContentType("text/javascript");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.setCharacterEncoding("iso-8859-1");
            String target = request.getPathInfo();
            System.err.println("Path: " + target);
            JSONObject response = new JSONObject();
            try {
                if ("/rules/add/".equals(target)) {
                    try {
                        JSONObject rule = readJSONObjectPOST(request);
                        MidiControlRule mcr = new MidiControlRule(rule);
                        MidiTools.instance.rules.addElement(mcr);
                        response.put("rule", mcr.toJSONObject());
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                } else if ("/rules/".equals(target)) {
                    response.put("rules", MidiTools.instance.rulesAsJSONArray());
                } else if ("/controls/".equals(target)) {
                    response.put("controls", MidiTools.instance.controlsAsJSONArray());
                } else if ("/info/".equals(target)) {
                    response.put("staticRoot", APIWebServer.instance.staticRoot);
                } else if ("/mappings/add/".equals(target)) {
                    String source = request.getParameter("source");
                    String destination = request.getParameter("destination");
                    MidiPort sourcePort = MidiPortManager.findTransmittingPortByName(source);
                    MidiPort destinationPort = MidiPortManager.findReceivingPortByName(destination);
                    if (sourcePort != null && destinationPort != null) {
                        MidiPortMapping mpm = MidiPortManager.createMidiPortMapping(sourcePort, destinationPort);
                        response.put("mapping", mpm.toJSONObject());
                    } else {
                        response.put("error", "Bad Request");
                    }
                } else if ("/mappings/".equals(target)) {
                    response.put("mappings", MidiTools.instance.mappingsAsJSONArray());
                }
            } catch (Exception x) {
                x.printStackTrace(System.err);
            }
            httpServletResponse.getWriter().println(response.toString());
            // request.setHandled(true);
        }
    }

    @Override
    public Collection<? extends MidiPort> getMidiPorts() 
    {
        return this.virtualPorts.values();
    }
}
