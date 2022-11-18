package org.openstatic;

import javax.imageio.ImageIO;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Sequencer.SyncMode;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortListener;
import org.openstatic.midi.MidiPortManager;
import org.openstatic.midi.MidiPortMapping;

import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class MidiPlayerPanel extends JPanel implements ActionListener, MidiPort, Runnable
{
    private Sequencer sequencer;
    private Sequence sequence;
    private File midiFile;
    private boolean opened;
    private JButton playButton;
    private JToggleButton repeatButton;
    private JButton pauseButton;
    private JButton previousButton;
    private JPanel buttonPanel;
    private Vector<Receiver> receivers;
    private JComboBox<String> selectFileField;
    private long lastRxAt;
    private Thread clockThread;
    private JPanel selectTrackPanel;
    private JButton selectFileButton;
    private JTextPane viewArea;
    private long lastDisplayUpdate;
    private ImageIcon myIcon;
    private boolean userPaused;

    private Receiver seqReceiver = new Receiver() {

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (MidiPlayerPanel.this.opened)
            {
                MidiPlayerPanel.this.lastRxAt = System.currentTimeMillis();
                MidiPlayerPanel.this.receivers.forEach((r) -> {
                    r.send(message, timeStamp);
                });
            }
        }

        @Override
        public void close() {
            
        }
        
    };
    public MidiPlayerPanel()
    {
        super(new BorderLayout());
        this.userPaused = true;
        this.receivers = new Vector<Receiver>();
        try
        {
            this.sequencer = MidiSystem.getSequencer(false);
            this.sequencer.setMasterSyncMode(SyncMode.MIDI_SYNC);
            this.sequencer.setSlaveSyncMode(SyncMode.MIDI_SYNC);
            this.sequencer.getTransmitter().setReceiver(this.seqReceiver);
            this.sequencer.open();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.selectFileField = new JComboBox<String>();
        this.selectFileField.addFocusListener(new FocusListener()
        {

            @Override
            public void focusGained(FocusEvent e) {
                String filename = null;
                if (MidiPlayerPanel.this.midiFile != null)
                    filename = MidiPlayerPanel.this.midiFile.getName();
                MidiPlayerPanel.this.refreshAssetChoices(filename);
            }

            @Override
            public void focusLost(FocusEvent e) {
                MidiPlayerPanel.this.loadSelectedFile();
            }
            
        });

        this.myIcon = getIcon("/midi-tools-res/midifile32.png");

        this.viewArea = new JTextPane();
        this.viewArea.setContentType("text/html");
        this.viewArea.setEditable(false);
        this.viewArea.setBackground(new Color(34,34,34));
        this.viewArea.setForeground(Color.WHITE);
        this.viewArea.setText("");

        this.selectFileField.addActionListener(this);
        this.buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1,5));
        
        this.previousButton = new JButton(getIcon("/midi-tools-res/prev.png"));
        this.previousButton.setActionCommand("previous");
        this.previousButton.addActionListener(this);
		buttonPanel.add(this.previousButton);

        this.playButton = new JButton(getIcon("/midi-tools-res/play.png"));
        this.playButton.setActionCommand("play");
        this.playButton.addActionListener(this);
        this.playButton.setOpaque(true);
        this.playButton.setForeground(Color.LIGHT_GRAY);
        buttonPanel.add(this.playButton);

        this.repeatButton = new JToggleButton(getIcon("/midi-tools-res/repeat.png"));
        this.repeatButton.setActionCommand("repeat");
        this.repeatButton.addActionListener(this);
        buttonPanel.add(this.repeatButton);

        this.pauseButton = new JButton(getIcon("/midi-tools-res/pause.png"));
        this.pauseButton.setActionCommand("pause");
        this.pauseButton.addActionListener(this);
        buttonPanel.add(this.pauseButton);
        this.selectTrackPanel = new JPanel(new BorderLayout());
        this.selectTrackPanel.add(new JLabel(" Select MIDI Track "), BorderLayout.WEST);
        this.selectTrackPanel.add(this.selectFileField, BorderLayout.CENTER);
        this.selectFileButton = new JButton("...");
        this.selectFileButton.addActionListener(this);
        this.selectTrackPanel.add(this.selectFileButton, BorderLayout.EAST);

        this.add(this.selectTrackPanel, BorderLayout.PAGE_START);
        this.add(this.viewArea, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.PAGE_END);
        this.clockThread = new Thread(this);
        this.clockThread.start();
    }

    public void refreshAssetChoices(String filename)
    {
        ArrayList<String> extens = new ArrayList<String>();
        extens.add(".mid");
        extens.add(".midi");
        this.selectFileField.setModel(MidiTools.getAssetComboBoxModel(extens));
        if (filename != null)
            this.selectFileField.setSelectedItem(filename);
    }

    public void loadFile(File file)
    {
        try
        {
            System.err.println("Player Loading: " + file.toString());
            this.midiFile = file;
            this.sequence = MidiSystem.getSequence(this.midiFile);
            this.sequencer.setSequence(this.sequence);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            this.midiFile = null;
        }
    }

    public ImageIcon getIcon()
    {
        return this.myIcon;
    }

    private ImageIcon getIcon(String name)
    {
        try
        {
            return new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream(name)));
        } catch (Exception e) {
            return null;
        }
    }

    public void play()
    {
        this.userPaused = false;
        if (!this.sequencer.isRunning())
        {
            this.loadSelectedFile();
            if (this.midiFile != null && this.sequence != null)
                this.sequencer.start();
        }
    }

    public void pause()
    {
        this.userPaused = true;
        if (this.sequencer.isRunning())
            this.sequencer.stop();
    }
    
    public void restartTrack()
    {
        this.sequencer.setTickPosition(0);
            loadSelectedFile();
    }


    @Override
    public void actionPerformed(ActionEvent e) 
    {
        String actionCommand = e.getActionCommand();
        if ("play".equals(actionCommand))
        {
            play();
        }
        if ("pause".equals(actionCommand))
        {
            pause();
        }
        if ("previous".equals(actionCommand))
        {
            restartTrack();
        }
        if ("next".equals(actionCommand))
        {
            this.sequencer.setTickPosition(this.sequencer.getTickLength());
        }
        if (e.getSource() == this.selectFileButton)
        {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select a Midi file"); 
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Midi File", "mid", "midi");
            fileChooser.setFileFilter(filter);  
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                final File fileToLoad = fileChooser.getSelectedFile();
                if (fileToLoad != null)
                {
                    Thread x = new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                File assetFile = MidiTools.addProjectAsset(fileToLoad);
                                String filename = assetFile.getName();
                                Thread.sleep(2000l);
                                MidiPlayerPanel.this.refreshAssetChoices(filename);
                            } catch (Exception ex) {

                            }
                        }
                    };
                    x.start();
                }
            }
        }
        if (e.getSource() == this.selectFileField)
        {
            loadSelectedFile();
        }
    }

    public void setSelectedFilename(String filename)
    {
        this.selectFileField.setSelectedItem(filename);
    }

    public String getSelectedFilename()
    {
        return this.selectFileField.getSelectedItem().toString();
    }

    public void loadSelectedFile()
    {
        String selectedFilename = this.selectFileField.getSelectedItem().toString();
        if (!this.sequencer.isRunning())
        {
            if (this.midiFile == null)
            {
                this.loadFile(new File(MidiTools.getAssetFolder(), selectedFilename));
            } else if (!this.midiFile.getName().equals(selectedFilename)) {
                this.loadFile(new File(MidiTools.getAssetFolder(), selectedFilename));
            }
        } else if (this.sequencer.getMicrosecondPosition() == 0) {
            this.loadFile(new File(MidiTools.getAssetFolder(), selectedFilename));
        }
    }

    @Override
    public void open() {
        this.opened = true;       
        MidiPortManager.firePortOpened(this);
 
    }

    @Override
    public void close() {
        this.opened = false;
        MidiPortManager.firePortClosed(this);
    }

    @Override
    public boolean isOpened() {
        return this.opened;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public long getMicrosecondPosition() {
        return this.sequencer.getMicrosecondPosition();
    }

    @Override
    public boolean equals(MidiPort port) {
        return port.getName().equals(this.getName());
    }

    @Override
    public String getName()
    {
        return "Internal Player";
    }

    public String toString()
    {
        return this.getName();
    }

    @Override
    public boolean canTransmitMessages() {
        return true;
    }

    public void addReceiver(Receiver r)
    {
        if (!this.receivers.contains(r))
        {
            this.receivers.add(r);
        }
    }
    
    public void removeReceiver(Receiver r)
    {
        if (this.receivers.contains(r))
        {
            this.receivers.remove(r);
        }
    }

    public Collection<Receiver> getReceivers()
    {
        return this.receivers;
    }

    public boolean hasReceiver(Receiver r)
    {
        return this.receivers.contains(r);
    }

    @Override
    public boolean canReceiveMessages() {
        return false;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        // TODO Auto-generated method stub
    }

    @Override
    public long getLastRxAt() {
        return this.lastRxAt;
    }

    @Override
    public long getLastTxAt() {
        return 0;
    }

    @Override
    public void run() {
        while(MidiPortManager.isRunning())
        {
            try
            {
                long cts = System.currentTimeMillis();
                boolean playing = false;
                if (this.sequencer.isRunning())
                {
                    playing = true;
                    if (!MidiPlayerPanel.this.playButton.isOpaque())
                    {
                        MidiPlayerPanel.this.playButton.setBackground(Color.GREEN);
                        MidiPlayerPanel.this.playButton.setOpaque(true);
                    }
                    ShortMessage sm = new ShortMessage(ShortMessage.TIMING_CLOCK);
                    this.seqReceiver.send(sm, this.sequencer.getMicrosecondPosition());
                    float mpq = this.sequencer.getTempoInMPQ();
                    long sleep = (long)((mpq / 24) * 1000f);
                    long start = System.nanoTime();
                    while(start + sleep >= System.nanoTime())
                    {
                        Thread.sleep(1);
                    }
                } else {
                    if (MidiPlayerPanel.this.playButton.isOpaque())
                    {
                        MidiPlayerPanel.this.playButton.setBackground(Color.LIGHT_GRAY);
                        MidiPlayerPanel.this.playButton.setOpaque(false);
                    }
                    Thread.sleep(1000);
                    if (this.repeatButton.isSelected())
                    {
                        if (this.sequence != null && !userPaused)
                        {
                            if (sequence.getMicrosecondLength() == this.sequencer.getMicrosecondPosition())
                            {
                                System.err.println("Honoring repeat mode");
                                this.sequencer.setMicrosecondPosition(0);
                                this.sequencer.start();
                            }
                        }
                    }
                }
                if (cts - this.lastDisplayUpdate > 1000l)
                {
                    this.lastDisplayUpdate = cts;
                    String state = "Stopped";
                    String midiFileName = "No MIDI file loaded";
                    if (this.midiFile != null)
                    {
                        midiFileName = this.midiFile.getName();
                    }
                    if (!playing && this.sequencer.getMicrosecondPosition() > 0)
                    {
                        state = "Paused";
                    } else if (playing) {
                        state = "Playing";
                    }
                    long durationSecond = 0;
                    if (this.sequence != null)
                        durationSecond = this.sequence.getMicrosecondLength() / 1000000l;
                    long trackProgressSeconds = this.getMicrosecondPosition() / 1000000l;
                    String trackProgress = getDurationBreakdown(trackProgressSeconds)+ " / " + getDurationBreakdown(durationSecond);

                    this.viewArea.setText("<html><body style=\"color: white; font-size: 24px; text-align: center; vertical-align: middle;\"><B>" + state + " " + midiFileName + "</B> [" + trackProgress + "]</body></html>");

                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }

    public static String getDurationBreakdown(long seconds)
    {
        NumberFormat formatter = new DecimalFormat("#00");
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        StringBuilder sb = new StringBuilder(64);
        //sb.append(formatter.format(hours));
        //sb.append(":");
        sb.append(formatter.format(minutes));
        sb.append(":");
        sb.append(formatter.format(seconds));
        return(sb.toString());
    }
}
