/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.urn;

/**
 * Implementations of this interface provide different strategies to generate a
 * NISS (namespace specific string) for a new URN. Each subnamespace
 * configuration can have its own instance. A NISS must be a unique ID within
 * the subnamespace.
 * 
 * MCR.URN.SubNamespace.[ConfigID].NISSBuilder=[Class], for example
 * MCR.URN.SubNamespace.Essen.NISSBuilder=org.mycore.services.urn.MCRNISSBuilderDateCounter
 * 
 * @author Frank Lützenkirchen
 */
public interface MCRNISSBuilder {
    /**
     * Initializes this instance of a MCRNISSBuilder. This method is only called
     * once for each instance before this builder is used.
     * 
     * @param configID
     *            the ID of a subnamespace configuration in mycore.properties
     */
    public void init(String configID);

    /**
     * Builds a new NISS. No MCRNISSBuilder object must generate the same NISS
     * twice, they must ensure the NISS is unique.
     */
    public String buildNISS();
}