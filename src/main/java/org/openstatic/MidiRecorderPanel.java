package org.openstatic;

import javax.imageio.ImageIO;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.openstatic.midi.MidiPort;
import org.openstatic.midi.MidiPortListener;
import org.openstatic.midi.MidiPortMapping;

import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;

public class MidiRecorderPanel extends JPanel implements ActionListener, MidiPortListener
{
    private Sequencer sequencer;
    private Sequence sequence;
    private File midiFile;
    private Receiver seqReceiver;
    private JButton playButton;
    private JToggleButton recordButton;
    private JToggleButton repeatButton;
    private JButton pauseButton;
    private JButton nextButton;
    private JButton previousButton;
    private JPanel buttonPanel;
    private Track recordingTrack;

    public MidiRecorderPanel()
    {
        super(new BorderLayout());
        try
        {
            this.sequencer = MidiSystem.getSequencer(false);
            this.seqReceiver = this.sequencer.getReceiver();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3,2));
        
        this.previousButton = new JButton(getIcon("/prev.png"));
        this.previousButton.setActionCommand("previous");
        this.previousButton.addActionListener(this);
		buttonPanel.add(this.previousButton);

        this.recordButton = new JToggleButton(getIcon("/record.png"));
        this.recordButton.setActionCommand("record");
        this.recordButton.addActionListener(this);
        buttonPanel.add(this.recordButton);

        this.playButton = new JButton(getIcon("/play.png"));
        this.playButton.setActionCommand("play");
        this.playButton.addActionListener(this);
        buttonPanel.add(this.playButton);

        this.nextButton = new JButton(getIcon("/next.png"));
        this.nextButton.setActionCommand("next");
        this.nextButton.addActionListener(this);
        buttonPanel.add(this.nextButton);

        this.repeatButton = new JToggleButton(getIcon("/repeat.png"));
        this.repeatButton.setActionCommand("repeat");
        this.repeatButton.addActionListener(this);
        buttonPanel.add(this.repeatButton);

        this.pauseButton = new JButton(getIcon("/pause.png"));
        this.pauseButton.setActionCommand("pause");
        this.pauseButton.addActionListener(this);
        buttonPanel.add(this.pauseButton);

        this.add(buttonPanel, BorderLayout.WEST);
    }

    public void loadFile(File file)
    {
        try
        {
            this.midiFile = file;
            this.sequence = MidiSystem.getSequence(this.midiFile);
            this.sequencer.setSequence(this.sequence);
        } catch (Exception e) {
            this.midiFile = null;
        }
    }

    public ImageIcon getIcon(String name)
    {
        try
        {
            return new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream(name)));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) 
    {
        String actionCommand = e.getActionCommand();
        if ("record".equals(actionCommand))
        {
            if (this.recordButton.isSelected())
            {
                this.sequencer.recordEnable(this.recordingTrack, -1);
            } else {
                this.sequencer.recordDisable(this.recordingTrack);
            }
        }
        if ("play".equals(actionCommand))
        {
            if (this.recordButton.isSelected())
            {
                this.sequencer.startRecording();
            } else {
                this.sequencer.start();
            }
        }
        if ("previous".equals(actionCommand))
        {
            this.sequencer.setTickPosition(0);
        }
        if ("next".equals(actionCommand))
        {
            this.sequencer.setTickPosition(this.sequencer.getTickLength());
        }
    }

    @Override
    public void portAdded(int idx, MidiPort port) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void portRemoved(int idx, MidiPort port) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void portOpened(MidiPort port) {
        port.addReceiver(this.seqReceiver);
        
    }

    @Override
    public void portClosed(MidiPort port) {
        port.removeReceiver(this.seqReceiver);
    }

    @Override
    public void mappingAdded(int idx, MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mappingRemoved(int idx, MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mappingOpened(MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mappingClosed(MidiPortMapping mapping) {
        // TODO Auto-generated method stub
        
    }
}
