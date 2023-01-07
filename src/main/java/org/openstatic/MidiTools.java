package org.openstatic;

import org.openstatic.midi.*;
import org.openstatic.midi.ports.LoggerMidiPort;
import org.openstatic.midi.ports.MIDIChannelMidiPort;
import org.openstatic.midi.ports.MidiRandomizerPort;
import org.openstatic.midi.ports.RTPMidiPort;
import org.openstatic.midi.providers.CollectionMidiPortProvider;
import org.openstatic.midi.providers.DeviceMidiPortProvider;
import org.openstatic.midi.providers.JackMidiPortProvider;
import org.openstatic.midi.providers.JoystickMidiPortProvider;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.InetAddress;
import java.net.NetworkInterface;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;

import javax.imageio.ImageIO;

import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Color;
import java.awt.Component;

import org.openstatic.routeput.*;
import org.openstatic.routeput.client.*;
import org.json.*;

public class MidiTools extends JFrame implements Runnable, ActionListener, MidiPortListener
{
    public static LinkedHashMap<String, BufferedImage> cachedImages = new LinkedHashMap<String, BufferedImage>();

    public static final String VERSION = "1.7";
    public static String LOCAL_SERIAL;
    public static long appLaunchTime;
    public static boolean windowWentVisible = false;
    private JList<MidiPort> midiList;
    private JPanel deviceQRPanel;
    private JLabel qrLabel;
    private MidiPortCellRenderer midiRenderer;
    private MidiPortListModel midiListModel;
    public LoggerMidiPort midi_logger_a;
    public LoggerMidiPort midi_logger_b;
    protected MidiControlsPanel midiControlsPanel;

    private Thread mainThread;
    protected ArrayBlockingQueue<Runnable> taskQueue;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu actionsMenu;
    private JMenu optionsMenu;
    private JMenu qrCodeMenu;
    //private JTabbedPane bottomTabbedPane;
    private JTabbedPane mainTabbedPane;

    private JCheckBoxMenuItem apiServerEnable;
    private JCheckBoxMenuItem bootstrapSSLItem;

    private JRadioButtonMenuItem localQrMenuItem;
    private JRadioButtonMenuItem remoteQrMenuItem;
    private JRadioButtonMenuItem canvasQrMenuItem;
    private JRadioButtonMenuItem noneQrMenuItem;
    
    private JMenuItem openInBrowserItem;
    private JMenuItem openCanvasBrowserItem;
    private JMenuItem aboutMenuItem;
    
    private JMenuItem exportConfigurationMenuItem;
    private JMenuItem importConfigurationMenuItem;
    private JMenuItem loadPluginMenuItem;
    private JMenuItem managePluginMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem resetConfigurationMenuItem;
    private File lastSavedFile;
    private JMenuItem exitMenuItem;
    public static MidiTools instance;
    private boolean keep_running;
    public APIWebServer apiServer;
    private MidiRandomizerPort randomizerPort;
    private ArrayList<RTPMidiPort> rtpMidiPorts;
    private Point windowLocation;
    private RoutePutClient routeputClient;
    private RoutePutSessionManager routeputSessionManager;
    public CollectionMidiPortProvider cmpp;
    public JackMidiPortProvider jmpp;
    private JMenuItem routeputConnectMenuItem;
    private MappingControlBox mappingControlBox;
    protected MidiControlRulePanel midiControlRulePanel;
    private RandomizerControlBox randomizerControlBox;
    private AssetManagerPanel assetManagerPanel;
    public JSONObject loadedProjectJSON;
    public MidiPlayerPanel midiPlayer;
    public HashMap<String, MidiToolsPlugin> plugins;
    public JSONObject pluginSettings;
    private ImageIcon diceIcon;
    public boolean isWindows;

