/*
 * 
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * This class implements a Dialog that shows messages and a progress bar while
 * creating, updating or deleting the derivates of a document. This class is a
 * singleton, there is only one instance at a time that you get with
 * getDialog(). The MCRUploadProgressMonitor provides methods to set the next
 * message that should be displayed and to start, update and finish the progress
 * bar.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUploadProgressMonitor extends JDialog {
    protected MCRUploadApplet applet;

    protected boolean canceled; // if true, upload is canceled

    protected boolean finished; // if true, upload process is finished

    protected String filename;

    protected JLabel lbFilename; // Current filename without path

    protected long sizeFile; // Size of file currently uploaded

    protected long bytesFile; // Number of bytes uploaded for current file

    protected String fileTrans;

    protected JLabel lbBytesFile; // Bytes current / total for this file

    protected int fileProg;

    protected JProgressBar pbFile; // Progress bar for current file

    protected int numFiles; // Total number of files uploading

    protected int fileCount; // Number of file currently uploading

    protected JLabel lbNumFiles; // Number of files transferred / total

    protected long sizeTotal; // Total size of all files uploading

    protected long bytesTotal; // Total number of bytes uploaded so far

    protected JLabel lbBytesTotal; // Bytes transferred / total of all files

    protected JProgressBar pbTotal; // Progress bar for total upload

    protected long startTime; // Time the upload startet

    protected long endTime; // Time the upload finished

    protected long lastUpdate; // Time the last data update was drawn

    protected JLabel lbThroughput; // Average number of bytes per second

    protected JLabel lbTime; // Time elapsed / estimated time remaining

    protected JButton button; // OK / Cancel button to close window

    /**
     * Creates a new upload progress monitor and makes it visible.
     * 
     * @param numFiles
     *            total number of files to be uploaded
     * @param sizeTotal
     *            total byte size of all files
     * @param applet
     *            the UploadApplet this monitor belongs to
     */
    protected MCRUploadProgressMonitor(int numFiles, long sizeTotal, MCRUploadApplet applet) {
        super((Frame) null, "Dateien werden hochgeladen", true);

        int width = 600;
        int height = 300;

        this.applet = applet;
        this.canceled = false;
        this.finished = false;
        this.filename = "";
        this.lbFilename = new JLabel(" ");
        this.sizeFile = 0;
        this.bytesFile = 0;
        this.lbBytesFile = new JLabel(" ");
        this.pbFile = new JProgressBar(0, 1000);
        this.numFiles = numFiles;
        this.fileCount = 0;
        this.lbNumFiles = new JLabel(" ");
        this.sizeTotal = sizeTotal;
        this.bytesTotal = 0;
        this.lbBytesTotal = new JLabel(" ");
        this.pbTotal = new JProgressBar(0, 1000);
        this.startTime = System.currentTimeMillis();
        this.lbThroughput = new JLabel(" ");
        this.lbTime = new JLabel(" ");
        this.button = new JButton("Abbrechen");

        button.setEnabled(true);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (finished)
                    MCRUploadProgressMonitor.this.close();
                else
                    MCRUploadProgressMonitor.this.cancel();
            }
        });

        pbFile.setStringPainted(true);
        pbTotal.setStringPainted(true);

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ignored) {
        }

        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setContentPane(content);

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        content.setLayout(gbl);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, (width / 2) - 10, 0, (width / 2) - 10);

        JLabel dummy = new JLabel("");
        gbl.setConstraints(dummy, gbc);
        content.add(dummy);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(2, 0, 2, 0);

        gbc.gridy = 1;
        gbl.setConstraints(lbNumFiles, gbc);
        content.add(lbNumFiles);

        gbc.gridy = 2;
        gbl.setConstraints(lbBytesTotal, gbc);
        content.add(lbBytesTotal);

        gbc.gridy = 4;
        gbl.setConstraints(lbFilename, gbc);
        content.add(lbFilename);

        gbc.gridy = 5;
        gbl.setConstraints(lbBytesFile, gbc);
        content.add(lbBytesFile);

        gbc.gridy = 7;
        gbl.setConstraints(lbThroughput, gbc);
        content.add(lbThroughput);

        gbc.gridy = 8;
        gbl.setConstraints(lbTime, gbc);
        content.add(lbTime);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 20, 0);

        gbc.gridy = 3;
        gbl.setConstraints(pbTotal, gbc);
        content.add(pbTotal);

        gbc.gridy = 6;
        gbl.setConstraints(pbFile, gbc);
        content.add(pbFile);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 0, 0, 0);

        gbc.gridy = 9;
        gbl.setConstraints(button, gbc);
        content.add(button);

        setSize(width, height);

        Dimension size = this.getSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - size.width) / 2, (screen.height - size.height) / 2);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        Runnable starter = new Runnable() {
            public void run() {
                MCRUploadProgressMonitor.this.setVisible(true);
                MCRUploadProgressMonitor.this.requestFocus();
            }
        };

        // This thread will update display even if upload gets very slow
        Thread updater = new Thread(new Runnable() {
            public void run() {

                while (MCRUploadProgressMonitor.this.lastUpdate == 0) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                    }
                }

                while (!(MCRUploadProgressMonitor.this.finished || MCRUploadProgressMonitor.this.canceled)) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                    }

                    if (MCRUploadProgressMonitor.this.finished || MCRUploadProgressMonitor.this.canceled)
                        return;

                    long now = System.currentTimeMillis();
                    if (now - MCRUploadProgressMonitor.this.lastUpdate > 900)
                        MCRUploadProgressMonitor.this.update();
                }
            }
        });

        SwingUtilities.invokeLater(starter);
        updater.start();
    }

    protected void cancel() {
        canceled = true;
        endTime = System.currentTimeMillis();
        end();
    }

    protected void close() {
        button.setEnabled(false);
        setVisible(false);
        dispose();

        if (applet != null)
            applet.returnToURL();
    }

    protected DecimalFormat df = new DecimalFormat("00");

    protected String formatTime(int sec) {
        if (sec < 60) {
            return sec + " Sek.";
        }

        int min = sec / 60;
        sec = sec - (min * 60);

        if (min < 60) {
            return min + ":" + df.format(sec) + " Min.";
        }

        int hh = min / 60;
        min = min - (hh * 60);

        return hh + ":" + df.format(min) + ":" + df.format(sec) + " Std.";
    }

    protected String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " Byte";
        }

        long kb = bytes / 1024L;
        bytes = bytes - (kb * 1024L);

        long nk = Math.round((bytes / 1024d) * 100d);

        if (kb < 1024) {
            return kb + "," + df.format(nk) + " KB";
        }

        long mb = kb / 1024L;
        kb = kb - (mb * 1024L);
        nk = Math.round((kb / 1024d) * 100d);

        return mb + "," + df.format(nk) + " MB";
    }

    protected void update() {
        final int permilleFile;

        if (sizeFile > 0) {
            permilleFile = (int) (((double) bytesFile / (double) sizeFile) * 1000);
        } else {
            permilleFile = 0;
        }

        final int permilleTotal;

        if (sizeTotal > 0) {
            permilleTotal = (int) (((double) bytesTotal / (double) sizeTotal) * 1000);
        } else {
            permilleTotal = 0;
        }

        final String sFile = formatSize(bytesFile) + " von " + formatSize(sizeFile) + " übertragen";

        lastUpdate = System.currentTimeMillis();
        long now = (endTime > 0 ? endTime : lastUpdate);
        final int sec = Math.max((int) (now - startTime) / 1000, 1);

        long bytesPerMilli = 1;
        if (now > startTime)
            bytesPerMilli = Math.max(1, bytesTotal / (now - startTime));
        int rest = (int) ((sizeTotal - bytesTotal) / bytesPerMilli / 1000);

        int throughput = Math.round(bytesTotal / sec);

        final String sTotal = formatSize(bytesTotal) + " von " + formatSize(sizeTotal) + " insgesamt übertragen";
        final String sThrough = formatSize(throughput) + " pro Sekunde";
        final String sName;

        if (canceled) {
            sName = "ABGEBROCHEN: Datei " + filename;
        } else {
            sName = "Datei " + filename;
        }

        final String sCounter;
        final String sTime;

        if (canceled) {
            sCounter = "ABGEBROCHEN: " + fileCount + " von " + numFiles + " Dateien übertragen";
            sTime = "Übertragung abgebrochen, Gesamtdauer " + formatTime(sec);
        } else if (bytesTotal < sizeTotal) {
            sCounter = "Übertrage Datei " + fileCount + " von " + numFiles;
            sTime = "Dauer bisher " + formatTime(sec) + " / geschätzt noch " + formatTime(rest);
        } else {
            sCounter = "Alle " + numFiles + " Dateien übertragen";
            sTime = "Übertragung beendet, Gesamtdauer " + formatTime(sec);
        }

        Runnable updater = new Runnable() {
            public void run() {
                lbFilename.setText(sName);
                pbFile.setValue(permilleFile);
                lbBytesFile.setText(sFile);
                lbNumFiles.setText(sCounter);
                lbBytesTotal.setText(sTotal);
                pbTotal.setValue(permilleTotal);
                lbThroughput.setText(sThrough);
                lbTime.setText(sTime);

                MCRUploadProgressMonitor.this.repaint();
            }
        };

        SwingUtilities.invokeLater(updater);
    }

    /**
     * Informs the monitor that the next file is being uploaded.
     * 
     * @param name
     *            the name of the file without path
     * @param size
     *            the byte size of the file
     */
    public void startFile(String name, long size) {
        filename = name;
        sizeFile = size;
        bytesFile = 0;
        fileCount++;
        update();
    }

    /**
     * Informs the monitor that a number of bytes was read for the current file.
     * This is the single number of bytes read in this iteration, not the total
     * number of files read from this file.
     * 
     * @param bytesToAdd
     *            the single number of bytes transferred in this step
     */
    public void progressFile(long bytesToAdd) {
        bytesFile += bytesToAdd;
        bytesTotal += bytesToAdd;
        update();
    }

    /**
     * Informs the monitor that the upload of the current file is finished.
     */
    public void endFile() {
        bytesFile = sizeFile;
        update();
    }

    /**
     * Informs the monitor that uploading of all files is finished.
     */
    public void finish() {
        bytesFile = sizeFile;
        bytesTotal = sizeTotal;
        fileCount = numFiles;
        finished = true;
        endTime = System.currentTimeMillis();
        end();
    }

    /**
     * Informs the monitor that uploading is canceled because some error
     * occured.
     */
    public void cancel(Exception ex) {
        canceled = true;
        finished = true;
        endTime = System.currentTimeMillis();
        MCRUploadProgressMonitor.reportException(ex);
        end();
    }

    /**
     * Shows a message dialog that displays an exception that occured.
     * 
     * @param ex
     *            the Exception to be shown
     */
    public static void reportException(Exception ex) {
        String title = "Fehler bei der Übertragung";
        StringBuffer msg = new StringBuffer();
        msg.append(title).append(":\n");
        msg.append(ex.getClass().getName()).append("\n");
        msg.append(ex.getLocalizedMessage()).append("\n");

        if (ex instanceof MCRUploadException) {
            MCRUploadException uex = (MCRUploadException) ex;
            msg.append("Fehlermeldung des Servers:\n");
            msg.append(uex.getServerSideClassName()).append("\n");
            msg.append(uex.getMessage()).append("\n");
            msg.append(uex.getServerSideStackTrace()).append("\n");
        }

        JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    protected void end() {
        finished = true;
        button.setText("Schliessen");
        button.setEnabled(true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                MCRUploadProgressMonitor.this.close();
            }
        });
        update();
    }

    public boolean isCanceled() {
        return canceled;
    }

    /**
     * A small test that reads all files in a given directory.
     * 
     * @param args
     *            first arg is the path of the directory to read from
     * @throws Exception
     *             if anything goes wrong
     */
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File[] files = dir.listFiles();

        int numFiles = files.length;
        long sizeTotal = 0;

        for (int i = 0; i < numFiles; i++)
            sizeTotal += files[i].length();

        MCRUploadProgressMonitor upm = new MCRUploadProgressMonitor(numFiles, sizeTotal, null);

        for (int i = 0; i < numFiles; i++) {
            if (upm.isCanceled())
                break;
            upm.startFile(files[i].getName(), files[i].length());

            FileInputStream fin = new FileInputStream(files[i]);
            byte[] buffer = new byte[65536];
            long num = 0;

            if (upm.isCanceled())
                break;
            while ((num = fin.read(buffer, 0, buffer.length)) != -1) {
                // Simulate a read error and the following cancel() invocation
                // if( i == 2 ) { upm.cancel( new java.io.IOException( "Simulierter Lesefehler" ) ); return; }

                if (upm.isCanceled())
                    break;
                upm.progressFile(num);
                Thread.sleep(300); // Simulate network transfer time
                if (upm.isCanceled())
                    break;
            }

            if (!upm.isCanceled())
                upm.endFile();
        }

        if (!upm.isCanceled())
            upm.finish();
    }
}
