package org.openstatic;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import javax.swing.border.Border;

public class MidiControlRuleCellRenderer extends JCheckBox implements ListCellRenderer<MidiControlRule>
{
   private Border selectedBorder;
   private Border regularBorder;

   public MidiControlRuleCellRenderer()
   {
       super();
       this.setOpaque(true);
       this.setBackground(Color.WHITE);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 3);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);

   }

   @Override
   public Component getListCellRendererComponent(JList<? extends MidiControlRule> list,
                                                 MidiControlRule rule,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      if (rule.getNickname() != null)
      {
         this.setText("<html><body style=\"padding: 3px 3px 3px 3px;\"><b style=\"font-size: 14px;\">" + rule.getNickname() + "</b><br />" + rule.toString() + "</body></html>");
      } else {
         this.setText("<html><body style=\"padding: 3px 3px 3px 3px;\">" + rule.toString() + "</body></html>");
      }
      if ((System.currentTimeMillis() - rule.getLastTriggered()) < 1000l)
      {
         this.setBackground(new Color(102,255,102));
      } else {
         this.setBackground(Color.WHITE);
      }
      this.setSelected(rule.isEnabled());

      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());

      if (isSelected && cellHasFocus)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(this.regularBorder);

      return this;
   }
}