    public MidiTools(String os_name)
    {
        super("MIDI Control Change Tool v" + MidiTools.VERSION);
        this.isWindows = os_name.contains("windows");
        if (this.isWindows)
            System.err.println("Windows Detected");
        MidiTools.appLaunchTime = System.currentTimeMillis();
        MidiTools.instance = this;
        this.plugins = new HashMap<String, MidiToolsPlugin>();
        this.taskQueue = new ArrayBlockingQueue<Runnable>(1000);
        this.keep_running = true;
        this.loadedProjectJSON = new JSONObject();
        this.apiServer = new APIWebServer();
        MidiPortManager.addProvider(this.apiServer);
        this.midi_logger_a = new LoggerMidiPort("Logger A");
        this.midiPlayer = new MidiPlayerPanel();
        this.midi_logger_b = this.midiPlayer.getLoggerMidiPort();
        this.randomizerPort = new MidiRandomizerPort("Randomizer");

        this.setLayout(new BorderLayout());
        try
        {
            BufferedImage windowIcon = ImageIO.read(getClass().getResource("/midi-tools-res/windows.png"));
            this.setIconImage(windowIcon);
        } catch (Exception iconException) {}
        
        // Enable CTRL+S
        this.getRootPane().registerKeyboardAction(new ActionListener() {//this, is a JFrame
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionEvent e2 = new ActionEvent(MidiTools.this.saveMenuItem, ActionEvent.ACTION_FIRST, "save");
                MidiTools.this.actionPerformed(e2);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_S,KeyEvent.CTRL_DOWN_MASK),JComponent.WHEN_IN_FOCUSED_WINDOW );
        this.qrCodeMenu = new JMenu("QR Code");
        this.qrCodeMenu.setMnemonic(KeyEvent.VK_Q);
        
        this.noneQrMenuItem = new JRadioButtonMenuItem("None");
        this.noneQrMenuItem.addActionListener(this);
        this.noneQrMenuItem.setSelected(true);

        this.localQrMenuItem = new JRadioButtonMenuItem("Local Web Interface");
        this.localQrMenuItem.addActionListener(this);
        this.localQrMenuItem.setEnabled(false);

        this.remoteQrMenuItem = new JRadioButtonMenuItem("Openstatic.org Web Interface");
        this.remoteQrMenuItem.addActionListener(this);
        this.remoteQrMenuItem.setEnabled(false);

        this.canvasQrMenuItem = new JRadioButtonMenuItem("Media Canvas for Images/Sound");
        this.canvasQrMenuItem.addActionListener(this);
        this.canvasQrMenuItem.setEnabled(false);

        this.qrCodeMenu.add(this.noneQrMenuItem);
        this.qrCodeMenu.add(this.localQrMenuItem);
        this.qrCodeMenu.add(this.remoteQrMenuItem);
        this.qrCodeMenu.add(this.canvasQrMenuItem);

        ButtonGroup qrButtonGroup = new ButtonGroup();
        qrButtonGroup.add(this.noneQrMenuItem);
        qrButtonGroup.add(this.localQrMenuItem);
        qrButtonGroup.add(this.remoteQrMenuItem);
        qrButtonGroup.add(this.canvasQrMenuItem);
        
        this.bootstrapSSLItem = new JCheckBoxMenuItem("Enable Openstatic.org Interface");
        this.bootstrapSSLItem.setEnabled(true);
        this.bootstrapSSLItem.setMnemonic(KeyEvent.VK_O);
        this.bootstrapSSLItem.addActionListener(this);
        this.bootstrapSSLItem.setActionCommand("bootstrap_ssl");
        
        this.openInBrowserItem = new JMenuItem("Open API in Default Browser");
        this.openInBrowserItem.setEnabled(false);
        this.openInBrowserItem.setMnemonic(KeyEvent.VK_B);
        this.openInBrowserItem.addActionListener(this);
        this.openInBrowserItem.setActionCommand("open_api");
    
        this.openCanvasBrowserItem = new JMenuItem("Open Media Canvas in Default Browser");
        this.openCanvasBrowserItem.setEnabled(false);
        this.openCanvasBrowserItem.setMnemonic(KeyEvent.VK_C);
        this.openCanvasBrowserItem.addActionListener(this);
        this.openCanvasBrowserItem.setActionCommand("open_canvas");
        
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

        this.loadPluginMenuItem = new JMenuItem("Install Plugin from File");
        this.loadPluginMenuItem.setMnemonic(KeyEvent.VK_P);
        this.loadPluginMenuItem.addActionListener(this);
        this.loadPluginMenuItem.setActionCommand("load_plugin");

        this.managePluginMenuItem = new JMenuItem("Manage Plugins");
        this.managePluginMenuItem.setMnemonic(KeyEvent.VK_M);
        this.managePluginMenuItem.addActionListener(this);
        this.managePluginMenuItem.setActionCommand("manage_plugins");
        
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
        this.fileMenu.add(this.managePluginMenuItem);
        this.fileMenu.add(this.loadPluginMenuItem);
        this.fileMenu.add(this.aboutMenuItem);
        this.fileMenu.add(new JSeparator());
        this.fileMenu.add(this.exitMenuItem);
        
        this.actionsMenu = new JMenu("Actions");
        this.actionsMenu.setMnemonic(KeyEvent.VK_A);

        this.routeputConnectMenuItem = new JMenuItem("Connect to MIDIChannel.net Room");
        this.routeputConnectMenuItem.setActionCommand("midichannel_net_connect");
        this.routeputConnectMenuItem.addActionListener(this);
        
        this.actionsMenu.add(this.routeputConnectMenuItem);
        this.actionsMenu.add(this.openInBrowserItem);
        this.actionsMenu.add(this.openCanvasBrowserItem);
        
        this.apiServerEnable = new JCheckBoxMenuItem("Enable API Server");
        this.apiServerEnable.addActionListener(this);
        this.apiServerEnable.setMnemonic(KeyEvent.VK_E);

        this.optionsMenu = new JMenu("Options");
        this.optionsMenu.setMnemonic(KeyEvent.VK_O);
        this.optionsMenu.add(this.apiServerEnable);
        this.optionsMenu.add(this.bootstrapSSLItem);
        this.optionsMenu.add(this.qrCodeMenu);

        this.menuBar.add(this.fileMenu);
        this.menuBar.add(this.actionsMenu);
        this.menuBar.add(this.optionsMenu);
        
        this.setJMenuBar(this.menuBar);
        
        this.mainTabbedPane = new JTabbedPane();

        
        this.midiControlsPanel = new MidiControlsPanel();
        //controlsScrollPane.setBorder(new TitledBorder("Midi Controls"));
        BufferedImage dialIconImage = null;
        try
        {
            dialIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/dial32.png"));
        } catch (Exception e) {}
        ImageIcon dialIcon = new ImageIcon(dialIconImage);
        this.mainTabbedPane.addTab("Midi Controls", dialIcon, midiControlsPanel);

        JPanel toysAndPower = new JPanel(new BorderLayout());
        toysAndPower.add(this.mainTabbedPane, BorderLayout.CENTER);
        this.add(toysAndPower, BorderLayout.CENTER);

        this.midiListModel = new MidiPortListModel();
        this.midiRenderer = new MidiPortCellRenderer();
        
        this.midiList = new JList<MidiPort>(this.midiListModel);
        this.midiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.midiList.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                Point point = e.getPoint();
                if (point.getX() < 34) // only over icon
                {
                    int index = MidiTools.this.midiList.locationToIndex(e.getPoint());

                    if (index != -1)
                    {
                        MidiPort source = (MidiPort) MidiTools.this.midiListModel.getElementAt(index);
                        if (source == MidiTools.this.midiList.getSelectedValue())
                        {
                            if (e.getButton() == MouseEvent.BUTTON1)
                            {
                                if (source.isOpened())
                                {
                                    source.close();
                                    source.removeReceiver(MidiTools.this.midiControlsPanel);
                                } else {
                                    source.open();
                                    source.addReceiver(MidiTools.this.midiControlsPanel);
                                }
                            }
                        }
                    }
                }
            }
        });
        this.midiList.setCellRenderer(this.midiRenderer);
        JScrollPane scrollPane2 = new JScrollPane(this.midiList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane2.setPreferredSize(new Dimension(255,0));
        scrollPane2.setBorder(new TitledBorder("MIDI Devices"));
        
        this.deviceQRPanel = new JPanel(new BorderLayout());
        this.deviceQRPanel.add(scrollPane2, BorderLayout.CENTER);
        
        this.add(this.deviceQRPanel, BorderLayout.WEST);
        
        //this.bottomTabbedPane = new JTabbedPane();
        //this.bottomTabbedPane.setPreferredSize(new Dimension(0, 232));
        BufferedImage scriptIconImage = null;
        try
        {
            scriptIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/script32.png"));
        } catch (Exception e) {}
        ImageIcon scriptIcon = new ImageIcon(scriptIconImage);
        this.midiControlRulePanel = new MidiControlRulePanel();
        this.mainTabbedPane.addTab("Control Change Rules", scriptIcon, this.midiControlRulePanel);
        
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
        this.diceIcon = new ImageIcon(diceIconImage);
        this.randomizerControlBox = new RandomizerControlBox(this.randomizerPort);

        BufferedImage folderIconImage = null;
        try
        {
            folderIconImage = ImageIO.read(getClass().getResource("/midi-tools-res/folder32.png"));
        } catch (Exception e) {}

        ImageIcon folderIcon = new ImageIcon(folderIconImage);
        this.assetManagerPanel = new AssetManagerPanel(getAssetFolder());
        this.mainTabbedPane.addTab("Project Assets", folderIcon, this.assetManagerPanel);

        //this.bottomTabbedPane.addTab("Logger B", logIcon, this.midi_logger_b);


        this.add(this.midiPlayer, BorderLayout.PAGE_END);

        
        //this.midi_logger.start();
        Runtime.getRuntime().addShutdownHook(new Thread() 
        { 
          public void run()
          { 
            MidiTools.this.keep_running = false;
            //System.out.println("Shutdown Hook is running!"); 
            saveConfig();
            eraseAssets();
          } 
        });
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
                System.err.println("Window Opened!");
                MidiTools.windowWentVisible = true;
            }

            @Override
            public void windowClosing(WindowEvent e) {
                MidiTools.this.tryToExit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void windowIconified(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
        this.cmpp = new CollectionMidiPortProvider();
        //this.bottomTabbedPane.addTab("MIDI Player", this.midiPlayer.getIcon(), this.midiPlayer);
        //this.bottomTabbedPane.setSelectedIndex(0);
        MidiPortManager.addProvider(cmpp);
        String openstaticUri = "wss://openstatic.org/channel/";
        System.err.println("OpenStatic URI: " + openstaticUri);        
        RoutePutChannel myChannel = RoutePutChannel.getChannel("midi-tools-" + MidiTools.LOCAL_SERIAL);
        MidiTools.this.routeputClient = new RoutePutClient(myChannel, openstaticUri);
        MidiTools.this.routeputClient.setAutoReconnect(true);
        MidiTools.this.routeputClient.setCollector(true);
        MidiTools.this.routeputClient.setProperty("description", "MIDI Control Change Tool v" + MidiTools.VERSION);
        MidiTools.this.routeputClient.setProperty("host", getLocalHostname());
        MidiTools.this.routeputSessionManager = new RoutePutSessionManager(myChannel, MidiTools.this.routeputClient);
        this.rtpMidiPorts = new ArrayList<RTPMidiPort>();
        MidiPortManager.addMidiPortListener(this);
        MidiPortManager.addProvider(new DeviceMidiPortProvider());
        MidiPortManager.addProvider(new JoystickMidiPortProvider());
        cmpp.add(this.midi_logger_a);
        cmpp.add(this.midi_logger_b);
        cmpp.add(this.midiPlayer);
        cmpp.add(this.randomizerPort);
        String localHostname = getLocalHostname();
        try
        {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
            {
                //System.err.println("Interface: " + netint.getDisplayName());
                // Create a JmDNS instance
                Enumeration<InetAddress> addresses = netint.getInetAddresses();
                Collections.list(addresses).forEach((address) -> {
                    if (address.isSiteLocalAddress())
                    {
                        try
                        {
                            RTPMidiPort rtpPort = new RTPMidiPort("RTP " + address.getHostAddress(), localHostname + " MidiTools", address, 5004);
                            MidiTools.this.rtpMidiPorts.add(rtpPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            cmpp.addAll(this.rtpMidiPorts);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        MidiPortManager.addProvider(this.routeputSessionManager);
        try
        {
            this.jmpp = new JackMidiPortProvider();
            MidiPortManager.addProvider(this.jmpp);
            System.err.println("Initialized Jack Suppport");
        } catch (Exception jmppex) {
            jmppex.printStackTrace(System.err);
        }
        MidiPortManager.refresh();
        logIt("Finished MidiTools Constructor");
        File plugin_root = getPluginFolder();
        if (plugin_root.exists())
        {
            try
            {
                File[] plug_files = plugin_root.listFiles();
                for (int i = 0; i < plug_files.length; i++)
                {
                    if (plug_files[i].getName().endsWith(".jar"))
                    {
                        try
                        {
                            this.loadPlugin(plug_files[i]);
                        } catch (Exception pl_ex) {
                            pl_ex.printStackTrace(System.err);
                        }
                    }
                }
            } catch (Exception ex2) {
                ex2.printStackTrace(System.err);
            }
        } else {
            plugin_root.mkdirs();
        }
        centerWindow();
    }


    public static String shellExec(String cmd[])
    {
        try
        {
            Process cmdProc = Runtime.getRuntime().exec(cmd);
            cmdProc.waitFor();
            return readStreamToString(cmdProc.getInputStream());
        } catch (Exception e) {
           
        }
        return null;
    }

    public static String readStreamToString(InputStream is)
    {
        String result = "";
        try
        {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            s.close();
        } catch (Exception e) {}
        return result;
    }

    public static String getLocalHostname()
    {
        String returnValue = "";
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME"))
        {
            returnValue = env.get("COMPUTERNAME");
        } else if (env.containsKey("HOSTNAME")) {
            returnValue = env.get("HOSTNAME");
        } else {
            String hostnameCommand = shellExec(new String[] {"hostname"});
            if (hostnameCommand != null)
            {
                String hostname = hostnameCommand.trim();
                if (!"".equals(hostname))
                    returnValue = hostname;
            }
        }
        if ("".equals(returnValue))
        {
            try
            {
                for(Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces(); n.hasMoreElements() && "".equals(returnValue);)
                {
                    NetworkInterface ni = n.nextElement();
                    for(Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements() && "".equals(returnValue);)
                    {
                        InetAddress ia = e.nextElement();
                        if (!ia.isLoopbackAddress() && ia.isSiteLocalAddress())
                        {
                            String hostname = ia.getHostName();
                            returnValue = hostname;
                        }
                    }
                }

            } catch (Exception e) {}
        }
        if ("".equals(returnValue))
        {
            returnValue = MidiPortManager.generateBigAlphaKey(5);
        }
        if (returnValue.contains(".local"))
        {
            returnValue = returnValue.replace(".local", "");
        }
        if (returnValue.contains(".lan"))
        {
            returnValue = returnValue.replace(".lan", "");
        }
        return returnValue;
    }

    public void tryToExit()
    {
        if (MidiTools.this.hasUnsavedProjectChanges() && MidiTools.this.lastSavedFile != null)
        {
            if (!this.lastSavedFile.getName().contains("Untitled.mtz"))
            {
                int answer = showUnsavedMessage();
            
                switch (answer) {
                    case JOptionPane.YES_OPTION:
                        saveProjectAs(MidiTools.this.lastSavedFile);
                        System.exit(0);
                        break;
                        
                    case JOptionPane.NO_OPTION:
                        System.exit(0);
                        break;
                        
                    case JOptionPane.CANCEL_OPTION:
                        break;
                }
            } else {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    private int showUnsavedMessage() {
        String[] buttonLabels = new String[] {"Yes", "No", "Cancel"};
        String defaultOption = buttonLabels[0];
         
        return JOptionPane.showOptionDialog(this,
                "You have made changes to this project, do you want to save them before closing?",
                "Warning",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                buttonLabels,
                defaultOption);    
    }

    public boolean isAlive()
    {
        return this.keep_running && this.mainThread.isAlive();
    }
    
    protected void start()
    {
        // All this happens after swing initializes
        this.mainThread = new Thread(this);
        this.mainThread.setDaemon(true);
        this.mainThread.start();
        this.randomizerPort.addReceiver(MidiTools.this.midiControlsPanel);
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
        port.addReceiver(MidiTools.this.midiControlsPanel);
        if (port instanceof RTPMidiPort)
        {
            RTPMidiPort rtpPort = (RTPMidiPort) port;
            RTPControlBox controlBox = new RTPControlBox(rtpPort);
            MidiTools.this.mainTabbedPane.addTab(rtpPort.getName(), controlBox.getIcon(), controlBox);
        }
        if (port instanceof MidiRandomizerPort)
        {
            this.mainTabbedPane.addTab("Randomizer", this.diceIcon, this.randomizerControlBox);
        }
        repaintDevices();
    }
    
    public void portClosed(MidiPort port)
    {
        this.midi_logger_b.println("MIDI Port Closed " + port.toString());
        port.removeReceiver(MidiTools.this.midiControlsPanel);
        if (port instanceof RTPMidiPort)
        {
            RTPMidiPort rtpPort = (RTPMidiPort) port;
            String portName = rtpPort.getName();
            int tabIndex = MidiTools.this.mainTabbedPane.indexOfTab(portName);
            MidiTools.this.mainTabbedPane.remove(tabIndex);
        }
        if (port instanceof MidiRandomizerPort)
        {
            int tabIndex = MidiTools.this.mainTabbedPane.indexOfTab("Randomizer");
            MidiTools.this.mainTabbedPane.remove(tabIndex);
        }
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
        this.localQrMenuItem.setEnabled(apiEnable);
        this.canvasQrMenuItem.setEnabled(apiEnable);
        if (!apiEnable)
        {
            if (this.localQrMenuItem.isSelected() || this.canvasQrMenuItem.isSelected())
            {
                setShowQR("none");
            }
        }
        this.openInBrowserItem.setEnabled(apiEnable);
        this.openCanvasBrowserItem.setEnabled(apiEnable);
        this.apiServer.setState(apiEnable);
        if (this.bootstrapSSLItem.getState() && apiEnable)
        {
            this.routeputClient.connect();
        }
    }
    
    protected void resetConfiguration()
    {
        logIt("Reset Configuration");
        for (Enumeration<MidiControl> mce = this.midiControlsPanel.getControlsEnumeration(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            mc.removeAllListeners();
        }
        try
        {
            // Clear rules first to prevent cascading triggers
            MidiTools.this.midiControlRulePanel.clear();
            // Now that there are no rules its safe to remove controls
            MidiTools.this.midiControlsPanel.clear();
            // finally lets clear mappings
            MidiPortManager.deleteAllMidiPortMappings();

            MidiTools.eraseAssets();

            this.randomizerPort.clearAllRules();

            Iterator<MidiToolsPlugin> pIterator = this.plugins.values().iterator();
            while(pIterator.hasNext())
            {
                MidiToolsPlugin plugin = pIterator.next();
                try
                {
                    plugin.loadProject(new JSONObject());
                } catch (Throwable pluginEx) {
                    System.err.println("TRAPPED PLUGIN THROWABLE....");
                    pluginEx.printStackTrace(System.err);
                }
            }
            this.loadedProjectJSON = null;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.setLastSavedFile(null);
    }

    public static void eraseAssets()
    {
        File[] assets = getAssetFolder().listFiles();
        for(int i = 0; i < assets.length; i++)
        {
            File asset = assets[i];
            asset.delete();
        }
    }
    
    public void setLastSavedFile(File f)
    {
        this.lastSavedFile = f;
    }

    public static String getProjectName()
    {
        String rv = null;
        if (MidiTools.instance.lastSavedFile != null)
        {
            rv = MidiTools.instance.lastSavedFile.getName().replace(".mtz", "");
        }
        return rv;
    }
    
    public static void repaintRules()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.windowWentVisible)
            {
                if (MidiTools.instance.mainTabbedPane.getSelectedIndex() == 1)
                {
                    if (MidiTools.instance.midiControlRulePanel != null)
                    {
                        MidiTools.instance.midiControlRulePanel.repaint();
                    }
                }
            }
        }
    }
    
    public static void repaintControls()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.windowWentVisible)
            {
                if (MidiTools.instance.mainTabbedPane.getSelectedIndex() == 0)
                {
                    if (MidiTools.instance.midiControlsPanel != null)
                    {
                        MidiTools.instance.midiControlsPanel.repaint();
                    }
                }
            }
        }
    }
    
    public static void repaintDevices()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.windowWentVisible)
            {
                if (MidiTools.instance.midiList != null)
                {
                    MidiTools.instance.midiList.repaint();
                }
            }
        }
    }
    
    public static void repaintMappings()
    {
        if (MidiTools.instance != null)
        {
            if (MidiTools.windowWentVisible)
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
    }
    
    public static MidiControl getMidiControlByIndex(int i)
    {
        return MidiTools.instance.midiControlsPanel.elementAt(i);
    }
    
    protected static int getIndexForMidiControl(MidiControl m)
    {
        return MidiTools.instance.midiControlsPanel.indexOf(m);
    }
    
    protected static void removeListenerFromControls(MidiControlListener mcl)
    {
        MidiTools.instance.midiControlsPanel.removeListenerFromControls(mcl);
    }

    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this.apiServerEnable)
        {
            boolean state = this.apiServerEnable.getState();
            changeAPIState(state);
            return;
        } else if (e.getSource() == this.noneQrMenuItem) {
            if (this.noneQrMenuItem.isSelected())
                this.setShowQR("none");
            return;
        } else if (e.getSource() == this.localQrMenuItem) {
            if (this.localQrMenuItem.isSelected())
                this.setShowQR("local");
            return;
        } else if (e.getSource() == this.remoteQrMenuItem) {
            if (this.remoteQrMenuItem.isSelected())
                this.setShowQR("remote");
            return;
        } else if (e.getSource() == this.canvasQrMenuItem) {
            if (this.canvasQrMenuItem.isSelected())
                this.setShowQR("canvas");
            return;
        } else if (e.getSource() == this.bootstrapSSLItem) {
            this.remoteQrMenuItem.setEnabled(this.bootstrapSSLItem.getState());
            if (this.bootstrapSSLItem.getState())
            {
                this.routeputClient.connect();
                this.routeputClient.setAutoReconnect(true);
            } else {
                if (this.remoteQrMenuItem.isSelected())
                {
                    this.setShowQR("none");
                }
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
            "Are you sure? This will clear all rules, controls, assets, and mappings",
            "Reset Confirmation",
            JOptionPane.YES_NO_OPTION);
            if(n == JOptionPane.YES_OPTION)
            {
                Thread x = new Thread()
                {
                    public void run()
                    {
                        resetConfiguration();
                    }
                };
                x.start();
            }
        } else if (cmd.equals("exit")) {
            tryToExit();
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
            browseTo("https://openstatic.org/projects/miditools/");
        } else if (cmd.equals("open_api")) {
            browseTo(getWebInterfaceURL());
        } else if (cmd.equals("open_canvas")) {
            browseTo(getCanvasURL());
        } else if (cmd.equals("save")) {
            saveProjectAs(this.lastSavedFile);
        } else if (cmd.equals("export")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to save");   
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Midi Tools Project", "mtz");
            fileChooser.setFileFilter(filter);
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                File fileToSave = fileChooser.getSelectedFile();
                if (!fileToSave.getName().endsWith(".mtz"))
                    fileToSave = new File(fileToSave.toString() + ".mtz");
                saveProjectAs(fileToSave);
            }
        } else if (cmd.equals("import")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to open");   
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Midi Tools Project", "mtz");
            fileChooser.setFileFilter(filter);
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                final File fileToLoad = fileChooser.getSelectedFile();
                resetConfiguration();
                (new Thread(() -> {
                    loadProject(fileToLoad);
                })).start();
            }
        } else if (cmd.equals("manage_plugins")) {
            PluginManagerWindow pmw = new PluginManagerWindow();
            pmw.setVisible(true);
        } else if (cmd.equals("load_plugin")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Specify a file to open");   
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Java Archive", "jar");
            fileChooser.setFileFilter(filter);
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                File fileToLoad = fileChooser.getSelectedFile();
                final Path pathToLoad = fileToLoad.toPath();
                final Path targetPluginPath = (new File(getPluginFolder(), fileToLoad.getName())).toPath();
                (new Thread(() -> {
                    try
                    {
                        Files.copy(pathToLoad, targetPluginPath, StandardCopyOption.REPLACE_EXISTING);
                        loadPlugin(targetPluginPath.toFile());
                    } catch (Exception eCopy) {
                        eCopy.printStackTrace(System.err);
                    }
                })).start();
            }
        } else if (cmd.equals("new_control")) {
            CreateControlDialog editr = new CreateControlDialog();
        }
    }

    public static File addProjectAsset(File file)
    {
        return MidiTools.instance.assetManagerPanel.addAsset(file);
    }

    public static File resolveProjectAsset(String filename)
    {
        return new File(getAssetFolder(), filename);
    }

    public static ComboBoxModel<String> getAssetComboBoxModel(ArrayList<String> extens)
    {
        Vector<String> assetNames = new Vector<String>();
        Iterator<File> files = MidiTools.instance.assetManagerPanel.getAllAssets().iterator();
        while (files.hasNext())
        {
            File file = files.next();
            String filename = file.getName();
            String filenameLower = filename.toLowerCase();
            if (extens == null)
            {
                assetNames.add(filename);
            } else {
                for (String string : extens) {
                    if (filenameLower.endsWith(string))
                    {
                        assetNames.add(filename);
                    }
                }
            }
        }
        ComboBoxModel<String> rm = new DefaultComboBoxModel<String>(assetNames);
        return rm;
    }

    public static Vector<String> getAllAssetNames()
    {
        Vector<String> assetNames = new Vector<String>();
        Iterator<File> files = MidiTools.instance.assetManagerPanel.getAllAssets().iterator();
        while (files.hasNext())
        {
            File file = files.next();
            String filename = file.getName();
            assetNames.add(filename);
        }
        return assetNames;
    }

    public static Vector<String> getSoundAssets()
    {
        Vector<String> assetNames = new Vector<String>();
        Iterator<File> files = MidiTools.instance.assetManagerPanel.getAllAssets().iterator();
        while (files.hasNext())
        {
            File file = files.next();
            String filename = file.getName();
            String filenameLower = filename.toLowerCase();
            if (filenameLower.endsWith(".wav"))
            {
                assetNames.add(filename);
            }
        }
        return assetNames;
    }

    public static Vector<String> getImageAssets()
    {
        Vector<String> assetNames = new Vector<String>();
        Iterator<File> files = MidiTools.instance.assetManagerPanel.getAllAssets().iterator();
        while (files.hasNext())
        {
            File file = files.next();
            String filename = file.getName();
            String filenameLower = filename.toLowerCase();
            if (filenameLower.endsWith(".png") || filenameLower.endsWith(".gif") || filenameLower.endsWith(".webp") || filenameLower.endsWith(".jpg") || filenameLower.endsWith(".jpeg") || filenameLower.endsWith(".svg"))
            {
                assetNames.add(filename);
            }
        }
        return assetNames;
    }

    public String getShowQr()
    {
        if (this.noneQrMenuItem.isSelected())
        {
            return "none";
        } else if (this.canvasQrMenuItem.isSelected()) {
            return "canvas";
        } else if (this.remoteQrMenuItem.isSelected()) {
            return "remote";
        } else if (this.localQrMenuItem.isSelected()) {
            return "local";
        } else {
            return "";
        }
    }

    private void setShowQR(String value)
    {
        if ("none".equals(value) && this.qrLabel != null) {
            this.deviceQRPanel.remove(this.qrLabel);
            this.qrLabel = null;
            this.deviceQRPanel.revalidate();
            this.noneQrMenuItem.setSelected(true);
        } else {
            if (this.qrLabel != null)
            {
                this.deviceQRPanel.remove(this.qrLabel);
                this.qrLabel = null;
                this.deviceQRPanel.revalidate();
            }
            String url = "";
            if ("local".equals(value))
            {
                String localIP = MidiTools.getLocalIP() ;
                url = "https://" + localIP + ":6124/";
                this.localQrMenuItem.setSelected(true);
                System.err.println("LOCAL QR");
            } else if ("remote".equals(value)) {
                url = "https://openstatic.org/mcct/?s=midi-tools-" + MidiTools.LOCAL_SERIAL;
                this.remoteQrMenuItem.setSelected(true);
                System.err.println("REMOTE QR");
            } else if ("canvas".equals(value)) {
                url = getCanvasURL();
                this.canvasQrMenuItem.setSelected(true);
                System.err.println("CANVAS QR");
            }
            if (!"".equals(url) && url != null)
            {
                this.qrLabel = new JLabel(new ImageIcon(MidiTools.QRCode(url)));
                this.qrLabel.setBackground(Color.WHITE);
                this.qrLabel.setOpaque(true);
                this.deviceQRPanel.add(this.qrLabel, BorderLayout.SOUTH);
                this.deviceQRPanel.revalidate();
            } else {
                this.noneQrMenuItem.setSelected(true);
            }
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

    public String getCanvasURL()
    {
        String localIP = MidiTools.getLocalIP() ;
        return "http://" + localIP + ":6123/canvas.html";
    }

    public static Vector<String> getCanvasNames()
    {
        Vector<String> canvasNames = new Vector<String>();
        canvasNames.add("(ALL)");
        canvasNames.add("(NONE)");
        try
        {
            // Check for new devices added
            for(Enumeration<MidiControlRule> newRuleEnum = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); newRuleEnum.hasMoreElements();)
            {
                MidiControlRule mcr = newRuleEnum.nextElement();
                if (mcr.getActionType() == MidiControlRule.ACTION_SOUND || mcr.getActionType() == MidiControlRule.ACTION_EFFECT_IMAGE)
                {
                    String canvasName = mcr.getCanvasName();
                    if (canvasName != null && !canvasNames.contains(canvasName))
                        canvasNames.add(canvasName);
                }
                
            }
        } catch (Exception e) {
        }
        return canvasNames;
    }

    public static void renamedFile(String original, String newName)
    {
        
        try
        {
            // Check for new devices added
            for(Enumeration<MidiControlRule> newRuleEnum = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); newRuleEnum.hasMoreElements();)
            {
                MidiControlRule mcr = newRuleEnum.nextElement();
                if (mcr.getActionType() == MidiControlRule.ACTION_SOUND || mcr.getActionType() == MidiControlRule.ACTION_PROC)
                {
                    String actionValue = mcr.getActionValue();
                    if (actionValue != null)
                    {
                        if (actionValue.contains(original))
                            mcr.setActionValue(actionValue.replace(original, newName));
                    }
                }
                
            }
        } catch (Exception e) {

        }
    }

    private long lastSecondAt = 0l;
    public void run()
    {
        System.err.println("Launched Main Thread");
        while(this.keep_running)
        {
            long ts = System.currentTimeMillis();
            try
            {
                if (ts - this.lastSecondAt > 1000l)
                {
                    //System.err.println("everySecond");
                    everySecond();
                    this.lastSecondAt = ts;
                }
                if (MidiTools.windowWentVisible)
                {
                    for (Enumeration<MidiControl> mce = this.midiControlsPanel.getControlsEnumeration(); mce.hasMoreElements();)
                    {
                        MidiControl mc = mce.nextElement();
                        if ((ts - mc.getLastChangeAt()) > 250l && !mc.isSettled())
                        {
                            mc.settle();
                        }
                    }
                }
                Thread.sleep(50);
            } catch (Exception e) {
                MidiTools.instance.midi_logger_b.printException(e);
            }
        }
        System.err.println("Left Main Thread");
    }

    private void everySecond() throws Exception
    {
        if ((System.currentTimeMillis() - MidiTools.appLaunchTime) > 60000l && !MidiTools.windowWentVisible)
        {
            System.err.println("INTERNAL FAILURE");
            System.exit(1);
        }

        // Wait till window is actually visible to do any stuff like this.
        if (MidiTools.windowWentVisible)
        {
            try
            {
                this.assetManagerPanel.refresh();
                if (this.keep_running)
                {
                    String newTitle = "MIDI Control Change Tool v" + MidiTools.VERSION;
                    if (this.lastSavedFile != null)
                    {
                        if (!this.lastSavedFile.getParentFile().equals(MidiTools.getConfigFolder()))
                        {
                            boolean unsavedProjectChanges = MidiTools.this.hasUnsavedProjectChanges();
                            if (unsavedProjectChanges)
                            {
                                newTitle = "MIDI Control Change Tool v" + MidiTools.VERSION + " - [*" + this.lastSavedFile.getName() + "]";
                            } else {
                                newTitle = "MIDI Control Change Tool v" + MidiTools.VERSION + " - [" + this.lastSavedFile.getName() + "]";
                            }
                        }
                    }
                    if (!newTitle.equals(this.getTitle()))
                        this.setTitle(newTitle);
                } else {
                    System.err.println("Not messing with title!");
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            repaintRules();
            repaintMappings();
            repaintDevices();
            if (this.isShowing())
                this.windowLocation = this.getLocationOnScreen();
            Component mainTabComponent = this.mainTabbedPane.getSelectedComponent();
            if (mainTabComponent != null)
                mainTabComponent.repaint();
        }
    }

    public boolean hasUnsavedProjectChanges()
    {
        if (this.loadedProjectJSON != null)
        {
            String loadedConfig = this.loadedProjectJSON.toString();
            String possibleConfig = compileProjectJSON().toString();
            if (!possibleConfig.equals(loadedConfig))
            {
                //System.err.println("loaded:" + loadedConfig + "\npossible:" + possibleConfig);
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    private void centerWindow()
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
        final String os_name = System.getProperty("os.name").toLowerCase();

        MidiTools.LOCAL_SERIAL = MidiTools.getLocalMAC();
        System.err.println("main() midi-tools");
        if (os_name.contains("linux")) 
        {
            try
            {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                
            }
        } else {
            try
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                
            }
        }
        MidiTools.eraseAssets();
        MidiTools mlb = new MidiTools(os_name);
        mlb.start();
        //logIt("loading config");
        File loadFile = null;
        for (int i = 0; i < args.length; i++)
        {
            String filename = args[i];
            System.err.println("Arg(" + String.valueOf(i) + ") = " + filename);
            if (filename.endsWith(".mtz"))
            {
                loadFile = new File(filename);
            }
        }
        mlb.loadConfig(loadFile);
        SwingUtilities.invokeLater(new Runnable() 
        {
            public void run() {
                try
                {
                    System.err.println("TRYING TO BRING WINDOW VISIBLE");
                    mlb.setVisible(true);
                    System.err.println("WINDOW BROUGHT VISIBLE");
                } catch (Throwable goVisible) {
                    System.exit(1);
                }
            }
        });
        logIt("Finished Startup");
    }

    public static boolean isRuleGroupEnabled(String groupName)
    {
        for (Enumeration<MidiControlRule> mcre = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); mcre.hasMoreElements();)
        {
            MidiControlRule mcr = mcre.nextElement();
            if (mcr.getRuleGroup().equals(groupName))
            {
                if (!mcr.isEnabled())
                    return false;
            }
        }
        return true;
    }

    public static void setRuleGroupEnabled(String groupName, boolean v)
    {
        boolean groupEnabled = isRuleGroupEnabled(groupName);
        if (v && !groupEnabled)
        {
            for (Enumeration<MidiControlRule> mcre = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); mcre.hasMoreElements();)
            {
                MidiControlRule mcr = mcre.nextElement();
                if (mcr.getRuleGroup().equals(groupName))
                {
                    mcr.setEnabled(true);
                }
            }
            MidiTools.instance.midi_logger_b.println("Rule Group Enabled " + groupName);
        } else if (!v && groupEnabled) {
            for (Enumeration<MidiControlRule> mcre = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); mcre.hasMoreElements();)
            {
                MidiControlRule mcr = mcre.nextElement();
                if (mcr.getRuleGroup().equals(groupName))
                {
                    mcr.setEnabled(false);
                }
            }
            MidiTools.instance.midi_logger_b.println("Rule Group Disabled " + groupName);
        }
    }
    
    public static void toggleRuleGroupEnabled(String groupName)
    {
        for (Enumeration<MidiControlRule> mcre = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); mcre.hasMoreElements();)
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
        for (Enumeration<MidiControlRule> mcre = MidiTools.instance.midiControlRulePanel.getRulesEnumeration(); mcre.hasMoreElements();)
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
        for (Enumeration<MidiControl> mce = MidiTools.instance.midiControlsPanel.getControlsEnumeration(); mce.hasMoreElements();)
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
        for (Enumeration<MidiControl> mce = MidiTools.instance.midiControlsPanel.getControlsEnumeration(); mce.hasMoreElements();)
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
        int cc = jo.optInt("cc", -1);
        int note = jo.optInt("note", -1);
        if (cc >= 0)
        {
            MidiControl mc = getMidiControlByChannelCC(channel, cc);
            if (mc == null)
            {
                mc = new MidiControl(jo);
                handleNewMidiControl(mc, index);
            }
            return mc;
        } else if (note >= 0) {
            MidiControl mc = getMidiControlByChannelNote(channel, note);
            if (mc == null)
            {
                mc = new MidiControl(jo);
                handleNewMidiControl(mc, index);
            }
            return mc;
        } else {
            return null;
        }
    }
    
    public static void removeMidiControl(MidiControl mc)
    {
        try
        {
            MidiTools.instance.midiControlsPanel.removeMidiControl(mc);
            JSONObject event = new JSONObject();
            event.put("event", "controlRemoved");
            event.put("control", mc.toJSONObject());
            MidiTools.instance.apiServer.broadcastJSONObject(event);
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
        try
        {
            if (MidiTools.windowWentVisible)
            {
                SwingUtilities.invokeAndWait(() -> {
                    MidiTools.instance.midiControlsPanel.insertElementAt(mc, index);
                });
            } else {
                MidiTools.instance.midiControlsPanel.insertElementAt(mc, index);
            }
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
    
    public static File getConfigFile()
    {
        return new File(getConfigFolder(), "midi-tools.json");
    }

    public static File getConfigFolder()
    {
        File configFolder = new File(System.getProperty("user.home"), ".midi-tools/");
        if (!configFolder.exists())
            configFolder.mkdirs();
        return configFolder;
    }

    public static File getAssetFolder()
    {
        File assetFolder = new File(getConfigFolder(), "assets/");
        if (!assetFolder.exists())
            assetFolder.mkdirs();
        return assetFolder;
    }

    public static File getPluginFolder()
    {
        File pluginFolder = new File(getConfigFolder(), "plugins/");
        if (!pluginFolder.exists())
            pluginFolder.mkdirs();
        return pluginFolder;
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
    
    public JSONArray rulesAsJSONArray()
    {
       JSONArray rulesArray = new JSONArray();
        for (Enumeration<MidiControlRule> mcre = this.midiControlRulePanel.getRulesEnumeration(); mcre.hasMoreElements();)
        {
            MidiControlRule mcr = mcre.nextElement();
            rulesArray.put(mcr.toSavableJSONObject());
        }
        return rulesArray;
    }
    
    public JSONArray mappingsAsJSONArray()
    {
       JSONArray mappingArray = new JSONArray();
        for (Iterator<MidiPortMapping> mpme = MidiPortManager.getMidiPortMappings().iterator(); mpme.hasNext();)
        {
            MidiPortMapping mpm = mpme.next();
            mappingArray.put(mpm.toSavableJSONObject());
        }
        return mappingArray;
    }
    
    
    public JSONArray controlsAsJSONArray()
    {
        JSONArray controlsArray = new JSONArray();
        for (Enumeration<MidiControl> mce = this.midiControlsPanel.getControlsEnumeration(); mce.hasMoreElements();)
        {
            MidiControl mc = mce.nextElement();
            controlsArray.put(mc.toSavableJSONObject());
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

    public void saveConfig()
    {
        File file = getConfigFile();
        logIt("Saving Configuration: " + file.getName());
        try
        {
            JSONObject configJson = new JSONObject();
            configJson.put("apiServer", this.apiServerEnable.getState());
            configJson.put("bootstrapSSL", this.bootstrapSSLItem.getState());
            configJson.put("showQrType", this.getShowQr());
            configJson.put("windowX", this.windowLocation.x);
            configJson.put("windowY", this.windowLocation.y);
            configJson.put("windowWidth", this.getWidth());
            configJson.put("windowHeight", this.getHeight());
            if (this.lastSavedFile == null)
            {
                // User didnt save the work, lets help out
                this.lastSavedFile = new File(getConfigFolder(), "Untitled.mtz");
            }
            // user has been using untiltled lets save automatically.
            if (this.lastSavedFile.getName().contains("Untitled.mtz"))
            {
                saveProjectAs(this.lastSavedFile);
            }
            configJson.put("lastSavedFile" , this.lastSavedFile.toString());
            Iterator<MidiToolsPlugin> pIterator = this.plugins.values().iterator();
            while(pIterator.hasNext())
            {
                MidiToolsPlugin plugin = pIterator.next();
                if (plugin != null)
                {
                    try
                    {
                        this.pluginSettings.put(plugin.getTitle(), plugin.getSettings());
                    } catch (Throwable pluginEx) {
                        System.err.println("TRAPPED PLUGIN THROWABLE....");
                        pluginEx.printStackTrace(System.err);
                    }
                }
            }
            configJson.put("plugins", this.pluginSettings);
            saveJSONObject(file, configJson);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void loadConfig(File loadProject)
    {
        File file = getConfigFile();
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
            if (configJson.has("bootstrapSSL"))
            {
                this.bootstrapSSLItem.setState(configJson.optBoolean("bootstrapSSL", false));
            }
            if (configJson.has("apiServer"))
            {
                boolean apiEnable = configJson.optBoolean("apiServer", false);
                changeAPIState(apiEnable);
            }
            if (configJson.has("windowX"))
            {
                newWindowLocation.x = configJson.optInt("windowX", 0);
            }
            if (configJson.has("windowY"))
            {
                newWindowLocation.y = configJson.optInt("windowY", 0);
            }

            if (configJson.has("showQrType"))
            {
                String state = configJson.optString("showQrType", "none");
                this.setShowQR(state);
            }
            Vector<Thread> pluginLoads = new Vector<Thread>();
            if (configJson.has("plugins"))
            {
                this.pluginSettings = configJson.getJSONObject("plugins");
                Iterator<MidiToolsPlugin> pIterator = this.plugins.values().iterator();
                while(pIterator.hasNext())
                {
                    final MidiToolsPlugin plugin = pIterator.next();
                    JSONObject pluginSettingData = this.pluginSettings.optJSONObject(plugin.getTitle());
                    if (pluginSettingData == null)
                        pluginSettingData = new JSONObject();
                    final JSONObject finalPluginSettingData = pluginSettingData;
                    Thread plugin_load_thread = new Thread(() -> {
                        try
                        {
                            plugin.loadSettings(MidiTools.this, finalPluginSettingData);
                        } catch (Throwable plugEx) {
                            System.err.println("TRAPPED PLUGIN THROWABLE....");
                            plugEx.printStackTrace(System.err);
                        }
                    });
                    plugin_load_thread.start();
                    pluginLoads.add(plugin_load_thread);
                }
            } else {
                if (this.pluginSettings == null)
                    this.pluginSettings = new JSONObject();
            }

            // Wait for each plugin to load its settings, 4 seconds max!
            pluginLoads.forEach((thread) -> {
                try
                {
                    synchronized(thread)
                    {
                        if (thread.isAlive())
                            thread.wait(4000);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            });
            if (loadProject == null)
            {
                if (configJson.has("lastSavedFile"))
                {
                    File lsf = new File(configJson.optString("lastSavedFile"));
                    if (lsf.exists())
                    {
                        this.loadProject(lsf);
                    } else {
                        System.err.println("Last saved doesn't exist..");
                    }
                } else {
                    System.err.println("Last saved key missing");
                }
            } else {
                loadProject(loadProject);
            }
            this.setSize(windowWidth, windowHeight);
            if (newWindowLocation != null)
            {
                this.setLocation(newWindowLocation);
            }
            System.err.println("CONFIG LOAD FINISHED");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            MidiTools.instance.midi_logger_b.printException(e);
        }
    }

    public JSONObject compileProjectJSON()
    {
        JSONObject configJson = new JSONObject();
        configJson.put("controls", this.controlsAsJSONArray());
        configJson.put("rules", this.rulesAsJSONArray());
        configJson.put("openReceivingPorts", this.openReceivingPortsAsJSONArray());
        configJson.put("openTransmittingPorts", this.openTransmittingPortsAsJSONArray());
        configJson.put("mappings", this.mappingsAsJSONArray());
        configJson.put("randomizerRules", this.randomizerPort.getAllRules());
        JSONObject projectPluginJson = new JSONObject();
        Iterator<MidiToolsPlugin> pIterator = this.plugins.values().iterator();
        while(pIterator.hasNext())
        {
            MidiToolsPlugin plugin = pIterator.next();
            try
            {
                projectPluginJson.put(plugin.getTitle(), plugin.getProject());
            } catch (Throwable pluginEx) {
                System.err.println("TRAPPED PLUGIN THROWABLE....");
                pluginEx.printStackTrace(System.err);
            }
        }
        configJson.put("plugins", projectPluginJson);
        configJson.put("assets", new JSONArray(getAllAssetNames()));
        configJson.put("player", this.midiPlayer.getProjectJSON());
        return configJson;
    }

    public void saveProjectAs(File file)
    {
        try
        {
            JSONObject configJson = compileProjectJSON();

            FileOutputStream fout = new FileOutputStream(file);
            ZipOutputStream zout = new ZipOutputStream(fout);
            ZipEntry ze = new ZipEntry("project.json");

            File[] assets = getAssetFolder().listFiles();
            for(int i = 0; i < assets.length; i++)
            {
                File asset = assets[i];
                ZipEntry zipEntry = new ZipEntry("assets/" + asset.getName());
                zout.putNextEntry(zipEntry);
                FileInputStream fis = new FileInputStream(asset);
                byte[] buffer = new byte[4092];
                int byteCount = 0;
                while ((byteCount = fis.read(buffer)) != -1)
                {
                    zout.write(buffer, 0, byteCount);
                }
                fis.close();
                zout.closeEntry();
            }
            zout.putNextEntry(ze);
            zout.write(configJson.toString(2).getBytes());
            zout.closeEntry();

            zout.close();
            this.loadedProjectJSON = configJson;
            this.setLastSavedFile(file);
            logIt("Project Saved: " + file.getName());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            //MidiTools.instance.midi_logger_b.printException(e);
        }
    }

    public void loadProject(File file)
    {
        if (file.exists())
        {
            try
            {
                ZipFile zip = new ZipFile(file);
                JSONObject configJson = new JSONObject();
                for (Enumeration<? extends ZipEntry> zEnumeration = zip.entries(); zEnumeration.hasMoreElements();)
                {
                    ZipEntry entry = zEnumeration.nextElement();
                    String entryName = entry.getName();
                    if (entryName.equals("project.json"))
                    {
                        configJson = readJSONObject(zip.getInputStream(entry));
                    } else if (entryName.startsWith("assets/")) {
                        String outFilename = entryName.substring(7);
                        logIt("Extracting Asset: " + outFilename);
                        File outFile = new File(getAssetFolder(), outFilename);
                        if (!outFile.exists())
                        {
                            InputStream is = zip.getInputStream(entry);
                            FileOutputStream fos = new FileOutputStream(outFile);
                            byte[] buffer = new byte[4092];
                            int byteCount = 0;
                            while ((byteCount = is.read(buffer)) != -1)
                            {
                                fos.write(buffer, 0, byteCount);
                            }
                            fos.close();
                            is.close();
                        }
                    }
                }
                zip.close();
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
                        this.midiControlRulePanel.addElement(mcr);
                    }
                }
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
                            p.addReceiver(MidiTools.this.midiControlsPanel);
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
                Vector<Thread> pluginLoads = new Vector<Thread>();
                if (configJson.has("plugins"))
                {
                    JSONObject pluginProjectSettings = configJson.getJSONObject("plugins");
                    Iterator<MidiToolsPlugin> pIterator = this.plugins.values().iterator();
                    while(pIterator.hasNext())
                    {
                        final MidiToolsPlugin plugin = pIterator.next();
                        final JSONObject pluginSettingData = pluginProjectSettings.optJSONObject(plugin.getTitle());
                        if (pluginSettingData != null)
                        {
                            Thread pluginLoadThread = new Thread(() -> {
                                try
                                {
                                    plugin.loadProject(pluginSettingData);
                                } catch (Throwable pluginEx) {
                                    System.err.println("TRAPPED PLUGIN THROWABLE....");
                                    pluginEx.printStackTrace(System.err);
                                }
                            });
                            pluginLoadThread.start();
                            pluginLoads.add(pluginLoadThread);
                        }
                    }
                }
                // Wait for each plugin to load its settings, 4 seconds max!
                pluginLoads.forEach((thread) -> {
                    try
                    {
                        synchronized(thread)
                        {
                            if (thread.isAlive())
                                thread.wait(4000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                });
                this.loadedProjectJSON = configJson;
                this.setLastSavedFile(file);
                Thread refreshPlayerThread = new Thread(() -> {
                    try
                    {
                        Thread.sleep(2000l);
                        MidiTools.this.assetManagerPanel.refresh();
                        MidiTools.this.midiPlayer.refreshAssetChoices();
                        MidiTools.this.midiPlayer.loadProject(MidiTools.this.loadedProjectJSON.optJSONObject("player"));
                        Vector<String> canvasNames = MidiTools.getCanvasNames();
                
                        JSONObject welcomeObject = new JSONObject();
                        welcomeObject.put("canvasList", new JSONArray(canvasNames));
                        welcomeObject.put("sounds", new JSONArray(MidiTools.getSoundAssets()));
                        welcomeObject.put("images", new JSONArray(MidiTools.getImageAssets()));
                        String projectName = MidiTools.getProjectName();
                        if (projectName != null)
                            welcomeObject.put("projectName", projectName);
                        MidiTools.this.apiServer.broadcastCanvasJSONObject(welcomeObject);
                    } catch (Exception e) {}
                });
                refreshPlayerThread.start();
                logIt("Project Loaded: " + file.getName());
            } catch (Exception e) {
                MidiTools.instance.midi_logger_b.printException(e);
            }
        } else {
            logIt("Project Doesn't Exist! " + file.getName());
        }
    }

    public static JSONObject readJSONObject(InputStream is)
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            int ch;
            while((ch = is.read()) != -1){
                builder.append((char)ch);
            }
            is.close();
            JSONObject props = new JSONObject(builder.toString());
            return props;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static JSONObject loadJSONObject(File file)
    {
        try
        {
            FileInputStream fis = new FileInputStream(file);
            return readJSONObject(fis);
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
            CopyOrGo copyOrGo = new CopyOrGo(MidiTools.instance, url);
            copyOrGo.setVisible(true);
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
        String return_ip = "127.0.0.1";
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

    public static BufferedImage resizeImage(String amount, BufferedImage in_image, boolean alpha)
    {
        if ("".equals(amount) || amount == null)
        {
            return in_image;
        } else {
            float scale_to_float = 0;
            float w = 0;
            float h = 0;
            float o_w = (float) in_image.getWidth();
            float o_h = (float) in_image.getHeight();
            if (amount.contains("x"))
            {
                String[] spl = amount.split("x");
                w = Float.valueOf(spl[0]).floatValue();
                h = Float.valueOf(spl[1]).floatValue();
            } else {
                scale_to_float = Float.valueOf(amount).floatValue();
                w = (o_w * scale_to_float);
                h = (o_h * scale_to_float);
            }
            if (!alpha)
            {
                AffineTransform at = new AffineTransform();
                at.scale(w/o_w, h/o_h);
                AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                BufferedImage rgbi = new BufferedImage(in_image.getWidth(), in_image.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgbi.createGraphics().drawImage(in_image, 0, 0, Color.WHITE, null);
                BufferedImage ri = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_RGB);
                scaleOp.filter(rgbi, ri);
                return ri;
            } else {
                AffineTransform at = new AffineTransform();
                at.scale(w/o_w, h/o_h);
                AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                BufferedImage rgbi = new BufferedImage(in_image.getWidth(), in_image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                rgbi.createGraphics().drawImage(in_image, 0, 0, new Color(1f,1f,1f,0f), null);
                BufferedImage ri = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);
                scaleOp.filter(rgbi, ri);
                return ri;
            }
        }
    }

    public static synchronized ImageIcon getCachedIcon(String url, String size)
    {
        return new ImageIcon(getCachedImage(url, size));
    }

    public static synchronized BufferedImage getCachedImage(String url, String size)
    {
        if (size == null)
            size = "";
        String key = size + "-" + url;
        if (MidiTools.cachedImages.containsKey(key))
        {
            return MidiTools.cachedImages.get(key);
        } else {
            try {
                BufferedImage image = null;
                if (url.startsWith("http://") || url.startsWith("https://"))
                {
                    System.err.println("Downloading " + url + " ...");
                    URL url2 = new URL(url);
                    image = ImageIO.read(url2);
                } else if (url.startsWith("/")) {
                    System.err.print("READING " + url + " ...");
                    image = ImageIO.read(MidiTools.class.getResourceAsStream(url));
                }
                if (image != null)
                {
                    BufferedImage ri = MidiTools.resizeImage(size, image, true);
                    MidiTools.cachedImages.put(key, ri);
                    return ri;
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            return null;
        }
    }

    public boolean removePlugin(String title, String localFile)
    {
        try
        {
            MidiToolsPlugin removedPlugin = MidiTools.instance.plugins.remove(title);
            int tabIndex = this.mainTabbedPane.indexOfTab(title);
            if (tabIndex >=0 )
                this.mainTabbedPane.removeTabAt(tabIndex);
            URLClassLoader urlLoader = (URLClassLoader) removedPlugin.getClass().getClassLoader();
            urlLoader.close();
            File x = new File(localFile);
            if (x.exists())
            {
                System.err.println("Flagging jar for delete on exit: " + localFile);
                FileOutputStream out = new FileOutputStream(x);
                out.write(new byte[0]);
                out.flush(); out.close();
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return false;
    }

    public boolean installPlugin(String urlPath, String filename)
    {
        try
        {
            File jarFile = new File(getPluginFolder(), filename);
            URL urlPlugin = new URL(urlPath + filename);
            InputStream urlInStream = urlPlugin.openStream();
            //Files.copy(urlInStream, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            FileOutputStream out = new FileOutputStream(jarFile);
            out.write(new byte[0]);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = urlInStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush(); 
            out.close();
            urlInStream.close();
            return loadPlugin(jarFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return false;
    }

    public boolean loadPlugin(File jarfile)
    {
        try
        {
            if (!jarfile.exists())
            {
                System.err.println("no jar found");
                return false;
            }
            URLClassLoader child = new URLClassLoader(new URL[] { jarfile.toURL() }, this.getClass().getClassLoader());
            JarFile jf = new JarFile(jarfile);
            Manifest mf = jf.getManifest();
            Attributes at = mf.getMainAttributes();
            String main_class = at.getValue("Main-Class");
            Class<?> c = Class.forName(main_class, true, child);
            Constructor<?> cons = c.getDeclaredConstructor();
            final MidiToolsPlugin new_plugin = (MidiToolsPlugin) cons.newInstance();
            Thread t = new Thread()
            {
                public void run()
                {
                    try
                    {
                        String pluginTitle = new_plugin.getTitle();
                        JSONObject newPluginSettings = new JSONObject();
                        if (MidiTools.this.pluginSettings != null)
                        {
                            if (MidiTools.this.pluginSettings.has(pluginTitle))
                            {
                                newPluginSettings = MidiTools.this.pluginSettings.optJSONObject(pluginTitle);
                            }
                        }
                        new_plugin.loadSettings(MidiTools.this, newPluginSettings);
                        JPanel panel = new_plugin.getPanel();
                        MidiTools.this.plugins.put(pluginTitle, new_plugin);
                        if (panel != null)
                        {
                            MidiTools.this.mainTabbedPane.addTab(pluginTitle, new_plugin.getIcon(), panel);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace(System.err);
                    }
                }
            };
            t.start();
            logIt("Loaded Plugin: " + jarfile.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logIt("Couldn't load plugin: " + jarfile.toString());
            jarfile.delete();
            return false;
        }
    }
}
