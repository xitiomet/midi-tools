package org.openstatic;

import org.openstatic.midi.*;
import org.openstatic.midi.ports.LoggerMidiPort;
import org.openstatic.midi.ports.MIDIChannelMidiPort;
import org.openstatic.midi.ports.MidiRandomizerPort;
import org.openstatic.midi.ports.RTPMidiPort;
import org.openstatic.midi.providers.CollectionMidiPortProvider;
import org.openstatic.midi.providers.DeviceMidiPortProvider;
import org.openstatic.midi.providers.JoystickMidiPortProvider;

import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Iterator;
import java.util.List;

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
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;

import org.openstatic.routeput.*;
import org.openstatic.routeput.client.*;

import javax.sound.midi.*;

import org.json.*;

public class MidiTools extends JFrame implements Runnable, Receiver, ActionListener, MidiPortListener
{
    public static final String VERSION = "1.3";
    public static String LOCAL_SERIAL;
    protected JList<MidiControl> controlList;
    private JList<MidiPort> midiList;
    private JList<MidiControlRule> rulesList;
    private JPopupMenu controlMenuPopup;
    private JPanel deviceQRPanel;
    private JLabel qrLabel;
    protected MidiControlCellRenderer midiControlCellRenderer;
    protected MidiControlRuleCellRenderer midiControlRuleCellRenderer;
    private MidiPortCellRenderer midiRenderer;
    private MidiPortListModel midiListModel;
    public LoggerMidiPort midi_logger_a;
    public LoggerMidiPort midi_logger_b;

    private Thread mainThread;
    protected DefaultListModel<MidiControl> controls;
    protected DefaultListModel<MidiControlRule> rules;
    protected ArrayBlockingQueue<Runnable> taskQueue;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu actionsMenu;
    private JMenu optionsMenu;
    private JTabbedPane bottomTabbedPane;
    private JTabbedPane mainTabbedPane;

    private JCheckBoxMenuItem apiServerEnable;
    private JCheckBoxMenuItem createControlOnInput;
    private JCheckBoxMenuItem showQrItem;
    private JCheckBoxMenuItem bootstrapSSLItem;

    private JMenuItem openInBrowserItem;
    private JMenuItem createNewControlItem;
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
    private long lastRuleClick;
    private long lastDeviceClick;
    private APIWebServer apiServer;
    private MidiRandomizerPort randomizerPort;
    private RTPMidiPort rtpMidiPort;
    private JSONObject options;
    private Point windowLocation;
    private RoutePutClient routeputClient;
    private RoutePutSessionManager routeputSessionManager;
    public CollectionMidiPortProvider cmpp;
    private JMenuItem routeputConnectMenuItem;
    private MappingControlBox mappingControlBox;
    private RandomizerControlBox randomizerControlBox;

