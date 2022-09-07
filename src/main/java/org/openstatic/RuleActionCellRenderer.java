package org.openstatic;

import java.awt.Component;
import java.awt.BorderLayout;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

public class RuleActionCellRenderer extends JPanel implements ListCellRenderer<String> 
{
    public JLabel label;
    private ImageIcon speakerIcon;
    private ImageIcon logIcon;
    private ImageIcon dialIcon;
    private ImageIcon disableIcon;
    private ImageIcon enableIcon;
    private ImageIcon gearsIcon;
    private ImageIcon toggleIcon;
    private ImageIcon urlIcon;
    private ImageIcon pluginIcon;

    public RuleActionCellRenderer()
    {
        super(new BorderLayout());
        this.label = new JLabel();
        this.add(this.label, BorderLayout.CENTER);
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

        } catch (Exception e) {

        }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {
        this.label.setText(value);
        if ("CALL URL".equals(value))
        {
            this.label.setIcon(this.urlIcon);
        } else if ("RUN PROGRAM".equals(value)) {
            this.label.setIcon(this.gearsIcon);
        } else if ("PLAY SOUND".equals(value)) {
            this.label.setIcon(this.speakerIcon);
        } else if ("TRANSMIT CONTROL CHANGE".equals(value)) {
            this.label.setIcon(this.dialIcon);
        } else if ("ENABLE RULE GROUP".equals(value)) {
            this.label.setIcon(this.enableIcon);
        } else if ("DISABLE RULE GROUP".equals(value)) {
            this.label.setIcon(this.disableIcon);
        } else if ("TOGGLE RULE GROUP".equals(value)) {
            this.label.setIcon(this.toggleIcon);
        } else if ("LOGGER A MESSAGE".equals(value)) {
            this.label.setIcon(this.logIcon);
        } else if ("LOGGER B MESSAGE".equals(value)) {
            this.label.setIcon(this.logIcon);
        } else if ("PLUGIN".equals(value)) {
            this.label.setIcon(this.pluginIcon);
        } else {
            this.label.setIcon(this.gearsIcon);
        }
        return this;
    }
    
}
