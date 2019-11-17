package org.openstatic;

import org.openstatic.midi.*;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.DefaultListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;

import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

import java.io.File;

import javax.sound.midi.*;

public class MidiControlRuleEditor extends JDialog implements ActionListener
{
    private MidiControlRule rule;
    private JComboBox eventSelector;
    private JComboBox controlSelector;
    private JComboBox actionSelector;
    private JTextField nicknameField;
    private JTextArea actionValueField;
    
    private TitledBorder selectFileBorder;
    private JComboBox deviceSelectAVF;
    private JComboBox channelSelectAVF;
    private JTextField ccAVF;
    private JTextField valueAVF;
    private JPanel transmitMidiPanel;

    private JPanel selectFilePanel;
    private JButton selectFileButton;
    private JTextArea selectFileField;
    private JLabel actionValueLabel;
    private JPanel actionValuePanel;

    private JButton saveButton;
    private JButton deleteButton;
    private JLabel data1Label;
    private DefaultListModel<String> data1Options;

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
                    String filename = fileToLoad.toString();
                    this.selectFileField.setText(filename);
                    this.actionValueField.setText(filename);
                }
            }
        }
        
        if (e.getSource() == this.actionSelector)
        {
            int avi = this.actionSelector.getSelectedIndex();
            this.rule.setActionType(avi);
            this.changeActionSelector(avi);
        }
        
        if (e.getSource() == this.deviceSelectAVF || e.getSource() == this.channelSelectAVF)
        {
            this.actionValueField.setText(this.deviceSelectAVF.getSelectedItem() + "," + this.channelSelectAVF.getSelectedItem() + 
                                          "," + this.ccAVF.getText() + "," + this.valueAVF.getText());
        }
        
        if (e.getSource() == this.deleteButton)
        {
            if (MidiTools.instance.rules.contains(this.rule))
                MidiTools.instance.rules.removeElement(this.rule);
            MidiTools.removeListenerFromControls(this.rule);
            this.dispose();
        }
        
        if (e.getSource() == this.saveButton)
        {
            if (!"".equals(this.nicknameField.getText()))
                this.rule.setNickname(this.nicknameField.getText());
            else
                this.rule.setNickname(null);
            if (!"".equals(this.actionValueField.getText()))
                this.rule.setActionValue(this.actionValueField.getText());
            else
                this.rule.setActionValue(null);
            int ci = this.controlSelector.getSelectedIndex();
            if (ci == -1)
                this.rule.setMidiControl(null);
            else
                this.rule.setMidiControl(MidiTools.getMidiControlByIndex(ci));
            if (!MidiTools.instance.rules.contains(this.rule))
                MidiTools.instance.rules.addElement(this.rule);
            this.rule.updateRule();
            this.dispose();
        }
    }

    public void changeActionSelector(int i)
    {
        this.actionValuePanel.removeAll();
        //System.err.println("changeActionSelector: " + String.valueOf(i));
        if (i == 0)
        {
            this.actionValuePanel.add(this.actionValueField, BorderLayout.CENTER);
            this.actionValueLabel.setText("URL");
        } else if (i == 1 || i == 2) {
            if (i == 1)
            {
                this.actionValueLabel.setText("Program");
            } else if (i == 2) {
                this.actionValueLabel.setText("Filename or URL");
            }
            this.actionValuePanel.add(this.selectFilePanel, BorderLayout.CENTER);
            this.selectFileField.setText(this.actionValueField.getText());
        } else if (i == 3) {
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
            this.actionValuePanel.add(this.transmitMidiPanel, BorderLayout.CENTER);
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

        Vector<String> actionList = new Vector<String>();
        for(int i = 0; i < 4; i++)
        {
            actionList.add(MidiControlRule.actionNumberToString(i));
        }
        Vector<String> eventModeList = new Vector<String>();
        for(int i = 0; i < 8; i++)
        {
            eventModeList.add(MidiControlRule.eventModeToString(i));
        }
        
        this.controlSelector = new JComboBox();
        this.controlSelector.setEditable(false);
        this.controlSelector.addActionListener(this);
        Vector<MidiControl> ctrls = new Vector<MidiControl>();
        for (Enumeration<MidiControl> cenum = MidiTools.instance.controls.elements(); cenum.hasMoreElements();)
            ctrls.add(cenum.nextElement());
        this.controlSelector.setModel(new DefaultComboBoxModel<MidiControl>(ctrls));

        this.eventSelector = new JComboBox(eventModeList);
        this.eventSelector.setEditable(false);
        this.eventSelector.addActionListener(this);

        this.actionSelector = new JComboBox(actionList);
        this.actionSelector.setEditable(false);
        this.actionSelector.addActionListener(this);
    
        this.nicknameField = new JTextField("");
        this.nicknameField.setHorizontalAlignment(SwingConstants.CENTER);

        this.actionValueField = new JTextArea("");
        this.actionValueField.setLineWrap(true);
        this.actionValueField.setBorder(new EtchedBorder());

        this.selectFilePanel = new JPanel(new BorderLayout());
        this.selectFilePanel.setBorder(new EtchedBorder());
        this.selectFileField = new JTextArea("");
        this.selectFileField.setLineWrap(true);
        this.selectFileField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                //System.err.println("Remove update");
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.selectFileField.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) 
            {
                //System.err.println("Insert update");
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.selectFileField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent arg0)
            {
                //System.err.println("Change update");
                MidiControlRuleEditor.this.actionValueField.setText(MidiControlRuleEditor.this.selectFileField.getText());
            }
        });
        this.selectFileButton = new JButton("...");
        this.selectFileButton.addActionListener(this);
        this.selectFilePanel.add(this.selectFileField, BorderLayout.CENTER);
        this.selectFilePanel.add(this.selectFileButton, BorderLayout.EAST);

        this.transmitMidiPanel = new JPanel(new GridLayout(4,2));
        this.deviceSelectAVF = new JComboBox();
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
        this.channelSelectAVF = new JComboBox(midiChannels);
        this.channelSelectAVF.addActionListener(this);
        this.ccAVF = new JTextField("{{cc}}");
        this.ccAVF.getDocument().addDocumentListener(xmitDL);
        this.valueAVF = new JTextField("{{value}}");
        this.valueAVF.getDocument().addDocumentListener(xmitDL);
        
        transmitMidiPanel.add(new JLabel("Device", SwingConstants.LEFT));
        transmitMidiPanel.add(this.deviceSelectAVF);
        transmitMidiPanel.add(new JLabel("Channel", SwingConstants.LEFT));
        transmitMidiPanel.add(this.channelSelectAVF);
        transmitMidiPanel.add(new JLabel("CC", SwingConstants.LEFT));
        transmitMidiPanel.add(this.ccAVF);
        transmitMidiPanel.add(new JLabel("Value", SwingConstants.LEFT));
        transmitMidiPanel.add(this.valueAVF);
        
        this.actionValuePanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(6,2));
        
        formPanel.add(new JLabel("Rule Name", SwingConstants.CENTER));
        formPanel.add(this.nicknameField);

        formPanel.add(new JLabel("Select Control", SwingConstants.CENTER));
        formPanel.add(this.controlSelector);

        formPanel.add(new JLabel("Select Event", SwingConstants.CENTER));
        formPanel.add(this.eventSelector);

        formPanel.add(new JLabel("Action Type", SwingConstants.CENTER));
        formPanel.add(this.actionSelector);
        
        this.actionValueLabel = new JLabel("Action Value", SwingConstants.CENTER);
        formPanel.add(this.actionValueLabel);
        formPanel.add(this.actionValuePanel);
        
        
        
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
        
        formPanel.add(deleteButton);
        formPanel.add(saveButton);

        this.add(formPanel, BorderLayout.CENTER);
        this.eventSelector.setSelectedIndex(this.rule.getEventMode());
        this.controlSelector.setSelectedIndex(MidiTools.getIndexForMidiControl(this.rule.getMidiControl()));
        
        if (this.rule.getNickname() != null)
            this.nicknameField.setText(this.rule.getNickname());
        if (this.rule.getActionValue() != null)
            this.actionValueField.setText(this.rule.getActionValue());
        
        int avi = this.rule.getActionType();
        this.actionSelector.setSelectedIndex(avi);
        this.changeActionSelector(avi);
        
        centerWindow();
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
