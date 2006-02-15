/*
 * $RCSfile$
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

package org.mycore.frontend.servlets;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jdom.Document;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorOutValidator;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML for metadata object and derivate objects.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
abstract public class MCRCheckBase extends MCRServlet {
    protected static Logger LOGGER = Logger.getLogger(MCRCheckBase.class);

    // The file separator
    String NL = System.getProperty("file.separator");

    // The Access Manager
    private static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    protected List errorlog;

    /**
     * The method is a dummy or works with the data and return an URL with the
     * next working step.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @return the next URL as String
     * @throws MCRActiveLinkException
     *             if links preventing the next step in the workflow
     */
    abstract public String getNextURL(MCRObjectID ID) throws MCRActiveLinkException;

    /**
     * The method send a message to the mail address for the MCRObjectType.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     */
    abstract public void sendMail(MCRObjectID ID);

    /**
     * A method to handle IO errors.
     * 
     * @param job
     *            the MCRServletJob
     */
    protected void errorHandlerIO(MCRServletJob job) throws Exception {
        String pagedir = CONFIG.getString("MCR.editor_page_dir", "");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + pagedir + "editor_error_store.xml"));
    }

    /**
     * provides a wrappe for editor validation and MCRObject creation.
     * 
     * For a new MetaDataType, e.g. MCRMetaFooBaar, create a method
     * 
     * <pre>
     *   boolean checkMCRMetaFooBar(Element)
     * </pre>
     * 
     * use the following methods in that method to do common tasks on element
     * validation
     * <ul>
     * <li>checkMetaObject(Element,Class)</li>
     * <li>checkMetaObjectWithLang(Element,Class)</li>
     * <li>checkMetaObjectWithLangNotEmpty(Element,Class)</li>
     * <li>checkMetaObjectWithLinks(Element,Class)</li>
     * </ul>
     * 
     * @author Thomas Scheffler (yagee)
     * 
     * @version $Revision$ $Date$
     */
    protected class EditorValidator extends MCREditorOutValidator {
        /**
         * instantiate the validator with the editor input <code>jdom_in</code>.
         * 
         * <code>id</code> will be set as the MCRObjectID for the resulting
         * object that can be fetched with
         * <code>generateValidMyCoReObject()</code>
         * 
         * @param jdom_in
         *            editor input
         */
        public EditorValidator(Document jdom_in, MCRObjectID id) {
            super(jdom_in, id);
        }

    }
}
