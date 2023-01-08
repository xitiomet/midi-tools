package org.openstatic;

import org.json.JSONObject;
import org.openstatic.midi.*;
import org.openstatic.midi.ports.MidiRandomizerPort;
import org.openstatic.util.JSONObjectDialog;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RandomizerControlBox extends JPanel implements ActionListener
{
    private JList<JSONObject> randomizerRuleList;
    private RandomizerRuleCellRenderer randomizerRuleCellRenderer;
    private long lastMappingClick;
    private JPanel buttonPanel;
    private JButton createRuleButton;
    private MidiRandomizerPort randomizerPort;
    private JButton selectAllButton;
    private JButton disableAllButton;
    private JButton enableAllButton;
    private JButton deleteButton;

    public RandomizerControlBox(MidiRandomizerPort randomizerPort)
    {
        super(new BorderLayout());
        this.randomizerPort = randomizerPort;
        this.randomizerRuleCellRenderer = new RandomizerRuleCellRenderer();
        this.randomizerRuleList = new JList<JSONObject>(new RandomizerListModel(this.randomizerPort));
        this.randomizerRuleList.setOpaque(true);
        this.randomizerRuleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.randomizerRuleList.setCellRenderer(this.randomizerRuleCellRenderer);
        this.randomizerRuleList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = RandomizerControlBox.this.randomizerRuleList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                   JSONObject rule = (JSONObject) RandomizerControlBox.this.randomizerRuleList.getModel().getElementAt(index);
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - RandomizerControlBox.this.lastMappingClick < 500 && RandomizerControlBox.this.lastMappingClick > 0)
                       {
                          RandomizerControlBox.this.randomizerPort.toggleRandomRule(index);
                       }
                       RandomizerControlBox.this.lastMappingClick = cms;
                   }
                   RandomizerControlBox.this.repaint();
               }
            }
        });
        JScrollPane mappingScrollPane = new JScrollPane(this.randomizerRuleList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon diceIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/dice32.png")));
            this.createRuleButton = new JButton(diceIcon);
            this.createRuleButton.setActionCommand("create");
            this.createRuleButton.setToolTipText("Create a new rule for the Randomizer Device");
            this.createRuleButton.addActionListener(this);
            this.buttonPanel.add(this.createRuleButton);

            ImageIcon selectAllIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/selectall32.png")));
            this.selectAllButton = new JButton(selectAllIcon);
            this.selectAllButton.addActionListener(this);
            this.selectAllButton.setActionCommand("select_all");
            this.selectAllButton.setToolTipText("Select All");
            this.buttonPanel.add(this.selectAllButton);

            ImageIcon disableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/disable32.png")));
            this.disableAllButton = new JButton(disableIcon);
            this.disableAllButton.addActionListener(this);
            this.disableAllButton.setActionCommand("disable_selected");
            this.disableAllButton.setToolTipText("Disable selected randomizer rules");
            this.buttonPanel.add(this.disableAllButton);

            ImageIcon enableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/enable32.png")));
            this.enableAllButton = new JButton(enableIcon);
            this.enableAllButton.addActionListener(this);
            this.enableAllButton.setActionCommand("enable_selected");
            this.enableAllButton.setToolTipText("Enable selected randomizer rules");
            this.buttonPanel.add(this.enableAllButton);

            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_selected");
            this.deleteButton.setToolTipText("Delete Selected randomizer rules");
            this.buttonPanel.add(this.deleteButton);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.add(mappingScrollPane, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.WEST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.createRuleButton) {
            JSONObject newRule = MidiRandomizerPort.defaultRuleJSONObject();
            JSONObjectDialog jod = new JSONObjectDialog("New Randomizer Rule", newRule);
            JSONObject returnRule = jod.getJSONObject();
            if (returnRule != null)
            {
                this.randomizerPort.addRandomRule(returnRule);
            } else {
                System.err.println("Return rule was null");
            }
        } else if (e.getSource() == this.enableAllButton) {
            Collection<JSONObject> selectedMappings = this.getSelectedRules();
            if (selectedMappings.size() == 0)
            {
                
            } else {
                Iterator<JSONObject> mIterator = selectedMappings.iterator();
                while (mIterator.hasNext())
                {
                    JSONObject rule = mIterator.next();
                    rule.put("enabled", true);
                }
                RandomizerControlBox.this.repaint();
            }
        } else if (e.getSource() == this.disableAllButton) {
            Collection<JSONObject> selectedRules = this.getSelectedRules();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<JSONObject> mIterator = selectedRules.iterator();
                while (mIterator.hasNext())
                {
                    JSONObject rule = mIterator.next();
                    rule.put("enabled", false);
                }
                RandomizerControlBox.this.repaint();
            }
        } else if (e.getSource() == this.deleteButton) {
            Collection<JSONObject> selectedRules = this.getSelectedRules();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<JSONObject> rIterator = selectedRules.iterator();
                while (rIterator.hasNext())
                {
                    JSONObject rule = rIterator.next();
                    this.randomizerPort.removeRandomRule(rule);
                }
                RandomizerControlBox.this.repaint();
            }
        } else if (e.getSource() == this.selectAllButton) {
            int rs = randomizerPort.getAllRules().length();
            int[] indices = new int[rs];
            for(int i = 0; i < rs; i++)
                indices[i] = i;
            this.randomizerRuleList.setSelectedIndices(indices);
        }
        
    }

    public Collection<JSONObject> getSelectedRules()
    {
        return this.randomizerRuleList.getSelectedValuesList();
    }
}
