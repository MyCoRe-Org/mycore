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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.fileupload.FileItem;
import org.jdom.Element;

/**
 * A single variable holding a value that was edited in a MyCoRe XML editor
 * form.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCREditorVariable implements Comparable {
    // Required attributes
    private String path;

    private String value;

    // Auto-generated attributes
    private String[] pathElements;

    private String attributeName;

    // Optional attributes
    private FileItem file = null;

    private String sortPattern = "";

    MCREditorVariable(String path, String value) {
        setPath(path);
        setValue(value);
    }

    public String getPath() {
        return path;
    }

    void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    Element asInputElement() {
        Element var = new Element("var");
        var.setAttribute("name", path);
        var.setAttribute("value", value);

        return var;
    }

    Element asRepeatElement() {
        Element var = new Element("repeat");
        var.setAttribute("path", path);
        var.setAttribute("value", value);

        return var;
    }

    void setFile(FileItem file) {
        this.file = file;
    }

    public FileItem getFile() {
        return file;
    }

    public boolean isAttribute() {
        return (attributeName != null);
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String[] getPathElements() {
        return pathElements;
    }

    void setPath(String path) {
        this.path = path;

        ArrayList elements = new ArrayList();

        for (StringTokenizer st = new StringTokenizer(path, "/"); st.hasMoreTokens();) {
            String token = st.nextToken();

            if ((!st.hasMoreTokens()) && (token.startsWith("@"))) {
                attributeName = token.substring(1);
            } else {
                elements.add(token);
            }
        }

        pathElements = (String[]) (elements.toArray(new String[0]));
    }

    void setSortNr(String sortNr) {
        StringTokenizer st = new StringTokenizer(sortNr, ".");
        StringBuffer sb = new StringBuffer();

        while (st.hasMoreTokens()) {
            int number = Integer.parseInt(st.nextToken());
            sb.append(sortFormatter.format(number));
        }

        this.sortPattern = sb.toString();
    }

    private static DecimalFormat sortFormatter = new DecimalFormat("0000");

    public int compareTo(Object o) {
        MCREditorVariable other = (MCREditorVariable) o;

        int length = Math.min(other.sortPattern.length(), this.sortPattern.length());
        String spo = other.sortPattern.substring(0, length);
        String spt = this.sortPattern.substring(0, length);

        return spt.compareTo(spo);
    }
}
