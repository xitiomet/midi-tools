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
       this.setOpaque(false);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 3);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);

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
      if (isSelected)
      {
         this.setBackground(list.getSelectionBackground());
         this.setForeground(list.getSelectionForeground());
      } else {
         this.setBackground(list.getBackground());
         this.setForeground(list.getForeground());
      }

      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());

      if (isSelected && cellHasFocus)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(this.regularBorder);

      return this;
   }
}
