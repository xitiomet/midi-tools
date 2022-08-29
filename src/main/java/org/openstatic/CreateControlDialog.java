package org.openstatic;

import org.openstatic.midi.*;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.JOptionPane;


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

public class CreateControlDialog extends JDialog implements ActionListener
{
    private JTextField nicknameField;
    private JComboBox<Integer> selectChannel;
    private JComboBox<Integer> selectCC;

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
            int channel = ((Integer)this.selectChannel.getSelectedItem()).intValue();
            int cc = ((Integer)this.selectCC.getSelectedItem()).intValue();
            MidiControl mc = MidiTools.getMidiControlByChannelCC(channel, cc);
            if (mc == null)
            {
                mc = new MidiControl(channel, cc);
                mc.setNickname(CreateControlDialog.this.nicknameField.getText());
                final MidiControl fmc = mc;
                Thread t = new Thread()
                {
                    public void run()
                    {
                        MidiTools.handleNewMidiControl(fmc);
                    }
                };
                t.start();
                this.dispose();
            } else {
                int n = JOptionPane.showConfirmDialog(null,
                "Control already exists! Update Nickname?",
                "Control Exists",
                JOptionPane.YES_NO_OPTION);
                if(n == JOptionPane.YES_OPTION)
                {
                    mc.setNickname(CreateControlDialog.this.nicknameField.getText());
                    MidiTools.repaintControls();
                    this.dispose();
                }
            }
        }
    }
    
    public CreateControlDialog()
    {
        super(MidiTools.instance, "Control Creator", true);
        this.setLayout(new BorderLayout());
        this.nicknameField = new JTextField("");
        
        Vector<Integer> midiChannels = new Vector<Integer>();
        for(int i = 1; i < 17; i++)
            midiChannels.add(Integer.valueOf(i));
        this.selectChannel = new JComboBox<Integer>(midiChannels);
        this.selectChannel.addActionListener(this);
        
        Vector<Integer> controlChangeNumbers = new Vector<Integer>();
        for(int i = 0; i < 128; i++)
            controlChangeNumbers.add(Integer.valueOf(i));
        this.selectCC = new JComboBox<Integer>(controlChangeNumbers);
        this.selectCC.addActionListener(this);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        
        formPanel.add(new JLabel("Control Name", SwingConstants.LEFT), gbc(1, 1, .4d));
        formPanel.add(this.nicknameField, gbc(2, 1, .6d));

        formPanel.add(new JLabel("MIDI Channel", SwingConstants.LEFT), gbc(1, 2, .4d));
        formPanel.add(this.selectChannel, gbc(2, 2, .6d));

        formPanel.add(new JLabel("Select Control Change#", SwingConstants.LEFT), gbc(1, 3, .4d));
        formPanel.add(this.selectCC, gbc(2, 3, .6d));

        
        this.saveButton = new JButton("Create Control");
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
