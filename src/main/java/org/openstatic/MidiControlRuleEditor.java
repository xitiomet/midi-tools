package org.openstatic;

import org.openstatic.midi.*;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.EtchedBorder;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import java.io.File;

public class MidiControlRuleEditor extends JDialog implements ActionListener
{
    private MidiControlRule rule;
    private JComboBox<String> eventSelector;
    private JComboBox<MidiControl> controlSelector;
    private JComboBox<String> pluginSelector;
    private JComboBox<String> pluginTargetSelector;
    private JComboBox<String> showImageModeSelector;
    private JCheckBox soloImageCheckBox;
    private JPanel imageOptionsPanel;
    private JComboBox<String> fillOptions;

    private JComboBox<Integer> actionSelector;
    private JTextField nicknameField;
    private JComboBox<String> ruleGroupField;
    private JComboBox<String> canvasSelectorField;

    private JTextArea actionValueField;
    
    private JComboBox<String> deviceSelectAVF;
    private JComboBox<String> channelSelectAVF;
    private JComboBox<String> selectRuleGroupDropdown;
    private JComboBox<MidiPortMapping> selectMappingDropdown;
    private JLabel ccLabel;
    private JTextField ccAVF;
    private JTextField valueAVF;
    private JTextField zIndexField;
    private JPanel transmitMidiPanel;
    private JCheckBox valueInvertedCheckBox;
    private JCheckBox valueSettledCheckBox;


    private JPanel modifierPanel;
    private JPanel selectFilePanel;
    private JButton selectFileButton;
    private JComboBox<String> selectFileField;
    private JLabel actionValueLabel;
    private JPanel actionValuePanel;

