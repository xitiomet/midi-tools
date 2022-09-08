package org.openstatic;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class FolderListModel implements ListModel<File>
{
    public File root;
    public File[] files;
    private Vector<ListDataListener> listeners = new Vector<ListDataListener>();

    public FolderListModel(File root)
    {
        this.root = root;
        this.files = this.root.listFiles();
    }

    public synchronized void refresh()
    {
        File[] newFiles = this.root.listFiles();
        List<File> newFilesList = new ArrayList<File>(Arrays.asList(newFiles));
        List<File> oldFilesList = new ArrayList<File>(Arrays.asList(this.files));
        this.files = newFiles;

        int location = 0;
        for (Iterator<File> oldFilesIterator = oldFilesList.iterator(); oldFilesIterator.hasNext(); )
        {
            File nextFile = oldFilesIterator.next();
            if (!newFilesList.contains(nextFile))
            {
                for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
                {
                    try
                    {
                        final ListDataListener ldl = ldle.nextElement();
                        final ListDataEvent lde = new ListDataEvent(nextFile, ListDataEvent.INTERVAL_REMOVED, location, location);
                        SwingUtilities.invokeAndWait(() -> {
                            ldl.intervalAdded(lde);
                        });
                    } catch (Exception mlex) {
                    }
                }
            }
            location++;
        }

        location = 0;
        for (Iterator<File> newFilesIterator = newFilesList.iterator(); newFilesIterator.hasNext(); )
        {
            File nextFile = newFilesIterator.next();
            if (!oldFilesList.contains(nextFile))
            {
                for (Enumeration<ListDataListener> ldle = ((Vector<ListDataListener>) this.listeners.clone()).elements(); ldle.hasMoreElements();)
                {
                    try
                    {
                        final ListDataListener ldl = ldle.nextElement();
                        final ListDataEvent lde = new ListDataEvent(nextFile, ListDataEvent.INTERVAL_ADDED, location, location);
                        SwingUtilities.invokeAndWait(() -> {
                            ldl.intervalAdded(lde);
                        });
                    } catch (Exception mlex) {
                    }
                }
            }
            location++;
        }
        
    }

    public int getSize()
    {
        try
        {
            return this.files.length;
        } catch (Exception e) {
            return 0;
        }
    }

    public File getElementAt(int index)
    {
        try
        {
            return this.files[index];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void addListDataListener(ListDataListener l)
    {
        if (!this.listeners.contains(l))
            this.listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l)
    {
        try
        {
            this.listeners.remove(l);
        } catch (Exception e) {}
    }
}
