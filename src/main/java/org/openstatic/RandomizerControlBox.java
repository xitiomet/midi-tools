package org.openstatic;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openstatic.midi.*;
import org.openstatic.midi.ports.MidiRandomizerPort;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RandomizerControlBox extends JPanel implements ActionListener, MidiPortListener
{
    private JList<JSONObject> randomizerRuleList;
    private RandomizerRuleCellRenderer randomizerRuleCellRenderer;
    private long lastMappingClick;
    private JPanel buttonPanel;
    private JButton clearMappingsButton;
    private JToggleButton connectButton;
    private JButton createRuleButton;
    private MidiRandomizerPort randomizerPort;

    public RandomizerControlBox(MidiRandomizerPort randomizerPort)
    {
        super(new BorderLayout());
        MidiPortManager.addMidiPortListener(this);
        this.randomizerPort = randomizerPort;
        this.randomizerRuleCellRenderer = new RandomizerRuleCellRenderer();
        this.randomizerRuleList = new JList<JSONObject>(new RandomizerListModel(this.randomizerPort));
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
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      int n = JOptionPane.showConfirmDialog(null, "Delete this randomizer rule?\n" + rule.toString(),
                        "Port Mapping",
                        JOptionPane.YES_NO_OPTION);
                        if(n == JOptionPane.YES_OPTION)
                        {
                            RandomizerControlBox.this.randomizerPort.removeRandomRule(rule);
                        }
                   }
                   RandomizerControlBox.this.repaint();
               }
            }
        });
        JScrollPane mappingScrollPane = new JScrollPane(this.randomizerRuleList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mappingScrollPane.setBorder(new TitledBorder("Randomizer Port Rules (right-click to delete, double-click to toggle)"));
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon diceIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/dice32.png")));
            this.createRuleButton = new JButton(diceIcon);
            this.createRuleButton.setActionCommand("create");
            this.createRuleButton.addActionListener(this);
            this.buttonPanel.add(this.createRuleButton);

            ImageIcon plugInIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/plug_in.png")));
            this.connectButton = new JToggleButton(plugInIcon);
            this.connectButton.setSelected(this.randomizerPort.isOpened());
            ChangeListener changeListener = new ChangeListener() {
              public void stateChanged(ChangeEvent changeEvent) {
                AbstractButton abstractButton = (AbstractButton) changeEvent.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
                boolean armed = buttonModel.isArmed();
                boolean pressed = buttonModel.isPressed();
                boolean selected = buttonModel.isSelected();
                //System.out.println("Changed: " + armed + "/" + pressed + "/" + selected);
                if (RandomizerControlBox.this.randomizerPort.isOpened() != selected)
                {
                    if (selected)
                    {
                        RandomizerControlBox.this.randomizerPort.open();
                    } else {
                        RandomizerControlBox.this.randomizerPort.close();
                    }
                }
              }
            };
            this.connectButton.addChangeListener(changeListener);
            this.buttonPanel.add(this.connectButton);

            ImageIcon eraseIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/erase.png")));
            this.clearMappingsButton = new JButton(eraseIcon);
            this.clearMappingsButton.setActionCommand("clear");
            this.clearMappingsButton.addActionListener(this);
            this.buttonPanel.add(this.clearMappingsButton);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.add(mappingScrollPane, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.WEST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.clearMappingsButton)
        {
            int n = JOptionPane.showConfirmDialog(null,
            "Are you sure you wish to clear all randomizer rules?",
            "Reset Mappings",
            JOptionPane.YES_NO_OPTION);
            if(n == JOptionPane.YES_OPTION)
            {
                this.randomizerPort.clearAllRules();
            }
        } else if (e.getSource() == this.createRuleButton) {
            JSONObject newRule = MidiRandomizerPort.defaultRuleJSONObject();
            JSONObjectDialog jod = new JSONObjectDialog("New Randomizer Rule", newRule);
            JSONObject returnRule = jod.getJSONObject();
            if (returnRule != null)
            {
                this.randomizerPort.addRandomRule(returnRule);
            } else {
                System.err.println("Return rule was null");
            }
        }
        
    }

    @Override
    public void portAdded(int idx, MidiPort port) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void portRemoved(int idx, MidiPort port) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void portOpened(MidiPort port) {
        // TODO Auto-generated method stub
        if (port.equals(this.randomizerPort))
        {
            if (port.isOpened() != this.connectButton.isSelected())
            {
                this.connectButton.setSelected(port.isOpened());
            }
        }
    }

    @Override
    public void portClosed(MidiPort port) {
        // TODO Auto-generated method stub
        if (port.equals(this.randomizerPort))
        {
            if (port.isOpened() != this.connectButton.isSelected())
            {
                this.connectButton.setSelected(port.isOpened());
            }
        }
    }

    @Override
    public void mappingAdded(int idx, MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mappingRemoved(int idx, MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mappingOpened(MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mappingClosed(MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }
}
