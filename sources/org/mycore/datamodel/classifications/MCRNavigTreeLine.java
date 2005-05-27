/**
 * Copyright (C) 2000 University of Essen, Germany
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
 * along with this program, normally in the file sources/gpl.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.classifications;

/**
 * Instances of MCRNavigTreeLine are the building blocks of a navigation tree.
 * Each MCRNavigTreeLine represents a category on a certain level of tree depth.
 * 
 * @author Frank Lützenkirchen
 * @author Anja Schaar
 *  
 */
class MCRNavigTreeLine {
    public MCRCategoryItem cat;

    public int level;

    public String status;

    public MCRNavigTreeLine(MCRCategoryItem cat, int level) {
        this.cat = cat;
        this.level = level;
        this.status = (cat.hasChildren() ? "T" : " ");

    }

}