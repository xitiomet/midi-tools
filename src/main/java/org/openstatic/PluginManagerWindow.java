package org.openstatic;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openstatic.util.PendingURLFetch;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Color;

public class PluginManagerWindow extends JDialog implements ActionListener, Runnable, ListSelectionListener
{
    private PendingURLFetch pluginFetch;
    private JList<JSONObject> pluginList;
    private JSONArray pluginData;
    private Thread fetchThread;
    private PluginListRenderer pluginListRenderer;
    private JButton closeButton;
    private JButton actionButton;
    private JPanel buttonPanel;
    private ImageIcon gears;
    private boolean fetchedOk;

    private class PluginListRenderer extends JPanel implements ListCellRenderer<JSONObject>
    {
        private Border selectedBorder;
        private JCheckBox checkBox;
        
        public PluginListRenderer() 
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
        public Component getListCellRendererComponent(JList<? extends JSONObject> list, JSONObject value, int index,
                boolean isSelected, boolean cellHasFocus) {
            String installed_text  = "(Not Installed)";
            if (value.optBoolean("installed"))
                installed_text = "(Installed)";
            String title = value.optString("title","(no title)");
            String description = value.optString("description", "(No Description)");
            this.checkBox.setText("<html><body style=\"padding: 3px 3px 3px 3px;\"><b>" + title + " " + installed_text + "</b><br /><span>" + description + "</span></body></html>");
            this.checkBox.setIcon(new ImageIcon(MidiTools.getCachedImage("https://openstatic.org/projects/miditools/" + value.optString("icon", "icon.png"), "32x32")));
            this.checkBox.setToolTipText(description);
            if (isSelected)
            {
                this.setBorder(this.selectedBorder);
            } else {
                this.setBorder(null);
            }
            return this;
        }
        
    };

    public void actionPerformed(ActionEvent e)
    {
        JSONObject selectedPlugin = (JSONObject) this.pluginList.getSelectedValue();
        String actionCommand = e.getActionCommand();
        if ("close".equals(actionCommand))
        {
            this.dispose();
        }
        if ("install".equals(actionCommand))
        {
            String filename = selectedPlugin.optString("filename");
            MidiTools.instance.installPlugin("https://openstatic.org/projects/miditools/", filename);
            selectedPlugin.put("installed", true);
            this.pluginList.setSelectedValue(null, false);
            this.pluginList.repaint();
        }
        if ("uninstall".equals(actionCommand))
        {
            MidiTools.instance.removePlugin(selectedPlugin.optString("title"), selectedPlugin.optString("localFile"));
            selectedPlugin.put("installed", false);
            this.pluginList.setSelectedValue(null, false);
            this.pluginList.repaint();
        }
    }
    
    public PluginManagerWindow()
    {
        super(MidiTools.instance, "MidiTools Plugin Manager", true);
        this.pluginListRenderer = new PluginListRenderer();
        this.setLayout(new BorderLayout());
        this.pluginFetch = new PendingURLFetch("https://openstatic.org/projects/miditools/plugins.json");
        this.fetchedOk = false;
        try
        {
            this.gears = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/gears.gif")));
        } catch (Exception gex) {
            gex.printStackTrace(System.err);
        }
        this.pluginList = new JList<JSONObject>()
        {
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                if (!PluginManagerWindow.this.fetchedOk)
                {
                    int iconHeight = PluginManagerWindow.this.gears.getIconHeight();
                    int x = (this.getWidth() - PluginManagerWindow.this.gears.getIconWidth()) / 2;
                    int y = ((this.getHeight() - iconHeight) / 2) - 30;
                    PluginManagerWindow.this.gears.paintIcon(this, g, x, y);
                    g.drawString("Searching for MidiTools Plugins", x -10, y + iconHeight + 20);
                }
            }
        };
        this.pluginList.setCellRenderer(pluginListRenderer);
        this.pluginList.addListSelectionListener(this);
        this.pluginList.setFixedCellHeight(50);
        JScrollPane pluginScrollPane = new JScrollPane(this.pluginList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(pluginScrollPane, BorderLayout.CENTER);
        this.actionButton = new JButton("Install");
        this.actionButton.addActionListener(this);
        this.actionButton.setActionCommand("install");
        this.actionButton.setEnabled(false);
        this.closeButton = new JButton("Close");
        this.closeButton.setActionCommand("close");
        this.closeButton.addActionListener(this);
        this.buttonPanel = new JPanel(new GridLayout(1,2));
        this.buttonPanel.add(this.actionButton);
        this.buttonPanel.add(this.closeButton);
        this.add(this.buttonPanel, BorderLayout.PAGE_END);
        this.fetchThread = new Thread(this);
        this.fetchThread.start();
        this.centerWindow();
    } 

    public JPanel labelComponent(String label, Component c)
    {
        JPanel x = new JPanel(new GridLayout(1,2));
        x.add(new JLabel(label, SwingConstants.CENTER));
        x.add(c);
        return x;
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 500;
        int wHeight = 450;
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setBounds(x, y, wWidth, wHeight);
        this.pluginList.setSize(wWidth, wHeight);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    @Override
    public void run() 
    {
        try
        {
            this.pluginFetch.run();
            String responseData = this.pluginFetch.getResponse();
            System.err.println("Plugins Response: " + responseData);
            this.pluginData = new JSONArray(responseData);
            int c = this.pluginData.length();
            JSONObject[] resultArray = new JSONObject[c];
            for(int i = 0; i < c; i++)
            {
                resultArray[i] = this.pluginData.getJSONObject(i);
                File pluginLocalFile = new File(MidiTools.getPluginFolder(), resultArray[i].optString("filename", "XXX"));
                boolean plugin_installed = pluginLocalFile.exists();
                resultArray[i].put("installed", plugin_installed);
                resultArray[i].put("localFile", pluginLocalFile.toString());
            }
            this.pluginList.setListData(resultArray);
            this.pluginList.repaint();
            this.fetchedOk = true;
        } catch (Exception pfex) {
            try
            {
                Thread.sleep(4000);
                this.fetchThread = new Thread(this);
                this.fetchThread.start();
            } catch (Exception rex) {}
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        JSONObject selectedPlugin = (JSONObject) this.pluginList.getSelectedValue();
        if (selectedPlugin == null)
        {
            this.actionButton.setText("(select a plugin)");
            this.actionButton.setEnabled(false);
        } else {
            this.actionButton.setEnabled(true);
            if (selectedPlugin.optBoolean("installed"))
            {
                this.actionButton.setText("Uninstall");
                this.actionButton.setActionCommand("uninstall");
            } else {
                this.actionButton.setText("Install");
                this.actionButton.setActionCommand("install");
            }
        }
        
    }
}
