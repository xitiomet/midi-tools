package org.openstatic;

import org.openstatic.midi.*;
import org.openstatic.midi.ports.LoggerMidiPort;
import org.openstatic.midi.ports.MidiRandomizerPort;
import org.openstatic.midi.ports.RTPMidiPort;
import org.openstatic.midi.providers.CollectionMidiPortProvider;
import org.openstatic.midi.providers.DeviceMidiPortProvider;
import org.openstatic.midi.providers.JoystickMidiPortProvider;

import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import java.net.URI;
import java.net.InetAddress;
import java.net.NetworkInterface;

import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.Desktop;

import org.openstatic.routeput.*;
import org.openstatic.routeput.client.*;

import javax.sound.midi.*;

import org.json.*;

public class MidiTools extends JFrame implements Runnable, Receiver, ActionListener, MidiPortListener
{
    public static final String VERSION = "1.0";
    public static String LOCAL_SERIAL;
    protected JList controlList;
    private JList midiList;
    private JList rulesList;
    private JList mappingList;
    private JPopupMenu controlMenuPopup;
    private JPanel deviceQRPanel;
    private JLabel qrLabel;
    protected MidiControlCellRenderer midiControlCellRenderer;
    protected MidiControlRuleCellRenderer midiControlRuleCellRenderer;
    private MidiPortCellRenderer midiRenderer;
    private MidiPortMappingCellRenderer midiPortMappingCellRenderer;
    private MidiPortListModel midiListModel;
    private LoggerMidiPort midi_logger;
    private Thread mainThread;
    private Thread taskThread;
    protected DefaultListModel<MidiControl> controls;
    protected DefaultListModel<MidiControlRule> rules;
    protected ArrayBlockingQueue<Runnable> taskQueue;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu controlsMenu;
    private JMenu apiMenu;
    private JTabbedPane bottomTabbedPane;
    private JCheckBoxMenuItem apiServerEnable;
    private JCheckBoxMenuItem createControlOnInput;
    private JCheckBoxMenuItem showQrItem;
    private JCheckBoxMenuItem bootstrapSSLItem;

    private JMenuItem openInBrowserItem;
    private JMenuItem createNewControlItem;
    private JMenuItem createNewMappingItem;
    private JMenuItem aboutMenuItem;
    private JMenuItem deleteControlMenuItem;
    private JMenuItem renameControlMenuItem;
    private JMenuItem createRuleMenuItem;
    private JMenuItem createRandomizerRuleMenuItem;
    private JMenuItem exportConfigurationMenuItem;
    private JMenuItem importConfigurationMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem resetConfigurationMenuItem;
    private File lastSavedFile;
    private JMenuItem exitMenuItem;
    public static MidiTools instance;
    private boolean keep_running;
    private long lastControlClick;
    private long lastRuleClick;
    private long lastDeviceClick;
    private long lastMappingClick;
    private APIWebServer apiServer;
    private MidiRandomizerPort randomizerPort;
    private RTPMidiPort rtpMidiPort;
    private JSONObject options;
    private Point windowLocation;
    private RoutePutClient routeputClient;
    private RoutePutSessionManager routeputSessionManager;

