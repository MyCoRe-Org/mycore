/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.DeDup.CriterionBuilder.test.dummy.Class",
        classNameOf = MCRDeDupTestCriterionBuilder.class)
})
public class MCRDeDupURIResolverTest {

    private static final Namespace XSI_NAMESPACE = Namespace.getNamespace("xsi",
        "http://www.w3.org/2001/XMLSchema-instance");

    private static final List<String> SESSION_KEY_PREFIXES = List.of("dedup-", "preview-");

    private final MCRDeDupKeyManager manager = new MCRDeDupKeyManager();

    private final MCRDeDupURIResolver resolver = new MCRDeDupURIResolver(SESSION_KEY_PREFIXES);

    private static MCRObjectID id(int number) {
        return MCRObjectID.getInstance(MCRObjectID.formatID("mcr", "test", number));
    }

    private static Element objectElement(int number) {
        Element element = new Element("mycoreobject")
            .setAttribute("ID", id(number).toString())
            .setAttribute("version", "test")
            .setAttribute("noNamespaceSchemaLocation", "noSchema", XSI_NAMESPACE);
        element.addNamespaceDeclaration(XSI_NAMESPACE);
        return element;
    }

    private Element resolve(String href) throws Exception {
        Source source = resolver.resolve(href, null);
        JDOMResult result = new JDOMResult();
        TransformerFactory.newInstance().newTransformer().transform(source, result);
        return result.getDocument().getRootElement();
    }

    @Test
    public void resolvesDuplicatesAsXml() throws Exception {
        manager.storeKeys(id(1), Set.of(
            new MCRDeDupCriterion("identifier", "doi:1"),
            new MCRDeDupCriterion("title-author", "meier: a title")));
        manager.storeKeys(id(2), Set.of(
            new MCRDeDupCriterion("identifier", "doi:1"),
            new MCRDeDupCriterion("title-author", "meier: a title")));

        Element result = resolve("dedup:duplicates:" + id(1));

        assertEquals("duplicates", result.getName());
        assertEquals(id(1).toString(), result.getAttributeValue("for"));

        List<Element> duplicates = result.getChildren("duplicate");
        assertEquals(1, duplicates.size());
        Element duplicate = duplicates.get(0);
        assertEquals(id(2).toString(), duplicate.getAttributeValue("id"));
        assertEquals(2, duplicate.getChildren("criterion").size(), "both matching criteria must be listed");
    }

    @Test
    public void emptyResultWhenNoDuplicates() throws Exception {
        manager.storeKeys(id(1), Set.of(new MCRDeDupCriterion("identifier", "doi:1")));

        Element result = resolve("dedup:duplicates:" + id(1));

        assertEquals("duplicates", result.getName());
        assertEquals(0, result.getChildren("duplicate").size());
    }

    @Test
    public void noDuplicateMarkingIsRespected() throws Exception {
        manager.storeKeys(id(1), Set.of(new MCRDeDupCriterion("identifier", "doi:1")));
        manager.storeKeys(id(2), Set.of(new MCRDeDupCriterion("identifier", "doi:1")));
        manager.addNoDuplicate(id(1), id(2), "junit");

        Element result = resolve("dedup:duplicates:" + id(1));

        assertEquals(0, result.getChildren("duplicate").size());
    }

    @Test
    public void resolvesDuplicatesForSessionObjectAsXml() throws Exception {
        String sessionKey = "dedup-preview";
        manager.storeKeys(id(2), Set.of(MCRDeDupTestCriterionBuilder.CRITERION));
        MCRSessionMgr.getCurrentSession().put(sessionKey, objectElement(1));

        Element result = resolve("dedup:duplicates-for-session:" + sessionKey);

        assertEquals("duplicates", result.getName());
        assertEquals(sessionKey, result.getAttributeValue("for"));

        List<Element> duplicates = result.getChildren("duplicate");
        assertEquals(1, duplicates.size());
        Element duplicate = duplicates.get(0);
        assertEquals(id(2).toString(), duplicate.getAttributeValue("id"));
        assertEquals(1, duplicate.getChildren("criterion").size());
        Element criterion = duplicate.getChild("criterion");
        assertEquals(MCRDeDupTestCriterionBuilder.CRITERION.type(), criterion.getAttributeValue("type"));
        assertEquals(MCRDeDupTestCriterionBuilder.CRITERION.value(), criterion.getAttributeValue("value"));
    }

    @Test
    public void acceptsSessionKeyWithAnyConfiguredPrefix() throws Exception {
        String sessionKey = "preview-object";
        manager.storeKeys(id(2), Set.of(MCRDeDupTestCriterionBuilder.CRITERION));
        MCRSessionMgr.getCurrentSession().put(sessionKey, objectElement(1));

        Element result = resolve("dedup:duplicates-for-session:" + sessionKey);

        assertEquals(1, result.getChildren("duplicate").size());
    }

    @Test
    public void rejectsSessionKeyWithoutAllowedPrefix() {
        String sessionKey = "evil-key";
        manager.storeKeys(id(2), Set.of(MCRDeDupTestCriterionBuilder.CRITERION));
        MCRSessionMgr.getCurrentSession().put(sessionKey, objectElement(1));

        assertThrows(TransformerException.class,
            () -> resolver.resolve("dedup:duplicates-for-session:" + sessionKey, null));
    }

    @Test
    public void rejectsUnknownAction() {
        assertThrows(TransformerException.class, () -> resolver.resolve("dedup:unknown:" + id(1), null));
    }

    @Test
    public void rejectsInvalidObjectId() {
        assertThrows(TransformerException.class, () -> resolver.resolve("dedup:duplicates:not a valid id", null));
    }

    @Test
    public void rejectsMalformedUri() {
        assertThrows(TransformerException.class, () -> resolver.resolve("dedup:duplicates", null));
    }
}
