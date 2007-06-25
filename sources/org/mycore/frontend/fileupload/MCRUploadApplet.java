/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.fileupload;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * This applet displays a GUI to upload files and directories to the server.
 * 
 * @author Harald Richter
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
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
        String httpSession = getParameter("httpSession");
        peerURL = addSessionInfo(getParameter("ServletsBase") + "MCRUploadServlet", httpSession);

        // If true, applet allows to select multiple files and directories.
        // If false, only a single file can be selected. Default is true.
        boolean selectMultiple = !"false".equals(getParameter("selectMultiple"));

        // List of file extensions to accept separated by comma or spaces,
        // default is to accept all files
        final String acceptFileTypes = getParameter("acceptFileTypes");

        System.out.println("Will connect with: " + peerURL);
        Color bg = getColorParameter("background-color");
        System.out.println("Background color: " + bg.toString());
        setBackground(bg);
        setLocale();

        chooserButton = new JButton(translateI18N("MCRUploadApplet.select"));
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

        locationButton = new JButton(translateI18N("MCRUploadApplet.submit"));
        locationButton.setEnabled(false);
        locationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLocationButton();
            }
        });

        locationChooser = new JFileChooser();
        locationChooser.setFileSelectionMode(selectMultiple ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY);
        locationChooser.setMultiSelectionEnabled(selectMultiple);

        // Add a file filter to accept only files with certain extensions
        if ((acceptFileTypes != null) && (acceptFileTypes.trim().length() > 0) && (!"*".equals(acceptFileTypes.trim()))) {
            locationChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            locationChooser.setFileFilter(new FileFilter() {
                Set accepted = new HashSet();

                public boolean accept(File f) {
                    if (accepted.isEmpty()) {
                        StringTokenizer st = new StringTokenizer(acceptFileTypes, " ,;");
                        while (st.hasMoreTokens())
                            accepted.add(st.nextToken());
                    }
                    String[] ext = f.getName().split("\\.");
                    return accepted.contains(ext[ext.length - 1]);
                }

                public String getDescription() {
                    return acceptFileTypes;
                }
            });
        }

        File c = new File("C:\\");

        if (c.exists()) {
            locationChooser.setCurrentDirectory(c);
        }

        JPanel content = new JPanel();
        setContentPane(content);

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        content.setLayout(gbl);
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.setBackground(bg);

        JLabel jlChoose = new JLabel(translateI18N("MCRUploadApplet.dirsel"));
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

        JLabel jlInput = new JLabel(translateI18N("MCRUploadApplet.altsel"));
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
        int result = locationChooser.showDialog(this, translateI18N("MCRUploadApplet.choose"));

        if (result == JFileChooser.APPROVE_OPTION) {
            doUpload(locationChooser.getSelectedFiles());
        }
    }

    protected void doUpload(final File[] selectedFiles) {
        chooserButton.setEnabled(false);
        locationButton.setEnabled(false);
        locationField.setEnabled(false);

        Thread th = new Thread() {
            public void run() {
                MCRUploadCommunicator comm = new MCRUploadCommunicator(peerURL, uploadId, MCRUploadApplet.this);
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

    /**
     * provides translation for the given label (property key).
     * 
     * Use the current locale that is needed for translation.
     * 
     * @param label
     * @return translated String
     */
    private final String translateI18N(String label) {
        String result;
        Locale currentLocale = getLocale();
        try {
            ResourceBundle message = ResourceBundle.getBundle("messages", currentLocale);
            result = message.getString(label);
        } catch (java.util.MissingResourceException mre) {
            result = "???" + label + "???";
            System.err.println(mre.getMessage());
        }

        return result;
    }

    private final Color getColorParameter(String name) {
        String value = getParameter(name);
        if (value == null) {
            System.err.println("Did not find color parameter: " + name);
            return Color.WHITE; // as a default if parameter is missing
        }
        int rgbValue;
        try {
            rgbValue = Integer.parseInt(value.substring(1), 16);
        } catch (NumberFormatException e) {
            System.err.println("Color parameter " + name + " has no valid value: " + value);
            // in this case return red
            return new Color((float) 1.0, (float) 0.0, (float) 0.0);
        }
        return new Color(rgbValue);
    }

    private String addSessionInfo(String url, String sessionId) {

        if ((url == null) || (sessionId == null)) {
            return url;
        }
        String path = url;
        String query = "";
        int queryPos = url.indexOf('?');
        if (queryPos >= 0) {
            path = url.substring(0, queryPos);
            query = url.substring(queryPos);
        }
        StringBuffer sb = new StringBuffer(path);
        sb.append(";jsessionid=");
        sb.append(sessionId);
        sb.append(query);
        return sb.toString();
    }

    private void setLocale() {
        String value = getParameter("locale");
        if (value != null) {
            setLocale(new Locale(value));
        }
    }

}
