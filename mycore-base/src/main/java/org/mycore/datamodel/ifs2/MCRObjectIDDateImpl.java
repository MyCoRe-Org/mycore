/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.ifs2;

import java.util.Date;

import org.mycore.datamodel.common.MCRObjectIDDate;

public class MCRObjectIDDateImpl implements MCRObjectIDDate {

    protected Date lastModified;

    protected String id;

    protected MCRObjectIDDateImpl() {
        super();
    }

    public MCRObjectIDDateImpl(Date lastModified, String id) {
        super();
        this.lastModified = lastModified;
        this.id = id;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getId() {
        return id;
    }

    protected void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    protected void setId(String id) {
        this.id = id;
    }

}
