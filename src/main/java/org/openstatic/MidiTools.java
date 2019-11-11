package org.openstatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;
import java.util.Arrays;

import java.net.URI;

import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.DefaultListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Desktop;

import javax.sound.midi.*;

import org.json.*;

public class MidiTools extends JFrame implements Runnable, Receiver, ActionListener
{
    protected JList controlList;
    private JList midiList;
    private JList rulesList;
    private JPopupMenu controlMenu;
    protected MidiControlCellRenderer midiControlCellRenderer;
    private MidiSourceCellRenderer midiRenderer;
    private MidiSourceListModel midiListModel;
    private Thread mainThread;
    protected DefaultListModel<MidiControl> controls;
    protected DefaultListModel<MidiControlRule> rules;
    protected LinkedBlockingQueue<Runnable> taskQueue;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu apiMenu;
    private JCheckBoxMenuItem apiServerEnable;
    private JMenuItem aboutMenuItem;
    private JMenuItem deleteControlMenuItem;
    private JMenuItem renameControlMenuItem;
    private JMenuItem createRuleMenuItem;
    private JMenuItem exportConfigurationMenuItem;
    private JMenuItem importConfigurationMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem resetConfigurationMenuItem;
    private File lastSavedFile;
    private JMenuItem exitMenuItem;
    public static MidiTools instance;
    private boolean keep_running;
    private long lastControlClick;
    private APIWebServer apiServer;
    private JSONObject options;

