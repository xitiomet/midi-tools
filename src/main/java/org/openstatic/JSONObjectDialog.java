package org.openstatic;

import org.openstatic.midi.*;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.DefaultListModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;
import javax.swing.JOptionPane;
import javax.swing.text.NumberFormatter;


import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

import java.text.NumberFormat;

import java.io.File;

import org.json.*;

public class JSONObjectDialog extends JDialog implements ActionListener
{
    private JSONObject jsonObject;
    private JSONObject editedJSONObject;
    
    private JPanel formPanel;
    
    private JButton saveButton;
    private JButton deleteButton;
    
    private int nextField;

    public void actionPerformed(ActionEvent e)
    {
        
        if (e.getSource() == this.deleteButton)
        {
            this.dispose();
        }
        
        if (e.getSource() == this.saveButton)
        {
            for(Iterator<String> keyIterator = this.editedJSONObject.keys(); keyIterator.hasNext();)
            {
                String key = keyIterator.next();
                this.jsonObject.put(key, this.editedJSONObject.get(key));
            }
            System.err.println(this.jsonObject.toString());
            this.dispose();
        }
    }
    
    public void createStringInput(final String field, String value)
    {
        JTextField stringInput = new JTextField(value);
        stringInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                //System.err.println("Remove update");
                JSONObjectDialog.this.editedJSONObject.put(field, stringInput.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) 
            {
                //System.err.println("Insert update");
                JSONObjectDialog.this.editedJSONObject.put(field, stringInput.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent arg0)
            {
                //System.err.println("Change update");
                JSONObjectDialog.this.editedJSONObject.put(field, stringInput.getText());
            }
        });
        formPanel.add(new JLabel(field, SwingConstants.LEFT), gbc(1, .4d));
        formPanel.add(stringInput, gbc(2, .6d));
        this.nextField++;
    }
    
    public void createIntegerInput(String field, int value)
    {
        NumberFormat longFormat = NumberFormat.getIntegerInstance();

        NumberFormatter numberFormatter = new NumberFormatter(longFormat);
        numberFormatter.setValueClass(Integer.class); //optional, ensures you will always get a long value
        numberFormatter.setAllowsInvalid(false); //this is the key!!
        
        JFormattedTextField integerInput = new JFormattedTextField(numberFormatter);
        integerInput.setText(String.valueOf(value));
        integerInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                //System.err.println("Remove update");
                try
                {
                    JSONObjectDialog.this.editedJSONObject.put(field, ((Integer) integerInput.getValue()).intValue());
                } catch (Exception xe) {
                    
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) 
            {
                //System.err.println("Insert update");
                try
                {
                    JSONObjectDialog.this.editedJSONObject.put(field, ((Integer) integerInput.getValue()).intValue());
                } catch (Exception xe) {
                    
                }
            }

            @Override
            public void changedUpdate(DocumentEvent arg0)
            {
                //System.err.println("Change update");
                try
                {
                    JSONObjectDialog.this.editedJSONObject.put(field, ((Integer) integerInput.getValue()).intValue());
                } catch (Exception xe) {
                    
                }
            }
        });
        formPanel.add(new JLabel(field, SwingConstants.LEFT), gbc(1, .4d));
        formPanel.add(integerInput, gbc(2, .6d));
        this.nextField++;
    }
    
    public void createBooleanInput(final String field, boolean value)
    {
        Vector<String> booleanInputs = new Vector<String>();
        booleanInputs.add("true");
        booleanInputs.add("false");
        final JComboBox booleanInput = new JComboBox();
        booleanInput.setModel(new DefaultComboBoxModel<String>( ((Vector<String>)booleanInputs) ));
        if (value)
        {
            booleanInput.setSelectedIndex(0);
        } else {
            booleanInput.setSelectedIndex(1);
        }
        booleanInput.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (booleanInput.getSelectedIndex() == 0)
                    {
                        JSONObjectDialog.this.editedJSONObject.put(field, true);
                    } else {
                        JSONObjectDialog.this.editedJSONObject.put(field, false);
                    }
                }
            });
        formPanel.add(new JLabel(field, SwingConstants.LEFT), gbc(1, .4d));
        formPanel.add(booleanInput, gbc(2, .6d));
        this.nextField++;
    }
    
    public JSONObjectDialog(String title, JSONObject jsonObject)
    {
        super(MidiTools.instance, title, true);
        this.nextField = 1;
        this.jsonObject = jsonObject;
        this.editedJSONObject = new JSONObject(jsonObject.toString());
        this.setLayout(new BorderLayout());
        this.formPanel = new JPanel(new GridBagLayout());

        for(Iterator<String> keyIterator = this.jsonObject.keys(); keyIterator.hasNext();)
        {
            String key = keyIterator.next();
            try
            {
                boolean value = this.jsonObject.getBoolean(key);
                createBooleanInput(key, value);
            } catch (Exception e1) {
                //e1.printStackTrace(System.err);
                try
                {
                    int value = this.jsonObject.getInt(key);
                    createIntegerInput(key, value);
                } catch (Exception e2) {
                    //e2.printStackTrace(System.err);
                    try
                    {
                        String value = this.jsonObject.getString(key);
                        createStringInput(key, value);
                    } catch (Exception e3) {
                        //e3.printStackTrace(System.err);
                    }
                }
            }
            
        }
        
        this.saveButton = new JButton("Save");
        this.deleteButton = new JButton("Cancel");
        this.saveButton.addActionListener(this);
        this.deleteButton.addActionListener(this);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);
        
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.add(formPanel, BorderLayout.PAGE_START);
        this.add(buttonPanel, BorderLayout.PAGE_END);
        centerWindow();
    }
    
    private GridBagConstraints gbc(int x, double weightx)
    {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = weightx;
        g.gridx = x;
        g.gridy = this.nextField;
        g.ipady = 2;
        g.ipadx = 5;
        return g;
    }    

    public JPanel labelComponent(String label, Component c)
    {
        JPanel x = new JPanel(new GridLayout(1,2));
        x.add(new JLabel(label, SwingConstants.CENTER));
        x.add(c);
        return x;
    }

    public void centerWindow()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        final float WIDTH = screenSize.width;
        final float HEIGHT = screenSize.height;
        int wWidth = 400;
        int wHeight = 250;
        int x = (int) ((WIDTH/2f) - ( ((float)wWidth) /2f ));
        int y = (int) ((HEIGHT/2f) - ( ((float)wHeight) /2f ));
        this.setBounds(x, y, wWidth, wHeight);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);
    }
}