    private JButton saveButton;
    private JButton deleteButton;

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.eventSelector)
        {
            this.rule.setEventMode(this.eventSelector.getSelectedIndex());
        }
        
        if (e.getSource() == this.selectFileButton)
        {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a file");   
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                File fileToLoad = fileChooser.getSelectedFile();
                if (fileToLoad != null)
                {
                    File assetFile = MidiTools.addProjectAsset(fileToLoad);
                    String filename = assetFile.getName();
                    this.selectFileField.setSelectedItem(filename);
                    if (((Integer)this.actionSelector.getSelectedItem()) == MidiControlRule.ACTION_EFFECT_IMAGE)
                        this.actionValueField.setText(filename + "," + getImageEffects());
                    else
                        this.actionValueField.setText(filename);
                }
            }
        }

        if (e.getSource() == this.showImageModeSelector || e.getSource() == this.soloImageCheckBox || e.getSource() == this.fillOptions || e.getSource() == this.zIndexField)
        {
            this.actionValueField.setText(this.selectFileField.getSelectedItem().toString() + "," + getImageEffects());
        }

        if (e.getSource() == this.pluginSelector)
        {
            String pluginName = this.pluginSelector.getSelectedItem().toString();
            MidiToolsPlugin plugin = MidiTools.instance.plugins.get(pluginName);
            this.pluginTargetSelector.setModel(this.getPluginTargetModel(plugin));
            this.actionValueField.setText(plugin.getTitle() + "," + this.pluginTargetSelector.getSelectedItem().toString());
        }

        if (e.getSource() == this.selectFileField)
        {
            String fileName = this.selectFileField.getSelectedItem().toString();
            if (((Integer)this.actionSelector.getSelectedItem()) == MidiControlRule.ACTION_EFFECT_IMAGE)
                this.actionValueField.setText(fileName + "," + getImageEffects());
            else
                this.actionValueField.setText(fileName);
        }

        if (e.getSource() == this.pluginTargetSelector)
        {
            String pluginName = this.pluginSelector.getSelectedItem().toString();
            MidiToolsPlugin plugin = MidiTools.instance.plugins.get(pluginName);
            this.actionValueField.setText(plugin.getTitle() + "," + this.pluginTargetSelector.getSelectedItem().toString());
        }
        
        if (e.getSource() == this.actionSelector)
        {
            int avi = (Integer) this.actionSelector.getSelectedItem();
            this.rule.setActionType(avi);
            this.changeActionSelector(avi);
        }
        
        if (e.getSource() == this.deviceSelectAVF || e.getSource() == this.channelSelectAVF)
        {
            this.actionValueField.setText(this.deviceSelectAVF.getSelectedItem() + "," + this.channelSelectAVF.getSelectedItem() + 
                                          "," + this.ccAVF.getText() + "," + this.valueAVF.getText());
        }
        
        if (e.getSource() == this.selectRuleGroupDropdown)
        {
            this.actionValueField.setText(this.selectRuleGroupDropdown.getSelectedItem().toString());
        }

        if (e.getSource() == this.selectMappingDropdown)
        {
            MidiPortMapping selectedMapping = (MidiPortMapping) this.selectMappingDropdown.getSelectedItem();
            this.actionValueField.setText(selectedMapping.getMappingId());
        }
        
        if (e.getSource() == this.deleteButton)
        {
            if (MidiTools.instance.midiControlRulePanel.contains(this.rule))
                MidiTools.instance.midiControlRulePanel.removeElement(this.rule);
            MidiTools.removeListenerFromControls(this.rule);
            this.dispose();
        }
        
        if (e.getSource() == this.saveButton)
        {
            if (!"".equals(this.nicknameField.getText()))
                this.rule.setNickname(this.nicknameField.getText());
            else
                this.rule.setNickname(null);
            if (!"".equals(this.ruleGroupField.getSelectedItem()))
                this.rule.setRuleGroup(this.ruleGroupField.getSelectedItem().toString());
            else
                this.rule.setRuleGroup("all");
            if (this.canvasSelectorField.getSelectedItem() != null)
            {
                if (!"".equals(this.canvasSelectorField.getSelectedItem()))
                    this.rule.setCanvasName(this.canvasSelectorField.getSelectedItem().toString());
                else
                    this.rule.setCanvasName(null);
            } else {
                this.rule.setCanvasName(null);
            }
            if (!"".equals(this.actionValueField.getText()))
                this.rule.setActionValue(this.actionValueField.getText());
            else
                this.rule.setActionValue(null);
            int ci = this.controlSelector.getSelectedIndex();
            if (ci == -1)
                this.rule.setMidiControl(null);
            else
                this.rule.setMidiControl(MidiTools.getMidiControlByIndex(ci));
            if (!MidiTools.instance.midiControlRulePanel.contains(this.rule))
                MidiTools.instance.midiControlRulePanel.addElement(this.rule);
            this.rule.setValueInverted(this.valueInvertedCheckBox.isSelected());
            this.rule.setValueSettled(this.valueSettledCheckBox.isSelected());
            this.rule.updateRule();
            this.dispose();
        }
    }

    private String getImageEffects()
    {
        String extras = "";
        if (this.soloImageCheckBox.isSelected())
            extras += " solo";
        String rs = this.showImageModeSelector.getSelectedItem().toString() + extras + " " + this.fillOptions.getSelectedItem().toString() + "," + this.zIndexField.getText();
        System.err.println("Image Effects: " + rs);
        return rs;
    }

    public void changeActionSelector(int i)
    {
        this.actionValuePanel.removeAll();
        //System.err.println("changeActionSelector: " + String.valueOf(i));
        if (i == MidiControlRule.ACTION_URL || i == MidiControlRule.LOGGER_A_MESSAGE || i == MidiControlRule.LOGGER_B_MESSAGE)
        {
            this.actionValuePanel.add(this.actionValueField, BorderLayout.CENTER);
            if (i == MidiControlRule.ACTION_URL)
            {
                this.actionValueLabel.setText("URL");
            } else if (i == MidiControlRule.LOGGER_A_MESSAGE || i == MidiControlRule.LOGGER_B_MESSAGE) {
                this.actionValueLabel.setText("Message to display");
            }
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
        } else if (i == MidiControlRule.ACTION_PROC || i == MidiControlRule.ACTION_SOUND || i == MidiControlRule.ACTION_EFFECT_IMAGE) {
            ArrayList<String> extens = new ArrayList<String>();
            if (i == MidiControlRule.ACTION_PROC)
            {
                this.actionValueLabel.setText("Program");
                extens.add(".cmd");
                extens.add(".bat");
                extens.add(".php");
                extens.add(".sh");
                extens.add(".exe");
            } else if (i == MidiControlRule.ACTION_SOUND) {
                this.actionValueLabel.setText("Asset Filename");
                extens.add(".wav");
                this.canvasSelectorField.setEnabled(true);
                this.canvasSelectorField.setSelectedItem(this.rule.getCanvasName());
            } else if (i == MidiControlRule.ACTION_EFFECT_IMAGE) {
                this.actionValueLabel.setText("Asset Filename");
                extens.add(".png");
                extens.add(".gif");
                extens.add(".jpg");
                extens.add(".jpeg");
                extens.add(".webp");
                extens.add(".svg");
                this.canvasSelectorField.setEnabled(true);
                this.canvasSelectorField.setSelectedItem(this.rule.getCanvasName());
            }
            this.actionValuePanel.add(this.selectFilePanel, BorderLayout.CENTER);
            this.selectFileField.setModel(MidiTools.getAssetComboBoxModel(extens));
            if (i == MidiControlRule.ACTION_EFFECT_IMAGE)
            {
                this.actionValuePanel.add(this.imageOptionsPanel, BorderLayout.PAGE_END);
                StringTokenizer st = new StringTokenizer(this.actionValueField.getText(), ",");
                if (st.hasMoreTokens())
                {
                    String filename = st.nextToken();
                    this.selectFileField.setSelectedItem(filename);
                }
                if (st.hasMoreTokens())
                {
                    String mode = st.nextToken();
                    this.showImageModeSelector.setSelectedItem(mode.replace(" solo", "").replace(" fill-x", "").replace(" fill-y", ""));
                    this.soloImageCheckBox.setSelected(mode.contains("solo"));
                    if (mode.contains("fill-x"))
                        this.fillOptions.setSelectedItem("fill-x");
                    else
                        this.fillOptions.setSelectedItem("fill-y");
                }
                if (st.hasMoreTokens())
                {
                    String zIn = st.nextToken();
                    this.zIndexField.setText(zIn);
                }
                Object selectedItem = this.selectFileField.getSelectedItem();
                if (selectedItem != null)
                    this.actionValueField.setText(selectedItem.toString() + "," + getImageEffects());
            } else {
                this.selectFileField.setSelectedItem(this.actionValueField.getText());
                Object selectedItem = this.selectFileField.getSelectedItem();
                if (selectedItem != null)
                    this.actionValueField.setText(selectedItem.toString());
            }
        } else if (i == MidiControlRule.ACTION_TRANSMIT) {
            this.actionValueLabel.setText("MIDI Message");
            refreshDevices();
            StringTokenizer st = new StringTokenizer(this.actionValueField.getText(), ",");
            if (st.countTokens() == 4)
            {
                String devName = st.nextToken();
                String chanNum = st.nextToken();
                String ccNum = st.nextToken();
                String val = st.nextToken();
                this.deviceSelectAVF.setSelectedItem(devName);
                this.channelSelectAVF.setSelectedItem(chanNum);
                this.ccAVF.setText(ccNum);
                this.valueAVF.setText(val);
            } else {
                this.deviceSelectAVF.setSelectedIndex(0);
                this.channelSelectAVF.setSelectedIndex(0);
                this.ccAVF.setText("{{cc}}");
                this.valueAVF.setText("{{value}}");                
            }
            this.valueAVF.setEnabled(true);
            this.ccLabel.setText("CC");
            this.actionValuePanel.add(this.transmitMidiPanel, BorderLayout.CENTER);
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
        } else if (i == MidiControlRule.ACTION_TRANSMIT_NOTE_ON) {
            this.actionValueLabel.setText("MIDI Message");
            refreshDevices();
            StringTokenizer st = new StringTokenizer(this.actionValueField.getText(), ",");
            if (st.countTokens() == 4)
            {
                String devName = st.nextToken();
                String chanNum = st.nextToken();
                String ccNum = st.nextToken();
                String val = st.nextToken();
                this.deviceSelectAVF.setSelectedItem(devName);
                this.channelSelectAVF.setSelectedItem(chanNum);
                this.ccAVF.setText(ccNum);
                this.valueAVF.setText(val);
            } else {
                this.deviceSelectAVF.setSelectedIndex(0);
                this.channelSelectAVF.setSelectedIndex(0);
                this.ccAVF.setText("{{note}}");
                this.valueAVF.setText("{{value}}");                
            }
            this.valueAVF.setEnabled(true);
            this.ccLabel.setText("NOTE#");
            this.actionValuePanel.add(this.transmitMidiPanel, BorderLayout.CENTER);
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
        } else if (i == MidiControlRule.ACTION_TRANSMIT_NOTE_OFF) {
            this.actionValueLabel.setText("MIDI Message");
            refreshDevices();
            StringTokenizer st = new StringTokenizer(this.actionValueField.getText(), ",");
            if (st.countTokens() == 4)
            {
                String devName = st.nextToken();
                String chanNum = st.nextToken();
                String ccNum = st.nextToken();
                String val = st.nextToken();
                this.deviceSelectAVF.setSelectedItem(devName);
                this.channelSelectAVF.setSelectedItem(chanNum);
                this.ccAVF.setText(ccNum);
                this.valueAVF.setText("0");
            } else {
                this.deviceSelectAVF.setSelectedIndex(0);
                this.channelSelectAVF.setSelectedIndex(0);
                this.ccAVF.setText("{{note}}");
                this.valueAVF.setText("0");                
            }
            this.valueAVF.setEnabled(false);
            this.ccLabel.setText("NOTE#");
            this.actionValuePanel.add(this.transmitMidiPanel, BorderLayout.CENTER);
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
        } else if (i == MidiControlRule.ACTION_DISABLE_RULE_GROUP || i == MidiControlRule.ACTION_ENABLE_RULE_GROUP || i == MidiControlRule.ACTION_TOGGLE_RULE_GROUP) {
            this.actionValueLabel.setText("Rule Group");
            this.selectRuleGroupDropdown.setModel(getRuleGroupModel());
            this.selectRuleGroupDropdown.setSelectedItem(this.actionValueField.getText());
            this.actionValuePanel.add(this.selectRuleGroupDropdown, BorderLayout.CENTER);
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
        } else if (i == MidiControlRule.ACTION_DISABLE_MAPPING || i == MidiControlRule.ACTION_ENABLE_MAPPING || i == MidiControlRule.ACTION_TOGGLE_MAPPING) {
            this.actionValueLabel.setText("Port Mapping");
            this.selectMappingDropdown.setSelectedItem(this.actionValueField.getText());
            this.actionValuePanel.add(this.selectMappingDropdown, BorderLayout.CENTER);
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
            MidiPortMapping selectedMapping = (MidiPortMapping) this.selectMappingDropdown.getSelectedItem();
            this.actionValueField.setText(selectedMapping.getMappingId());
        } else if (i == MidiControlRule.ACTION_PLUGIN) {
            try
            {
                StringTokenizer st = new StringTokenizer(this.actionValueField.getText(), ",");
                this.actionValueLabel.setText("Plugin");
                this.pluginSelector.setModel(this.getPluginModel());
                if (st.countTokens() == 2)
                {
                    String pluginName = st.nextToken();
                    String targetName = st.nextToken();
                    MidiToolsPlugin plugin = MidiTools.instance.plugins.get(pluginName);
                    this.pluginTargetSelector.setModel(this.getPluginTargetModel(plugin));
                    this.pluginSelector.setSelectedItem(pluginName);
                    this.pluginTargetSelector.setSelectedItem(targetName);
                    this.actionValueField.setText(pluginName + "," + targetName);
                } else {
                    String firstPlugin = this.pluginSelector.getSelectedItem().toString();
                    MidiToolsPlugin plugin = MidiTools.instance.plugins.get(firstPlugin);
                    this.pluginTargetSelector.setModel(this.getPluginTargetModel(plugin));
                    try
                    {
                        this.actionValueField.setText(firstPlugin + "," + this.pluginTargetSelector.getSelectedItem().toString());
                    } catch (Exception e2) {
                        e2.printStackTrace(System.err);
                    }
                }
                this.actionValuePanel.add(this.pluginSelector, BorderLayout.CENTER);
                this.actionValuePanel.add(this.pluginTargetSelector, BorderLayout.PAGE_END);
            } catch (Exception e) {
                MidiTools.instance.midi_logger_a.printException(e);
                this.actionValuePanel.removeAll();
                this.actionValuePanel.add(this.actionValueField, BorderLayout.CENTER);
            }
            this.canvasSelectorField.setEnabled(false);
            this.canvasSelectorField.setSelectedItem("");
        }
        this.actionValuePanel.revalidate();
        this.actionValuePanel.repaint();
    }

    public MidiControlRuleEditor(MidiControlRule rule)
    {
        this(rule, false);
    }
    
    public MidiControlRuleEditor(MidiControlRule rule, boolean newRule)
    {
        super(MidiTools.instance, "Rule Editor", true);
        this.setLayout(new BorderLayout());
        this.rule = rule;

        Vector<Integer> actionList = new Vector<Integer>();
        actionList.add(MidiControlRule.ACTION_PLUGIN);
        actionList.add(MidiControlRule.ACTION_EFFECT_IMAGE);
        actionList.add(MidiControlRule.ACTION_URL);
        actionList.add(MidiControlRule.ACTION_PROC);
        actionList.add(MidiControlRule.ACTION_SOUND);
        actionList.add(MidiControlRule.ACTION_TRANSMIT);
        actionList.add(MidiControlRule.ACTION_TRANSMIT_NOTE_ON);
        actionList.add(MidiControlRule.ACTION_TRANSMIT_NOTE_OFF);
        actionList.add(MidiControlRule.LOGGER_A_MESSAGE);
        actionList.add(MidiControlRule.LOGGER_B_MESSAGE);
        actionList.add(MidiControlRule.ACTION_ENABLE_RULE_GROUP);
        actionList.add(MidiControlRule.ACTION_DISABLE_RULE_GROUP);
        actionList.add(MidiControlRule.ACTION_TOGGLE_RULE_GROUP);
        actionList.add(MidiControlRule.ACTION_ENABLE_MAPPING);
        actionList.add(MidiControlRule.ACTION_DISABLE_MAPPING);
        actionList.add(MidiControlRule.ACTION_TOGGLE_MAPPING);


        Vector<String> eventModeList = new Vector<String>();
        for(int i = 0; i < 14; i++)
        {
            eventModeList.add(MidiControlRule.eventModeToString(i));
        }
        
        this.controlSelector = new JComboBox<MidiControl>();
        this.controlSelector.setEditable(false);
        this.controlSelector.addActionListener(this);
        this.controlSelector.setBackground(Color.WHITE);
        Vector<MidiControl> ctrls = new Vector<MidiControl>();
        for (Enumeration<MidiControl> cenum = MidiTools.instance.midiControlsPanel.getControlsEnumeration(); cenum.hasMoreElements();)
            ctrls.add(cenum.nextElement());
        this.controlSelector.setModel(new DefaultComboBoxModel<MidiControl>(ctrls));

        this.selectMappingDropdown = new JComboBox<MidiPortMapping>();
        this.selectMappingDropdown.setEditable(false);
        this.selectMappingDropdown.addActionListener(this);
        this.setBackground(Color.WHITE);
        Vector<MidiPortMapping> mappings = new Vector<MidiPortMapping>();
        for (Iterator<MidiPortMapping> mpmIterator = MidiPortManager.getMidiPortMappings().iterator(); mpmIterator.hasNext();)
            mappings.add(mpmIterator.next());
        this.selectMappingDropdown.setModel(new DefaultComboBoxModel<MidiPortMapping>(mappings));

        this.eventSelector = new JComboBox<String>(eventModeList);
        this.eventSelector.setEditable(false);
        this.eventSelector.addActionListener(this);
        this.eventSelector.setBackground(Color.WHITE);

        this.actionSelector = new JComboBox<Integer>(actionList);
        this.actionSelector.setEditable(false);
        this.actionSelector.setRenderer(new RuleActionCellRenderer());
        this.actionSelector.addActionListener(this);
        this.actionSelector.setBackground(Color.WHITE);
        
        this.pluginSelector = new JComboBox<String>();
        this.pluginSelector.setEditable(false);
        this.pluginSelector.addActionListener(this);
        this.pluginSelector.setBackground(Color.WHITE);

        this.pluginTargetSelector = new JComboBox<String>();
        this.pluginTargetSelector.setEditable(false);
        this.pluginTargetSelector.addActionListener(this);
        this.pluginTargetSelector.setBackground(Color.WHITE);

        this.soloImageCheckBox = new JCheckBox();
        this.soloImageCheckBox.addActionListener(this);

        this.showImageModeSelector = new JComboBox<String>();
        this.showImageModeSelector.setEditable(false);
        this.showImageModeSelector.addActionListener(this);
        this.showImageModeSelector.setBackground(Color.WHITE);
        
        Vector<String> imageModes = new Vector<String>();
        imageModes.add("opacity");
        imageModes.add("none");
        imageModes.add("scale");
        imageModes.add("rotate");
        imageModes.add("curtain");
        imageModes.add("riser");
        imageModes.add("opacity scale");
        imageModes.add("rotate scale");
        imageModes.add("opacity rotate");
        imageModes.add("opacity rotate scale");
        this.showImageModeSelector.setModel(new DefaultComboBoxModel<String>(imageModes));

        Vector<String> fills = new Vector<String>();
        fills.add("fill-y");
        fills.add("fill-x");
        this.fillOptions = new JComboBox<String>();
        this.fillOptions.setModel(new DefaultComboBoxModel<String>(fills));
        this.fillOptions.addActionListener(this);
        this.fillOptions.setBackground(Color.WHITE);
        this.fillOptions.setEditable(false);

        this.zIndexField = new JTextField("0");
        this.zIndexField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.selectFileField.getSelectedItem().toString() + "," + getImageEffects());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.selectFileField.getSelectedItem().toString() + "," + getImageEffects());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.selectFileField.getSelectedItem().toString() + "," + getImageEffects());
            }
            
        });

        this.imageOptionsPanel = new JPanel(new GridLayout(4,2));
        this.imageOptionsPanel.add(new JLabel("Image Effect (by value)"));
        this.imageOptionsPanel.add(this.showImageModeSelector);
        this.imageOptionsPanel.add(new JLabel("Image Stretch"));
        this.imageOptionsPanel.add(this.fillOptions);
        this.imageOptionsPanel.add(new JLabel("Solo (clear canvas first)"));
        this.imageOptionsPanel.add(this.soloImageCheckBox);
        this.imageOptionsPanel.add(new JLabel("Layer number (zIndex)"));
        this.imageOptionsPanel.add(this.zIndexField);

        this.selectRuleGroupDropdown = new JComboBox<String>();
        this.selectRuleGroupDropdown.setEditable(true);
        this.selectRuleGroupDropdown.addActionListener(this);
        this.selectRuleGroupDropdown.setBackground(Color.WHITE);
    
        this.nicknameField = new JTextField("");
        //this.nicknameField.setHorizontalAlignment(SwingConstants.CENTER);

        this.ruleGroupField = new JComboBox<String>();
        this.ruleGroupField.setEditable(true);
        this.ruleGroupField.setModel(getRuleGroupModel());
        this.ruleGroupField.setBackground(Color.WHITE);
        this.ruleGroupField.setToolTipText("Assign this rule to a named group");

        this.canvasSelectorField = new JComboBox<String>();
        this.canvasSelectorField.setEditable(true);
        this.canvasSelectorField.setModel(getCanvasSelectorModel());
        this.canvasSelectorField.setBackground(Color.WHITE);
        this.canvasSelectorField.setToolTipText("Select the Canvas to display this media, select (NONE) for local or enter a name like \"Main Display\" (Canvases are created dynamically)");


        //this.ruleGroupField.setHorizontalAlignment(SwingConstants.CENTER);

        this.actionValueField = new JTextArea("");
        this.actionValueField.setLineWrap(true);
        this.actionValueField.setBorder(new EtchedBorder());
        this.actionValueField.setBackground(Color.WHITE);

        this.selectFilePanel = new JPanel(new BorderLayout());
        this.selectFilePanel.setBorder(new EtchedBorder());
        this.selectFileField = new JComboBox<String>();
        this.selectFileField.setEditable(true);
        this.selectFileField.addActionListener(this);
        this.selectFileField.setSelectedItem(this.actionValueField.getText());
        this.selectFileField.setToolTipText("Select a file from the project's assets");
        
        this.selectFileButton = new JButton("...");
        this.selectFileButton.addActionListener(this);
        this.selectFilePanel.add(this.selectFileField, BorderLayout.CENTER);
        this.selectFilePanel.add(this.selectFileButton, BorderLayout.EAST);

        this.transmitMidiPanel = new JPanel(new GridBagLayout());
        this.deviceSelectAVF = new JComboBox<String>();
        this.deviceSelectAVF.addActionListener(this);
        Vector<String> midiChannels = new Vector<String>();
        for(int i = 1; i < 17; i++)
            midiChannels.add(String.valueOf(i));
            
        DocumentListener xmitDL = new DocumentListener()
        {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                //System.err.println("Remove update");
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.deviceSelectAVF.getSelectedItem() + "," + 
                                                                    MidiControlRuleEditor.this.channelSelectAVF.getSelectedItem() + "," +
                                                                    MidiControlRuleEditor.this.ccAVF.getText() + "," +
                                                                    MidiControlRuleEditor.this.valueAVF.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) 
            {
                //System.err.println("Insert update");
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.deviceSelectAVF.getSelectedItem() + "," + 
                                                                    MidiControlRuleEditor.this.channelSelectAVF.getSelectedItem() + "," +
                                                                    MidiControlRuleEditor.this.ccAVF.getText() + "," +
                                                                    MidiControlRuleEditor.this.valueAVF.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent arg0)
            {
                //System.err.println("Change update");
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.deviceSelectAVF.getSelectedItem() + "," + 
                                                                    MidiControlRuleEditor.this.channelSelectAVF.getSelectedItem() + "," +
                                                                    MidiControlRuleEditor.this.ccAVF.getText() + "," +
                                                                    MidiControlRuleEditor.this.valueAVF.getText());
            }
        };
        this.channelSelectAVF = new JComboBox<String>(midiChannels);
        this.channelSelectAVF.addActionListener(this);
        this.ccAVF = new JTextField("{{cc}}");
        this.ccAVF.getDocument().addDocumentListener(xmitDL);
        this.valueAVF = new JTextField("{{value}}");
        this.valueAVF.getDocument().addDocumentListener(xmitDL);
        
        transmitMidiPanel.add(new JLabel("Device", SwingConstants.LEFT), gbc(1, 1, .4d));
        transmitMidiPanel.add(this.deviceSelectAVF, gbc(2, 1, .6d));
        transmitMidiPanel.add(new JLabel("Channel", SwingConstants.LEFT), gbc(1, 2, .4d));
        transmitMidiPanel.add(this.channelSelectAVF, gbc(2, 2, .6d));
        this.ccLabel = new JLabel("CC", SwingConstants.LEFT);
        transmitMidiPanel.add(this.ccLabel, gbc(1, 3, .4d));
        transmitMidiPanel.add(this.ccAVF, gbc(2, 3, .6d));
        transmitMidiPanel.add(new JLabel("Value/Velocity", SwingConstants.LEFT), gbc(1, 4, .4d));
        transmitMidiPanel.add(this.valueAVF, gbc(2, 4, .6d));
        
        this.modifierPanel = new JPanel(new GridLayout(2,1));
        this.valueInvertedCheckBox = new JCheckBox("Value Inverted");
        this.valueInvertedCheckBox.setSelected(this.rule.isValueInverted());
        this.valueInvertedCheckBox.setToolTipText("Invert the received value from 0-127 to 127-0");
        this.modifierPanel.add(this.valueInvertedCheckBox);
        this.valueSettledCheckBox = new JCheckBox("Value Settled");
        this.valueSettledCheckBox.setSelected(this.rule.shouldValueSettle());
        this.valueSettledCheckBox.setToolTipText("Dont fire the rule until the value stops changing");
        this.modifierPanel.add(this.valueSettledCheckBox);

        this.actionValuePanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        
        formPanel.add(new JLabel("Rule Name", SwingConstants.LEFT), gbc(1, 1, .4d));
        formPanel.add(this.nicknameField, gbc(2, 1, .6d));

        formPanel.add(new JLabel("Rule Group", SwingConstants.LEFT), gbc(1, 2, .4d));
        formPanel.add(this.ruleGroupField, gbc(2, 2, .6d));

        formPanel.add(new JLabel("Select Control", SwingConstants.LEFT), gbc(1, 3, .4d));
        formPanel.add(this.controlSelector, gbc(2, 3, .6d));

        formPanel.add(new JLabel("Modifiers", SwingConstants.LEFT), gbc(1, 4, .4d));
        formPanel.add(this.modifierPanel, gbc(2, 4, .6d));        

        formPanel.add(new JLabel("Select Event", SwingConstants.LEFT), gbc(1, 5, .4d));
        formPanel.add(this.eventSelector, gbc(2, 5, .6d));

        formPanel.add(new JLabel("Action Type", SwingConstants.LEFT), gbc(1, 6, .4d));
        formPanel.add(this.actionSelector, gbc(2, 6, .6d));

        formPanel.add(new JLabel("Target Canvas", SwingConstants.LEFT), gbc(1, 7, .4d));
        formPanel.add(this.canvasSelectorField, gbc(2, 7, .6d));
        
        this.actionValueLabel = new JLabel("Action Value", SwingConstants.LEFT);
        formPanel.add(this.actionValueLabel, gbc(1, 8, .4d));
        formPanel.add(this.actionValuePanel, gbc(2, 8, .6d));
        
        
        
        if (newRule)
        {
            this.saveButton = new JButton("Create Rule");
            this.deleteButton = new JButton("Cancel");
        } else {
            this.saveButton = new JButton("Save Rule");
            this.deleteButton = new JButton("Delete Rule");
        }
        this.saveButton.addActionListener(this);
        this.deleteButton.addActionListener(this);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);
        
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.add(formPanel, BorderLayout.PAGE_START);
        this.add(buttonPanel, BorderLayout.PAGE_END);
        this.eventSelector.setSelectedIndex(this.rule.getEventMode());
        this.controlSelector.setSelectedIndex(MidiTools.getIndexForMidiControl(this.rule.getMidiControl()));
        
        if (this.rule.getNickname() != null)
            this.nicknameField.setText(this.rule.getNickname());
        if (this.rule.getRuleGroup() != null)
            this.ruleGroupField.setSelectedItem(this.rule.getRuleGroup());
        if (this.rule.getActionValue() != null)
            this.actionValueField.setText(this.rule.getActionValue());
        if (this.rule.getCanvasName() != null)
            this.canvasSelectorField.setSelectedItem(this.rule.getCanvasName());
        else
            this.canvasSelectorField.setSelectedItem("");
        int avi = this.rule.getActionType();
        this.actionSelector.setSelectedItem(avi);
        this.changeActionSelector(avi);
        
        centerWindow();
    }
    
    private GridBagConstraints gbc(int x, int y, double weightx)
    {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = weightx;
        g.gridx = x;
        g.gridy = y;
        g.ipady = 2;
        g.ipadx = 5;
        return g;
    }

    public void refreshDevices()
    {
        try
        {
            DefaultComboBoxModel<String> deviceModel = new DefaultComboBoxModel<String>();
            // Check for new devices added
            for(Iterator<MidiPort> newLocalDevicesIterator = MidiPortManager.getReceivingPorts().iterator(); newLocalDevicesIterator.hasNext();)
            {
                MidiPort p = newLocalDevicesIterator.next();
                deviceModel.addElement(p.getName());
            }
            this.deviceSelectAVF.setModel(deviceModel);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public DefaultComboBoxModel<String> getRuleGroupModel()
    {
        try
        {
            Vector<String> ruleGroups = new Vector<String>();
            // Check for new devices added
            for(Enumeration<MidiControlRule> newRuleEnum = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); newRuleEnum.hasMoreElements();)
            {
                MidiControlRule mcr = newRuleEnum.nextElement();
                String groupName = mcr.getRuleGroup();
                if (!ruleGroups.contains(groupName))
                    ruleGroups.add(groupName);
            }
            DefaultComboBoxModel<String> ruleGroupModel = new DefaultComboBoxModel<String>(ruleGroups);
            return ruleGroupModel;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public DefaultComboBoxModel<String> getCanvasSelectorModel()
    {
        try
        {
            DefaultComboBoxModel<String> canvasGroupModel = new DefaultComboBoxModel<String>(MidiTools.getCanvasNames());
            return canvasGroupModel;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    public DefaultComboBoxModel<String> getPluginModel()
    {
        try
        {
            Vector<String> plugins = new Vector<String>();
            // Check for new devices added
            Iterator<MidiToolsPlugin> pIterator = MidiTools.instance.plugins.values().iterator();
            while(pIterator.hasNext())
            {
                MidiToolsPlugin plugin = pIterator.next();
                Collection<String> rule_targets = plugin.getRuleTargets();
                if (rule_targets != null)
                {
                    if (rule_targets.size() > 0)
                        plugins.add(plugin.getTitle());
                }
            }
            DefaultComboBoxModel<String> pluginModel = new DefaultComboBoxModel<String>(plugins);
            return pluginModel;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public DefaultComboBoxModel<String> getPluginTargetModel(MidiToolsPlugin mtp)
    {
        try
        {
            Vector<String> pluginTargets = new Vector<String>(mtp.getRuleTargets());
            DefaultComboBoxModel<String> pluginModel = new DefaultComboBoxModel<String>(pluginTargets);
            return pluginModel;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            Vector<String> pluginTargets = new Vector<String>();
            pluginTargets.add("");
            DefaultComboBoxModel<String> pluginModel = new DefaultComboBoxModel<String>(pluginTargets);
            return pluginModel;
        }
    }

    public JPanel labelComponent(String label, Component c)
    {
        JPanel x = new JPanel(new GridLayout(1,2));
        x.add(new JLabel(label, SwingConstants.CENTER));
        x.add(c);
        return x;
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 400;
        int wHeight = 450;
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setBounds(x, y, wWidth, wHeight);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }
}
