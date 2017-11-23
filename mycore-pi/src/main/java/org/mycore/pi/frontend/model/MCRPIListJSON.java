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

package org.mycore.pi.frontend.model;

import java.util.List;

import org.mycore.pi.MCRPIRegistrationInfo;

public class MCRPIListJSON {

    public MCRPIListJSON(String type, int from, int size, int count, List<MCRPIRegistrationInfo> list) {
        this.type = type;
        this.from = from;
        this.size = size;
        this.count = count;
        this.list = list;
    }

    public String type;

    public int from;

    public int size;

    public int count;

    public List<MCRPIRegistrationInfo> list;
}
