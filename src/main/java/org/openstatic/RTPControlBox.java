package org.openstatic;

import org.openstatic.midi.*;
import org.openstatic.midi.ports.RTPMidiPort;

import io.github.leovr.rtipmidi.session.AppleMidiSessionClient;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.Icon;
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

public class RTPControlBox extends JPanel implements ActionListener
{
    private JList<AppleMidiSessionClient> mappingList;
    private AppleMidiSessionClientCellRenderer appleMidiSessionClientCellRenderer;
    private long lastMappingClick;
    private JPanel buttonPanel;
    private JButton connectButton;
    private JButton selectAllButton;
    private JButton disableAllButton;
    private RTPMidiPort port;
    private ImageIcon icon;

    public RTPControlBox(RTPMidiPort port)
    {
        super(new BorderLayout());
        this.port = port;
        this.appleMidiSessionClientCellRenderer = new AppleMidiSessionClientCellRenderer(port);
        this.mappingList = new JList<AppleMidiSessionClient>(port);
        this.mappingList.setOpaque(true);
        this.mappingList.setCellRenderer(this.appleMidiSessionClientCellRenderer);
        this.mappingList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.mappingList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = RTPControlBox.this.mappingList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                   AppleMidiSessionClient client = (AppleMidiSessionClient) RTPControlBox.this.mappingList.getModel().getElementAt(index);
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - RTPControlBox.this.lastMappingClick < 500 && RTPControlBox.this.lastMappingClick > 0)
                       {
                          if (!client.isConnected() && !client.hasServerConnection(port.getAppleMidiServer()))
                          {
                            client.start();
                          }
                       }
                       RTPControlBox.this.lastMappingClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      int n = JOptionPane.showConfirmDialog(null, "Disconnect from?\n" + client.getRemoteName(),
                        "Port Mapping",
                        JOptionPane.YES_NO_OPTION);
                        if(n == JOptionPane.YES_OPTION)
                        {
                            if (client.isConnected())
                               client.stopClient();
                        }
                   }
                   RTPControlBox.this.repaint();
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
            this.connectButton.setToolTipText("Connect to Server");
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
            this.disableAllButton.setToolTipText("Disconnect Selected Connections");
            this.buttonPanel.add(this.disableAllButton);

            this.icon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/rtpnet32.png")));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.add(mappingScrollPane, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.WEST);
    }

    public Icon getIcon()
    {
        return this.icon;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.connectButton)
        {
            Collection<AppleMidiSessionClient> selectedRules = this.getSelectedMappings();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<AppleMidiSessionClient> mIterator = selectedRules.iterator();
                while (mIterator.hasNext())
                {
                    AppleMidiSessionClient client = mIterator.next();
                    if (!client.isConnected() && !client.hasServerConnection(port.getAppleMidiServer()))
                        client.start();
                }
            }
        } else if (e.getSource() == this.disableAllButton) {
            Collection<AppleMidiSessionClient> selectedRules = this.getSelectedMappings();
            if (selectedRules.size() == 0)
            {
                
            } else {
                Iterator<AppleMidiSessionClient> mIterator = selectedRules.iterator();
                while (mIterator.hasNext())
                {
                    AppleMidiSessionClient client = mIterator.next();
                    client.stopClient();
                }
                this.mappingList.repaint();
            }
        } else if (e.getSource() == this.selectAllButton) {
            int rs = port.getRemoteServers().size();
            int[] indices = new int[rs];
            for(int i = 0; i < rs; i++)
                indices[i] = i;
            this.mappingList.setSelectedIndices(indices);
        }
        
    }

    public Collection<AppleMidiSessionClient> getSelectedMappings()
    {
        return this.mappingList.getSelectedValuesList();
    }
}
