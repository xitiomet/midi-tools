package org.openstatic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.io.File;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;

public class MidiControlRulePanel extends JPanel implements ActionListener 
{
    protected DefaultListModel<MidiControlRule> rules;
    protected MidiControlRuleCellRenderer midiControlRuleCellRenderer;
    private JList<MidiControlRule> rulesList;
    private long lastRuleClick;
    private JPanel buttonPanel;
    private JButton createRuleButton;
    private JButton selectAllButton;
    private JButton disableAllButton;
    private JButton enableAllButton;
    private JButton deleteButton;
    private JButton labelButton;

    public MidiControlRulePanel()
    {
        super(new BorderLayout());
        this.rules = new DefaultListModel<MidiControlRule>();
        this.midiControlRuleCellRenderer = new MidiControlRuleCellRenderer();
        // Setup rule list
        this.rulesList = new JList<MidiControlRule>(this.rules);
        this.rulesList.setOpaque(true);
        this.rulesList.setBackground(Color.WHITE);
        this.rulesList.setDropTarget(this.drop_targ);
        this.rulesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.rulesList.setCellRenderer(this.midiControlRuleCellRenderer);
        JScrollPane ruleScrollPane = new JScrollPane(this.rulesList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.rulesList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
            int index = MidiControlRulePanel.this.rulesList.locationToIndex(e.getPoint());

            if (index != -1)
            {
                MidiControlRule source = (MidiControlRule) MidiControlRulePanel.this.rules.getElementAt(index);
                if (e.getButton() == MouseEvent.BUTTON1)
                {
                    long cms = System.currentTimeMillis();
                    if (cms - MidiControlRulePanel.this.lastRuleClick < 500 && MidiControlRulePanel.this.lastRuleClick > 0)
                    {
                        source.toggleEnabled();
                    }
                    MidiControlRulePanel.this.lastRuleClick = cms;
                } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                    MidiControlRuleEditor editor = new MidiControlRuleEditor(source);
                }
            }
            }
        });
        this.add(ruleScrollPane, BorderLayout.CENTER);

        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon scriptIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/script32.png")));
            this.createRuleButton = new JButton(scriptIcon);
            this.createRuleButton.addActionListener(this);
            this.createRuleButton.setActionCommand("create_rule");
            this.createRuleButton.setToolTipText("Create a new rule");

            ImageIcon labelIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/label32.png")));
            this.labelButton = new JButton(labelIcon);
            this.labelButton.addActionListener(this);
            this.labelButton.setActionCommand("edit_rules");
            this.labelButton.setToolTipText("Edit selected Rules");

            ImageIcon selectAllIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/selectall32.png")));
            this.selectAllButton = new JButton(selectAllIcon);
            this.selectAllButton.addActionListener(this);
            this.selectAllButton.setActionCommand("select_all");
            this.selectAllButton.setToolTipText("Select All");

            ImageIcon disableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/disable32.png")));
            this.disableAllButton = new JButton(disableIcon);
            this.disableAllButton.addActionListener(this);
            this.disableAllButton.setActionCommand("disable_selected");
            this.disableAllButton.setToolTipText("Disable selected Rules");

            ImageIcon enableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/enable32.png")));
            this.enableAllButton = new JButton(enableIcon);
            this.enableAllButton.addActionListener(this);
            this.enableAllButton.setActionCommand("enable_selected");
            this.enableAllButton.setToolTipText("Enable selected Rules");

            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_selected");
            this.deleteButton.setToolTipText("Delete selected Rules");
        } catch (Exception e) {}
        this.buttonPanel.add(this.createRuleButton);
        this.buttonPanel.add(this.labelButton);
        this.buttonPanel.add(this.selectAllButton);
        this.buttonPanel.add(this.disableAllButton);
        this.buttonPanel.add(this.enableAllButton);
        this.buttonPanel.add(this.deleteButton);
        this.add(buttonPanel, BorderLayout.WEST);
    }

    private DropTarget drop_targ = new DropTarget()
    {
        public synchronized void drop(DropTargetDropEvent evt)
        {
            try
            {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                final List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (droppedFiles.size() == 1)
                {
                    try
                    {
                        final File droppedFile = droppedFiles.get(0);
                        Thread t = new Thread(() -> {
                            MidiControlRulePanel.this.handleFileDrop(droppedFile);
                        });
                        t.start();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                } else {
                    for(int i = 0; i < droppedFiles.size(); i++)
                    {
                        final int fi = i;
                        Thread t = new Thread(() -> {
                            MidiControlRulePanel.this.handleFileDrop(droppedFiles.get(fi));
                        });
                        t.start();
                    }
                }
                System.err.println("Cleanly LEFT DROP Routine");
                evt.dropComplete(true);
            } catch (Exception ex) {
                evt.dropComplete(true);
                System.err.println("Exception During DROP Routine");
                ex.printStackTrace();
            }
        }
    };

    public void handleFileDrop(File file)
    {
        System.err.println("File dropped: " + file.toString());
        String filename = file.getName();
        String filenameLower = filename.toLowerCase();
        if (filenameLower.endsWith(".wav"))
        {
            MidiControlRule newRule = new MidiControlRule(null, MidiControlRule.EVENT_INCREASE, MidiControlRule.ACTION_SOUND, MidiTools.addProjectAsset(file).getName());
            newRule.setNickname(filename.substring(0, filename.length()-4));
            if (!MidiControlRulePanel.this.contains(newRule))
                MidiControlRulePanel.this.addElement(newRule);
        }
        if (filenameLower.endsWith(".exe") || filenameLower.endsWith(".bat") || filenameLower.endsWith(".cmd") || filenameLower.endsWith(".php"))
        {
            MidiControlRule newRule = new MidiControlRule(null, MidiControlRule.EVENT_SETTLE, MidiControlRule.ACTION_PROC, file.getAbsolutePath() + ",{{value}}");
            newRule.setNickname(filename.substring(0, filename.length()-4));
            if (!MidiControlRulePanel.this.contains(newRule))
                MidiControlRulePanel.this.addElement(newRule);
        }
    }
    

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();

        if (cmd.equals("create_rule")) {
            MidiControlRule newRule = new MidiControlRule(null, 1, 0, null);
            MidiControlRuleEditor editor = new MidiControlRuleEditor(newRule, true);
        } else if (cmd.equals("enable_selected")) {
            Collection<MidiControlRule> selectedRules = this.getSelectedRules();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<MidiControlRule> rIterator = selectedRules.iterator();
                while (rIterator.hasNext())
                {
                    MidiControlRule rule = rIterator.next();
                    rule.setEnabled(true);
                }
            }
        } else if (cmd.equals("disable_selected")) {
            Collection<MidiControlRule> selectedRules = this.getSelectedRules();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<MidiControlRule> rIterator = selectedRules.iterator();
                while (rIterator.hasNext())
                {
                    MidiControlRule rule = rIterator.next();
                    rule.setEnabled(false);
                }
            }
        } else if (cmd.equals("delete_selected")) {
            Collection<MidiControlRule> selectedRules = this.getSelectedRules();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<MidiControlRule> rIterator = selectedRules.iterator();
                while (rIterator.hasNext())
                {
                    MidiControlRule rule = rIterator.next();
                    this.removeElement(rule);
                }
            }
        } else if (cmd.equals("select_all")) {
            int rs = this.rules.size();
            int[] indices = new int[rs];
            for(int i = 0; i < rs; i++)
                indices[i] = i;
            this.rulesList.setSelectedIndices(indices);
        } else if (cmd.equals("edit_rules")) {
            Collection<MidiControlRule> selectedRules = this.getSelectedRules();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<MidiControlRule> rIterator = selectedRules.iterator();
                while (rIterator.hasNext())
                {
                    MidiControlRule rule = rIterator.next();
                    MidiControlRuleEditor editor = new MidiControlRuleEditor(rule);
                }
            }
        }
        
    }

    public MidiControlRule elementAt(int i)
    {
        return this.rules.elementAt(i);
    }

    public int indexOf(MidiControlRule rule)
    {
        return this.rules.indexOf(rule);
    }

    public Enumeration<MidiControlRule> getRulesEnumeration()
    {
        return this.rules.elements();
    }

    public Collection<MidiControlRule> getSelectedRules()
    {
        return this.rulesList.getSelectedValuesList();
    }

    public void addElement(MidiControlRule rule)
    {
        this.rules.addElement(rule);
    }

    public boolean removeElement(MidiControlRule rule)
    {
        rule.setEnabled(false);
        MidiTools.removeListenerFromControls(rule);
        return this.rules.removeElement(rule);
    }

    public boolean contains(MidiControlRule rule)
    {
        return this.rules.contains(rule);
    }

    public void clear()
    {
        Enumeration<MidiControlRule> rulesEnum = this.rules.elements();
        Vector<MidiControlRule> removalVector = new Vector<MidiControlRule>();
        while(rulesEnum.hasMoreElements())
        {
            removalVector.add(rulesEnum.nextElement());
        }
        removalVector.forEach((rule) -> {
            this.removeElement(rule);
        });
    }
}
