package org.openstatic;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openstatic.util.SoundFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.awt.Desktop;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;

public class AssetManagerPanel extends JPanel implements ActionListener, ListDataListener
{
    private JList<File> assetJList;
    private JPanel buttonPanel;
    private JButton selectAllButton;
    private JButton deleteButton;
    private JButton labelButton;
    private JButton addFileButton;
    private JButton extractFileButton;
    private FolderListModel folderListModel;
    private FileCellRenderer fileCellRenderer;
    private File root;
    private File lastDirectory;
    private long lastAssetClick;
    
    public AssetManagerPanel(File directory)
    {
        super(new BorderLayout());
        this.root = directory;
        this.lastDirectory = new File(".");
        this.fileCellRenderer = new FileCellRenderer();
        this.folderListModel = new FolderListModel(directory);
        this.folderListModel.addListDataListener(this);
        this.assetJList = new JList<File>(this.folderListModel);
        this.assetJList.setOpaque(true);
        this.assetJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.assetJList.setCellRenderer(this.fileCellRenderer);
        this.assetJList.setDropTarget(this.drop_targ);
        this.assetJList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                int index = AssetManagerPanel.this.assetJList.locationToIndex(e.getPoint());

                if (index != -1)
                {
                    File file = (File) AssetManagerPanel.this.folderListModel.getElementAt(index);
                    if (e.getButton() == MouseEvent.BUTTON1)
                    {
                        long cms = System.currentTimeMillis();
                        if (cms - AssetManagerPanel.this.lastAssetClick < 500 && AssetManagerPanel.this.lastAssetClick > 0)
                        {
                            try
                            {
                                String lowerName = file.getName().toLowerCase();
                                if (lowerName.endsWith(".wav"))
                                {
                                    final SoundFile sf = new SoundFile(file.getName());
                                    Thread t = new Thread(() -> {
                                        sf.playAndWait();
                                        sf.close();
                                    });
                                    t.start();
                                } else {
                                    Desktop.getDesktop().open(file);
                                }
                            } catch (Exception dex) {
                                dex.printStackTrace(System.err);
                            }
                        }
                        AssetManagerPanel.this.lastAssetClick = cms;
                    }
                }
            }
        });
        JScrollPane mappingScrollPane = new JScrollPane(this.assetJList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.buttonPanel = new JPanel();
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.Y_AXIS));
        try
        {
            ImageIcon diceIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/addfile32.png")));
            this.addFileButton = new JButton(diceIcon);
            this.addFileButton.setActionCommand("addfile");
            this.addFileButton.setToolTipText("Add File to this project's assets");
            this.addFileButton.addActionListener(this);
            this.buttonPanel.add(this.addFileButton);

            ImageIcon eportIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/extract32.png")));
            this.extractFileButton = new JButton(eportIcon);
            this.extractFileButton.setActionCommand("exportfile");
            this.extractFileButton.setToolTipText("Export Selected File");
            this.extractFileButton.addActionListener(this);
            this.buttonPanel.add(this.extractFileButton);

            ImageIcon labelIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/label32.png")));
            this.labelButton = new JButton(labelIcon);
            this.labelButton.addActionListener(this);
            this.labelButton.setActionCommand("rename_file");
            this.labelButton.setToolTipText("Rename Selected File");
            this.buttonPanel.add(this.labelButton);

            ImageIcon selectAllIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/selectall32.png")));
            this.selectAllButton = new JButton(selectAllIcon);
            this.selectAllButton.addActionListener(this);
            this.selectAllButton.setActionCommand("select_all");
            this.selectAllButton.setToolTipText("Select All Files");
            this.buttonPanel.add(this.selectAllButton);


            ImageIcon trashIcon = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("/midi-tools-res/trash32.png")));
            this.deleteButton = new JButton(trashIcon);
            this.deleteButton.addActionListener(this);
            this.deleteButton.setActionCommand("delete_selected");
            this.deleteButton.setToolTipText("Delete Selected File");
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
        } else if (e.getSource() == this.addFileButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(this.lastDirectory);
            fileChooser.setDialogTitle("Specify a file to import");   
            fileChooser.setMultiSelectionEnabled(true);
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                this.lastDirectory = fileChooser.getCurrentDirectory();
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for(int i = 0; i < selectedFiles.length; i++)
                {
                    this.addAsset(selectedFiles[i]);
                }
            }
        } else if (e.getSource() == this.labelButton) {
            Collection<File> selectedFiles = this.getSelectedFiles();
            if (selectedFiles.size() == 0)
            {
                
            } else {
                Iterator<File> fileIterator = selectedFiles.iterator();
                while (fileIterator.hasNext())
                {
                    File file = fileIterator.next();
                    String originalFilename = file.getName();
                    String s = (String) JOptionPane.showInputDialog(this,"Rename File\n" + originalFilename, originalFilename);
                    if (s != null)
                    {
                        file.renameTo(new File(file.getParent(), s));
                        //Trigger update to all rules using this file!
                        MidiTools.renamedFile(originalFilename, s);
                    }
                }
                AssetManagerPanel.this.repaint();
            }
        } else if (e.getSource() == this.extractFileButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(this.lastDirectory);
            fileChooser.setDialogTitle("Specify a location to export selected files");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int userSelection = fileChooser.showOpenDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION)
            {
                this.lastDirectory = fileChooser.getCurrentDirectory();
                Iterator<File> fIterator = this.getSelectedFiles().iterator();
                while(fIterator.hasNext())
                {
                    File file = fIterator.next();
                    this.exportAsset(file, fileChooser.getSelectedFile());
                }
            }
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

    public void exportAsset(File file, File directory)
    {
        final Path fileToExport = file.toPath();
        final Path targetPath = (new File(directory, file.getName())).toPath();
        (new Thread(() -> {
            try
            {
                Files.copy(fileToExport, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception eCopy) {
                eCopy.printStackTrace(System.err);
            }
        })).start();
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

    @Override
    public void intervalAdded(ListDataEvent e) {
        System.err.println("Interval Added!");
        JSONObject updatedAssets = new JSONObject();
        updatedAssets.put("images", new JSONArray(MidiTools.getImageAssets()));
        updatedAssets.put("sounds", new JSONArray(MidiTools.getSoundAssets()));
        MidiTools.instance.apiServer.broadcastCanvasJSONObject(updatedAssets);
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        
    }
}
