package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


import javax.swing.border.Border;

public class MidiPortMappingCellRenderer extends JCheckBox implements ListCellRenderer<MidiPortMapping>
{
   private Border selectedBorder;
   private Border regularBorder;

   public MidiPortMappingCellRenderer()
   {
       super();
       this.setOpaque(true);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 3);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);

   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiPortMapping> list,
                                                 MidiPortMapping mapping,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      this.setText(mapping.toString());
      if ((System.currentTimeMillis() -  mapping.getLastActiveAt()) < 1000l)
      {
         this.setBackground(new Color(102,255,102));
      } else {
         this.setBackground(Color.WHITE);
      }
      if (isSelected)
      {
         this.setForeground(list.getSelectionForeground());
      } else {
         this.setForeground(list.getForeground());
      }
      this.setSelected(mapping.isOpened());
      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());
      if (isSelected && cellHasFocus)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(this.regularBorder);

      return this;
   }
}
