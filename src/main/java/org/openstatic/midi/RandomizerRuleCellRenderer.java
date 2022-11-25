package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;
import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.json.JSONObject;

import javax.swing.border.Border;

public class RandomizerRuleCellRenderer extends JPanel implements ListCellRenderer<JSONObject>
{
   private Border selectedBorder;
   private JCheckBox checkBox;

   public RandomizerRuleCellRenderer()
   {
      super(new BorderLayout());
      this.setOpaque(true);
      this.setBackground(Color.WHITE);
      this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 1);
      this.checkBox = new JCheckBox("");
      this.checkBox.setOpaque(false);
      this.add(this.checkBox, BorderLayout.CENTER);

   }

   @Override
   public Component getListCellRendererComponent(JList<? extends JSONObject> list,
                                                 JSONObject rule,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      String ruleHTML = "<html>CH# "+ String.valueOf(rule.getInt("channel")) +" CC# "+ String.valueOf(rule.getInt("cc")) +" Value Range " + String.valueOf(rule.getInt("min")) + "-" + String.valueOf(rule.getInt("max"))
                      + " Change Delay " + String.valueOf(rule.getInt("changeDelay")) + "ms" + (rule.optBoolean("smooth",false) ? " SMOOTH" :"") + "</html>";
      this.checkBox.setText(ruleHTML);
      this.checkBox.setSelected(rule.optBoolean("enabled", true));
      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());
      if (isSelected)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(null);

      return this;
   }
}
