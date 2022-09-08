package org.openstatic;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;

public class AssetManagerPanel extends JPanel implements ActionListener
{
    private JList<File> assetJList;
    private JPanel buttonPanel;
    private JButton selectAllButton;
    private JButton deleteButton;
    private JButton addFileButton;
    private FolderListModel folderListModel;
    private FileCellRenderer fileCellRenderer;
    private File root;
    
    public AssetManagerPanel(File directory)
    {
        super(new BorderLayout());
        this.root = directory;
        this.fileCellRenderer = new FileCellRenderer();
        this.folderListModel = new FolderListModel(directory);
        this.assetJList = new JList<File>(this.folderListModel);
        this.assetJList.setOpaque(true);
        this.assetJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.assetJList.setCellRenderer(this.fileCellRenderer);
        this.assetJList.setDropTarget(this.drop_targ);
        JScrollPane mappingScrollPane = new JScrollPane(this.assetJList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon diceIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/addfile32.png")));
            this.addFileButton = new JButton(diceIcon);
            this.addFileButton.setActionCommand("addfile");
            this.addFileButton.setToolTipText("Add File to this project");
            this.addFileButton.addActionListener(this);
            this.buttonPanel.add(this.addFileButton);

            ImageIcon selectAllIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/selectall32.png")));
            this.selectAllButton = new JButton(selectAllIcon);
            this.selectAllButton.addActionListener(this);
            this.selectAllButton.setActionCommand("select_all");
            this.selectAllButton.setToolTipText("Select All");
            this.buttonPanel.add(this.selectAllButton);


            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_selected");
            this.deleteButton.setToolTipText("Delete Selected mappings");
            this.buttonPanel.add(this.deleteButton);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.add(mappingScrollPane, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.WEST);
    }

    private DropTarget drop_targ = new DropTarget()
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
                            AssetManagerPanel.this.addAsset(droppedFile);
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
                            AssetManagerPanel.this.addAsset(droppedFiles.get(fi));
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.deleteButton) {
            Collection<File> selectedFiles = this.getSelectedFiles();
            if (selectedFiles.size() == 0)
            {
                
            } else {
                Iterator<File> fIterator = selectedFiles.iterator();
                while (fIterator.hasNext())
                {
                    File file = fIterator.next();
                    file.delete();
                }
            }
        } else if (e.getSource() == this.selectAllButton) {
            int rs = folderListModel.getSize();
            int[] indices = new int[rs];
            for(int i = 0; i < rs; i++)
                indices[i] = i;
            this.assetJList.setSelectedIndices(indices);
        }
        
    }

    public File addAsset(File file)
    {
        final Path pathToImport = file.toPath();
        final Path targetPath = (new File(this.root, file.getName())).toPath();
        (new Thread(() -> {
            try
            {
                Files.copy(pathToImport, targetPath, StandardCopyOption.REPLACE_EXISTING);
                AssetManagerPanel.this.refresh();
            } catch (Exception eCopy) {
                eCopy.printStackTrace(System.err);
            }
        })).start();
        return targetPath.toFile();
    }

    public void refresh()
    {
        this.folderListModel.refresh();
    }

    public Collection<File> getSelectedFiles()
    {
        return this.assetJList.getSelectedValuesList();
    }

    public Collection<File> getAllAssets()
    {
        return new ArrayList<File>(Arrays.asList(this.folderListModel.files));
    }
}
