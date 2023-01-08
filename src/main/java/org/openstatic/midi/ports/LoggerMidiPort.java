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

package org.openstatic.midi.ports;

import org.openstatic.midi.*;
import javax.sound.midi.*;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.text.SimpleDateFormat;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.FocusListener;

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
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.Element;
import javax.swing.JToggleButton;
import javax.swing.JButton;

public class LoggerMidiPort extends JPanel implements MidiPort, ActionListener, Runnable, FocusListener
{
    private boolean hasFocus;
    private boolean opened;
    private boolean keep_running;
    private String name;
    private JTextPane viewArea;
    private JScrollPane midi_log_scroller;
    private JToggleButton autoscroll;
    private JToggleButton portControl;
    private String initBody = "<html><body style=\"padding: 4px 4px 4px 4px; margin: 0px 0px 0px 0px; color: white; background-color: #222222; font-size: 14px; font-family: \"terminal\", monospace;\"><table style=\"width: 100%; text-align: left;\" cellspacing=\"0\" cellpadding=\"0\"></table></body></html>";
    private JButton clearLog;
    private JPanel buttonPanel;
    private ArrayBlockingQueue<Runnable> taskQueue;
    private Thread taskThread;
    private int beatPulse;
    private long lastTxAt;
    private long txCount;
    private long rxCount;

    /*
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
    }
    */
    
    private void maybeScrollToBottom()
    {
        if (LoggerMidiPort.this.autoscroll != null)
        {
            if (LoggerMidiPort.this.autoscroll.isSelected() && this.isVisible())
            {
                JScrollBar scrollBar = midi_log_scroller.getVerticalScrollBar();
                boolean scrollBarAtBottom = isScrollBarFullyExtended(scrollBar);
                if (!scrollBarAtBottom) {
                    //System.err.println("Need to scroll to bottom");
                    LoggerMidiPort.this.taskQueue.add(() -> {
                        LoggerMidiPort.scrollToBottom(LoggerMidiPort.this.viewArea);
                    });
                } else {
                    //System.err.println("Dont need to scroll");
                }
            }
        }
    }
    
    private boolean isScrollBarFullyExtended(JScrollBar vScrollBar)
    {
        BoundedRangeModel model = vScrollBar.getModel();
        return (model.getExtent() + model.getValue()) == model.getMaximum();
    }
    
