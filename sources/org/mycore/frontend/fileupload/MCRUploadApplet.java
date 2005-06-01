/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

/**
 * This applet displays a GUI to upload files and directories to the server.
 * 
 * @author Harald Richter
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRUploadApplet extends JApplet {
    protected String uploadId;

    protected String peerURL;

    protected String targetURL;

    protected JButton chooserButton;

    protected JTextField locationField;

    protected JButton locationButton;

    protected JFileChooser locationChooser;

    public void init() {
        uploadId = getParameter("uploadId");
        targetURL = getParameter("url");
        peerURL = getParameter("ServletsBase") + "UploadServlet";

        //TODO: Refactor parameters from web page
        //TODO: I18N of strings and messages
        //TODO: Refactor thread handling

        try {
            UIManager
                    .setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ignored) {
        }

        chooserButton = new JButton("ausw\u00e4hlen...");
        chooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleChooserButton();
            }
        });

        locationField = new JTextField(30);
        locationField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                locationButton.setEnabled(locationField.getText().length() > 0);
            }
        });

        locationButton = new JButton("\u00fcbertragen...");
        locationButton.setEnabled(false);
        locationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLocationButton();
            }
        });

        locationChooser = new JFileChooser();
        locationChooser
                .setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        locationChooser.setMultiSelectionEnabled(true);
        File c = new File("C:\\");
        if (c.exists())
            locationChooser.setCurrentDirectory(c);

        JPanel content = new JPanel();
        setContentPane(content);
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        content.setLayout(gbl);
        content.setBackground(Color.white);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel jlChoose = new JLabel(
                "Wählen Sie Dateien oder Verzeichnisse aus:");
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbl.setConstraints(jlChoose, gbc);
        content.add(jlChoose);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbl.setConstraints(chooserButton, gbc);
        content.add(chooserButton);

        JLabel jlInput = new JLabel("oder geben Sie einen absoluten Pfad ein:");
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbl.setConstraints(jlInput, gbc);
        content.add(jlInput);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbl.setConstraints(locationField, gbc);
        content.add(locationField);

        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbl.setConstraints(locationButton, gbc);
        content.add(locationButton);
    }

    protected void handleLocationButton() {
        File[] selectedFiles = new File[1];
        selectedFiles[0] = new File(locationField.getText());
        doUpload(selectedFiles);
    }

    protected void handleChooserButton() {
        int result = locationChooser.showDialog(this,
                "Datei(en) oder Verzeichnis(se) w\u00e4hlen");
        if (result == JFileChooser.APPROVE_OPTION)
            doUpload(locationChooser.getSelectedFiles());
    }

    protected void doUpload(final File[] selectedFiles) {
        chooserButton.setEnabled(false);
        locationButton.setEnabled(false);
        locationField.setEnabled(false);

        Thread th = new Thread() {
            public void run() {
                MCRUploadCommunicator comm = new MCRUploadCommunicator(peerURL,
                        uploadId, MCRUploadApplet.this);
                comm.uploadFiles(selectedFiles);
            }
        };
        th.start();
    }

    void returnToURL() {
        try {
            getAppletContext().showDocument(new URL(targetURL));
        } catch (MalformedURLException exc) {
            System.out.println("MALFORMED URL: " + targetURL);
        }
    }
}

