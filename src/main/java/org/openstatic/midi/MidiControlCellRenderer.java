package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import java.awt.BorderLayout;

import javax.swing.border.Border;

import java.awt.Dimension;

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
            Dimension ps = new Dimension(224, 54);
            int fontSize = 14;
            String nickname = control.getNickname();
            MidiPort port = control.getLastReceivedFromMidiPort();
            String portName = "Nothing Received Yet";
            if (port != null)
            {
                portName = port.getName();
            }
            String description = "<br /><i style=\"color: black;\">" + portName + "</i>";
            if (control.getControlNumber() >= 0)
            {
                this.controlLabel.setText("<html><body style=\"padding: 3px 3px 3px 3px; width: 150px;\"><b style=\"font-size: "+String.valueOf(fontSize)+"px;\">" + nickname + "</b>" + description + "<br />" +
                                        "<i style=\"color: blue;\">Ch=" + String.valueOf(control.getChannel()) + "</i> " + 
                                        "<i style=\"color: #672E97;\">CC=" + String.valueOf(control.getControlNumber()) + "</i> " + 
                                        "<span style=\"color: red;\"> V=" + String.valueOf(control.getValue())+ "</span></body></html>");
            } else {
                this.controlLabel.setText("<html><body style=\"padding: 3px 3px 3px 3px; width: 150px;\"><b style=\"font-size: "+String.valueOf(fontSize)+"px;\">" + nickname + "</b>" + description + "<br />" +
                                        "<i style=\"color: blue;\">Ch=" + String.valueOf(control.getChannel()) + "</i> " + 
                                        "<i style=\"color: #672E97;\">Note=" + control.getNoteName() + "</i> " + 
                                        "<span style=\"color: red;\"> V=" + String.valueOf(control.getValue())+ "</span></body></html>");
            }
            int value = control.getValue();
            if (value >= 0)
                valueBar.setValue(value);
            else
                valueBar.setValue(0);

            this.setFont(list.getFont());
            this.setEnabled(list.isEnabled());
            controlLabel.setPreferredSize(ps);
            if (isSelected)
             this.setBorder(border);
            else
             this.setBorder(null);
         } catch (Exception e) {
             e.printStackTrace(System.err);
         }

      return this;
   }
}
