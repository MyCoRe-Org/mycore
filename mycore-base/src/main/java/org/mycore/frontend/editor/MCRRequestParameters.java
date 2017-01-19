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

package org.mycore.frontend.editor;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Wrapper class around an HTTP request that allows to treat both ordinary form
 * submission and multipart/form-data submissions with uploaded files in the
 * same way.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRRequestParameters {
    protected final static Logger logger = LogManager.getLogger(MCREditorServlet.class);

    private Hashtable<String, String[]> parameters = new Hashtable<String, String[]>();

    private Hashtable<String, FileItem> files = new Hashtable<String, FileItem>();
    
    private ArrayList<FileItem> filelist = new ArrayList<FileItem>();

    private static int threshold;

    private static long maxSize;

    private static String tmpPath;

    private static DiskFileItemFactory factory;

    static {
        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.Editor.";

        threshold = config.getInt(prefix + "FileUpload.MemoryThreshold", 1000000);
        maxSize = config.getLong(prefix + "FileUpload.MaxSize", 5000000);
        tmpPath = config.getString(prefix + "FileUpload.TempStoragePath");
        File tmp = new File(tmpPath);
        if (!tmp.isDirectory()) {
            tmp.mkdir();
        }
        factory = new DiskFileItemFactory();
        factory.setSizeThreshold(threshold);
        factory.setRepository(tmp);
    }

    public MCRRequestParameters(HttpServletRequest req) {
        if (ServletFileUpload.isMultipartContent(req)) {
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(maxSize);
            upload.setHeaderEncoding("UTF-8");

            List<FileItem> items = null;

            try {
                items = upload.parseRequest(req);
            } catch (FileUploadException ex) {
                String msg = "Error while parsing http multipart/form-data request from file upload webpage";
                throw new MCRException(msg, ex);
            }

            for (Object item1 : items) {
                FileItem item = (FileItem) item1;

                String name = item.getFieldName();
                String value = null;

                if (item.isFormField()) {
                    try {
                        value = item.getString("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        throw new MCRConfigurationException("UTF-8 is unsupported encoding !?", ex);
                    }
                } else {
                    value = item.getName();
                }

                if (!item.isFormField()) filelist.add(item);
                
                if (value != null && value.trim().length() > 0 && !files.containsKey(name)) {
                    if (!item.isFormField()) {
                        files.put(name, item);
                    }

                    String[] values = new String[1];
                    values[0] = value;
                    parameters.put(name, values);
                }
            }
        } else {
            for (Enumeration<String> e = req.getParameterNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                String[] values = req.getParameterValues(name);

                if (values != null && values.length > 0) {
                    parameters.put(name, values);
                }
            }
        }
    }

    public Enumeration<String> getParameterNames() {
        return parameters.keys();
    }

    public String getParameter(String name) {
        String[] values = getParameterValues(name);

        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    public FileItem getFileItem(String name) {
        return files.get(name);
    }
    
    public List<FileItem> getFileList() {
        return (List<FileItem>) filelist.clone();
    }
}
