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

package org.mycore.solr.index.file.tika;

import java.io.StringReader;
import java.util.ArrayList;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.Assert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import junit.framework.TestCase;

public class MCRSimpleTikaMapperTest extends TestCase {

    public final static String TEST_JSON
        = """
            {
               "pdf:unmappedUnicodeCharsPerPage": "0",
               "pdf:PDFVersion": "1.7",
               "pdf:hasXFA": "false",
               "access_permission:modify_annotations": "true",
               "access_permission:can_print_degraded": "true",
               "X-TIKA:Parsed-By-Full-Set": [
                 "org.apache.tika.parser.DefaultParser",
                 "org.apache.tika.parser.pdf.PDFParser"
               ],
               "pdf:num3DAnnotations": "0",
               "dcterms:created": "2021-08-02T14:14:13Z",
               "dcterms:modified": "2021-08-02T14:14:13Z",
               "dc:format": "application/pdf; version=1.7",
               "pdf:overallPercentageUnmappedUnicodeChars": "0.0",
               "access_permission:fill_in_form": "true",
               "pdf:docinfo:modified": "2021-08-02T14:14:13Z",
               "pdf:hasCollection": "false",
               "pdf:encrypted": "false",
               "pdf:containsNonEmbeddedFont": "true",
               "Content-Length": "1406",
               "pdf:hasMarkedContent": "false",
               "Content-Type": "application/pdf",
               "pdf:producer": "iText® 7.1.15 ©2000-2021 iText Group NV (AGPL-version)",
               "pdf:totalUnmappedUnicodeChars": "0",
               "access_permission:extract_for_accessibility": "true",
               "access_permission:assemble_document": "true",
               "xmpTPg:NPages": "1",
               "pdf:hasXMP": "false",
               "pdf:charsPerPage": "469",
               "access_permission:extract_content": "true",
               "access_permission:can_print": "true",
               "X-TIKA:Parsed-By": [
                 "org.apache.tika.parser.DefaultParser",
                 "org.apache.tika.parser.pdf.PDFParser"
               ],
               "X-TIKA:content": "\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\nBecker, Klaus : Einschränkungen im Lehrbetrieb gravierender als in Forschung und Transfer: Die \\nAuswirkungen der Corona-Pandemie auf Forschung und Transfer an der Technischen \\nHochschule (TH) Köln – ein Einblick. In: DUZ Wissenschaft & Management . Berlin, DUZ \\nVerlags- und Medienhaus GmbH (2020a), Nr. 10, S. 50–52\\n\\nBecker, Klaus : Transfer an der TH Köln: Wissen auch gesellschaftlich wirksam machen – mit \\nund in der Region Köln. In: Köln-Magazin (2020b), Nr. 2, S. 54–55\\n\\n\\n",
               "access_permission:can_modify": "true",
               "pdf:docinfo:producer": "iText® 7.1.15 ©2000-2021 iText Group NV (AGPL-version)",
               "pdf:docinfo:created": "2021-08-02T14:14:13Z",
               "pdf:containsDamagedFont": "false"
             }""";
    public static final String X_TIKA_PARSED_BY = "X-TIKA:Parsed-By";

    public void testMap() throws MCRTikaMappingException {
        MCRSimpleTikaMapper simpleTikaMapper = new MCRSimpleTikaMapper();
        JsonObject rootObject;

        SolrInputDocument doc = new SolrInputDocument();
        try (StringReader stringReader = new StringReader(TEST_JSON)) {
            JsonStreamParser parser = new JsonStreamParser(stringReader);
            JsonElement root = parser.next();
            rootObject = root.getAsJsonObject();
        }

        // Test with stripNamespace = false and multiValue = true
        simpleTikaMapper.setStripNamespace(false);
        simpleTikaMapper.setMultiValue(true);
        String skName = MCRTikaMapper.simplifyKeyName(X_TIKA_PARSED_BY);
        simpleTikaMapper.map(X_TIKA_PARSED_BY, rootObject.get(X_TIKA_PARSED_BY), doc, null, null);
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
        simpleTikaMapper.map(X_TIKA_PARSED_BY, rootObject.get(X_TIKA_PARSED_BY), doc, null, null);
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
        simpleTikaMapper.map(X_TIKA_PARSED_BY, rootObject.get(X_TIKA_PARSED_BY), doc, null, null);
        field = doc.getField("parsed_by");
        Assert.assertNotNull("There should be a \"parsed_by\" entry", field);
        Assert.assertEquals("There should be just one \"parsed_by\" entries", 1, field.getValueCount());
        Assert.assertEquals("The \"parsed_by\" entry should be 'org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser'",
                "org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser", field.getValue());


        // Test with stripNamespace = false and multiValue = false
        doc = new SolrInputDocument();
        simpleTikaMapper.setStripNamespace(false);
        simpleTikaMapper.setMultiValue(false);
        skName = MCRTikaMapper.simplifyKeyName(X_TIKA_PARSED_BY);
        simpleTikaMapper.map(X_TIKA_PARSED_BY, rootObject.get(X_TIKA_PARSED_BY), doc, null, null);
        field = doc.getField(skName);
        Assert.assertNotNull("There should be a " + skName + " entry", field);
        Assert.assertEquals("There should be just one " + skName + " entries", 1, field.getValueCount());
        Assert.assertEquals("The " + skName + " entry should be 'org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser'",
                "org.apache.tika.parser.DefaultParser\norg.apache.tika.parser.pdf.PDFParser", field.getValue());
    }
}
