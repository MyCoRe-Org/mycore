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

package org.mycore.orcid2.work;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.v3.work.MCRORCIDWorkEventHandlerImpl;

public class MCRORCIDWorkEventHandlerImplTest extends MCRJPATestCase {

    @Test
    public void testNoContributor() throws IOException, JDOMException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        Document document = saxBuilder.build(classLoader.getResourceAsStream(
            "MCRORCIDWorkEventHandlerImplTest/mods_no_author.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);
        
        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 1);

        MCRORCIDWorkEventHandlerImpl eventHandler = new MCRORCIDWorkEventHandlerImpl();
        eventHandler.handleObjectCreated(null, mcro);
    }

    @Test
    public void testContributor() throws IOException, JDOMException {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionHelper.isTransactionActive();
        ClassLoader classLoader = getClass().getClassLoader();
        SAXBuilder saxBuilder = new SAXBuilder();

        Document document = saxBuilder.build(classLoader.getResourceAsStream(
            "MCRORCIDWorkEventHandlerImplTest/mods_author.xml"));
        MCRObject mcro = new MCRObject();

        MCRMODSWrapper mw = new MCRMODSWrapper(mcro);

        mw.setMODS(document.getRootElement().detach());
        mw.setID("junit", 2);

        MCRORCIDWorkEventHandlerImpl eventHandler = new MCRORCIDWorkEventHandlerImpl();
        eventHandler.handleObjectCreated(null, mcro);
    }

}
