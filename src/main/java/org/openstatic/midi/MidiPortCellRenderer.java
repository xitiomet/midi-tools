package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import javax.swing.border.Border;

public class MidiPortCellRenderer extends JCheckBox implements ListCellRenderer<MidiPort>
{
   private Border selectedBorder;
   private Border regularBorder;

   public MidiPortCellRenderer()
   {
       super();
       this.setBackground(Color.WHITE);
       this.setOpaque(false);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);
       this.setBorder(this.regularBorder);
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiPort> list,
                                                 MidiPort device,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      String direction = "[]";
      if (device.canTransmitMessages() && device.canReceiveMessages())
      {
          direction = "&#10094;&#10095;";
      } else if (device.canTransmitMessages()) {
          direction = "&#10095;&#10095;";
      } else if (device.canReceiveMessages()) {
          direction = "&#10094;&#10094;";
      }
      this.setText("<html>" + direction + " " + device.getName() + "</html>");
      this.setSelected(device.isOpened());

      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());

      return this;
   }
}
