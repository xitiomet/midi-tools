package org.openstatic.midi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import javax.swing.border.Border;

import org.openstatic.MidiTools;

public class MidiPortCellRenderer extends JPanel implements ListCellRenderer<MidiPort>
{
   private Border selectedBorder;
   private Border regularBorder;
   private JCheckBox checkbox;
   private Dimension dimension;

   public MidiPortCellRenderer()
   {
       super(new BorderLayout());
       this.dimension = new Dimension(120, 34);
       this.setBackground(Color.WHITE);
       this.setOpaque(true);
       this.setPreferredSize(this.dimension);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 1);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 1);
       this.setBorder(this.regularBorder);
       this.checkbox = new JCheckBox();
       this.checkbox.setBackground(Color.WHITE);
       this.checkbox.setOpaque(false);
       this.add(checkbox, BorderLayout.CENTER);
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
      if (device.isOpened())
         this.checkbox.setIcon(MidiTools.getCachedIcon("/midi-tools-res/midiport.png", "32x32"));
      else
         this.checkbox.setIcon(MidiTools.getCachedIcon("/midi-tools-res/midiportclosed.png", "32x32"));
      String devName = device.getName();
      if (devName.length() > 23)
      {
        devName = devName.substring(0, 20) + "...";
      }
      this.checkbox.setText("<html><b style=\"font-size: 10px;\">" + direction + " " + devName + "</b><br /><i>Tx "+ String.valueOf(device.getTxCount()) +" Rx " + String.valueOf(device.getRxCount()) + "</i></html>");
      this.checkbox.setSelected(device.isOpened());
      if ((System.currentTimeMillis() -  device.getLastRxAt()) < 1000l)
      {
         this.setBackground(new Color(102,255,102));
      } else if ((System.currentTimeMillis() -  device.getLastTxAt()) < 1000l) {
        if (device.isOpened())
        {
            this.setBackground(new Color(255,192,102));
        } else {
            this.setBackground(new Color(255,102,102));
        }
      } else {
         this.setBackground(Color.WHITE);
      }
      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());
      if (isSelected && list.hasFocus())
      {
        this.setBorder(this.selectedBorder);
      } else {
        this.setBorder(this.regularBorder);
      }
      return this;
   }
}