    public LoggerMidiPort(String name)
    {
        super(new BorderLayout());
        this.beatPulse = 1;
        this.taskQueue = new ArrayBlockingQueue<Runnable>(10000);
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        this.viewArea = new JTextPane();
        this.viewArea.setContentType("text/html");
        this.viewArea.setEditable(false);
        this.viewArea.setBackground(new Color(34,34,34));
        this.viewArea.setText(this.initBody);
           
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
            this.clearLog.setToolTipText("Clear Log");
        
            ImageIcon scrollIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/scroll.png")));
            this.autoscroll = new JToggleButton(scrollIcon);
            this.autoscroll.setSelected(true);
            this.autoscroll.setToolTipText("Autoscroll");
            
            ImageIcon portIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/midi-small.png")));
            this.portControl = new JToggleButton(portIcon);
            this.portControl.setSelected(this.opened);
            this.portControl.setToolTipText("Open this MIDI port (literally to the logger)");
            ChangeListener changeListener = new ChangeListener() {
              public void stateChanged(ChangeEvent changeEvent) {
                AbstractButton abstractButton = (AbstractButton) changeEvent.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
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
        this.addFocusListener(this);
    }

    public void hidePortControl()
    {
        try
        {
            this.buttonPanel.remove(this.portControl);
        } catch (Exception e) {}
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

    public static short toShort(byte msb, byte lsb) 
    {
        return (short) ((0xff00 & (short) (msb << 8)) | (0x00ff & (short) lsb));
    }

    public static short toShort(byte[] data) {
        if ((data == null) || (data.length != 2)) {
            return 0x0;
        }
        return (short) ((0xff & data[0]) << 8 | (0xff & data[1]));
    }
    
    public static float mapFloat(float x, float in_min, float in_max, float out_min, float out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static String shortMessageToString(ShortMessage msg)
    {
        String channelText = "CH " + String.format("%02d", msg.getChannel()+1);
        String commandText = "";
        String data1Value = "?";
        if (msg.getCommand() == ShortMessage.PITCH_BEND)
        {
            int bendValue = (msg.getData2() << 7) | msg.getData1();
            float bendAmount = mapFloat(bendValue, 0, 16383, -1.0f, 1.0f);
            commandText = "<b style=\"color: orange;\">PITCH BEND</b>";
            String bendText = String.format("%1.2f",bendAmount);
            if (!bendText.startsWith("-"))
                bendText = "+" + bendText;
            return channelText + " " + commandText + " " + bendText;
        } else if (msg.getCommand() == ShortMessage.CONTROL_CHANGE) {
            commandText = "<b style=\"color: yellow;\">CONTROL CHANGE " + String.format("%3d", msg.getData1()) + "</b>";
            return channelText + " " + commandText + " = " + String.valueOf(msg.getData2());
        } else if (msg.getCommand() == ShortMessage.NOTE_ON) {
            data1Value = MidiPortManager.noteNumberToString(msg.getData1());
            commandText = "<b style=\"color: green;\">NOTE ON</b>";
            return channelText + " " + commandText + " " +  data1Value + " = " + String.valueOf(msg.getData2());
        } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
            data1Value = MidiPortManager.noteNumberToString(msg.getData1());
            commandText = "<b style=\"color: red;\">NOTE OFF</b>";
            return channelText + " " + commandText + " " + data1Value + " = " + String.valueOf(msg.getData2());
        } else if (msg.getCommand() == 240) {
            byte[] data = msg.getMessage();
            data1Value = String.valueOf(Byte.toUnsignedInt(data[0]));
            for(int i = 1; i < msg.getLength(); i++)
            {
                data1Value += ", " + String.valueOf(Byte.toUnsignedInt(data[i]));
            }
            commandText = "<b style=\"color: purple;\">SYSTEM EXCLUSIVE</b> [" + data1Value + "]";
            int command = Byte.toUnsignedInt(data[0]);
            if (command == ShortMessage.CONTINUE)
            {
                commandText = "<b style=\"color: purple;\">SYSEX CONTINUE</b>";
            } else if (command == ShortMessage.STOP) {
                commandText = "<b style=\"color: purple;\">SYSEX STOP</b>";
            } else if (command == ShortMessage.START) {
                commandText = "<b style=\"color: purple;\">SYSEX START</b>";
            } else if (command == ShortMessage.SONG_SELECT) {
                commandText = "<b style=\"color: purple;\">SYSEX SONG SELECT</b>";
            } else if (command == ShortMessage.SONG_POSITION_POINTER) {
                short result = toShort(data[1], data[2]); // MSB / LSB
                commandText = "<b style=\"color: purple;\">SYSEX SONG POSITION</b> = " + String.valueOf(result) + " beats, " + String.valueOf(result / 1024) + " seconds";
            }
            return commandText;
        } else {
            commandText = "<b style=\"color: purple;\">MIDI COMMAND " + String.valueOf(msg.getCommand()) + "</b>";
            byte[] data = msg.getMessage();
            data1Value = "[" + String.valueOf(Byte.toUnsignedInt(data[0]));
            for(int i = 1; i < msg.getLength(); i++)
            {
                data1Value += ", " + String.valueOf(Byte.toUnsignedInt(data[i]));
            }
            data1Value += "]";
            return channelText + " " + commandText + " " + data1Value;
        }
    }

    public void printException(Exception e)
    {
        println("Exception - " + e.toString());
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            println(baos.toString());
        } catch (Exception e2) {}
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
                        if (table.getElementCount() > 5000)
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
                    final ArrayList<Runnable> taskArray = new ArrayList<Runnable>();
                    this.taskQueue.drainTo(taskArray);
                    SwingUtilities.invokeAndWait(() -> {
                        for(Iterator<Runnable> taskIterator = taskArray.iterator(); taskIterator.hasNext();)
                        {
                            Runnable swingTask = taskIterator.next();
                            swingTask.run();
                        }
                    });
                } else {
                    Thread.sleep(10);
                }
                maybeScrollToBottom();
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
        this.lastTxAt = System.currentTimeMillis();
        if (message instanceof ShortMessage && this.opened)
        {
            this.txCount++;
            ShortMessage smsg = (ShortMessage) message;
            if (smsg.getStatus() == ShortMessage.TIMING_CLOCK)
            {
                if (this.beatPulse >= 24)
                {
                    this.beatPulse = 0;
                }
                this.beatPulse++;
            } else {
                this.println(shortMessageToString(smsg));
            }
        }
    }

    public long getLastRxAt()
    {
        return 0;
    }

    public long getLastTxAt()
    {
        return this.lastTxAt;
    }
    
    public String toString()
    {
        return this.name;
    }

    @Override
    public void focusGained(FocusEvent e) {
        this.hasFocus = true;
        
    }

    @Override
    public void focusLost(FocusEvent e) {
        this.hasFocus = false;
        
    }

    public String getCCName(int channel, int cc)
    {
        return null;
    }

    @Override
    public long getRxCount() {
        return this.rxCount;
    }

    @Override
    public long getTxCount() {
        return this.txCount;
    }
}
