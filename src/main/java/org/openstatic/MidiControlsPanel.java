package org.openstatic;

import org.openstatic.midi.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.BorderLayout;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JPopupMenu;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.sound.midi.*;

public class MidiControlsPanel extends JPanel implements ActionListener, Receiver
{
    private long lastControlClick;
    protected JList<MidiControl> controlList;
    protected MidiControlCellRenderer midiControlCellRenderer;
    protected DefaultListModel<MidiControl> controls;
    private JPopupMenu controlMenuPopup;
    private JMenuItem deleteControlMenuItem;
    private JMenuItem renameControlMenuItem;
    private JMenuItem createRuleMenuItem;
    private JPanel buttonPanel;
    private JToggleButton listenForMidiButton;
    private JButton createControlButton;
    private JButton createRuleButton;
    private JButton deleteButton;

    public MidiControlsPanel()
    {
        super(new BorderLayout());
        this.controlMenuPopup = new JPopupMenu("control");
        
        this.deleteControlMenuItem = new JMenuItem("Delete Control");
        this.deleteControlMenuItem.setMnemonic(KeyEvent.VK_D);
        this.deleteControlMenuItem.addActionListener(this);
        this.deleteControlMenuItem.setActionCommand("delete_control");
        
        this.renameControlMenuItem = new JMenuItem("Rename Control");
        this.renameControlMenuItem.setMnemonic(KeyEvent.VK_R);
        this.renameControlMenuItem.addActionListener(this);
        this.renameControlMenuItem.setActionCommand("rename_control");
        
        this.createRuleMenuItem = new JMenuItem("Create Rule");
        this.createRuleMenuItem.setMnemonic(KeyEvent.VK_C);
        this.createRuleMenuItem.addActionListener(this);
        this.createRuleMenuItem.setActionCommand("create_rule");
        
        this.controlMenuPopup.add(this.renameControlMenuItem);
        this.controlMenuPopup.add(this.deleteControlMenuItem);
        this.controlMenuPopup.add(this.createRuleMenuItem);

        this.midiControlCellRenderer = new MidiControlCellRenderer();
        this.controls = new DefaultListModel<MidiControl>();
        this.controlList = new JList<MidiControl>(this.controls);
        this.controlList.setCellRenderer(this.midiControlCellRenderer);
        this.controlList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                int index = MidiControlsPanel.this.controlList.locationToIndex(e.getPoint());
                if (index != -1)
                {
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - MidiControlsPanel.this.lastControlClick < 500 && MidiControlsPanel.this.lastControlClick > 0)
                       {
                            MidiControlsPanel.this.controlMenuPopup.show(MidiControlsPanel.this.controlList, e.getX(), e.getY()); 
                       }
                       MidiControlsPanel.this.lastControlClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                     MidiControlsPanel.this.controlMenuPopup.show(MidiControlsPanel.this.controlList, e.getX(), e.getY()); 
                   }
               }
            }
        });
        this.controlList.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_DELETE)
                {
                    MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        (new Thread(() -> {
                            removeMidiControl(t);
                        })).start();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) { }

            @Override
            public void keyTyped(KeyEvent e) { }
        });

        
        JScrollPane controlsScrollPane = new JScrollPane(this.controlList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(controlsScrollPane, BorderLayout.CENTER);

        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon listenIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/listen32.png")));
            this.listenForMidiButton = new JToggleButton(listenIcon);
            this.listenForMidiButton.setToolTipText("Listen to controllers and create controls automatically");

            ImageIcon dialIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/dial32.png")));
            this.createControlButton = new JButton(dialIcon);
            this.createControlButton.setActionCommand("new_control");
            this.createControlButton.addActionListener(this);
            this.createControlButton.setToolTipText("Create new control");

            ImageIcon scriptIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/script32.png")));
            this.createRuleButton = new JButton(scriptIcon);
            this.createRuleButton.addActionListener(this);
            this.createRuleButton.setActionCommand("create_rule");
            this.createRuleButton.setToolTipText("Create rule for selected control");

            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_control");
            this.deleteButton.setToolTipText("Delete Selected Control");
        } catch (Exception e) {}
        this.listenForMidiButton.setSelected(false);
        this.buttonPanel.add(this.listenForMidiButton);
        this.buttonPanel.add(this.createControlButton);
        this.buttonPanel.add(this.createRuleButton);
        this.buttonPanel.add(this.deleteButton);
        this.add(buttonPanel, BorderLayout.WEST);
    }

    // Receiver Method
    public void send(MidiMessage msg, long timeStamp)
    {
        if(msg instanceof ShortMessage)
        {
            final ShortMessage sm = (ShortMessage) msg;
            /*
            if (sm.getData1() > 0)
                System.err.println("Recieved Short Message " + MidiPortManager.shortMessageToString(sm));
                */
            if (sm.getCommand() == ShortMessage.CONTROL_CHANGE || sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
            {
                boolean should_repaint = false;
                boolean found_control = false;
                for (Enumeration<MidiControl> mce = this.getControlsEnumeration(); mce.hasMoreElements();)
                {
                    MidiControl mc = mce.nextElement();
                    if (mc.messageMatches(sm))
                    {
                        //System.err.println(shortMessageToString(sm) + " = " + String.valueOf(sm.getData2()));
                        try
                        {
                            mc.processMessage(sm);
                            found_control = true;
                            should_repaint = true;
                        } catch (Exception e) {
                            MidiTools.instance.midi_logger_b.printException(e);
                        }
                    }
                }
                if (!found_control && this.listenForMidiButton.isSelected() && sm.getCommand() == ShortMessage.CONTROL_CHANGE)
                {
                    int channel = sm.getChannel()+1;
                    int cc = sm.getData1();
                    if (cc != 121 && cc != 123)
                    {
                        MidiControl mc = new MidiControl(channel,cc);
                        MidiTools.handleNewMidiControl(mc);
                    }
                }
                if (!found_control && this.listenForMidiButton.isSelected() && sm.getCommand() == ShortMessage.NOTE_ON)
                {
                    int channel = sm.getChannel()+1;
                    int note = sm.getData1() % 12;
                    MidiControl mc = new MidiControl(channel, note, true);
                    MidiTools.handleNewMidiControl(mc);
                }
                if (should_repaint)
                {
                    this.controlList.repaint();
                }
            }
        } else {
            MidiTools.logIt("Unknown non-short message " + msg.toString());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();

        if (cmd.equals("create_rule")) {
            MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
            MidiControlRule newRule = new MidiControlRule(t, 1, 0, null);
            MidiControlRuleEditor editor = new MidiControlRuleEditor(newRule, true);
        } else if (cmd.equals("rename_control")) {
            MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
            String s = (String) JOptionPane.showInputDialog(this,"Rename Control", t.getNickname());
            if (s!= null && t != null)
            {
                t.setNickname(s);
            }
        } else if (cmd.equals("delete_control")) {
            MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
            if (t != null)
            {
                (new Thread(() -> {
                    removeMidiControl(t);
                })).start();
            }
        } else if (cmd.equals("new_control")) {
            CreateControlDialog editr = new CreateControlDialog();
        }
        
    }

    public MidiControl elementAt(int i)
    {
        return this.controls.elementAt(i);
    }

    public int indexOf(MidiControl control)
    {
        return this.controls.indexOf(control);
    }

    public void removeListenerFromControls(MidiControlListener mcl)
    {
        for (Enumeration<MidiControl> mce = MidiControlsPanel.this.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            mc.removeMidiControlListener(mcl);
        }
    }

    public Enumeration<MidiControl> getControlsEnumeration()
    {
        return this.controls.elements();
    }

    public void insertElementAt(MidiControl control, int index)
    {
        this.controls.insertElementAt(control, index);
    }

    public void removeMidiControl(MidiControl mc)
    {
        MidiTools.logIt("Removed Midi Control: " + mc.getNickname());
        try
        {
            mc.removeAllListeners();
            SwingUtilities.invokeAndWait(() -> {
                MidiControlsPanel.this.controls.removeElement(mc);
            });
            for (Enumeration<MidiControlRule> re = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); re.hasMoreElements();)
            {
                MidiControlRule rule = re.nextElement();
                if (rule.getMidiControl() == mc)
                    rule.setMidiControl(null);
            }
            this.repaint();
        } catch (Exception e) {
            MidiTools.instance.midi_logger_b.printException(e);
        }
    }

    public void clear()
    {
        this.controls.clear();
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
}
