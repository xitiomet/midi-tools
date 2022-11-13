package org.openstatic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import javax.swing.border.Border;

import org.openstatic.midi.MidiToolsPlugin;
import org.openstatic.midi.ports.RTPMidiPort;

import io.github.leovr.rtipmidi.session.AppleMidiSessionClient;

public class AppleMidiSessionClientCellRenderer extends JPanel implements ListCellRenderer<AppleMidiSessionClient>
{
   private Border selectedBorder;
   private Border regularBorder;
   private JCheckBox checkBox;
   private ImageIcon urlIcon;
   private RTPMidiPort rtpPort;
   
   public AppleMidiSessionClientCellRenderer(RTPMidiPort rtpPort)
   {
      super(new BorderLayout());
      this.setOpaque(true);
      this.setBackground(Color.WHITE);
      this.rtpPort = rtpPort;
      this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 1);
      this.checkBox = new JCheckBox("");
      this.checkBox.setOpaque(false);
      this.add(this.checkBox, BorderLayout.CENTER);
      try
      {
         this.urlIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/rtpnet32.png")));
      } catch (Exception e) {

      }
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends AppleMidiSessionClient> list,
                                                 AppleMidiSessionClient client,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      this.checkBox.setIcon(this.urlIcon);
      String ipString = client.getRemoteAddress().toString() + ":" + String.valueOf(client.getControlPort());
      if (ipString.startsWith("/"))
       ipString = ipString.substring(1);
      if (client.isConnected() || client.hasServerConnection(this.rtpPort.getAppleMidiServer()))
      {
         this.checkBox.setText("<html><body style=\"padding: 3px 3px 3px 3px;\"><b style=\"font-size: 14px;\">" + client.getRemoteName() + "</b><br />" + ipString + " (Connected)</body></html>");
      } else {
         this.checkBox.setText("<html><body style=\"padding: 3px 3px 3px 3px;\"><span style=\"font-size: 14px;\">" + client.getRemoteName() + "</span><br />" + ipString + " (Not Connected)</body></html>");
      }
      
      this.checkBox.setSelected(client.isConnected());
      this.setBackground(Color.WHITE);
      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());

      if (isSelected)
      {
         this.setBorder(this.selectedBorder);
      } else {
         this.setBorder(this.regularBorder);
      }
      return this;
   }
}
