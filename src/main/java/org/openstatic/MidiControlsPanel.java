package org.openstatic;

import org.openstatic.midi.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.BorderLayout;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.sound.midi.*;

public class MidiControlsPanel extends JPanel implements ActionListener, Receiver
{
    protected JList<MidiControl> controlList;
    protected MidiControlCellRenderer midiControlCellRenderer;
    protected DefaultListModel<MidiControl> controls;
    private JPanel buttonPanel;
    private JToggleButton listenForMidiButton;
    private JButton createControlButton;
    private JButton createRuleButton;
    private JButton deleteButton;
    private JButton labelButton;
    private JButton selectAllButton;

    public MidiControlsPanel()
    {
        super(new BorderLayout());

        this.midiControlCellRenderer = new MidiControlCellRenderer();
        this.controls = new DefaultListModel<MidiControl>();
        this.controlList = new JList<MidiControl>(this.controls);
        this.controlList.setCellRenderer(this.midiControlCellRenderer);
        this.controlList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        t.manualAdjust(t.getValue()+1);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        t.manualAdjust(t.getValue()-1);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        if (t.getValue() != 127)
                            t.manualAdjust(127);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        if (t.getValue() == 127)
                            t.manualAdjust(0);
                        else
                            t.manualAdjust(127);
                    }
                }

            }

            @Override
            public void keyReleased(KeyEvent e) 
            {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    MidiControl t = (MidiControl) MidiControlsPanel.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        if (t.getValue() != 0)
                            t.manualAdjust(0);
                    }
                }
            }

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
            this.createRuleButton.setToolTipText("Create rule for selected controls");

            ImageIcon labelIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/label32.png")));
            this.labelButton = new JButton(labelIcon);
            this.labelButton.addActionListener(this);
            this.labelButton.setActionCommand("label_control");
            this.labelButton.setToolTipText("Rename selected controls");

            ImageIcon selectAllIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/selectall32.png")));
            this.selectAllButton = new JButton(selectAllIcon);
            this.selectAllButton.addActionListener(this);
            this.selectAllButton.setActionCommand("select_all");
            this.selectAllButton.setToolTipText("Select All");

            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_control");
            this.deleteButton.setToolTipText("Delete Selected Controls");
        } catch (Exception e) {}
        this.listenForMidiButton.setSelected(false);
        this.buttonPanel.add(this.listenForMidiButton);
        this.buttonPanel.add(this.createControlButton);
        this.buttonPanel.add(this.createRuleButton);
        this.buttonPanel.add(this.labelButton);
        this.buttonPanel.add(this.selectAllButton);
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

        if (cmd.equals("create_rule")) 
        {
            Collection<MidiControl> selectedControls = this.getSelectedControls();
            if (selectedControls.size() == 0)
            {
                
            } else {
                Iterator<MidiControl> controlIterator = selectedControls.iterator();
                int defaultEventMode = 0;
                int defaultActionType = 0;
                String defaultRuleGroupName = "all";
                String defaultActionValue = null;
                boolean defaultInverted = false;
                boolean defaultSettled = false;
                while (controlIterator.hasNext())
                {
                    MidiControl control = controlIterator.next();
                    MidiControlRule newRule = new MidiControlRule(control, defaultEventMode, defaultActionType, defaultActionValue);
                    newRule.setValueInverted(defaultInverted);
                    newRule.setValueSettled(defaultSettled);
                    newRule.setRuleGroup(defaultRuleGroupName);
                    MidiControlRuleEditor editor = new MidiControlRuleEditor(newRule, true);
                    defaultEventMode = newRule.getEventMode();
                    defaultActionType = newRule.getActionType();
                    defaultRuleGroupName = newRule.getRuleGroup();
                    defaultActionValue = newRule.getActionValue();
                    defaultInverted = newRule.isValueInverted();
                    defaultSettled = newRule.shouldValueSettle();
                }
                MidiControlsPanel.this.repaint();
            }
        } else if (cmd.equals("label_control")) {
            Collection<MidiControl> selectedControls = this.getSelectedControls();
            if (selectedControls.size() == 0)
            {
                
            } else {
                Iterator<MidiControl> controlIterator = selectedControls.iterator();
                while (controlIterator.hasNext())
                {
                    MidiControl control = controlIterator.next();
                    String s = (String) JOptionPane.showInputDialog(this,"Rename Control " + control.toString(), control.getNickname());
                    control.setNickname(s);
                }
                MidiControlsPanel.this.repaint();
            }
        } else if (cmd.equals("delete_control")) {
            Collection<MidiControl> selectedControls = this.getSelectedControls();
            if (selectedControls.size() == 0)
            {
                
            } else {
                Iterator<MidiControl> controlIterator = selectedControls.iterator();
                while (controlIterator.hasNext())
                {
                    MidiControl control = controlIterator.next();
                    this.removeMidiControl(control);
                }
                MidiControlsPanel.this.repaint();
            }
        } else if (cmd.equals("new_control")) {
            CreateControlDialog editr = new CreateControlDialog();
        } else if (cmd.equals("select_all")) {
            int rs = this.controls.size();
            int[] indices = new int[rs];
            for(int i = 0; i < rs; i++)
                indices[i] = i;
            this.controlList.setSelectedIndices(indices);
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

    public Collection<MidiControl> getSelectedControls()
    {
        return this.controlList.getSelectedValuesList();
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
            MidiControlsPanel.this.controls.removeElement(mc);
            for (Enumeration<MidiControlRule> re = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); re.hasMoreElements();)
            {
                MidiControlRule rule = re.nextElement();
                if (rule.getMidiControl() == mc)
                    rule.setMidiControl(null);
            }
            MidiTools.repaintControls();
        } catch (Exception e) {
            MidiTools.instance.midi_logger_b.printException(e);
        }
    }

    public void clear()
    {
        Enumeration<MidiControl> controlsEnum = this.controls.elements();
        Vector<MidiControl> removalVector = new Vector<MidiControl>();
        while(controlsEnum.hasMoreElements())
        {
            removalVector.add(controlsEnum.nextElement());
        }
        removalVector.forEach((control) -> {
            this.removeMidiControl(control);
        });
    }

    @Override
    public void close() {
        
    }
}
