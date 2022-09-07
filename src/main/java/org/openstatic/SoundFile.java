package org.openstatic;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;

public class SoundFile
{
    private File soundFile;
    private float volume;
    private LinkedBlockingQueue<Clip> clipQueue;
    private int queueSize;
    private int clipTrackedDuration;

    public SoundFile(String strFilename)
    {
        this(new File(strFilename));
    }

    public SoundFile(File file)
    {
        this.clipTrackedDuration = 0;
        this.clipQueue = new LinkedBlockingQueue<Clip>();
        this.volume = 1.0f;
        this.queueSize = 3;
        System.err.println("New Sound: " + file.toString());
        this.soundFile = file;
        this.loadAudioFileAsClip();
        this.forceClipFirstLoad();
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
                    //play the clip silently to force the system audio to load it.
                    gainControl.setValue(-80f);
                    clip.start();
                    Thread.sleep(100);
                    while(clip.isActive())
                    {
                        Thread.sleep(1000);
                        this.clipTrackedDuration++;
                    }
                    System.err.println("Sound Test Finished: " + this.soundFile.toString() + " " + String.valueOf(this.clipTrackedDuration) + "s");
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
}
