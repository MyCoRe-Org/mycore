/*
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

package org.mycore.mods.bibtex;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.xml.MCRNodeBuilder;

public class MCRBibTeX2MODSTransformerTest extends MCRTestCase {

    @Test
    public void testField2XPathTransformation() throws Exception {
        String src, res;

        src = "@article{Doe2015, title={MyCoRe in a nutshell}, author={Doe, John}}";
        res = "mods:mods[mods:genre='article']" + "[mods:titleInfo/mods:title='MyCoRe in a nutshell']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]";
        testTransformation(src, res);

        src = "@inproceedings{Doe2015, title={MyCoRe in a nutshell}, author={Doe, John}, booktitle={Proceedings of the 5th MyCoRe User workshop}}";
        res = "mods:mods[mods:genre='inproceedings']" + "[mods:titleInfo/mods:title='MyCoRe in a nutshell']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]"
                + "[mods:relatedItem[@type='host'][mods:genre='proceedings'][mods:titleInfo/mods:title='Proceedings of the 5th MyCoRe User workshop']]";
        testTransformation(src, res);

        src = "@article{Doe2015, title={MyCoRe in a nutshell}, author={Doe, John}, journal={Journal of Advanced Repository Software}}";
        res = "mods:mods[mods:genre='article']" + "[mods:titleInfo/mods:title='MyCoRe in a nutshell']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]"
                + "[mods:relatedItem[@type='host'][mods:genre='journal'][mods:titleInfo/mods:title='Journal of Advanced Repository Software']]";
        testTransformation(src, res);

        src = "@article{Doe2015, title={MyCoRe}, author={Doe, John}, journal={Journal}, pages={34 -- 91}}";
        res = "mods:mods[mods:genre='article']" + "[mods:titleInfo/mods:title='MyCoRe']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]"
                + "[mods:relatedItem[@type='host'][mods:genre='journal']" + "[mods:titleInfo/mods:title='Journal']"
                + "[mods:part/mods:extent[@unit='pages'][mods:start='34'][mods:end='91']]]";
        testTransformation(src, res);
    }

    @Test
    public void testMoveToRelatedItemTransformations() throws Exception {
        String src, res;

        src = "@book{Doe2015, title={MyCoRe unleashed}, editor={Doe, John}}";
        res = "mods:mods[mods:genre='book']" + "[mods:titleInfo/mods:title='MyCoRe unleashed']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='edt'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]";
        testTransformation(src, res);

        src = "@incollection{Doe2015, title={Introduction}, author={Foo, Bar}, booktitle={MyCoRe unleashed}, editor={Doe, John}}";
        res = "mods:mods[mods:genre='incollection']" + "[mods:titleInfo/mods:title='Introduction']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Foo'][mods:namePart[@type='given']='Bar']]"
                + "[mods:relatedItem[@type='host'][mods:genre='collection']"
                + "[mods:titleInfo/mods:title='MyCoRe unleashed']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='edt'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]]";
        testTransformation(src, res);

        src = "@book{Doe2015, title={Introduction}, author={Foo, Bar}, edition={4th}}";
        res = "mods:mods[mods:genre='book']" + "[mods:titleInfo/mods:title='Introduction']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Foo'][mods:namePart[@type='given']='Bar']]"
                + "[mods:originInfo/mods:edition='4th']";
        testTransformation(src, res);

        src = "@incollection{Doe2015, title={Introduction}, author={Foo, Bar}, booktitle={MyCoRe unleashed}, edition={4th}}";
        res = "mods:mods[mods:genre='incollection']" + "[mods:titleInfo/mods:title='Introduction']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Foo'][mods:namePart[@type='given']='Bar']]"
                + "[mods:relatedItem[@type='host'][mods:genre='collection']"
                + "[mods:titleInfo/mods:title='MyCoRe unleashed']" + "[mods:originInfo/mods:edition='4th']]";
        testTransformation(src, res);

        src = "@article{Doe2015, title={MyCoRe in a nutshell}, author={Doe, John}, journal={Journal of Advanced Repository Software}, issn={1234-5678}}";
        res = "mods:mods[mods:genre='article']" + "[mods:titleInfo/mods:title='MyCoRe in a nutshell']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]"
                + "[mods:relatedItem[@type='host'][mods:genre='journal'][mods:titleInfo/mods:title='Journal of Advanced Repository Software']"
                + "[mods:identifier[@type='issn']='1234-5678']]";
        testTransformation(src, res);

        src = "@incollection{Doe2015, title={MyCoRe in a nutshell}, author={Doe, John}, booktitle={Advanced Repository Software}, series={LNCS}, issn={1234-5678}}";
        res = "mods:mods[mods:genre='incollection']" + "[mods:titleInfo/mods:title='MyCoRe in a nutshell']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]"
                + "[mods:relatedItem[@type='host'][mods:genre='collection'][mods:titleInfo/mods:title='Advanced Repository Software']"
                + "[mods:relatedItem[@type='series'][mods:titleInfo/mods:title='LNCS'][mods:identifier[@type='issn']='1234-5678']]]";
        testTransformation(src, res);
    }

    @Test
    public void testUnsupportedField() throws Exception {
        String src, res;

        src = "@book{Doe2015, title={MyCoRe unleashed}, author={Doe, John}, rating={ugly}}";
        res = "mods:mods[mods:genre='book']" + "[mods:titleInfo/mods:title='MyCoRe unleashed']"
                + "[mods:name[@type='personal'][mods:role/mods:roleTerm[@type='code'][@authority='marcrelator']='aut'][mods:namePart[@type='family']='Doe'][mods:namePart[@type='given']='John']]"
                + "[mods:extension[field[@name='rating']='ugly']]";
        testTransformation(src, res);
    }

    private void testTransformation(String bibTeX, String expectedMODSXPath) throws Exception {
        MCRJDOMContent resultingContent = new MCRBibTeX2MODSTransformer().transform(new MCRStringContent(bibTeX));
        Element resultingMODS = resultingContent.asXML().getRootElement().getChild("mods", MCRConstants.MODS_NAMESPACE)
                .detach();

        for (Element extension : resultingMODS.getChildren("extension", MCRConstants.MODS_NAMESPACE)) {
            for (Element source : extension.getChildren("source")) {
                source.detach();
            }
            if (extension.getChildren().isEmpty()) {
                extension.detach();
            }
        }

        String result = new MCRJDOMContent(resultingMODS).asString();
        String expected = new MCRJDOMContent(new MCRNodeBuilder().buildElement(expectedMODSXPath, null, null))
                .asString();
        assertEquals(expected, result);
    }

    @Test
    public void testGenreMapping() throws Exception {
        String bibTeX = "@incollection{Doe2015, title={MyCoRe in a nutshell}, booktitle={Advanced Repository Software}}";
        testGenreMapping(bibTeX, "incollection", "collection");

        bibTeX = "@inproceedings{Doe2015, title={MyCoRe in a nutshell}, booktitle={Advanced Repository Software}}";
        testGenreMapping(bibTeX, "inproceedings", "proceedings");

        bibTeX = "@article{Doe2015, title={MyCoRe in a nutshell}, author={Doe, John}, journal={Journal of Advanced Repository Software}}";
        testGenreMapping(bibTeX, "article", "journal");
    }

    private void testGenreMapping(String bibTeX, String genre, String hostGenre) throws IOException {
        MCRJDOMContent result = new MCRBibTeX2MODSTransformer().transform(new MCRStringContent(bibTeX));
        Element mods = result.asXML().getRootElement().getChild("mods", MCRConstants.MODS_NAMESPACE);
        assertEquals(genre, mods.getChildText("genre", MCRConstants.MODS_NAMESPACE));
        assertEquals(hostGenre, mods.getChild("relatedItem", MCRConstants.MODS_NAMESPACE).getChildText("genre",
                MCRConstants.MODS_NAMESPACE));
    }
}
