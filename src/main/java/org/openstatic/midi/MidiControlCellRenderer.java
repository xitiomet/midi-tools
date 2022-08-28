package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import java.awt.BorderLayout;

import javax.swing.border.Border;

public class MidiControlCellRenderer extends JPanel implements ListCellRenderer<MidiControl>
{
   private Border border;
   private JLabel controlLabel;
   private JProgressBar valueBar;
   
   public MidiControlCellRenderer()
   {
      super();
      this.setOpaque(false);
      this.border = BorderFactory.createLineBorder(Color.RED, 1);
      this.setLayout(new BorderLayout());
      this.controlLabel = new JLabel();
      this.valueBar = new JProgressBar(0, 127);
      this.valueBar.setStringPainted(true);
      this.add(this.controlLabel, BorderLayout.WEST);
      this.add(this.valueBar, BorderLayout.CENTER);
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiControl> list,
                                                 MidiControl control,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
       try
       {
            if (control.getControlNumber() >= 0)
            {
                this.controlLabel.setText("<html><body style=\"padding: 3px 3px 3px 3px; width: 150px;\"><b style=\"font-size: 18px;\">" + control.getNickname() + "</b><br />" +
                                        "<i style=\"color: blue;\">Ch=" + String.valueOf(control.getChannel()) + "</i> " + 
                                        "<i style=\"color: #672E97;\">CC=" + String.valueOf(control.getControlNumber()) + "</i> " + 
                                        "<span style=\"color: red;\">V=" + String.valueOf(control.getValue())+ "</span></body></html>");
            } else {
                this.controlLabel.setText("<html><body style=\"padding: 3px 3px 3px 3px; width: 150px;\"><b style=\"font-size: 18px;\">" + control.getNickname() + "</b><br />" +
                                        "<i style=\"color: blue;\">Ch=" + String.valueOf(control.getChannel()) + "</i> " + 
                                        "<i style=\"color: #672E97;\">Note=" + control.getNoteName() + "</i> " + 
                                        "<span style=\"color: red;\">V=" + String.valueOf(control.getValue())+ "</span></body></html>");
            }
            int value = control.getValue();
            if (value >= 0)
                valueBar.setValue(value);
            else
                valueBar.setValue(0);
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
             this.setBorder(border);
            else
             this.setBorder(null);
         } catch (Exception e) {
             e.printStackTrace(System.err);
         }

      return this;
   }
}
