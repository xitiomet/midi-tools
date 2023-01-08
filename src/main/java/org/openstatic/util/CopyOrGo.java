package org.openstatic.util;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Toolkit;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.net.URI;
import java.awt.event.ActionEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Point;

public class CopyOrGo extends JDialog implements ActionListener 
{
   private JTextArea urlField;

   private JButton btnGo;
   private JButton btnCopy;
   private JButton btnCancel;

   public CopyOrGo(Frame parent, String url)
   {
      super(parent,"MIDI Tools wants to open a URL",true);
      Point loc = parent.getLocation();
      Dimension pDim = parent.getSize();
      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panel.setLayout(new BorderLayout());

      this.urlField = new JTextArea(url);
      this.urlField.setMaximumSize(new Dimension(320, 240));
      this.urlField.setLineWrap(true);
      this.urlField.setRows(5);
      panel.add(this.urlField, BorderLayout.CENTER);

      this.btnGo = new JButton("Open in Browser");
      this.btnGo.addActionListener(this);
      this.btnGo.setDefaultCapable(true);
      this.btnCopy = new JButton("Copy URL");
      this.btnCopy.addActionListener(this);
      this.btnCancel = new JButton("Cancel");
      this.btnCancel.addActionListener(this);

      this.getRootPane().setDefaultButton(this.btnGo);

      JPanel buttonPanel = new JPanel();
      buttonPanel.add(this.btnCancel);
      buttonPanel.add(this.btnCopy);
      buttonPanel.add(this.btnGo);

      panel.add(buttonPanel, BorderLayout.PAGE_END);
      getContentPane().add(panel);
      pack();
      setLocation(loc.x + (pDim.width / 2) - (this.getWidth() / 2), loc.y + (pDim.height / 2) - (this.getHeight() / 2));
   }
   public void actionPerformed(ActionEvent ae) {
      Object source = ae.getSource();
      if (source == btnGo)
      {
        try
        {
            Desktop dt = Desktop.getDesktop();
            dt.browse(new URI(this.urlField.getText()));
            dispose();
        } catch (Exception e) {

        }
      } else if (source == btnCopy) {
        Toolkit.getDefaultToolkit()
        .getSystemClipboard()
        .setContents(
                new StringSelection(this.urlField.getText()),
                null
        );
        dispose();
      } else if (source == btnCancel) {
        dispose();
      }
   }
}
