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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.services.fieldquery.MCRFieldValue;

public class MCRIndexEntry {

    private static final Logger LOGGER = Logger.getLogger(MCRData2FieldsXML.class);

    private List<MCRFieldValue> values = new ArrayList<MCRFieldValue>();

    private String entryID;

    private String returnID;

    public List<MCRFieldValue> getFieldValues() {
        return values;
    }

    public void addValue(MCRFieldValue value) {
        LOGGER.debug("MCRData2Fields " + value.getFieldName() + " := " + value.getValue());
        values.add(value);
    }

    public String getEntryID() {
        return entryID;
    }

    public String getReturnID() {
        return (returnID == null ? entryID : returnID);
    }

    public void setEntryID(String entryID) {
        this.entryID = entryID;
    }

    public void setReturnID(String returnID) {
        this.returnID = returnID;
    }
}
