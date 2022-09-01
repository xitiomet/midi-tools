package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;


import javax.swing.border.Border;

public class MidiPortMappingCellRenderer extends JPanel implements ListCellRenderer<MidiPortMapping>
{
   private Border selectedBorder;
   private JCheckBox checkBox;

   public MidiPortMappingCellRenderer()
   {
       super(new BorderLayout());
       this.setOpaque(true);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 1);
       this.checkBox = new JCheckBox("");
       this.checkBox.setOpaque(false);
       this.add(this.checkBox, BorderLayout.CENTER);
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiPortMapping> list,
                                                 MidiPortMapping mapping,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      this.checkBox.setText(mapping.toString());
      if ((System.currentTimeMillis() -  mapping.getLastActiveAt()) < 1000l)
      {
         this.setBackground(new Color(102,255,102));
      } else {
         this.setBackground(Color.WHITE);
      }
      this.checkBox.setSelected(mapping.isOpened());
      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());
      if (isSelected)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(null);

      return this;
   }
}