    public MidiTools()
    {
        super("MIDI Control Change Tool v" + MidiTools.VERSION);
        MidiTools.instance = this;
        this.options = new JSONObject();
        this.taskQueue = new ArrayBlockingQueue<Runnable>(1000);
        this.keep_running = true;
        this.apiServer = new APIWebServer();
        MidiPortManager.addProvider(this.apiServer);
        this.midi_logger_a = new LoggerMidiPort("Logger A");
        this.midi_logger_b = new LoggerMidiPort("Logger B");
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
        
        this.bootstrapSSLItem = new JCheckBoxMenuItem("Enable Openstatic.org Interface");
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
        
        this.actionsMenu = new JMenu("Actions");
        this.actionsMenu.setMnemonic(KeyEvent.VK_A);

        this.createNewControlItem = new JMenuItem("Create Control");
        this.createNewControlItem.setActionCommand("new_control");
        this.createNewControlItem.addActionListener(this);
        this.createNewControlItem.setMnemonic(KeyEvent.VK_C);
        
        this.createControlOnInput = new JCheckBoxMenuItem("Create Control on MIDI Input");
        this.createControlOnInput.addActionListener(this);
        this.createControlOnInput.setState(true);
        this.options.put("createControlOnInput", true);

        this.routeputConnectMenuItem = new JMenuItem("Connect to MIDIChannel.net Room");
        this.routeputConnectMenuItem.setActionCommand("midichannel_net_connect");
        this.routeputConnectMenuItem.addActionListener(this);
        
        this.actionsMenu.add(this.createNewControlItem);
        this.actionsMenu.add(this.routeputConnectMenuItem);
        this.actionsMenu.add(this.openInBrowserItem);
        
        this.apiServerEnable = new JCheckBoxMenuItem("Enable API Server");
        this.apiServerEnable.addActionListener(this);
        this.apiServerEnable.setMnemonic(KeyEvent.VK_E);

        this.optionsMenu = new JMenu("Options");
        this.optionsMenu.setMnemonic(KeyEvent.VK_O);
        this.optionsMenu.add(this.apiServerEnable);
        this.optionsMenu.add(this.showQrItem);
        this.optionsMenu.add(this.bootstrapSSLItem);
        this.optionsMenu.add(this.createControlOnInput);

        this.menuBar.add(this.fileMenu);
        this.menuBar.add(this.actionsMenu);
        this.menuBar.add(this.optionsMenu);
        
        this.setJMenuBar(this.menuBar);
        
        this.controls = new DefaultListModel<MidiControl>();
        this.rules = new DefaultListModel<MidiControlRule>();

        this.midiControlCellRenderer = new MidiControlCellRenderer();
        this.midiControlRuleCellRenderer = new MidiControlRuleCellRenderer();
        
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

        this.mainTabbedPane = new JTabbedPane();
        
        JScrollPane controlsScrollPane = new JScrollPane(this.controlList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //controlsScrollPane.setBorder(new TitledBorder("Midi Controls"));
        BufferedImage dialIconImage = null;
        try
        {
            dialIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/dial32.png"));
        } catch (Exception e) {}
        ImageIcon dialIcon = new ImageIcon(dialIconImage);
        this.mainTabbedPane.addTab("Midi Controls", dialIcon, controlsScrollPane);

        JPanel toysAndPower = new JPanel(new BorderLayout());
        toysAndPower.add(this.mainTabbedPane, BorderLayout.CENTER);

        // Setup rule list
        this.rulesList = new JList(this.rules);
        this.rulesList.setDropTarget(drop_targ);
        this.rulesList.setCellRenderer(this.midiControlRuleCellRenderer);
        JScrollPane ruleScrollPane = new JScrollPane(this.rulesList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ruleScrollPane.setBorder(new TitledBorder("Rules for Incoming Control Change Messages (right-click to edit, double-click to toggle, drop wav files for sound triggers)"));
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
        
        this.midiList = new JList<MidiPort>(this.midiListModel);
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
        BufferedImage scriptIconImage = null;
        try
        {
            scriptIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/script32.png"));
        } catch (Exception e) {}
        ImageIcon scriptIcon = new ImageIcon(scriptIconImage);
        this.mainTabbedPane.addTab("Control Change Rules", scriptIcon, ruleScrollPane);
        
        // Setup rule list
        this.mappingControlBox = new MappingControlBox();
        
        BufferedImage cableIconImage = null;
        try
        {
            cableIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/cable32.png"));
        } catch (Exception e) {}
        ImageIcon cableIcon = new ImageIcon(cableIconImage);
        this.mainTabbedPane.addTab("Port Mappings", cableIcon, mappingControlBox);
        

        BufferedImage logIconImage = null;
        try
        {
            logIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/log32.png"));
        } catch (Exception e) {}
        ImageIcon logIcon = new ImageIcon(logIconImage);
        this.mainTabbedPane.addTab("Logger A", logIcon, this.midi_logger_a);


        BufferedImage diceIconImage = null;
        try
        {
            diceIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/dice32.png"));
        } catch (Exception e) {}
        ImageIcon diceIcon = new ImageIcon(diceIconImage);
        this.randomizerControlBox = new RandomizerControlBox(this.randomizerPort);
        this.mainTabbedPane.addTab("Randomizer", diceIcon, this.randomizerControlBox);


        this.bottomTabbedPane.addTab("Logger B", this.midi_logger_b);
        this.bottomTabbedPane.setSelectedIndex(0);

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
        this.cmpp = new CollectionMidiPortProvider();
        MidiPortManager.addProvider(cmpp);
        String openstaticUri = "wss://openstatic.org/channel/";
        System.err.println("OpenStatic URI: " + openstaticUri);
        RoutePutChannel myChannel = RoutePutChannel.getChannel("midi-tools-" + MidiTools.LOCAL_SERIAL);
        MidiTools.this.routeputClient = new RoutePutClient(myChannel, openstaticUri);
        MidiTools.this.routeputClient.setAutoReconnect(true);
        MidiTools.this.routeputClient.setCollector(true);
        MidiTools.this.routeputClient.setProperty("description", "MIDI Control Change Tool v" + MidiTools.VERSION);
        MidiTools.this.routeputClient.setProperty("host", RTPMidiPort.getLocalHost().getHostName());
        MidiTools.this.routeputSessionManager = new RoutePutSessionManager(myChannel, MidiTools.this.routeputClient);
        Thread svcs = new Thread(() -> {
            this.rtpMidiPort = new RTPMidiPort("RTP Network", "RTP MidiTools" , 5004);
            MidiPortManager.addMidiPortListener(this);
            MidiPortManager.addProvider(new DeviceMidiPortProvider());
            MidiPortManager.addProvider(new JoystickMidiPortProvider());
            cmpp.add(this.midi_logger_a);
            cmpp.add(this.midi_logger_b);
            cmpp.add(this.randomizerPort);
            cmpp.add(this.rtpMidiPort);
            MidiPortManager.addProvider(this.routeputSessionManager);
            //MidiPortManager.registerVirtualPort("#lobby", new RouteputMidiPort("lobby", "openstatic.org"));
        });
        svcs.start();
        logIt("Finished MidiTools Constructor");
        centerWindow();
    }

    DropTarget drop_targ = new DropTarget()
    {
        public synchronized void drop(DropTargetDropEvent evt)
        {
            try
            {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                final List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (droppedFiles.size() == 1)
                {
                    try
                    {
                        final File droppedFile = droppedFiles.get(0);
                        Thread t = new Thread(() -> {
                            MidiTools.this.handleFileDrop(droppedFile);
                        });
                        t.start();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                } else {
                    for(int i = 0; i < droppedFiles.size(); i++)
                    {
                        final int fi = i;
                        Thread t = new Thread(() -> {
                            MidiTools.this.handleFileDrop(droppedFiles.get(fi));
                        });
                        t.start();
                    }
                }
                System.err.println("Cleanly LEFT DROP Routine");
                evt.dropComplete(true);
            } catch (Exception ex) {
                evt.dropComplete(true);
                System.err.println("Exception During DROP Routine");
                ex.printStackTrace();
            }
        }
    };

    public void handleFileDrop(File file)
    {
        System.err.println("File dropped: " + file.toString());
        String filename = file.getName();
        String filenameLower = filename.toLowerCase();
        if (filenameLower.endsWith(".wav"))
        {
            MidiControlRule newRule = new MidiControlRule(null, MidiControlRule.EVENT_INCREASE, MidiControlRule.ACTION_SOUND, file.getAbsolutePath());
            newRule.setNickname(filename.substring(0, filename.length()-4));
            if (!MidiTools.instance.rules.contains(newRule))
                MidiTools.instance.rules.addElement(newRule);
        }
        if (filenameLower.endsWith(".exe") || filenameLower.endsWith(".bat") || filenameLower.endsWith(".cmd") || filenameLower.endsWith(".php"))
        {
            MidiControlRule newRule = new MidiControlRule(null, MidiControlRule.EVENT_SETTLE, MidiControlRule.ACTION_PROC, file.getAbsolutePath() + ",{{value}}");
            newRule.setNickname(filename.substring(0, filename.length()-4));
            if (!MidiTools.instance.rules.contains(newRule))
                MidiTools.instance.rules.addElement(newRule);
        }
    }
    
    public void start()
    {
        // All this happens after swing initializes
        this.mainThread = new Thread(this);
        this.mainThread.setDaemon(true);
        this.mainThread.start();
        this.randomizerPort.addReceiver(MidiTools.this);
    }
    
    public void portAdded(int idx, MidiPort port)
    {
        this.midi_logger_b.println("MIDI Port Added " + port.toString());
        repaintDevices();
    }
    
    public void portRemoved(int idx, MidiPort port)
    {
        this.midi_logger_b.println("MIDI Port Removed " + port.toString());
        repaintDevices();
    }
    
    public void portOpened(MidiPort port)
    {
        this.midi_logger_b.println("MIDI Port Opened " + port.toString());
        port.addReceiver(MidiTools.this);
        repaintDevices();
    }
    
    public void portClosed(MidiPort port)
    {
        this.midi_logger_b.println("MIDI Port Closed " + port.toString());
        port.removeReceiver(MidiTools.this);
        repaintDevices();
    }
    
    public void mappingAdded(int idx, MidiPortMapping mapping) 
    {
        this.midi_logger_b.println("MIDI Port Mapping Added " + mapping.toString());
        MidiTools.repaintMappings(); 
    }
    
    public void mappingRemoved(int idx, MidiPortMapping mapping)
    {
        this.midi_logger_b.println("MIDI Port Mapping Removed " + mapping.toString());
        MidiTools.repaintMappings();
    }
    
    public void mappingOpened(MidiPortMapping mapping)
    {
        this.midi_logger_b.println("MIDI Port Mapping Opened " + mapping.toString());
        MidiTools.repaintMappings();
    }
    public void mappingClosed(MidiPortMapping mapping)
    {
        this.midi_logger_b.println("MIDI Port Mapping Closed " + mapping.toString());
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
        logIt("Reset Configuration");
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
                    MidiTools.this.mappingControlBox.clearMidiPortMappings();
                });
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
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
        if (MidiTools.instance != null)
        {
            if (MidiTools.instance.mainTabbedPane.getSelectedIndex() == 1)
            {
                if (MidiTools.instance.rulesList != null)
                {
                    MidiTools.instance.rulesList.repaint();
                }
            }
        }
    }
    
    public static void repaintControls()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.instance.mainTabbedPane.getSelectedIndex() == 0)
            {
                if (MidiTools.instance.controlList != null)
                {
                    MidiTools.instance.controlList.repaint();
                }
            }
        }
    }
    
    public static void repaintDevices()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.instance.midiList != null)
            {
                MidiTools.instance.midiList.repaint();
            }
        }
    }
    
    public static void repaintMappings()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.instance.mainTabbedPane.getSelectedIndex() == 2)
            {
                if (MidiTools.instance.mappingControlBox != null)
                {
                    MidiTools.instance.mappingControlBox.repaint();
                }
            }
        }
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
                this.routeputClient.setAutoReconnect(true);
            } else {
                this.routeputClient.setAutoReconnect(false);
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
        } else if (cmd.equals("midichannel_net_connect")) {
            JSONObject newRule = new JSONObject();
            newRule.put("channel", "lobby");
            JSONObjectDialog jod = new JSONObjectDialog("New MIDIChannel.net Connection", newRule);
            MIDIChannelMidiPort rpcmp = new MIDIChannelMidiPort(newRule.optString("channel","lobby"));
            this.cmpp.add(rpcmp);
        } else if (cmd.equals("about")) {
            browseTo("http://openstatic.org/projects/miditools/");
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
            return "https://openstatic.org/mcct/?s=midi-tools-" + MidiTools.LOCAL_SERIAL;
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
                MidiTools.instance.midi_logger_b.printException(e);
            }
        }
    }

    public void everySecond() throws Exception
    {
        repaintRules();
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
        int wWidth = 1024;
        int wHeight = 600;
        Dimension d = new Dimension(wWidth, wHeight);
        this.setSize(d);
        //this.setMaximumSize(d);
        this.setMinimumSize(d);
        //this.setResizable(false);
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.windowLocation = new Point(x,y);
        this.setLocation(x, y);
        logIt("Centered Window");
    }

    public static void main(String[] args)
    {
        MidiPortManager.init();
        MidiTools.LOCAL_SERIAL = MidiTools.getLocalMAC();
        System.err.println("main() midi-tools");
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            
        }
        MidiTools mlb = new MidiTools();
        mlb.start();
        logIt("loading config");
        mlb.setVisible(true);
        mlb.loadConfig();
        logIt("finished config load");
    }

    public static void setRuleGroupEnabled(String groupName, boolean v)
    {
        if (v)
        {
            MidiTools.instance.midi_logger_b.println("Rule Group Enabled " + groupName);
        } else {
            MidiTools.instance.midi_logger_b.println("Rule Group Disabled " + groupName);
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
    
    public static MidiControl getMidiControlByChannelNote(int channel, int note)
    {
        for (Enumeration<MidiControl> mce = MidiTools.instance.controls.elements(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            if (mc.getChannel() == channel && mc.getNoteNumber() == note)
            {
                return mc;
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
        logIt("Removed Midi Control: " + mc.getNickname());
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
            MidiTools.instance.midi_logger_b.println("MIDI Control Removed " + mc.toString());
        } catch (Exception e) {
            MidiTools.instance.midi_logger_b.printException(e);
        }
    }
    
    public static void handleNewMidiControl(final MidiControl mc)
    {
        handleNewMidiControl(mc, 0);
    }
    
    public static void handleNewMidiControl(final MidiControl mc, int index)
    {
        logIt("Added Midi Control: " + mc.getNickname());
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
            MidiTools.instance.midi_logger_b.println("MIDI Control Added " + mc.toString());
        } catch (Exception e) {
            MidiTools.instance.midi_logger_b.printException(e);
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
            if (sm.getCommand() == ShortMessage.CONTROL_CHANGE || sm.getCommand() == ShortMessage.NOTE_ON || sm.getCommand() == ShortMessage.NOTE_OFF)
            {
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
                            MidiTools.instance.midi_logger_b.printException(e);
                        }
                    }
                }
                if (!found_control && this.createControlOnInput.getState() && sm.getCommand() == ShortMessage.CONTROL_CHANGE)
                {
                    int channel = sm.getChannel()+1;
                    int cc = sm.getData1();
                    if (cc != 121 && cc != 123)
                    {
                        MidiControl mc = new MidiControl(channel,cc);
                        handleNewMidiControl(mc);
                    }
                }
                if (!found_control && this.createControlOnInput.getState() && sm.getCommand() == ShortMessage.NOTE_ON)
                {
                    int channel = sm.getChannel()+1;
                    int note = sm.getData1() % 12;
                    MidiControl mc = new MidiControl(channel, note, true);
                    handleNewMidiControl(mc);
                }
                if (should_repaint)
                {
                    repaintControls();
                }
            }
        } else {
            logIt("Unknown non-short message " + msg.toString());
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
        logIt("Loading Configuration: " + file.getName());
        try
        {
            JSONObject configJson = loadJSONObject(file);

            int windowWidth = this.getWidth();
            int windowHeight = this.getHeight();
            
            windowWidth = configJson.optInt("windowWidth", 1200);
            windowHeight = configJson.optInt("windowHeight", 860);
            
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
            MidiTools.instance.midi_logger_b.printException(e);
        }
    }

    public static void logIt(String text)
    {
        System.err.println(text);
        if (MidiTools.instance != null)
        {
            if (MidiTools.instance.midi_logger_b != null)
            {
                MidiTools.instance.midi_logger_b.println(text);
            }
            if (MidiTools.instance.routeputClient != null)
            {
                RoutePutMessage logMsg = new RoutePutMessage();
                logMsg.setType(RoutePutMessage.TYPE_LOG_INFO);
                logMsg.put("text", text);
                MidiTools.instance.routeputClient.send(logMsg);
            }
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
        logIt("Saving Configuration: " + file.getName());
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
            MidiTools.instance.midi_logger_b.printException(e);
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
            ps.print(obj.toString(2));
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
            MidiTools.instance.midi_logger_b.printException(e);
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
