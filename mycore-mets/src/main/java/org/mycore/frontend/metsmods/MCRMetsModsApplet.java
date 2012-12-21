/*
 * 
 * $Revision: 1.12 $ $Date: 2009/03/23 10:03:47 $
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

package org.mycore.frontend.metsmods;

import java.applet.AppletContext;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.frontend.fileupload.MCRMetsModsCommunicator;

/**
 * This applet allows to modify a mets file.
 * 
 * @author Stefan Freitag (sasf)
 * 
 * @version $Revision: 1.12 $ $Date: 2009/03/23 10:03:47 $
 */

public class MCRMetsModsApplet extends JApplet {

    private static final long serialVersionUID = 6331438473047147435L;

    MCRMetsModsPictureContainer mmpc;
    JButton save_btn = new JButton("Save");
    JButton cancel_btn = new JButton("Cancel");

    JScrollPane scroller = new JScrollPane();

    ArrayList<MCRMetsModsPicture> pictures;

    protected String uploadId;
    protected String peerURL;
    protected String targetURL;

    public void init() {
        JPanel panel = new JPanel();
        setContentPane(panel);

        uploadId = this.getParameter("uploadId");
        String httpSession = this.getParameter("httpSession");
        peerURL = addSessionInfo(this.getParameter("ServletsBase") + "MCRUploadServlet", httpSession);
        final String mets_url = this.getParameter("metsfile");
        if (mets_url == null) {
            JOptionPane.showMessageDialog(this, "Sorry, there was no valid mets.xml file found!");
            return;
        }
        targetURL = this.getParameter("url");

        if (this.getParameter("language").compareTo("id") == 0)
            this.setLocale(new Locale("in"));
        else
            this.setLocale(new Locale(this.getParameter("language")));

        panel.setLayout(null);

        save_btn.setBounds(10, 370, 120, 20);
        save_btn.setFont(new Font("Arial", Font.PLAIN, 12));
        cancel_btn.setBounds(140, 370, 120, 20);
        cancel_btn.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel jp_t = new JPanel();
        jp_t.setBackground(java.awt.Color.orange);
        jp_t.setLayout(null);
        JLabel title_label1 = new JLabel(translateI18N("MCRMetsmodsApplet.title.filename"));
        JLabel title_label2 = new JLabel(translateI18N("MCRMetsmodsApplet.title.index"));
        JLabel title_label3 = new JLabel(translateI18N("MCRMetsmodsApplet.title.orderlabel"));
        title_label1.setBounds(10, 2, 100, 20);
        title_label2.setBounds(120, 2, 100, 20);
        title_label3.setBounds(230, 2, 100, 20);

        jp_t.add(title_label1);
        jp_t.add(title_label2);
        jp_t.add(title_label3);

        ((JPanel) this.getGlassPane()).setLayout(null);
        jp_t.setBounds(15, 15, this.getWidth() - 45, 20);
        ((JPanel) this.getGlassPane()).add(jp_t);
        this.getGlassPane().setVisible(true);

        pictures = openMETS(mets_url);

        mmpc = new MCRMetsModsPictureContainer();

        scroller = new JScrollPane(mmpc);
        scroller.setBounds(10, 10, this.getWidth() - 20, this.getHeight() - 50);

        int k = 0;
        for (MCRMetsModsPicture picture1 : pictures)
            if (((MCRMetsModsPicture) picture1).getPicture().length() * 10 > k)
                k = ((MCRMetsModsPicture) picture1).getPicture().length() * 10;

        title_label2.setBounds(k + 10, 2, 100, 20); // adapting text length of filename for position of label
        title_label3.setBounds(k + 130, 2, 100, 20); // adapting text length of filename for position of label

        for (MCRMetsModsPicture picture : pictures) {
            MCRMetsModsPicture mmpic = (MCRMetsModsPicture) picture;
            MCRMetsModsPictureItem mmpi = new MCRMetsModsPictureItem(mmpic, k);
            mmpc.addItem(mmpi);
        }

        mmpc.setPreferredSize(mmpc.getSize());

        save_btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pictures = new ArrayList<MCRMetsModsPicture>();
                // pictures = processChangedList();
                for (int i = 0; i < mmpc.itemlist.size(); i++)
                    pictures.add(mmpc.itemlist.get(i).getMetsModsPicture());

                final String mets = closeMETS(mets_url, pictures);

                if (mets == null)
                    return;

                Thread th = new Thread() {
                    public void run() {
                        MCRMetsModsCommunicator comm = new MCRMetsModsCommunicator(peerURL, uploadId);
                        comm.uploadMets(mets);

                        returnToURL();
                    }
                };
                th.start();

            }
        });

        cancel_btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                returnToURL();
            }
        });

        save_btn.setText(translateI18N("MCRMetsmodsApplet.save"));
        cancel_btn.setText(translateI18N("MCRMetsmodsApplet.cancel"));

        panel.add(scroller);
        panel.add(save_btn);
        panel.add(cancel_btn);

    }

    private void returnToURL() {
        try {
            URL url = new URL(targetURL);
            AppletContext context = getAppletContext();
            context.showDocument(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String closeMETS(String mets_url, ArrayList<MCRMetsModsPicture> picturelist) {
        try {
            URL url = new URL(mets_url);
            InputStream di = url.openConnection().getInputStream();
            Document doc = new SAXBuilder().build(di);
            di.close();
            String st = MCRMetsModsUtil.getMetsFile(picturelist, doc);
            return st;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;

    }

    private ArrayList<MCRMetsModsPicture> openMETS(String mets_url) {
        try {
            URL url = new URL(mets_url);
            InputStream di = url.openConnection().getInputStream();
            Document doc = new SAXBuilder().build(di);
            di.close();
            ArrayList<MCRMetsModsPicture> piclist = MCRMetsModsUtil.getFileList(doc);
            return piclist;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
        StringBuilder sb = new StringBuilder(path);
        sb.append(";jsessionid=");
        sb.append(sessionId);
        sb.append(query);
        return sb.toString();
    }

    private String translateI18N(String label) {
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
}
