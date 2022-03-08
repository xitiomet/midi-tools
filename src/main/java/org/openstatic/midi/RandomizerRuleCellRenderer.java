package org.openstatic.midi;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import javax.sound.midi.*;
import java.util.Iterator;

import javax.swing.border.Border;

public class RandomizerRuleCellRenderer extends JCheckBox implements ListCellRenderer<JSONObject>
{
   private Border selectedBorder;
   private Border regularBorder;

   public RandomizerRuleCellRenderer()
   {
       super();
       this.setOpaque(false);
       this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 3);
       this.regularBorder = BorderFactory.createLineBorder(new Color(1f,1f,1f,1f), 3);

   }

   @Override
   public Component getListCellRendererComponent(JList<? extends JSONObject> list,
                                                 JSONObject rule,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      String ruleHTML = "<html>CH# "+ String.valueOf(rule.getInt("channel")) +" CC# "+ String.valueOf(rule.getInt("cc")) +" Value Range " + String.valueOf(rule.getInt("min")) + "-" + String.valueOf(rule.getInt("max"))
                      + " Change Delay " + String.valueOf(rule.getInt("changeDelay")) + (rule.optBoolean("smooth",false) ? " SMOOTH" :"") + "</html>";
      this.setText(ruleHTML);
      if (isSelected)
      {
         this.setBackground(list.getSelectionBackground());
         this.setForeground(list.getSelectionForeground());
      } else {
         this.setBackground(list.getBackground());
         this.setForeground(list.getForeground());
      }
      this.setSelected(rule.optBoolean("enabled", true));
      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());
      if (isSelected && cellHasFocus)
         this.setBorder(this.selectedBorder);
      else
         this.setBorder(this.regularBorder);

      return this;
   }
}
