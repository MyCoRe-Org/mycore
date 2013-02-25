/*
 * $Revision$ 
 * $Date$
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

package org.mycore.services.fieldquery.data2fields;

import java.io.BufferedReader;
import java.io.Reader;

import org.mycore.common.MCRNormalizer;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.plugins.TextFilterPluginManager;

public class MCRData2FieldsFileTextFilter implements MCRData2Fields {

    private static TextFilterPluginManager pm = TextFilterPluginManager.getInstance();

    private String text = "";

    private MCRFieldsSelector selector;

    public MCRData2FieldsFileTextFilter(String index, MCRFile file) {
        this.selector = new MCRFieldsSelectorFile(index, file, "fileTextContent");

        MCRFileContentType ct = file.getContentType();
        if (pm.isSupported(ct)) {

            try {
                Reader reader = pm.transform(ct, file.getContentAsInputStream());
                BufferedReader in = new BufferedReader(reader);

                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    sb.append(line).append(" ");
                }

                text = MCRNormalizer.normalizeString(sb.toString());
            } catch (Exception ignored) {
            }
        }
    }

    public void addFieldValues(MCRIndexEntry entry) {
        MCRRelevantFields fields = MCRRelevantFields.getFieldsFor(selector);
        for (MCRFieldDef field : fields.getFields())
            entry.addValue(new MCRFieldValue(field.getName(), text));
    }
}