    public MidiTools()
    {
        super("MIDI Control Change Tool v" + MidiTools.VERSION);
        MidiTools.instance = this;
        this.options = new JSONObject();
        this.taskQueue = new ArrayBlockingQueue<Runnable>(1000);
        this.keep_running = true;
        this.apiServer = new APIWebServer();
        MidiPortManager.addProvider(this.apiServer);
        this.midi_logger = new LoggerMidiPort("Logger");
        this.randomizerPort = new MidiRandomizerPort("Randomizer");

        this.setLayout(new BorderLayout());
        try
        {
            BufferedImage windowIcon = ImageIO.read(getClass().getResource("/midi-tools-res/windows.png"));
            this.setIconImage(windowIcon);
        } catch (Exception iconException) {}
        
        this.controlMenuPopup = new JPopupMenu("control");
        
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
        
        this.showQrItem = new JCheckBoxMenuItem("Show QR Code");
        this.showQrItem.setEnabled(false);
        this.showQrItem.setMnemonic(KeyEvent.VK_Q);
        this.showQrItem.addActionListener(this);
        this.showQrItem.setActionCommand("show_qr");
        
        this.bootstrapSSLItem = new JCheckBoxMenuItem("Use Openstatic.org Relay");
        this.bootstrapSSLItem.setEnabled(true);
        this.bootstrapSSLItem.setMnemonic(KeyEvent.VK_O);
        this.bootstrapSSLItem.addActionListener(this);
        this.bootstrapSSLItem.setActionCommand("bootstrap_ssl");
        
        this.openInBrowserItem = new JMenuItem("Open in Browser");
        this.openInBrowserItem.setEnabled(false);
        this.openInBrowserItem.setMnemonic(KeyEvent.VK_B);
        this.openInBrowserItem.addActionListener(this);
        this.openInBrowserItem.setActionCommand("open_api");
        
        this.controlMenuPopup.add(this.renameControlMenuItem);
        this.controlMenuPopup.add(this.deleteControlMenuItem);
        this.controlMenuPopup.add(this.createRuleMenuItem);
        
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
        
        this.controlsMenu = new JMenu("MIDI");
        this.controlsMenu.setMnemonic(KeyEvent.VK_C);
        this.createNewControlItem = new JMenuItem("Create Control");
        this.createNewControlItem.setActionCommand("new_control");
        this.createNewControlItem.addActionListener(this);
        this.createNewControlItem.setMnemonic(KeyEvent.VK_C);
        
        this.createNewMappingItem = new JMenuItem("Create Port Mapping");
        this.createNewMappingItem.setActionCommand("new_mapping");
        this.createNewMappingItem.addActionListener(this);
        this.createNewMappingItem.setMnemonic(KeyEvent.VK_M);
        
        this.createControlOnInput = new JCheckBoxMenuItem("Create Control on Midi Input");
        this.createControlOnInput.addActionListener(this);
        this.createControlOnInput.setState(true);
        
        this.createRandomizerRuleMenuItem = new JMenuItem("Create Rule for Randomizer");
        this.createRandomizerRuleMenuItem.setActionCommand("new_random_rule");
        this.createRandomizerRuleMenuItem.addActionListener(this);
        
        this.options.put("createControlOnInput", true);
        this.controlsMenu.add(this.createNewMappingItem);
        this.controlsMenu.add(this.createControlOnInput);
        this.controlsMenu.add(this.createNewControlItem);
        this.controlsMenu.add(this.createRandomizerRuleMenuItem);
        
        this.apiMenu = new JMenu("Web Interface");
        this.apiMenu.setMnemonic(KeyEvent.VK_W);
        this.apiServerEnable = new JCheckBoxMenuItem("Enable Local Server");
        this.apiServerEnable.addActionListener(this);
        this.apiServerEnable.setMnemonic(KeyEvent.VK_E);

        this.apiMenu.add(this.apiServerEnable);
        this.apiMenu.add(this.showQrItem);
        this.apiMenu.add(this.bootstrapSSLItem);
        this.apiMenu.add(this.openInBrowserItem);
        
        this.menuBar.add(this.fileMenu);
        this.menuBar.add(this.controlsMenu);
        this.menuBar.add(this.apiMenu);
        
        this.setJMenuBar(this.menuBar);
        
        this.controls = new DefaultListModel<MidiControl>();
        this.rules = new DefaultListModel<MidiControlRule>();

        this.midiControlCellRenderer = new MidiControlCellRenderer();
        this.midiControlRuleCellRenderer = new MidiControlRuleCellRenderer();
        this.midiPortMappingCellRenderer = new MidiPortMappingCellRenderer();
        
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
                            MidiTools.this.controlMenuPopup.show(MidiTools.this.controlList, e.getX(), e.getY()); 
                       }
                       MidiTools.this.lastControlClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      MidiTools.this.controlMenuPopup.show(MidiTools.this.controlList, e.getX(), e.getY()); 
                   }
               }
            }
        });
        this.controlList.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_DELETE)
                {
                    MidiControl t = (MidiControl) MidiTools.this.controlList.getSelectedValue();
                    if (t != null)
                    {
                        (new Thread(() -> {
                            removeMidiControl(t);
                        })).start();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) { }

            @Override
            public void keyTyped(KeyEvent e) { }
        });

        
        JScrollPane controlsScrollPane = new JScrollPane(this.controlList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlsScrollPane.setBorder(new TitledBorder("Midi Controls"));
        JPanel toysAndPower = new JPanel(new BorderLayout());
        toysAndPower.add(controlsScrollPane, BorderLayout.CENTER);

        // Setup rule list
        this.rulesList = new JList(this.rules);
        this.rulesList.setCellRenderer(this.midiControlRuleCellRenderer);
        JScrollPane ruleScrollPane = new JScrollPane(this.rulesList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ruleScrollPane.setBorder(new TitledBorder("Rules for Incoming Control Change Messages (right-click to edit, double-click to toggle)"));
        this.rulesList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = MidiTools.this.rulesList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                   MidiControlRule source = (MidiControlRule) MidiTools.this.rules.getElementAt(index);
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - MidiTools.this.lastRuleClick < 500 && MidiTools.this.lastRuleClick > 0)
                       {
                          source.toggleEnabled();
                       }
                       MidiTools.this.lastRuleClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      MidiControlRuleEditor editor = new MidiControlRuleEditor(source);
                   }
               }
            }
        });
        
        this.add(toysAndPower, BorderLayout.CENTER);

        this.midiListModel = new MidiPortListModel();
        this.midiRenderer = new MidiPortCellRenderer();
        
        this.midiList = new JList(this.midiListModel);
        this.midiList.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
               int index = MidiTools.this.midiList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                  MidiPort source = (MidiPort) MidiTools.this.midiListModel.getElementAt(index);
                  if (e.getButton() == MouseEvent.BUTTON1)
                  {
                    long cms = System.currentTimeMillis();
                    if (cms - MidiTools.this.lastDeviceClick < 500 && MidiTools.this.lastDeviceClick > 0)
                    {
                      if (source.isOpened())
                      {
                          source.close();
                          source.removeReceiver(MidiTools.this);
                      } else {
                          source.open();
                          source.addReceiver(MidiTools.this);
                      }
                    }
                    MidiTools.this.lastDeviceClick = cms;
                  }
               }
            }
        });
        this.midiList.setCellRenderer(this.midiRenderer);
        JScrollPane scrollPane2 = new JScrollPane(this.midiList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane2.setBorder(new TitledBorder("MIDI Devices (double-click to toggle)"));
        
        this.deviceQRPanel = new JPanel(new BorderLayout());
        this.deviceQRPanel.add(scrollPane2, BorderLayout.CENTER);
        
        
        this.add(this.deviceQRPanel, BorderLayout.WEST);
        
        this.bottomTabbedPane = new JTabbedPane();
        this.bottomTabbedPane.setPreferredSize(new Dimension(0, 200));
        this.bottomTabbedPane.addTab("Control Change Rules", ruleScrollPane);
        
        // Setup rule list
        this.mappingList = new JList(new MidiPortMappingListModel());
        this.mappingList.setCellRenderer(this.midiPortMappingCellRenderer);
        this.mappingList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
               int index = MidiTools.this.mappingList.locationToIndex(e.getPoint());

               if (index != -1)
               {
                   MidiPortMapping mapping = (MidiPortMapping) MidiTools.this.mappingList.getModel().getElementAt(index);
                   if (e.getButton() == MouseEvent.BUTTON1)
                   {
                       long cms = System.currentTimeMillis();
                       if (cms - MidiTools.this.lastMappingClick < 500 && MidiTools.this.lastMappingClick > 0)
                       {
                          mapping.toggle();
                       }
                       MidiTools.this.lastMappingClick = cms;
                   } else if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                      int n = JOptionPane.showConfirmDialog(null, "Delete this port mapping?\n" + mapping.toString(),
                        "Port Mapping",
                        JOptionPane.YES_NO_OPTION);
                        if(n == JOptionPane.YES_OPTION)
                        {
                            MidiPortManager.removeMidiPortMapping(mapping);
                        }
                   }
                   MidiTools.repaintMappings();
               }
            }
        });
        JScrollPane mappingScrollPane = new JScrollPane(this.mappingList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mappingScrollPane.setBorder(new TitledBorder("MIDI Port Mappings (right-click to delete, double-click to toggle)"));
        this.bottomTabbedPane.addTab("Port Mappings", mappingScrollPane);
        
        this.bottomTabbedPane.addTab("Logger", this.midi_logger);
        
        this.add(this.bottomTabbedPane, BorderLayout.PAGE_END);

        //this.midi_logger.start();
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
        
        String openstaticUri = "wss://openstatic.org/channel/";
        System.err.println("OpenStatic URI: " + openstaticUri);
        this.routeputClient = new RoutePutClient(RoutePutChannel.getChannel(MidiTools.LOCAL_SERIAL), openstaticUri);
        this.routeputClient.setAutoReconnect(true);
        this.routeputClient.setCollector(true);
        this.routeputClient.setProperty("description", "MIDI Control Change Tool v" + MidiTools.VERSION);
        this.routeputClient.setProperty("host", RTPMidiPort.getLocalHost().getHostName());
        this.routeputSessionManager = new RoutePutSessionManager(this.routeputClient);
        MidiPortManager.addProvider(this.routeputSessionManager);

        this.rtpMidiPort = new RTPMidiPort("RTP Network", "RTP MidiTools" , 5004);
        MidiPortManager.addMidiPortListener(this);
        CollectionMidiPortProvider cmpp = new CollectionMidiPortProvider();
        MidiPortManager.addProvider(new DeviceMidiPortProvider());
        MidiPortManager.addProvider(new JoystickMidiPortProvider());
        MidiPortManager.addProvider(cmpp);
        cmpp.add(this.midi_logger);
        cmpp.add(this.randomizerPort);
        cmpp.add(this.rtpMidiPort);
        //MidiPortManager.registerVirtualPort("#lobby", new RouteputMidiPort("lobby", "openstatic.org"));
        MidiPortManager.init();
        System.err.println("Finished MidiTools Constructor");
    }
    
    public void start()
    {
        this.taskThread = new Thread(() -> {
            while(this.keep_running)
            {
                try
                {
                    Runnable r = this.taskQueue.poll(1, TimeUnit.SECONDS);
                    if (r != null)
                    {
                        r.run();
                        String taskName = r.toString();
                        if (!taskName.contains("Lambda"))
                            System.err.println("TaskComplete> " + taskName);
                    }
                } catch (Exception e) {
                    MidiTools.instance.midi_logger.printException(e);
                }
            }
        });
        this.taskThread.setDaemon(true);
        this.taskThread.start();
        // All this happens after swing initializes
        this.mainThread = new Thread(this);
        this.mainThread.setDaemon(true);
        this.mainThread.start();
        this.midi_logger.addReceiver(MidiTools.this);
        this.randomizerPort.addReceiver(MidiTools.this);
    }
    
    public void portAdded(int idx, MidiPort port)
    {
        this.midi_logger.println("MIDI Port Added " + port.toString());
        repaintDevices();
    }
    
    public void portRemoved(int idx, MidiPort port)
    {
        this.midi_logger.println("MIDI Port Removed " + port.toString());
        repaintDevices();
    }
    
    public void portOpened(MidiPort port)
    {
        this.midi_logger.println("MIDI Port Opened " + port.toString());
        port.addReceiver(MidiTools.this);
        repaintDevices();
    }
    
    public void portClosed(MidiPort port)
    {
        this.midi_logger.println("MIDI Port Closed " + port.toString());
        port.removeReceiver(MidiTools.this);
        repaintDevices();
    }
    
    public void mappingAdded(int idx, MidiPortMapping mapping) 
    {
        this.midi_logger.println("MIDI Port Mapping Added " + mapping.toString());
        MidiTools.repaintMappings(); 
    }
    
    public void mappingRemoved(int idx, MidiPortMapping mapping)
    {
        this.midi_logger.println("MIDI Port Mapping Removed " + mapping.toString());
        MidiTools.repaintMappings();
    }
    
    public void mappingOpened(MidiPortMapping mapping)
    {
        this.midi_logger.println("MIDI Port Mapping Opened " + mapping.toString());
        MidiTools.repaintMappings();
    }
    public void mappingClosed(MidiPortMapping mapping)
    {
        this.midi_logger.println("MIDI Port Mapping Closed " + mapping.toString());
        MidiTools.repaintMappings();
    }
    
    public void changeAPIState(boolean apiEnable)
    {
        this.apiServerEnable.setState(apiEnable);
        this.showQrItem.setEnabled(apiEnable);
        if (!apiEnable)
        {
            this.showQrItem.setState(false);
            this.setShowQR(false);
        }
        this.openInBrowserItem.setEnabled(apiEnable);
        this.apiServer.setState(apiEnable);
        if (this.bootstrapSSLItem.getState() && apiEnable)
        {
            this.routeputClient.connect();
        }
    }
    
    public void resetConfiguration()
    {
        (new Thread (() -> {
            for (Enumeration<MidiControl> mce = this.controls.elements(); mce.hasMoreElements();)
            {
                MidiControl mc = mce.nextElement();
                mc.removeAllListeners();
            }
            try
            {
                SwingUtilities.invokeAndWait(() -> {
                    MidiTools.this.controls.clear();
                    MidiTools.this.rules.clear();
                });
            } catch (Exception e) {}
            this.setLastSavedFile(null);
        })).start();
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
    
    public static void repaintRules()
    {
        if (MidiTools.instance.rulesList != null)
        {
            MidiTools.instance.rulesList.repaint();
        }
    }
    
    public static void repaintControls()
    {
        if (MidiTools.instance.controlList != null)
        {
            MidiTools.instance.controlList.repaint();
        }
    }
    
    public static void repaintDevices()
    {
        if (MidiTools.instance.midiList != null)
        {
            MidiTools.instance.midiList.repaint();
        }
    }
    
    public static void repaintMappings()
    {
        if (MidiTools.instance.mappingList != null)
        {
            MidiTools.instance.mappingList.repaint();
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

    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.apiServerEnable)
        {
            boolean state = this.apiServerEnable.getState();
            this.options.put("apiServer", state);
            changeAPIState(state);
            return;
        } else if (e.getSource() == this.createControlOnInput) {
            boolean state = this.createControlOnInput.getState();
            this.options.put("createControlOnInput", state);
            return;
        } else if (e.getSource() == this.showQrItem) {
            boolean state = this.showQrItem.getState();
            this.setShowQR(state);
            return;
        } else if (e.getSource() == this.bootstrapSSLItem) {
            if (this.showQrItem.getState())
            {
                this.setShowQR(false);
                this.setShowQR(true);
            }
            if (this.bootstrapSSLItem.getState())
            {
                this.routeputClient.connect();
            } else {
                this.routeputClient.close();
            }
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
        } else if (cmd.equals("new_random_rule")) {
            JSONObject newRule = MidiRandomizerPort.defaultRuleJSONObject();
            JSONObjectDialog jod = new JSONObjectDialog("New Randomizer Rule", newRule);
            MidiTools.this.randomizerPort.addRandomRule(newRule);
        } else if (cmd.equals("about")) {
            browseTo("http://openstatic.org/miditools/");
        } else if (cmd.equals("open_api")) {
            browseTo(getWebInterfaceURL());
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
                final File fileToLoad = fileChooser.getSelectedFile();
                (new Thread(() -> {
                    loadConfigFrom(fileToLoad);
                })).start();
            }
        } else if (cmd.equals("create_rule")) {
            MidiControl t = (MidiControl) MidiTools.this.controlList.getSelectedValue();
            MidiControlRule newRule = new MidiControlRule(t, 1, 0, null);
            MidiControlRuleEditor editor = new MidiControlRuleEditor(newRule, true);
        } else if (cmd.equals("new_control")) {
            CreateControlDialog editr = new CreateControlDialog();
        } else if (cmd.equals("new_mapping")) {
            CreateMappingDialog editr = new CreateMappingDialog();
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
                (new Thread(() -> {
                    removeMidiControl(t);
                })).start();
            }
        }
    }

    public void setShowQR(boolean value)
    {
        if (value)
        {
            this.qrLabel = new JLabel(new ImageIcon(MidiTools.QRCode(getWebInterfaceURL())));
            this.qrLabel.setBackground(Color.WHITE);
            this.qrLabel.setOpaque(true);
            this.deviceQRPanel.add(this.qrLabel, BorderLayout.SOUTH);
            this.deviceQRPanel.revalidate();
        } else if (this.qrLabel != null) {
            this.deviceQRPanel.remove(this.qrLabel);
            this.qrLabel = null;
            this.deviceQRPanel.revalidate();
        }
    }
    
    public String getWebInterfaceURL()
    {
        String localIP = MidiTools.getLocalIP() ;
        if (this.bootstrapSSLItem.getState())
            return "https://openstatic.org/mcct/?s=" + MidiTools.LOCAL_SERIAL;
        else
            return "https://" + localIP + ":6124/";
            
    }

    public void run()
    {
        while(this.keep_running)
        {
            try
            {
                everySecond();
                Thread.sleep(1000);
            } catch (Exception e) {
                MidiTools.instance.midi_logger.printException(e);
            }
        }
    }

    public void everySecond() throws Exception
    {
        for (Enumeration<MidiControl> mce = this.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            if (!mc.isSettled())
                mc.settle();
        }
        if (this.isShowing())
            this.windowLocation = this.getLocationOnScreen();
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 800;
        int wHeight = 560;
        Dimension d = new Dimension(wWidth, wHeight);
        this.setSize(d);
        //this.setMaximumSize(d);
        this.setMinimumSize(d);
        //this.setResizable(false);
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.windowLocation = new Point(x,y);
        this.setLocation(x, y);
    }

    public static void main(String[] args)
    {
        MidiTools.LOCAL_SERIAL = MidiTools.getLocalMAC();
        System.err.println("main() midi-tools");
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            
        }
        MidiTools mlb = new MidiTools();
        mlb.start();
        System.err.println("loading config");
        mlb.loadConfig();
        mlb.setVisible(true);
        System.err.println("finished config load");
    }

    public static void setRuleGroupEnabled(String groupName, boolean v)
    {
        if (v)
        {
            MidiTools.instance.midi_logger.println("Rule Group Enabled " + groupName);
        } else {
            MidiTools.instance.midi_logger.println("Rule Group Disabled " + groupName);
        }
        for (Enumeration<MidiControlRule> mcre = MidiTools.instance.rules.elements(); mcre.hasMoreElements();)
        {
            MidiControlRule mcr = mcre.nextElement();
            if (mcr.getRuleGroup().equals(groupName))
            {
                mcr.setEnabled(v);
            }
        }
    }
    
    public static void toggleRuleGroupEnabled(String groupName)
    {
        for (Enumeration<MidiControlRule> mcre = MidiTools.instance.rules.elements(); mcre.hasMoreElements();)
        {
            MidiControlRule mcr = mcre.nextElement();
            if (mcr.getRuleGroup().equals(groupName))
            {
                mcr.toggleEnabled();
            }
        }
    }
    
    public static MidiControlRule getMidiControlRuleById(String ruleId)
    {
        for (Enumeration<MidiControlRule> mcre = MidiTools.instance.rules.elements(); mcre.hasMoreElements();)
        {
            MidiControlRule mcr = mcre.nextElement();
            if (mcr.getRuleId().equals(ruleId))
            {
                return mcr;
            }
        }
        return null;
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
        return createMidiControlFromJSON(jo, 0);
    }

    public static MidiControl createMidiControlFromJSON(JSONObject jo, int index)
    {
        int channel = jo.optInt("channel", 0);
        int cc = jo.optInt("cc", 0);
        MidiControl mc = getMidiControlByChannelCC(channel, cc);
        if (mc == null)
        {
            mc = new MidiControl(jo);
            handleNewMidiControl(mc, index);
        }
        return mc;
    }
    
    public static void removeMidiControl(MidiControl mc)
    {
        try
        {
            mc.removeAllListeners();
            SwingUtilities.invokeAndWait(() -> {
                MidiTools.instance.controls.removeElement(mc);
            });
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
            MidiTools.repaintControls();
            MidiTools.instance.midi_logger.println("MIDI Control Removed " + mc.toString());
        } catch (Exception e) {
            MidiTools.instance.midi_logger.printException(e);
        }
    }
    
    public static void handleNewMidiControl(final MidiControl mc)
    {
        handleNewMidiControl(mc, 0);
    }
    
    public static void handleNewMidiControl(final MidiControl mc, int index)
    {
        try
        {
            SwingUtilities.invokeAndWait(() -> {
                MidiTools.instance.controls.insertElementAt(mc, index);
            });
            mc.addMidiControlListener(MidiTools.instance.apiServer);
            mc.addMidiControlListener(MidiTools.instance.routeputSessionManager);
            JSONObject event = new JSONObject();
            event.put("event", "controlAdded");
            event.put("control", mc.toJSONObject());
            MidiTools.instance.apiServer.broadcastJSONObject(event);
            MidiTools.repaintControls();
            MidiTools.instance.midi_logger.println("MIDI Control Added " + mc.toString());
        } catch (Exception e) {
            MidiTools.instance.midi_logger.printException(e);
        }
    }

    // Receiver Method
    public void send(MidiMessage msg, long timeStamp)
    {
        if(msg instanceof ShortMessage)
        {
            final ShortMessage sm = (ShortMessage) msg;
            /*
            if (sm.getData1() > 0)
                System.err.println("Recieved Short Message " + MidiPortManager.shortMessageToString(sm));
                */
            if (sm.getCommand() == ShortMessage.CONTROL_CHANGE)
            {
                addTask(() -> {
                    boolean should_repaint = false;
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
                                MidiTools.instance.midi_logger.printException(e);
                            }
                        }
                    }
                    if (!found_control && this.createControlOnInput.getState())
                    {
                        MidiControl mc = new MidiControl(sm.getChannel()+1, sm.getData1());
                        handleNewMidiControl(mc);
                    }
                    if (should_repaint)
                    {
                        repaintControls();
                    }
                });
            }
        } else {
            System.err.println("Unknown non-short message " + msg.toString());
        }
    }

    public void close() {}
    
    
    public static File getConfigFile()
    {
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir, ".midi-tools.json");
    }
    
    public void loadConfig()
    {
        loadConfigFrom(getConfigFile(), false);
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

            int windowWidth = this.getWidth();
            int windowHeight = this.getHeight();
            
            windowWidth = configJson.optInt("windowWidth", 800);
            windowHeight = configJson.optInt("windowHeight", 560);
            
            Point newWindowLocation = this.windowLocation;
            if (newWindowLocation == null)
            {
                Toolkit tk = Toolkit.getDefaultToolkit();
                Dimension screenSize = tk.getScreenSize();
                final float WIDTH = screenSize.width;
                final float HEIGHT = screenSize.height;
                int x = (int) ((WIDTH/2f) - ( ((float)windowWidth) /2f ));
                int y = (int) ((HEIGHT/2f) - ( ((float)windowHeight) /2f ));
                newWindowLocation = new Point(x,y);
            }
            if (configJson.has("controls"))
            {
                JSONArray controlsArray = configJson.getJSONArray("controls");
                for (int m = 0; m < controlsArray.length(); m++)
                {
                    createMidiControlFromJSON(controlsArray.getJSONObject(m),m);
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
            if (configJson.has("bootstrapSSL"))
            {
                this.bootstrapSSLItem.setState(configJson.optBoolean("bootstrapSSL", false));
            }
            if (configJson.has("apiServer"))
            {
                boolean apiEnable = configJson.optBoolean("apiServer", false);
                changeAPIState(apiEnable);
            }
            if (configJson.has("options"))
            {
                this.options = configJson.getJSONObject("options");
                this.createControlOnInput.setState(this.options.optBoolean("createControlOnInput", true));
            }
            if (configJson.has("windowX"))
            {
                newWindowLocation.x = configJson.optInt("windowX", 0);
            }
            if (configJson.has("windowY"))
            {
                newWindowLocation.y = configJson.optInt("windowY", 0);
            }

            if (configJson.has("showQr"))
            {
                boolean state = configJson.optBoolean("showQr", false);
                this.showQrItem.setState(state);
                this.setShowQR(state);
            }
            if (remember)
                this.setLastSavedFile(file);
            if (configJson.has("openReceivingPorts"))
            {
                JSONArray portsArray = configJson.getJSONArray("openReceivingPorts");
                for (int m = 0; m < portsArray.length(); m++)
                {
                    String portName = portsArray.getString(m);
                    MidiPort p = MidiPortManager.findReceivingPortByName(portName);
                    if (p != null)
                    {
                        System.err.println("Found Receiving Port " + p.getName());
                        p.open();
                        p.addReceiver(MidiTools.this);
                    }
                }
            }
            if (configJson.has("openTransmittingPorts"))
            {
                JSONArray portsArray = configJson.getJSONArray("openTransmittingPorts");
                for (int m = 0; m < portsArray.length(); m++)
                {
                    String portName = portsArray.getString(m);
                    MidiPort p = MidiPortManager.findTransmittingPortByName(portName);
                    if (p != null)
                    {
                        System.err.println("Found Transmitting Port " + p.getName());
                        p.open();
                    }
                }
            }
            if (configJson.has("mappings"))
            {
                JSONArray mappingsArray = configJson.getJSONArray("mappings");
                for (int m = 0; m < mappingsArray.length(); m++)
                {
                    JSONObject mappingObj = mappingsArray.getJSONObject(m);
                    MidiPortMapping mpm = new MidiPortMapping(mappingObj);
                    MidiPortManager.addMidiPortMapping(mpm);
                }
            }
            if (configJson.has("randomizerRules"))
            {
                JSONArray rulesArray = configJson.getJSONArray("randomizerRules");
                this.randomizerPort.setAllRules(rulesArray);
            }
            this.setSize(windowWidth, windowHeight);
            if (newWindowLocation != null)
            {
                this.setLocation(newWindowLocation);
            }
        } catch (Exception e) {
            MidiTools.instance.midi_logger.printException(e);
        }
    }
    
    public void saveConfig()
    {
        saveConfigAs(getConfigFile(), false);
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
    
    public JSONArray mappingsAsJSONArray()
    {
       JSONArray mappingArray = new JSONArray();
        for (Iterator<MidiPortMapping> mpme = MidiPortManager.getMidiPortMappings().iterator(); mpme.hasNext();)
        {
            MidiPortMapping mpm = mpme.next();
            mappingArray.put(mpm.toJSONObject());
        }
        return mappingArray;
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
    
    public JSONArray openReceivingPortsAsJSONArray()
    {
        JSONArray portsArray = new JSONArray();
        for(Iterator<MidiPort> portsIterator = MidiPortManager.getReceivingPorts().iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if (t.isOpened())
                portsArray.put(t.getName());
        }
        return portsArray;
    }
    
    public JSONArray openTransmittingPortsAsJSONArray()
    {
        JSONArray portsArray = new JSONArray();
        for(Iterator<MidiPort> portsIterator = MidiPortManager.getTransmittingPorts().iterator(); portsIterator.hasNext();)
        {
            MidiPort t = portsIterator.next();
            if (t.isOpened())
                portsArray.put(t.getName());
        }
        return portsArray;
    }

    public void saveConfigAs(File file, boolean remember)
    {
        try
        {
            JSONObject configJson = new JSONObject();
            configJson.put("controls", this.controlsAsJSONArray());
            configJson.put("rules", this.rulesAsJSONArray());
            configJson.put("options", this.options);
            configJson.put("apiServer", this.apiServerEnable.getState());
            configJson.put("bootstrapSSL", this.bootstrapSSLItem.getState());
            configJson.put("showQr", this.showQrItem.getState());
            configJson.put("openReceivingPorts", this.openReceivingPortsAsJSONArray());
            configJson.put("openTransmittingPorts", this.openTransmittingPortsAsJSONArray());
            configJson.put("mappings", this.mappingsAsJSONArray());
            configJson.put("windowX", this.windowLocation.x);
            configJson.put("windowY", this.windowLocation.y);
            configJson.put("windowWidth", this.getWidth());
            configJson.put("windowHeight", this.getHeight());
            configJson.put("randomizerRules", this.randomizerPort.getAllRules());
            saveJSONObject(file, configJson);
            if (remember)
                this.setLastSavedFile(file);
        } catch (Exception e) {
            MidiTools.instance.midi_logger.printException(e);
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

    public static JSONObject MidiPortToJSONObject(MidiPort port)
    {
        JSONObject dev = new JSONObject();
        dev.put("name", port.getName());
        if (port.canTransmitMessages() && port.canReceiveMessages())
        {
            dev.put("type", "both");
        } else if (port.canTransmitMessages()) {
            dev.put("type", "output");
        } else if (port.canReceiveMessages()) {
            dev.put("type", "input");
        }
        dev.put("opened", port.isOpened());
        return dev;
    }
    
    public static BufferedImage QRCode(String url)
    {
        try
        {
            ByteArrayOutputStream stream = QRCode.from(url).withSize(150, 150).to(ImageType.PNG).stream();
            return ImageIO.read(new ByteArrayInputStream(stream.toByteArray()));
        } catch (Exception e) {
            MidiTools.instance.midi_logger.printException(e);
            return null;
        }
    }
    
    public static String getLocalIP()
    {
        String return_ip = "";
        try
        {
            for(Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces(); n.hasMoreElements();)
            {
                NetworkInterface ni = n.nextElement();
                for(Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();)
                {
                    InetAddress ia = e.nextElement();
                    String this_ip = ia.getHostAddress();
                    if (!ia.isLoopbackAddress() && ia.isSiteLocalAddress())
                        return_ip = this_ip;
                }
            }

        } catch (Exception e) {}
        return return_ip;
    }
    
    public static String getLocalMAC()
    {
        String return_mac = "";
        try
        {
            for(Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces(); n.hasMoreElements();)
            {
                NetworkInterface ni = n.nextElement();
                for(Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements();)
                {
                    InetAddress ia = e.nextElement();
                    if (!ia.isLoopbackAddress() && ia.isSiteLocalAddress())
                    {
                        byte[] mac = ni.getHardwareAddress();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++)
                        {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                        }
                        return_mac = sb.toString();
                    }
                }
            }

        } catch (Exception e) {}
        return return_mac;
    }
}
