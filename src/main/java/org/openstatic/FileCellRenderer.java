package org.openstatic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;

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

public class FileCellRenderer extends JPanel implements ListCellRenderer<File>
{
   private Border selectedBorder;
   private Border regularBorder;
   private JCheckBox checkBox;
   private ImageIcon speakerIcon;
   private ImageIcon gearsIcon;
   private ImageIcon fileIcon;
   private ImageIcon folderIcon;
   private ImageIcon imageIcon;
   private ImageIcon soundFontIcon;
   private ImageIcon midiFileIcon;
   
   public FileCellRenderer()
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
         this.gearsIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/gears32.png")));
         this.folderIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/folder32.png")));
         this.imageIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/image32.png")));
         this.fileIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/file32.png")));
         this.soundFontIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/soundfont32.png")));
         this.midiFileIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/midifile32.png")));
      } catch (Exception e) {

      }
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends File> list,
                                                 File file,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus)
   {
      String filename = file.getName();
      int lastDot = filename.lastIndexOf(".");
      String exten = "";
      if (lastDot >= 0)
         exten = filename.substring(lastDot).toLowerCase();
      this.checkBox.setText("<html><body style=\"padding: 3px 3px 3px 3px;\">" + filename + "</body></html>");
      if (file.isDirectory())
      {
         this.checkBox.setIcon(this.folderIcon);
      } else if (exten.equals(".wav")) {
         this.checkBox.setIcon(this.speakerIcon);
      } else if (exten.equals(".mid") || exten.equals(".midi")) {
         this.checkBox.setIcon(this.midiFileIcon);
      } else if (exten.equals(".sf2")) {
         this.checkBox.setIcon(this.soundFontIcon);
      } else if (exten.equals(".gif") || exten.equals(".png") || exten.equals(".jpg") || exten.equals(".jpeg") || exten.equals(".bmp")) {
         this.checkBox.setIcon(this.imageIcon);
      } else if (exten.equals(".exe") || exten.equals(".cmd") || exten.equals(".bat") || exten.equals(".sh") || exten.equals(".php") || exten.equals(".py")) {
         this.checkBox.setIcon(this.gearsIcon);
      } else {
         this.checkBox.setIcon(this.fileIcon);
      }
      if (isSelected)
      {
         this.setBorder(this.selectedBorder);
      } else {
         this.setBorder(this.regularBorder);
      }
      return this;
   }
}
