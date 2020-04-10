/*
    Copyright (C) 2020 Brian Dunigan

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.openstatic.midi;

import javax.sound.midi.*;
import java.util.Collection;
import java.util.Date;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;
import javax.swing.JTextArea;
import java.io.OutputStream;
import java.io.IOException;
import java.awt.Font;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.HTML;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.JToggleButton;
import javax.swing.JButton;

public class LoggerMidiPort extends JPanel implements MidiPort, ActionListener
{
    private boolean opened;
    private String name;
    private JTextPane viewArea;
    private JScrollPane midi_log_scroller;
    private StringBuffer logBuffer;
    private JToggleButton autoscroll;
    private JToggleButton portControl;

    private JButton clearLog;
    private JPanel buttonPanel;

    public LoggerMidiPort(String name)
    {
        super(new BorderLayout());
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        this.logBuffer = new StringBuffer();
        this.viewArea = new JTextPane();
        this.viewArea.setContentType("text/html");
        this.viewArea.setEditable(false);
        this.viewArea.setText("<html><body style=\"padding: 4px 4px 4px 4px; margin: 0px 0px 0px 0px; color: white; background-color: black; font-size: 14px; font-family: \"terminal\", monospace;\"></body></html>");
        this.midi_log_scroller = new JScrollPane(this.viewArea);
        this.midi_log_scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        this.name = name;
        try
        {
            ImageIcon eraseIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/erase.png")));
            this.clearLog = new JButton(eraseIcon);
            this.clearLog.setActionCommand("clear");
            this.clearLog.addActionListener(this);
        
            ImageIcon scrollIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/scroll.png")));
            this.autoscroll = new JToggleButton(scrollIcon);
            this.autoscroll.setSelected(true);
            
            ImageIcon portIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/midi-small.png")));
            this.portControl = new JToggleButton(portIcon);
            this.portControl.setSelected(this.opened);
            ChangeListener changeListener = new ChangeListener() {
              public void stateChanged(ChangeEvent changeEvent) {
                AbstractButton abstractButton = (AbstractButton) changeEvent.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
                boolean armed = buttonModel.isArmed();
                boolean pressed = buttonModel.isPressed();
                boolean selected = buttonModel.isSelected();
                //System.out.println("Changed: " + armed + "/" + pressed + "/" + selected);
                if (LoggerMidiPort.this.isOpened() != selected)
                {
                    if (selected)
                    {
                        LoggerMidiPort.this.open();
                    } else {
                        LoggerMidiPort.this.close();
                    }
                }
              }
            };
            this.portControl.addChangeListener(changeListener);
            
            this.buttonPanel.add(this.portControl);
            this.buttonPanel.add(this.autoscroll);
            this.buttonPanel.add(this.clearLog);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.add(midi_log_scroller, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.WEST);
    }
    
    public void open()
    {
        if (!this.isOpened())
        {
            this.opened = true;
            this.portControl.setSelected(true);
            MidiPortManager.firePortOpened(this);
        }
    }
    
    public void close()
    {
        if (this.isOpened())
        {
            MidiPortManager.firePortClosed(this);
            this.opened = false;
            this.portControl.setSelected(false);
        }
    }
    
    public boolean isOpened()
    {
        return this.opened;
    }
    
    // if a port is not available we shouldnt use it.
    public boolean isAvailable()
    {
        return true;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if ("clear".equals(e.getActionCommand()))
        {
            this.viewArea.setText("<html><body style=\"padding: 4px 4px 4px 4px; margin: 0px 0px 0px 0px; color: white; background-color: black; font-size: 14px; font-family: \"terminal\", monospace;\"></body></html>");
        }
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public long getMicrosecondPosition()
    {
        return System.currentTimeMillis() * 1000l;
    }
    
    public boolean equals(MidiPort port)
    {
        return this.name.equals(port.getName());
    }
    
    // does the midi port have an output?
    public boolean canTransmitMessages()
    {
        return false;
    }
    
    // add a receiver for the device to transmit to, canTransmitMessages should be true
    public void addReceiver(Receiver r)
    {
        
    }
    
    // removes a receiver from the device
    public void removeReceiver(Receiver r)
    {
        
    }
    
    // check if this port has a receiver
    public boolean hasReceiver(Receiver r)
    {
        return false;
    }
    
    public Collection<Receiver> getReceivers()
    {
        return null;
    }
    
    // does the midi port have an input?
    public boolean canReceiveMessages()
    {
        return true;
    }
    
    public static String shortMessageToString(ShortMessage msg)
    {
        String channelText = "Channel = " + String.valueOf(msg.getChannel()+1);
        String commandText = "";
        String data1Name = "?";
        String data1Value = "?";
        
        if (msg.getCommand() == ShortMessage.CONTROL_CHANGE)
        {
            data1Name = "CC";
            data1Value = String.valueOf(msg.getData1());
            commandText = "<b style=\"color: yellow;\">CONTROL CHANGE</b>";
        } else if (msg.getCommand() == ShortMessage.NOTE_ON) {
            data1Name = "Note";
            data1Value = MidiPortManager.noteNumberToString(msg.getData1());
            commandText = "<b style=\"color: green;\">NOTE ON</b>";
        } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
            data1Name = "Note";
            data1Value = MidiPortManager.noteNumberToString(msg.getData1());
            commandText = "<b style=\"color: red;\">NOTE OFF</b>";
        }
        String data1Text = data1Name + " = " + data1Value;
        return commandText + " " + channelText + ", " + data1Text + ", value = " + String.valueOf(msg.getData2());
    }
    
    public void println(String text)
    {
        this.println(System.currentTimeMillis(), text);
    }
    
    public void println(long timeStamp, final String text)
    {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        final String date_time = df.format(new Date(timeStamp));
        this.addText("<i style=\"color: #777777;\">" + date_time + "</i> " + text + "<br />");
    }
    
    public void addText(final String html_to_add)
    {
        //System.err.println("ADD TEXT: " + html_to_add);
        Thread vx = new Thread()
        {
            public void run()
            {
                try
                {
                    SwingUtilities.invokeAndWait(new Runnable(){
                        public void run()
                        {
                            try
                            {
                                HTMLDocument doc_html = (HTMLDocument) LoggerMidiPort.this.viewArea.getDocument();
                                Element[] roots = doc_html.getRootElements();
                                Element body = null;
                                for( int i = 0; i < roots[0].getElementCount(); i++ )
                                {
                                    Element element = roots[0].getElement( i );
                                    if( element.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.BODY )
                                    {
                                        body = element;
                                        break;
                                    }
                                }
                                if (body != null)
                                {
                                    doc_html.insertBeforeEnd(body, "<div>" + html_to_add + "</div>");
                                    if (body.getElementCount() > 15000)
                                    {
                                        Element el = body.getElement(0);
                                        doc_html.removeElement(el);
                                    }
                                }
                            } catch (Exception ins_exc) {
                                ins_exc.printStackTrace(System.err);
                            }
                            if (LoggerMidiPort.this.autoscroll.isSelected())
                                LoggerMidiPort.this.scrollToBottom();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        };
        vx.start();
    }
    
    private void scrollToBottom()
    {
        viewArea.setSelectionStart(viewArea.getDocument().getLength());
    }
    
    // transmit to this device. canReceiveMessages should be true.
    public void send(MidiMessage message, long timeStamp)
    {
        if (message instanceof ShortMessage && this.opened)
        {
            ShortMessage smsg = (ShortMessage) message;
            this.println(timeStamp/1000l, shortMessageToString(smsg));
        }
    }
    
    public String toString()
    {
        return this.name;
    }
}
