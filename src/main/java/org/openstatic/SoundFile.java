package org.openstatic;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;

import java.net.URL;
import java.net.URLConnection;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;

import java.util.Random;

public class SoundFile
{
    private String sound_url;
    private File soundFile;
    private Thread downloadThread;
    private AudioInputStream audioStream;
    private float volume;

    private File saveUrlAsWav(String urlString) throws Exception
    {
        String random_filename = generateBigAlphaKey(10);
        File file = File.createTempFile(random_filename, ".wav");
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try
        {
            URLConnection urlc = new URL(urlString).openConnection();
            urlc.setDoInput(true);
            in = new BufferedInputStream(urlc.getInputStream());
            fout = new FileOutputStream(file);
            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1)
            {
                fout.write(data, 0, count);
            }
        } finally {
            if (in != null)
                in.close();
            if (fout != null)
            {
                fout.flush();
                fout.close();
            }
        }
        return file;
    }

    private static String generateBigAlphaKey(int key_length)
    {
        Random n = new Random(System.currentTimeMillis());
        String alpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuffer return_key = new StringBuffer();
        for (int i = 0; i < key_length; i++)
        {
            return_key.append(alpha.charAt(n.nextInt(alpha.length())));
        }
        return return_key.toString();
    }
    
    public String getSoundUrl()
    {
        return this.sound_url;
    }

    public SoundFile(String strFilename)
    {
        this.volume = 1.0f;
        //System.err.println("New Sound: " + strFilename);
        try
        {
            this.sound_url = strFilename;
            if (this.sound_url.startsWith("http://") || this.sound_url.startsWith("https://"))
            {
                this.downloadThread = new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            SoundFile.this.soundFile = saveUrlAsWav(SoundFile.this.sound_url);
                            System.err.println("Saved URL: " + SoundFile.this.soundFile.getAbsolutePath());
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                    }
                };
                this.downloadThread.start();
            } else {
                this.soundFile = new File(this.sound_url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SoundFile(File file)
    {
        this.volume = 1.0f;
        //System.err.println("New Sound: " + file.toString());
        this.soundFile = file;
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

    public File getFile()
    {
        return this.soundFile;
    }

    public void playAndWait()
    {
        if (this.downloadThread != null)
        {
            try
            {
                this.downloadThread.join();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        //System.err.println("Playing Sound: " + this.soundFile.toString());
        try
        {
            this.audioStream = AudioSystem.getAudioInputStream(this.soundFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Line.Info info = new Line.Info(Clip.class);
        Clip clip = null;
        try
        {
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(this.audioStream);
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
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
