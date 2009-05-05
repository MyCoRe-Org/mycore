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

package org.mycore.frontend.workflow;

import org.jdom.Document;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * provides a wrapper for editor validation and MCRObject creation.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCREditorOutValidator extends MCREditorOutValidatorBase {
    /**
     * instantiate the validator with the editor input <code>jdom_in</code>.
     * 
     * <code>id</code> will be set as the MCRObjectID for the resulting object that can be fetched with <code>generateValidMyCoReObject()</code>
     * 
     * @param jdom_in
     *            editor input
     */
    public MCREditorOutValidator(Document jdom_in, MCRObjectID id) {
        super(jdom_in, id);
    }

}
