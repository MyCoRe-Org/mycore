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

package org.mycore.solr.index.file.tika;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Assert;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

public class MCRSimpleTikaMapperTest extends TestCase {

    public static final String X_TIKA_PARSED_BY = "X-TIKA:Parsed-By";
    public String testJson;

    public MCRSimpleTikaMapperTest() {
        super();

        try (InputStream is = MCRClassTools.getClassLoader().getResourceAsStream("tika_test.json")) {
            testJson = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    public void testMap() throws MCRTikaMappingException, IOException {
        MCRSimpleTikaMapper simpleTikaMapper = new MCRSimpleTikaMapper();
        TreeNode root;

        SolrInputDocument doc = new SolrInputDocument();
        try (StringReader stringReader = new StringReader(testJson)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonParser parser = mapper.createParser(stringReader);
            root = mapper.readTree(parser);
        }

        // Test with stripNamespace = false and multiValue = true
        simpleTikaMapper.setStripNamespace(false);
        simpleTikaMapper.setMultiValue(true);
        String skName = MCRTikaMapper.simplifyKeyName(X_TIKA_PARSED_BY);
        simpleTikaMapper.map(X_TIKA_PARSED_BY, root.get(X_TIKA_PARSED_BY), doc, null, null);
        SolrInputField field = doc.getField(skName);
        Assert.assertNotNull("There should be a " + skName + " entry", field);
        Assert.assertEquals("There should be two " + skName + " entries", 2, field.getValueCount());
        ArrayList<Object> values = new ArrayList<>(field.getValues());
        Assert.assertEquals("First " + skName + " entry should be 'org.apache.tika.parser.DefaultParser'",
            "org.apache.tika.parser.DefaultParser", values.get(0));
        Assert.assertEquals("Second " + skName + " entry should be 'org.apache.tika.parser.pdf.PDFParser'",
            "org.apache.tika.parser.pdf.PDFParser", values.get(1));

        // Test with stripNamespace = true and multiValue = true
        doc = new SolrInputDocument();
        simpleTikaMapper.setStripNamespace(true);
        simpleTikaMapper.setMultiValue(true);
        skName = MCRTikaMapper.simplifyKeyName(X_TIKA_PARSED_BY);
        simpleTikaMapper.map(X_TIKA_PARSED_BY, root.get(X_TIKA_PARSED_BY), doc, null, null);
        field = doc.getField("parsed_by");
        Assert.assertNotNull("There should be a \"parsed_by\" entry", field);
        Assert.assertEquals("There should be two \"parsed_by\" entries", 2, field.getValueCount());
        values = new ArrayList<>(field.getValues());
        Assert.assertEquals("First \"parsed_by\" entry should be 'org.apache.tika.parser.DefaultParser'",
            "org.apache.tika.parser.DefaultParser", values.get(0));
        Assert.assertEquals("Second \"parsed_by\" entry should be 'org.apache.tika.parser.pdf.PDFParser'",
            "org.apache.tika.parser.pdf.PDFParser", values.get(1));

        // Test with stripNamespace = true and multiValue = false
        doc = new SolrInputDocument();
        simpleTikaMapper.setStripNamespace(true);
        simpleTikaMapper.setMultiValue(false);
        skName = MCRTikaMapper.simplifyKeyName(X_TIKA_PARSED_BY);
        simpleTikaMapper.map(X_TIKA_PARSED_BY, root.get(X_TIKA_PARSED_BY), doc, null, null);
        field = doc.getField("parsed_by");
        Assert.assertNotNull("There should be a \"parsed_by\" entry", field);
        Assert.assertEquals("There should be just one \"parsed_by\" entries", 1, field.getValueCount());
        Assert.assertEquals(

            "The \"parsed_by\" entry should be 'org.apache.tika.parser.DefaultParser\n" +
                "org.apache.tika.parser.pdf.PDFParser'",
            "org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser", field.getValue());

        // Test with stripNamespace = false and multiValue = false
        doc = new SolrInputDocument();
        simpleTikaMapper.setStripNamespace(false);
        simpleTikaMapper.setMultiValue(false);
        skName = MCRTikaMapper.simplifyKeyName(X_TIKA_PARSED_BY);
        simpleTikaMapper.map(X_TIKA_PARSED_BY, root.get(X_TIKA_PARSED_BY), doc, null, null);
        field = doc.getField(skName);
        Assert.assertNotNull("There should be a " + skName + " entry", field);
        Assert.assertEquals("There should be just one " + skName + " entries", 1, field.getValueCount());
        Assert.assertEquals(
            "The " + skName
                + " entry should be 'org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser'",
            "org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser", field.getValue());
    }
}
