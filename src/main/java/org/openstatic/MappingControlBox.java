package org.openstatic;

import org.openstatic.midi.*;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MappingControlBox extends JPanel implements ActionListener
{
    private JList<MidiPortMapping> mappingList;
    private MidiPortMappingCellRenderer midiPortMappingCellRenderer;
    private long lastMappingClick;
    private JPanel buttonPanel;
    private JButton clearMappingsButton;
    private JButton connectButton;

    public MappingControlBox()
    {
        super(new BorderLayout());
        this.midiPortMappingCellRenderer = new MidiPortMappingCellRenderer();
        this.mappingList = new JList<MidiPortMapping>(new MidiPortMappingListModel());
        this.mappingList.setCellRenderer(this.midiPortMappingCellRenderer);
        this.mappingList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = MappingControlBox.this.mappingList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                   MidiPortMapping mapping = (MidiPortMapping) MappingControlBox.this.mappingList.getModel().getElementAt(index);
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - MappingControlBox.this.lastMappingClick < 500 && MappingControlBox.this.lastMappingClick > 0)
                       {
                          mapping.toggle();
                       }
                       MappingControlBox.this.lastMappingClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      int n = JOptionPane.showConfirmDialog(null, "Delete this port mapping?\n" + mapping.toString(),
                        "Port Mapping",
                        JOptionPane.YES_NO_OPTION);
                        if(n == JOptionPane.YES_OPTION)
                        {
                            MidiPortManager.removeMidiPortMapping(mapping);
                        }
                   }
                   MappingControlBox.this.repaint();
               }
            }
        });
        JScrollPane mappingScrollPane = new JScrollPane(this.mappingList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mappingScrollPane.setBorder(new TitledBorder("MIDI Port Mappings (right-click to delete, double-click to toggle)"));
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon plugInIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/plug_in.png")));
            this.connectButton = new JButton(plugInIcon);
            this.connectButton.setActionCommand("connect");
            this.connectButton.addActionListener(this);
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

    public void clearMidiPortMappings()
    {
        ListModel<MidiPortMapping> mappingListModel = this.mappingList.getModel();
        int mappingsCount = mappingListModel.getSize();
        ArrayList<MidiPortMapping> mappingsToRemove = new ArrayList<MidiPortMapping>();
        for (int i = 0; i < mappingsCount; i++)
        {
            MidiPortMapping mapping = mappingListModel.getElementAt(i);
            if (mapping != null)
            {
                mappingsToRemove.add(mapping);
            }
        }
        mappingsToRemove.forEach((mapping) -> { 
            System.err.println("Removing Mapping: " + mapping.toString());
            mapping.close();
            MidiPortManager.removeMidiPortMapping(mapping);
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.clearMappingsButton)
        {
            int n = JOptionPane.showConfirmDialog(null,
            "Are you sure you wish to clear all port mappings?",
            "Reset Mappings",
            JOptionPane.YES_NO_OPTION);
            if(n == JOptionPane.YES_OPTION)
            {
                this.clearMidiPortMappings();
            }
        } else if (e.getSource() == this.connectButton)
        {
            CreateMappingDialog editr = new CreateMappingDialog();
        }
        
    }
}
