package org.openstatic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import javax.swing.border.Border;

import org.openstatic.midi.MidiToolsPlugin;

public class MidiControlRuleCellRenderer extends JPanel implements ListCellRenderer<MidiControlRule>
{
   private Border selectedBorder;
   private Border regularBorder;
   private JCheckBox checkBox;
   private ImageIcon speakerIcon;
   private ImageIcon logIcon;
   private ImageIcon dialIcon;
   private ImageIcon disableIcon;
   private ImageIcon enableIcon;
   private ImageIcon gearsIcon;
   private ImageIcon toggleIcon;
   private ImageIcon urlIcon;
   private ImageIcon pluginIcon;
   private ImageIcon imageIcon;
   private ImageIcon mappingIcon;
   private ImageIcon noteIcon;

   
   public MidiControlRuleCellRenderer()
   {
      super(new BorderLayout());
      this.setOpaque(true);
      this.setBackground(Color.WHITE);
      this.selectedBorder = BorderFactory.createLineBorder(Color.RED, 1);
      this.checkBox = new JCheckBox("");
      this.checkBox.setOpaque(false);
      this.add(this.checkBox, BorderLayout.CENTER);
      try
      {
         this.speakerIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/speaker32.png")));
         this.logIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/log32.png")));
         this.disableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/disable32.png")));
         this.enableIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/enable32.png")));
         this.dialIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/dial32.png")));
         this.gearsIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/gears32.png")));
         this.toggleIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/toggle32.png")));
         this.urlIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/url32.png")));
         this.pluginIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/plug_in.png")));
         this.imageIcon = new  ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/image32.png")));
         this.mappingIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/cable32.png")));
         this.noteIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/midifile32.png")));
      } catch (Exception e) {

      }
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
         this.checkBox.setText("<html><body style=\"padding: 3px 3px 3px 3px;\"><b style=\"font-size: 14px;\">" + rule.getNickname() + "</b><br />" + rule.toString() + "</body></html>");
      } else {
         this.checkBox.setText("<html><body style=\"padding: 3px 3px 3px 3px;\">" + rule.toString() + "</body></html>");
      }
      if (rule.getActionType() == MidiControlRule.ACTION_SOUND)
      {
         this.checkBox.setIcon(this.speakerIcon);
      } else if (rule.getActionType() == MidiControlRule.LOGGER_A_MESSAGE || rule.getActionType() == MidiControlRule.LOGGER_B_MESSAGE) {
         this.checkBox.setIcon(this.logIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_TRANSMIT) {
         this.checkBox.setIcon(this.dialIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_TRANSMIT_NOTE_ON) {
         this.checkBox.setIcon(this.noteIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_TRANSMIT_NOTE_OFF) {
         this.checkBox.setIcon(this.noteIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_ENABLE_RULE_GROUP) {
         this.checkBox.setIcon(this.enableIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_DISABLE_RULE_GROUP) {
         this.checkBox.setIcon(this.disableIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_TOGGLE_RULE_GROUP) {
         this.checkBox.setIcon(this.toggleIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_PLUGIN) {
         MidiToolsPlugin plugin = rule.getSelectedPlugin();
         if (plugin != null)
         {
            Icon pluginSelfIcon = plugin.getIcon();
            if (pluginSelfIcon != null)
               this.checkBox.setIcon(pluginSelfIcon);
            else
               this.checkBox.setIcon(this.pluginIcon);
         } else {
            this.checkBox.setIcon(this.pluginIcon);
         }
      } else if (rule.getActionType() == MidiControlRule.ACTION_PROC) {
         this.checkBox.setIcon(this.gearsIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_URL) {
         this.checkBox.setIcon(this.urlIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_EFFECT_IMAGE) {
         this.checkBox.setIcon(this.imageIcon);
      } else if (rule.getActionType() == MidiControlRule.ACTION_ENABLE_MAPPING || rule.getActionType() == MidiControlRule.ACTION_DISABLE_MAPPING || rule.getActionType() == MidiControlRule.ACTION_TOGGLE_MAPPING) {
         this.checkBox.setIcon(this.mappingIcon);
      } else {
         this.checkBox.setIcon(this.gearsIcon);
      }
      if (rule.isEnabled())
      {
         
         if ((System.currentTimeMillis() - rule.getLastFailed()) < 1000l)
         {
            this.setBackground(new Color(255,102,102));
         } else if ((System.currentTimeMillis() - rule.getLastTriggered()) < 1000l) {
            this.setBackground(new Color(102,255,102));
         } else {
            this.setBackground(Color.WHITE);
         }
      } else {
         this.setBackground(Color.GRAY);
      }
      this.checkBox.setSelected(rule.isEnabled());

      this.setFont(list.getFont());
      this.setEnabled(list.isEnabled());

      if (isSelected)
      {
         this.setBorder(this.selectedBorder);
      } else {
         this.setBorder(this.regularBorder);
      }
      return this;
   }
}
