package org.openstatic.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;

import org.openstatic.MidiTools;

public class SoundFile
{
    private File soundFile;
    private File tempSoundFile;
    private float volume;
    private LinkedBlockingQueue<Clip> clipQueue;
    private int queueSize;
    private int clipTrackedDuration;
    private String createdWithFilename;

    public SoundFile(String strFilename)
    {
        this.createdWithFilename = strFilename;
        this.clipTrackedDuration = 0;
        this.clipQueue = new LinkedBlockingQueue<Clip>();
        this.volume = 1.0f;
        this.queueSize = 3;
        File potentialFile = new File(strFilename);
        if (!potentialFile.exists())
        {
            potentialFile = MidiTools.resolveProjectAsset(strFilename);
        }
        this.soundFile = potentialFile;
        try
        {
            // Make a copy of the wave as a temp file to prevent file locking
            this.tempSoundFile = File.createTempFile("snd", ".wav");
            Files.copy(this.soundFile.toPath(), this.tempSoundFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.tempSoundFile.deleteOnExit();
            this.soundFile = this.tempSoundFile;
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        if (this.soundFile.exists())
        {
            System.err.println("New Sound: " + this.soundFile.toString());
            this.loadAudioFileAsClip();
            this.forceClipFirstLoad();
        } else {
            System.err.println("Sound doesn't exist: " + this.soundFile.toString());
        }
    }

    public void forceClipFirstLoad()
    {
        Thread x = new Thread(() -> {
            try
            {
                Clip clip = this.clipQueue.poll();
                if (clip != null)
                {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                    //play the clip silently to force the system audio to load it.
                    gainControl.setValue(-80f);
                    muteControl.setValue(true);
                    clip.start();
                    Thread.sleep(100);
                    while(clip.isActive())
                    {
                        Thread.sleep(1000);
                        this.clipTrackedDuration++;
                    }
                    //System.err.println("Sound Test Finished: " + this.soundFile.toString() + " " + String.valueOf(this.clipTrackedDuration) + "s");
                    clip.flush();
                    clip.close();
                }
                this.loadAudioFileAsClip();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        x.start();
    }

    public void setVolume(float volume)
    {
        this.volume = volume;
    }

    public void play()
    {
        Thread t = new Thread()
        {
            public void run()
            {
                SoundFile.this.playAndWait();
            }
        };
        t.start();
    }

    private void loadAudioFileAsClip()
    {
        boolean issue = false;
        if (this.soundFile.exists())
        {
            while(this.clipQueue.size() < this.queueSize && !issue)
            {
                try
                {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(this.soundFile);
                    Line.Info info = new Line.Info(Clip.class);
                    Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.open(audioStream);
                    this.clipQueue.put(clip);
                } catch (Exception e) {
                    issue = true;
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public boolean wasCreatedWith(String filename)
    {
        if (filename != null)
        {
            return filename.equals(this.createdWithFilename);
        } else {
            return false;
        }
    }

    public File getFile()
    {
        return this.soundFile;
    }

    public void playAndWait()
    {
        System.err.println("Playing Sound: " + this.soundFile.toString());
        //System.err.println("Clip Queue Size: " + String.valueOf(this.clipQueue.size()));
        try
        {
            Clip clip = this.clipQueue.poll();
            if (clip == null)
            {
                this.queueSize++;
                loadAudioFileAsClip();
                clip = this.clipQueue.poll();
            }
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            //System.err.println("Setting Volume: " + String.valueOf(this.volume));
            gainControl.setValue(this.volume);
            clip.start();
            Thread.sleep(100);
            while(clip.isActive())
            {
                Thread.sleep(1000);
            }
            //System.err.println("Sound Finished: " + this.soundFile.toString());
            clip.flush();
            clip.close();
            this.loadAudioFileAsClip();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close()
    {
        try
        {
            Clip clip = this.clipQueue.poll();
            while (clip != null)
            {
                clip.close();
                clip = this.clipQueue.poll();
            }
            if (this.tempSoundFile != null)
            {
                this.tempSoundFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
