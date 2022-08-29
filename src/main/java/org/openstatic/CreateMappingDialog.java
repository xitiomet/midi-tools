package org.openstatic;

import org.openstatic.midi.*;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;

import java.util.Vector;

public class CreateMappingDialog extends JDialog implements ActionListener
{
    private JTextField nicknameField;
    private JComboBox<MidiPort> selectSource;
    private JComboBox<MidiPort> selectDestination;

    private JButton saveButton;
    private JButton deleteButton;

    public void actionPerformed(ActionEvent e)
    {
        
        if (e.getSource() == this.deleteButton)
        {
            this.dispose();
        }
        
        if (e.getSource() == this.saveButton)
        {
            MidiPortMapping mapping = MidiPortManager.createMidiPortMapping((MidiPort)this.selectSource.getSelectedItem(), (MidiPort)this.selectDestination.getSelectedItem());
            if (!"".equals(this.nicknameField.getText()))
                mapping.setNickname(nicknameField.getText());
            this.dispose();
        }
    }
    
    public CreateMappingDialog()
    {
        super(MidiTools.instance, "Mapping Creator", true);
        this.setLayout(new BorderLayout());
        this.nicknameField = new JTextField("");
        
        this.selectSource = new JComboBox<MidiPort>();
        this.selectSource.setModel(new DefaultComboBoxModel<MidiPort>( ((Vector<MidiPort>)MidiPortManager.getTransmittingPorts()) ));

        this.selectDestination = new JComboBox<MidiPort>();
        this.selectDestination.setModel(new DefaultComboBoxModel<MidiPort>( ((Vector<MidiPort>)MidiPortManager.getReceivingPorts()) ));
        
        JPanel formPanel = new JPanel(new GridBagLayout());

        formPanel.add(new JLabel("Nickname", SwingConstants.LEFT), gbc(1, 1, .4d));
        formPanel.add(this.nicknameField, gbc(2, 1, .6d));

        formPanel.add(new JLabel("Source Device", SwingConstants.LEFT), gbc(1, 2, .4d));
        formPanel.add(this.selectSource, gbc(2, 2, .6d));

        formPanel.add(new JLabel("Destination Device", SwingConstants.LEFT), gbc(1, 3, .4d));
        formPanel.add(this.selectDestination, gbc(2, 3, .6d));

        
        this.saveButton = new JButton("Create Mapping");
        this.deleteButton = new JButton("Cancel");
        this.saveButton.addActionListener(this);
        this.deleteButton.addActionListener(this);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);
        
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.add(formPanel, BorderLayout.PAGE_START);
        this.add(buttonPanel, BorderLayout.PAGE_END);
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
        int wHeight = 250;
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setBounds(x, y, wWidth, wHeight);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }
}