    public MidiTools()
    {
        super("MIDI Control Change Tool");
        this.options = new JSONObject();
        this.taskQueue = new LinkedBlockingQueue<Runnable>();
        this.keep_running = true;
        MidiTools.instance = this;
        this.apiServer = new APIWebServer();

        centerWindow();
        this.setLayout(new BorderLayout());
        try
        {
            BufferedImage windowIcon = ImageIO.read(getClass().getResource("/windows.png"));
            this.setIconImage(windowIcon);
        } catch (Exception iconException) {}
        
        this.controlMenu = new JPopupMenu("control");
        
        this.deleteControlMenuItem = new JMenuItem("Delete Control");
        this.deleteControlMenuItem.setMnemonic(KeyEvent.VK_D);
        this.deleteControlMenuItem.addActionListener(this);
        this.deleteControlMenuItem.setActionCommand("delete_control");
        
        this.renameControlMenuItem = new JMenuItem("Rename Control");
        this.renameControlMenuItem.setMnemonic(KeyEvent.VK_R);
        this.renameControlMenuItem.addActionListener(this);
        this.renameControlMenuItem.setActionCommand("rename_control");
        
        this.createRuleMenuItem = new JMenuItem("Create Rule");
        this.createRuleMenuItem.setMnemonic(KeyEvent.VK_C);
        this.createRuleMenuItem.addActionListener(this);
        this.createRuleMenuItem.setActionCommand("create_rule");
        
        this.controlMenu.add(this.renameControlMenuItem);
        this.controlMenu.add(this.deleteControlMenuItem);
        this.controlMenu.add(this.createRuleMenuItem);
        
        this.menuBar = new JMenuBar();
        
        this.fileMenu = new JMenu("File");
        this.fileMenu.setMnemonic(KeyEvent.VK_F);
        this.resetConfigurationMenuItem = new JMenuItem("New Project");
        this.resetConfigurationMenuItem.setMnemonic(KeyEvent.VK_N);
        this.resetConfigurationMenuItem.addActionListener(this);
        this.resetConfigurationMenuItem.setActionCommand("reset");
        
        this.exportConfigurationMenuItem = new JMenuItem("Save Project As");
        this.exportConfigurationMenuItem.setMnemonic(KeyEvent.VK_A);
        this.exportConfigurationMenuItem.addActionListener(this);
        this.exportConfigurationMenuItem.setActionCommand("export");
        
        this.saveMenuItem = new JMenuItem("Save Project");
        this.saveMenuItem.setMnemonic(KeyEvent.VK_S);
        this.saveMenuItem.addActionListener(this);
        this.saveMenuItem.setActionCommand("save");
        
        this.importConfigurationMenuItem = new JMenuItem("Open Project");
        this.importConfigurationMenuItem.setMnemonic(KeyEvent.VK_O);
        this.importConfigurationMenuItem.addActionListener(this);
        this.importConfigurationMenuItem.setActionCommand("import");
        
        this.aboutMenuItem = new JMenuItem("About");
        this.aboutMenuItem.setMnemonic(KeyEvent.VK_B);
        this.aboutMenuItem.addActionListener(this);
        this.aboutMenuItem.setActionCommand("about");
        
        this.exitMenuItem = new JMenuItem("Exit");
        this.exitMenuItem.setMnemonic(KeyEvent.VK_X);
        this.exitMenuItem.addActionListener(this);
        this.exitMenuItem.setActionCommand("exit");
        
        this.fileMenu.add(this.resetConfigurationMenuItem);
        this.fileMenu.add(this.saveMenuItem);
        this.fileMenu.add(this.exportConfigurationMenuItem);
        this.fileMenu.add(this.importConfigurationMenuItem);
        this.fileMenu.add(new JSeparator());
        this.fileMenu.add(this.aboutMenuItem);
        this.fileMenu.add(new JSeparator());
        this.fileMenu.add(this.exitMenuItem);
        
        this.apiMenu = new JMenu("API");
        this.apiServerEnable = new JCheckBoxMenuItem("Enable API Web Server");
        this.apiServerEnable.addActionListener(this);
        this.apiMenu.add(this.apiServerEnable);
        
        this.menuBar.add(this.fileMenu);
        this.menuBar.add(this.apiMenu);
        
        this.setJMenuBar(this.menuBar);
        
        this.controls = new DefaultListModel<MidiControl>();
        this.rules = new DefaultListModel<MidiControlRule>();

        this.midiControlCellRenderer = new MidiControlCellRenderer();

        // Setup toy list
        this.controlList = new JList(this.controls);
        this.controlList.setCellRenderer(this.midiControlCellRenderer);
        this.controlList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                int index = MidiTools.this.controlList.locationToIndex(e.getPoint());
                if (index != -1)
                {
                    if (e.getButton() == MouseEvent.BUTTON1)
                    {
                       long cms = System.currentTimeMillis();
                       if (cms - MidiTools.this.lastControlClick < 500 && MidiTools.this.lastControlClick > 0)
                       {
                            MidiTools.this.controlMenu.show(MidiTools.this.controlList, e.getX(), e.getY()); 
                       }
                       MidiTools.this.lastControlClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      MidiTools.this.controlMenu.show(MidiTools.this.controlList, e.getX(), e.getY()); 
                   }
               }
            }
        });
        
        JScrollPane lovenseToyScrollPane = new JScrollPane(this.controlList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        lovenseToyScrollPane.setBorder(new TitledBorder("Midi Controls"));
        JPanel toysAndPower = new JPanel(new BorderLayout());
        toysAndPower.add(lovenseToyScrollPane, BorderLayout.CENTER);

        // Setup rule list
        this.rulesList = new JList(this.rules);
        JScrollPane ruleScrollPane = new JScrollPane(this.rulesList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ruleScrollPane.setBorder(new TitledBorder("Rules for Incoming MIDI Messages (click to edit)"));
        this.rulesList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = MidiTools.this.rulesList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                  MidiControlRule source = (MidiControlRule) MidiTools.this.rules.getElementAt(index);
                  MidiControlRuleEditor editor = new MidiControlRuleEditor(source);
               }
            }
        });
        
        this.add(toysAndPower, BorderLayout.CENTER);

        this.midiListModel = new MidiSourceListModel();
        this.midiRenderer = new MidiSourceCellRenderer();
        MidiSourceManager.addMidiSourceListener(this.midiListModel);
        this.midiList = new JList(this.midiListModel);
        this.midiList.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
               int index = MidiTools.this.midiList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                  MidiSource source = (MidiSource) MidiTools.this.midiListModel.getElementAt(index);
                  if (source.isOpened())
                  {
                      source.close();
                  } else {
                      source.open();
                      source.setReceiver(MidiTools.this);
                  }
                  repaint();
               }
            }
        });
        this.midiList.setCellRenderer(this.midiRenderer);
        JScrollPane scrollPane2 = new JScrollPane(this.midiList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane2.setBorder(new TitledBorder("MIDI Input Devices"));
        this.add(scrollPane2, BorderLayout.WEST);
        this.add(ruleScrollPane, BorderLayout.PAGE_END);

        this.mainThread = new Thread(this);
        this.mainThread.setDaemon(true);
        this.mainThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread() 
        { 
          public void run() 
          { 
            MidiTools.this.keep_running = false;
            //System.out.println("Shutdown Hook is running!"); 
            saveConfig();
          } 
        }); 
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadConfig();
        this.apiServerEnable.setState(this.options.optBoolean("apiServer", false));
        this.apiServer.setState(this.apiServerEnable.getState());
    }
    
    public void resetConfiguration()
    {
        for (Enumeration<MidiControl> mce = this.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            mc.removeAllListeners();
        }
        this.controls.clear();
        this.rules.clear();
        this.setLastSavedFile(null);
    }
    
    public void setLastSavedFile(File f)
    {
        this.lastSavedFile = f;
        if (this.lastSavedFile != null)
        {
            this.setTitle("MIDI Control Change Tool - [" + this.lastSavedFile.toString() + "]");
        } else {
            this.setTitle("MIDI Control Change Tool");
        }
    }
    
    public static void addTask(Runnable r)
    {
        MidiTools.instance.taskQueue.add(r);
    }
    
    public static MidiControl getMidiControlByIndex(int i)
    {
        return MidiTools.instance.controls.elementAt(i);
    }
    
    public static int getIndexForMidiControl(MidiControl m)
    {
        return MidiTools.instance.controls.indexOf(m);
    }
    
    public static void removeListenerFromControls(MidiControlListener mcl)
    {
        for (Enumeration<MidiControl> mce = MidiTools.instance.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            mc.removeMidiControlListener(mcl);
        }
    }

    protected static String noteNumberToString(int i)
    {
        String[] noteString = new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return noteString[i%12] + String.valueOf( ((int)Math.floor(((float)i)/12f)) - 2);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.apiServerEnable)
        {
            boolean state = this.apiServerEnable.getState();
            this.options.put("apiServer", state);
            this.apiServer.setState(state);
            return;
        }
        String cmd = e.getActionCommand();
        if (cmd.equals("save") && this.lastSavedFile == null)
            cmd = "export";
        
        if (cmd.equals("reset"))
        {
            int n = JOptionPane.showConfirmDialog(null,
            "Are you sure? This will clear all rules and controls",
            "Reset Confirmation",
            JOptionPane.YES_NO_OPTION);
            if(n == JOptionPane.YES_OPTION)
            {
                resetConfiguration();
            }
        } else if (cmd.equals("exit")) {
            System.exit(0);
        } else if (cmd.equals("about")) {
            browseTo("http://openstatic.org/miditools/");
        } else if (cmd.equals("save")) {
            saveConfigAs(this.lastSavedFile);
        } else if (cmd.equals("export")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to save");   
            FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Data", "json");
            fileChooser.setFileFilter(filter);
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                File fileToSave = fileChooser.getSelectedFile();
                saveConfigAs(fileToSave);
            }
        } else if (cmd.equals("import")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to open");   
            FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Data", "json");
            fileChooser.setFileFilter(filter);
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                File fileToLoad = fileChooser.getSelectedFile();
                loadConfigFrom(fileToLoad);
            }
        } else if (cmd.equals("create_rule")) {
            MidiControl t = (MidiControl) MidiTools.this.controlList.getSelectedValue();
            MidiControlRule newRule = new MidiControlRule(t, 1, 0, null);
            MidiControlRuleEditor editor = new MidiControlRuleEditor(newRule, true);
        } else if (cmd.equals("rename_control")) {
            MidiControl t = (MidiControl) MidiTools.this.controlList.getSelectedValue();
            String s = (String)JOptionPane.showInputDialog(this,"Rename Control", t.getNickname());
            if (s!= null && t != null)
            {
                t.setNickname(s);
            }
        } else if (cmd.equals("delete_control")) {
            MidiControl t = (MidiControl) MidiTools.this.controlList.getSelectedValue();
            if (t != null)
            {
                removeMidiControl(t);
            }
        }
    }

    public void run()
    {
        while(this.keep_running)
        {
            try
            {
                for (Enumeration<MidiControl> mce = this.controls.elements(); mce.hasMoreElements();)
                {
                    MidiControl mc = mce.nextElement();
                    if (!mc.isSettled())
                        mc.settle();
                }
                Runnable r = this.taskQueue.poll(1, TimeUnit.SECONDS);
                if (r != null)
                {
                    r.run();
                    String taskName = r.toString();
                    if (!taskName.contains("Lambda"))
                        System.err.println("TaskComplete> " + taskName);
                }
                MidiSourceManager.refresh();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 800;
        int wHeight = 500;
        Dimension d = new Dimension(wWidth, wHeight);
        this.setSize(d);
        //this.setMaximumSize(d);
        this.setMinimumSize(d);
        //this.setResizable(false);
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setLocation(x, y);
    }

    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            
        }
        MidiTools mlb = new MidiTools();
        mlb.setVisible(true);
    }
    
    private static String shortMessageToString(ShortMessage msg)
    {
        String channelText = "[CH=" + String.valueOf(msg.getChannel()+1) + "]";
        String commandText = "";
        String data1Name = "?";
        String data1Value = "?";
        
        if (msg.getCommand() == ShortMessage.CONTROL_CHANGE)
        {
            data1Name = "CC";
            data1Value = String.valueOf(msg.getData1());
            commandText = "CONTROL CHANGE";
        } else if (msg.getCommand() == ShortMessage.NOTE_ON) {
            data1Name = "NOTE";
            data1Value = MidiTools.noteNumberToString(msg.getData1());
            commandText = "NOTE ON";
        } else if (msg.getCommand() == ShortMessage.NOTE_OFF) {
            data1Name = "NOTE";
            data1Value = MidiTools.noteNumberToString(msg.getData1());
            commandText = "NOTE OFF";
        }
        String data1Text = "(" + data1Name + "=" + data1Value + ")";;
        return commandText + " " + channelText + " " + data1Text + " value=" + String.valueOf(msg.getData2());
    }
    
    public static MidiControl getMidiControlByChannelCC(int channel, int cc)
    {
        for (Enumeration<MidiControl> mce = MidiTools.instance.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            if (mc.getChannel() == channel && mc.getControlNumber() == cc)
            {
                return mc;
            }
        }
        return null;
    }

    public static MidiControl createMidiControlFromJSON(JSONObject jo)
    {
        int channel = jo.optInt("channel", 0);
        int cc = jo.optInt("cc", 0);
        MidiControl mc = getMidiControlByChannelCC(channel, cc);
        if (mc == null)
        {
            mc = new MidiControl(jo);
            handleNewMidiControl(mc);
        }
        return mc;
    }
    
    public static void removeMidiControl(MidiControl mc)
    {
        mc.removeAllListeners();
        MidiTools.instance.controls.removeElement(mc);
        for (Enumeration<MidiControlRule> re = MidiTools.instance.rules.elements(); re.hasMoreElements();)
        {
            MidiControlRule rule = re.nextElement();
            if (rule.getMidiControl() == mc)
                rule.setMidiControl(null);
        }
        JSONObject event = new JSONObject();
        event.put("event", "controlRemoved");
        event.put("control", mc.toJSONObject());
        MidiTools.instance.apiServer.broadcastJSONObject(event);
    }
    
    public static void handleNewMidiControl(MidiControl mc)
    {
        mc.addMidiControlListener(MidiTools.instance.apiServer);
        MidiTools.instance.controls.addElement(mc);
        JSONObject event = new JSONObject();
        event.put("event", "controlAdded");
        event.put("control", mc.toJSONObject());
        MidiTools.instance.apiServer.broadcastJSONObject(event);
    }

    // Receiver Method
    public void send(MidiMessage msg, long timeStamp)
    {
        boolean should_repaint = false;
        if(msg instanceof ShortMessage)
        {
            ShortMessage sm = (ShortMessage) msg;
            //System.err.println("Recieved Short Message Channel=" + String.valueOf(sm.getChannel()) + " CC=" + String.valueOf(sm.getData1()) + " value=" + String.valueOf(sm.getData2()));
            if (sm.getCommand() == ShortMessage.CONTROL_CHANGE)
            {
                boolean found_control = false;
                for (Enumeration<MidiControl> mce = this.controls.elements(); mce.hasMoreElements();)
                {
                    MidiControl mc = mce.nextElement();
                    if (mc.messageMatches(sm))
                    {
                        //System.err.println(shortMessageToString(sm) + " = " + String.valueOf(sm.getData2()));
                        try
                        {
                            mc.processMessage(sm);
                            found_control = true;
                            should_repaint = true;
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
                if (!found_control)
                {
                    MidiControl mc = new MidiControl(sm.getChannel()+1, sm.getData1());
                    handleNewMidiControl(mc);
                }
            }
        } else {
            System.err.println("Unknown non-short message " + msg.toString());
        }
        if (should_repaint)
        {
            this.controlList.repaint();
        }
    }

    public void close() {}
    

    public static MidiDevice findMidiDeviceReceiverByName(String devName)
    {
        try
        {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            Vector<MidiDevice.Info> newLocalDevices = new Vector<MidiDevice.Info>(Arrays.asList(infos));

            // Check for new devices added
            for(Iterator<MidiDevice.Info> newLocalDevicesIterator = newLocalDevices.iterator(); newLocalDevicesIterator.hasNext();)
            {
                MidiDevice.Info di = newLocalDevicesIterator.next();
                MidiDevice device = MidiSystem.getMidiDevice(di);
                String dName = di.toString();
                if (dName.equals(devName) && device.getMaxReceivers() != 0)
                {
                    //System.err.println("Found Device By Name: " + devName);
                    return device;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    public void loadConfig()
    {
        loadConfigFrom(new File("config.json"), false);
    }
    
    public void loadConfigFrom(File file)
    {
        loadConfigFrom(file, true);
    }


    public void loadConfigFrom(File file, boolean remember)
    {
        try
        {
            JSONObject configJson = loadJSONObject(file);
            if (configJson.has("controls"))
            {
                JSONArray controlsArray = configJson.getJSONArray("controls");
                for (int m = 0; m < controlsArray.length(); m++)
                {
                    createMidiControlFromJSON(controlsArray.getJSONObject(m));
                }
            }
            if (configJson.has("rules"))
            {
                JSONArray rulesArray = configJson.getJSONArray("rules");
                for (int m = 0; m < rulesArray.length(); m++)
                {
                    MidiControlRule mcr = new MidiControlRule(rulesArray.getJSONObject(m));
                    this.rules.addElement(mcr);
                }
            }
            if (configJson.has("options"))
                this.options = configJson.getJSONObject("options");
            if (remember)
                this.setLastSavedFile(file);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public void saveConfig()
    {
        saveConfigAs(new File("config.json"), false);
    }

    public void saveConfigAs(File file)
    {
        saveConfigAs(file, true);
    }
    
    public JSONArray rulesAsJSONArray()
    {
       JSONArray rulesArray = new JSONArray();
        for (Enumeration<MidiControlRule> mcre = this.rules.elements(); mcre.hasMoreElements();)
        {
            MidiControlRule mcr = mcre.nextElement();
            rulesArray.put(mcr.toJSONObject());
        }
        return rulesArray;
    }
    
    public JSONArray controlsAsJSONArray()
    {
        JSONArray controlsArray = new JSONArray();
        for (Enumeration<MidiControl> mce = this.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            controlsArray.put(mc.toJSONObject());
        }
        return controlsArray;
    }

    public void saveConfigAs(File file, boolean remember)
    {
        try
        {
            JSONObject configJson = new JSONObject();
            configJson.put("controls", this.controlsAsJSONArray());
            configJson.put("rules", this.rulesAsJSONArray());
            configJson.put("options", this.options);
            saveJSONObject(file, configJson);
            if (remember)
                this.setLastSavedFile(file);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static JSONObject loadJSONObject(File file)
    {
        try
        {
            FileInputStream fis = new FileInputStream(file);
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = fis.read()) != -1){
                builder.append((char)ch);
            }
            fis.close();
            JSONObject props = new JSONObject(builder.toString());
            return props;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void saveJSONObject(File file, JSONObject obj)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fos);
            ps.print(obj.toString());
            ps.close();
            fos.close();
        } catch (Exception e) {
        }
    }
    
    public static boolean browseTo(String url)
    {
        try
        {
            Desktop dt = Desktop.getDesktop();
            dt.browse(new URI(url));
            return true;
        } catch (Exception dt_ex) {
            return false;
        }
    }
}
