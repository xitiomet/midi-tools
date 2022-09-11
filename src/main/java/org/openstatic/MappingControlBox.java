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
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MappingControlBox extends JPanel implements ActionListener
{
    private JList<MidiPortMapping> mappingList;
    private MidiPortMappingCellRenderer midiPortMappingCellRenderer;
    private long lastMappingClick;
    private JPanel buttonPanel;
    private JButton connectButton;
    private JButton selectAllButton;
    private JButton disableAllButton;
    private JButton enableAllButton;
    private JButton deleteButton;

    public MappingControlBox()
    {
        super(new BorderLayout());
        this.midiPortMappingCellRenderer = new MidiPortMappingCellRenderer();
        this.mappingList = new JList<MidiPortMapping>(new MidiPortMappingListModel());
        this.mappingList.setOpaque(true);
        this.mappingList.setCellRenderer(this.midiPortMappingCellRenderer);
        this.mappingList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon plugInIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/plug_in.png")));
            this.connectButton = new JButton(plugInIcon);
            this.connectButton.setActionCommand("connect");
            this.connectButton.addActionListener(this);
            this.connectButton.setToolTipText("Create a new port mapping (Basically a virtual midi cable)");
            this.buttonPanel.add(this.connectButton);

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
            this.disableAllButton.setToolTipText("Disable selected mappings");
            this.buttonPanel.add(this.disableAllButton);

            ImageIcon enableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/enable32.png")));
            this.enableAllButton = new JButton(enableIcon);
            this.enableAllButton.addActionListener(this);
            this.enableAllButton.setActionCommand("enable_selected");
            this.enableAllButton.setToolTipText("Enable selected mappings");
            this.buttonPanel.add(this.enableAllButton);

            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_selected");
            this.deleteButton.setToolTipText("Delete Selected mappings");
            this.buttonPanel.add(this.deleteButton);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.add(mappingScrollPane, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.WEST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.connectButton)
        {
            CreateMappingDialog editr = new CreateMappingDialog();
        } else if (e.getSource() == this.enableAllButton) {
            Collection<MidiPortMapping> selectedMappings = this.getSelectedMappings();
            if (selectedMappings.size() == 0)
            {
                
            } else {
                Iterator<MidiPortMapping> mIterator = selectedMappings.iterator();
                while (mIterator.hasNext())
                {
                    MidiPortMapping mapping = mIterator.next();
                    mapping.setOpen(true);
                }
            }
        } else if (e.getSource() == this.disableAllButton) {
            Collection<MidiPortMapping> selectedRules = this.getSelectedMappings();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<MidiPortMapping> mIterator = selectedRules.iterator();
                while (mIterator.hasNext())
                {
                    MidiPortMapping mapping = mIterator.next();
                    mapping.setOpen(false);
                }
            }
        } else if (e.getSource() == this.deleteButton) {
            Collection<MidiPortMapping> selectedRules = this.getSelectedMappings();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<MidiPortMapping> rIterator = selectedRules.iterator();
                while (rIterator.hasNext())
                {
                    MidiPortMapping mapping = rIterator.next();
                    MidiPortManager.removeMidiPortMapping(mapping);
                }
            }
        } else if (e.getSource() == this.selectAllButton) {
            int rs = MidiPortManager.getMidiPortMappings().size();
            int[] indices = new int[rs];
            for(int i = 0; i < rs; i++)
                indices[i] = i;
            this.mappingList.setSelectedIndices(indices);
        }
        
    }

    public Collection<MidiPortMapping> getSelectedMappings()
    {
        return this.mappingList.getSelectedValuesList();
    }
}
