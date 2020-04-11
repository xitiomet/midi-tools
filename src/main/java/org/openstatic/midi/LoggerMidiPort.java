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
import java.util.Iterator;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import javax.swing.JTextArea;
import java.io.OutputStream;
import java.io.IOException;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.HTML;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.JToggleButton;
import javax.swing.JButton;

public class LoggerMidiPort extends JPanel implements MidiPort, ActionListener, Runnable
{
    private boolean opened;
    private boolean keep_running;
    private String name;
    private JTextPane viewArea;
    private JScrollPane midi_log_scroller;
    private StringBuffer logBuffer;
    private JToggleButton autoscroll;
    private JToggleButton portControl;
    private String initBody = "<html><body style=\"padding: 4px 4px 4px 4px; margin: 0px 0px 0px 0px; color: white; background-color: black; font-size: 14px; font-family: \"terminal\", monospace;\"><table style=\"width: 100%; text-align: left;\" cellspacing=\"0\" cellpadding=\"0\"></table></body></html>";
    private JButton clearLog;
    private JPanel buttonPanel;
    private ArrayBlockingQueue<Runnable> taskQueue;
    private Thread taskThread;
    
    class ScrollingDocumentListener implements DocumentListener
    {
        public void changedUpdate(DocumentEvent e) {
            //maybeScrollToBottom();
        }

        public void insertUpdate(DocumentEvent e) {
            maybeScrollToBottom();
        }

        public void removeUpdate(DocumentEvent e) {
            //maybeScrollToBottom();
        }

        private void maybeScrollToBottom()
        {
            if (LoggerMidiPort.this.autoscroll.isSelected())
            {
                JScrollBar scrollBar = midi_log_scroller.getVerticalScrollBar();
                boolean scrollBarAtBottom = isScrollBarFullyExtended(scrollBar);
                if (!scrollBarAtBottom) {
                    LoggerMidiPort.this.taskQueue.add(() -> {
                        LoggerMidiPort.this.scrollToBottom(LoggerMidiPort.this.viewArea);
                    });
                }
            }
        }
        
        public boolean isScrollBarFullyExtended(JScrollBar vScrollBar)
        {
            BoundedRangeModel model = vScrollBar.getModel();
            return (model.getExtent() + model.getValue()) == model.getMaximum();
        }
    }
    
    public LoggerMidiPort(String name)
    {
        super(new BorderLayout());
        this.taskQueue = new ArrayBlockingQueue<Runnable>(10000);
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        this.logBuffer = new StringBuffer();
        this.viewArea = new JTextPane();
        this.viewArea.setContentType("text/html");
        this.viewArea.setEditable(false);
        this.viewArea.setBackground(Color.BLACK);
        this.viewArea.setText(this.initBody);
        
        Document document = this.viewArea.getDocument();
        document.addDocumentListener(new ScrollingDocumentListener());
        
        DefaultCaret caret = (DefaultCaret) this.viewArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
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
        this.start();
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
    
    public void start()
    {
        this.keep_running = true;
        this.taskThread = new Thread(this);
        this.taskThread.start();
    }
    
    public void stop()
    {
        this.keep_running = false;
        this.taskThread = null;
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
            this.viewArea.setText(initBody);
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
        this.addText("<i style=\"color: #777777;\">" + date_time + "</i>", text);
    }
    
    private void addText(final String timestamp, final String html_to_add)
    {
        //System.err.println("ADD TEXT: " + html_to_add);
        this.taskQueue.add(new Runnable(){
            public void run()
            {
                try
                {
                    HTMLDocument doc_html = (HTMLDocument) LoggerMidiPort.this.viewArea.getDocument();
                    Element[] roots = doc_html.getRootElements();
                    Element body = null;
                    Element table = null;
                    for( int i = 0; i < roots[0].getElementCount(); i++ )
                    {
                        Element element = roots[0].getElement( i );
                        if( element.getAttributes().getAttribute( StyleConstants.NameAttribute ) == HTML.Tag.BODY )
                        {
                            body = element;
                            table = body.getElement(0);
                            break;
                        }
                    }
                    if (table != null)
                    {
                        if (table.getElementCount() > 10000)
                        {
                            Element el = table.getElement(0);
                            doc_html.removeElement(el);
                        }
                        doc_html.insertBeforeEnd(table, "<tr><td style=\"width: 160px;\">" + timestamp + "</td><td style=\"text-align: left;\">" + html_to_add + "</td></tr>");
                    }
                } catch (Exception ins_exc) {
                    ins_exc.printStackTrace(System.err);
                }
            }
        });
    }
    
    public void run()
    {
        while(this.keep_running)
        {
            try
            {
                if (this.taskQueue.size() > 0)
                {
                    final ArrayList<Runnable> taskArray = new ArrayList();
                    this.taskQueue.drainTo(taskArray);
                    SwingUtilities.invokeLater(() -> {
                        for(Iterator<Runnable> taskIterator = taskArray.iterator(); taskIterator.hasNext();)
                        {
                            Runnable swingTask = taskIterator.next();
                            swingTask.run();
                        }
                    });
                } else {
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    public static void scrollToBottom(JComponent component)
    {
        Rectangle visibleRect = component.getVisibleRect();
        visibleRect.y = component.getHeight() - visibleRect.height;
        component.scrollRectToVisible(visibleRect);
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
