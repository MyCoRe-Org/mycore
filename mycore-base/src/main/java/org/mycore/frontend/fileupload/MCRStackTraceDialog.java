/**
 * 
 */
package org.mycore.frontend.fileupload;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author shermann
 */
public class MCRStackTraceDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * @param ex
     *            the Exception to display, must not be null. If Exception is
     *            null an IllegalArgumentException will be thrown
     * @param title
     *            the title of the dialog
     */
    public MCRStackTraceDialog(Exception ex, String title) {
        if (ex == null) {
            throw new IllegalArgumentException("The Exception parameter must not be null");
        }
        StringBuilder msg = new StringBuilder();
        msg.append(getExceptionStackTrace(ex) + "\n");

        if (ex instanceof MCRUploadException) {
            MCRUploadException uex = (MCRUploadException) ex;
            msg.append("Fehlermeldung des Servers:\n");
            msg.append(uex.getServerSideClassName()).append("\n");
            msg.append(uex.getMessage()).append("\n");
            msg.append(uex.getServerSideStackTrace()).append("\n");
        }

        // create the dialog
        this.setModal(true);
        this.setTitle(title);
        this.getContentPane().setLayout(new FlowLayout());
        this.setResizable(false);

        // create the text area with scrollbars
        final JTextArea textArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // configure the textarea
        textArea.setLineWrap(false);
        textArea.setRows(30);
        textArea.setColumns(60);
        textArea.setText(msg.toString());
        textArea.setEditable(true);
        textArea.setEnabled(true);

        // create the panel which will contain the scrollpane
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;

        panel.add(scrollPane, constraints);

        // create the panel containing the ok and copy to clipboard button
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton ok = new JButton("OK");

        JButton copy2Clipbrd = new JButton("Copy to Clipboard");
        // add the action handling for the copy2clipboarch button
        copy2Clipbrd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getText()), null);
            }
        });

        buttonPanel.add(copy2Clipbrd, constraints);
        buttonPanel.add(ok, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(buttonPanel, constraints);

        // add the action handling for the ok button
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // hide the dialog
                MCRStackTraceDialog.this.dispose();
            }
        });

        this.getContentPane().add(panel);
    }

    /**
     * @param ex
     * @return the Stacktrace information as String
     */
    private String getExceptionStackTrace(Exception ex) {
        if (ex == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Dialog#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean flag) {
        this.pack();
        Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();

        int x = (int) (screenRes.getWidth() / 2 - this.getSize().getWidth() / 2);
        int y = (int) (screenRes.getHeight() / 2 - this.getSize().getHeight() / 2);

        this.setLocation(x, y);
        super.setVisible(flag);
    }
}
