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

package org.mycore.datamodel.metadata.validator;

import org.jdom2.Element;

/**
 * Validates the output of the Editor framework.
 * Implementors have to be thread safe.
 * @author Thomas Scheffler (yagee)
 * @version $Revision: 1 $ $Date: 08.05.2009 11:51:35 $
 */
public interface MCREditorMetadataValidator {

    /**
     * Gives hints to the editor form developer.
     * 
     * If an element is not valid it is removed from the JDOM document.
     * The returned String then can give a hint to the form developer, why it is removed.
     * This method may throw a {@link RuntimeException} in which case the whole validation process will fail.
     * @return 
     *  null, everything is OK
     *  validation error message
     */
    String checkDataSubTag(Element datasubtag);
}
