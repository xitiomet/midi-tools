package org.openstatic;

import org.json.*;

import java.io.IOException;
import java.io.BufferedReader;

import java.net.InetAddress;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;


public class APIWebServer implements MidiControlListener
{
    private Server httpServer;
    protected ArrayList<Session> wsSessions;
    protected static APIWebServer instance;

    public APIWebServer()
    {
        APIWebServer.instance = this;
        this.wsSessions = new ArrayList<Session>();
        httpServer = new Server(6123);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(ApiServlet.class, "/api/*");
        context.addServlet(EventsWebSocketServlet.class, "/events/*");
        httpServer.setHandler(context);
    }
    
    public void handleWebSocketEvent(JSONObject j, Session session)
    {
        
    }
    
    public void setState(boolean b)
    {
        if (b)
        {
            try
            {
                httpServer.start();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        } else {
            try
            {
                httpServer.stop();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    public void controlValueChanged(MidiControl control, int old_value, int new_value)
    {
        JSONObject event = new JSONObject();
        event.put("event", "controlValueChanged");
        event.put("control", control.toJSONObject());
        event.put("oldValue", old_value);
        event.put("newValue", new_value);
        broadcastJSONObject(event);
    }
    
    public void controlValueSettled(MidiControl control, int old_value, int new_value)
    {
        JSONObject event = new JSONObject();
        event.put("event", "controlValueSettled");
        event.put("control", control.toJSONObject());
        event.put("oldValue", old_value);
        event.put("newValue", new_value);
        broadcastJSONObject(event);
    }

    public void broadcastJSONObject(JSONObject jo)
    {
        String message = jo.toString();
        for(Session s : this.wsSessions)
        {
            s.getRemote().sendStringByFuture(message);
        }
    }

    public static class EventsWebSocketServlet extends WebSocketServlet
    {
        @Override
        public void configure(WebSocketServletFactory factory)
        {
            //factory.getPolicy().setIdleTimeout(10000);
            factory.register(EventsWebSocket.class);
        }
    }
    
    @WebSocket
    public static class EventsWebSocket
    {
     
        @OnWebSocketMessage
        public void onText(Session session, String message) throws IOException
        {
            try
            {
                JSONObject jo = new JSONObject(message);
                APIWebServer.instance.handleWebSocketEvent(jo, session);
            } catch (Exception e) {}
        }
     
        @OnWebSocketConnect
        public void onConnect(Session session) throws IOException
        {
            System.out.println(session.getRemoteAddress().getHostString() + " connected!");
            APIWebServer.instance.wsSessions.add(session);
        }
     
        @OnWebSocketClose
        public void onClose(Session session, int status, String reason)
        {
            System.out.println(session.getRemoteAddress().getHostString() + " closed!");
            APIWebServer.instance.wsSessions.remove(session);
        }
     
    }


    public static class ApiServlet extends HttpServlet
    {
        public JSONObject readJSONObjectPOST(HttpServletRequest request)
        {
            StringBuffer jb = new StringBuffer();
            String line = null;
            try
            {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null)
                {
                    jb.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            try
            {
                JSONObject jsonObject =  new JSONObject(jb.toString());
                return jsonObject;
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                return new JSONObject();
            }
        }

        public boolean isNumber(String v)
        {
            try
            {
                Integer.parseInt(v);
                return true;
            } catch(NumberFormatException e){
                return false;
            }
        }
        
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException
        {
            httpServletResponse.setContentType("text/javascript");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            httpServletResponse.setCharacterEncoding("iso-8859-1");
            String target = request.getPathInfo();
            System.err.println("Path: " + target);
            JSONObject response = new JSONObject();
            try
            {
                if ("/rules/add/".equals(target))
                {
                    try
                    {
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
                }
            } catch (Exception x) {
                x.printStackTrace(System.err);
            }
            httpServletResponse.getWriter().println(response.toString());
            //request.setHandled(true);
        }
    }
}
