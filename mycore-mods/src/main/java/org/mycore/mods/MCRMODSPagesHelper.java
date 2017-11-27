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

package org.mycore.mods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xpath.NodeSet;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.mycore.common.MCRConstants;

/**
 * Builds a mods:extent[@unit='pages'] element from text containing pages information. 
 * For example, the input "pp. 3-4" will generate mods:start and mods:end elements. 
 * Additionally, variants of hyphens are normalized to a unique character.
 * Third, incomplete end page numbers are completed, e.g. S. 3845 - 53 will result in 
 * mods:end=3853.
 * 
 * @author Frank L\u00FCtzenkirchen 
 **/
public class MCRMODSPagesHelper {

    private static final HyphenNormalizer hyphenNormalizer = new HyphenNormalizer();

    private static final EndPageCompleter endPageNormalizer = new EndPageCompleter();

    private static final ExtentPagesBuilder extentBuilder = new ExtentPagesBuilder();

    public static Element buildExtentPages(String input) {
        input = input.trim();
        input = hyphenNormalizer.normalize(input);
        Element extent = extentBuilder.buildExtent(input);
        endPageNormalizer.completeEndPage(extent);
        return extent;
    }

    public static NodeSet buildExtentPagesNodeSet(String input) throws JDOMException {
        Element extent = buildExtentPages(input);
        org.w3c.dom.Element domElement = new DOMOutputter().output(extent);
        NodeSet nodeSet = new NodeSet();
        nodeSet.addNode(domElement);
        return nodeSet;
    }
}

/**
 * Normalizes the different variants of hyphens in a given input text to a simple "minus" character.  
 * 
 * @author Frank L\u00FCtzenkirchen 
 **/
class HyphenNormalizer {

    private static final char HYPHEN_NORM = '-';

    private char[] HYPHEN_VARIANTS = { '\u002D', '\u2010', '\u2011', '\u2012', '\u2013', '\u2015', '\u2212', '\u2E3B',
        '\uFE58', '\uFE63' };

    String normalize(String input) {
        for (char hypenVariant : HYPHEN_VARIANTS)
            input = input.replace(hypenVariant, HYPHEN_NORM);
        return input;
    }
}

/**
 * When start and end page are given, often only the differing prefix of the end page number is specified, e.g.
 * "3845 - 53" meaning end page is 3853. This class completes the value of mods:end if start and end page
 * are both numbers.    
 * 
 * @author Frank L\u00FCtzenkirchen 
 **/
class EndPageCompleter {

    void completeEndPage(Element extent) {
        String start = extent.getChildText("start", MCRConstants.MODS_NAMESPACE);
        String end = extent.getChildText("end", MCRConstants.MODS_NAMESPACE);
        if (isNumber(start) && isNumber(end) && start.length() > end.length()) {
            end = start.substring(0, start.length() - end.length()) + end;
            extent.getChild("end", MCRConstants.MODS_NAMESPACE).setText(end);
        }
    }

    boolean isNumber(String value) {
        return ((value != null) && value.matches("\\d+"));
    }
}

/**
 * Builds a mods:extent element containing appropriate child elements representing the same 
 * pages information as some textual input. For example, the input "pp. 3-4" will generate mods:start and
 * mods:end elements. 
 * 
 * @author Frank L\u00FCtzenkirchen 
 **/
class ExtentPagesBuilder {

    private static final String OPTIONAL = "?";

    private static final String ZERO_OR_MORE = "*";

    private static final String ONE_OR_MORE = "+";

    private static final String NUMBER = "([0-9]+)";

    private static final String PAGENR = "([a-zA-Z0-9\\.]+)";

    private static final String SPACE = "\\s";

    private static final String SPACES = SPACE + ONE_OR_MORE;

    private static final String SPACES_OPTIONAL = SPACE + ZERO_OR_MORE;

    private static final String DOT = "\\.";

    private static final String HYPHEN = SPACES_OPTIONAL + "-" + SPACES_OPTIONAL;

    private static final String PAGENR_W_HYPHEN = "([a-zA-Z0-9-\\.]+)";

    private static final String HYPHEN_SEPARATED = SPACES + "-" + SPACES;

    private static final String PAGE = "([sSp]{1,2}" + DOT + OPTIONAL + SPACES_OPTIONAL + ")" + OPTIONAL;

    private static final String PAGES = "(pages|[Ss]eiten|S\\.)";

    private static final String FF = "(" + SPACES + "ff?\\.?)" + OPTIONAL;

    private List<PagesPattern> patterns = new ArrayList<>();

    ExtentPagesBuilder() {
        PagesPattern startEnd = new PagesPattern(PAGE + PAGENR + HYPHEN + PAGENR + DOT + OPTIONAL);
        startEnd.addMapping("start", 2);
        startEnd.addMapping("end", 3);
        patterns.add(startEnd);

        PagesPattern startEndVariant = new PagesPattern(
            PAGE + PAGENR_W_HYPHEN + HYPHEN_SEPARATED + PAGENR_W_HYPHEN + DOT + OPTIONAL);
        startEndVariant.addMapping("start", 2);
        startEndVariant.addMapping("end", 3);
        patterns.add(startEndVariant);

        PagesPattern startTotal = new PagesPattern(
            PAGE + PAGENR + SPACES + "\\(" + NUMBER + SPACES_OPTIONAL + PAGES + "\\)");
        startTotal.addMapping("start", 2);
        startTotal.addMapping("total", 3);
        patterns.add(startTotal);

        PagesPattern startOnly = new PagesPattern(PAGE + PAGENR + FF);
        startOnly.addMapping("start", 2);
        patterns.add(startOnly);

        PagesPattern totalOnly = new PagesPattern("\\(?" + PAGENR + SPACES + PAGES + OPTIONAL + "\\)?");
        totalOnly.addMapping("total", 1);
        patterns.add(totalOnly);

        PagesPattern list = new PagesPattern("(.+)");
        list.addMapping("list", 1);
        patterns.add(list);
    }

    /**
     * Builds a mods:extent element containing appropriate child elements representing the same 
     * pages information as the textual input. For example, the input "3-4" will generate mods:start and
     * mods:end elements. 
     * 
     * @param input the textual pages information, e.g. "S. 3-4" or "p. 123 (9 pages)" 
     */
    Element buildExtent(String input) {
        Element extent = buildExtent();

        for (PagesPattern pattern : patterns) {
            PagesMatcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                matcher.addMODSto(extent);
                break;
            }
        }
        return extent;
    }

    /** Builds a new mods:extent element to hold pages information */
    private Element buildExtent() {
        Element extent = new Element("extent", MCRConstants.MODS_NAMESPACE);
        extent.setAttribute("unit", "pages");
        return extent;
    }
}

/**
 * Represents a text pattern containing pages information, like start & end page,
 * or start and total number of pages as textual representation. Manages a mapping
 * between matching groups in pattern and the corresponding MODS elements, e.g. 
 * mods:start and mods:end.
 * 
 * @author Frank L\u00FCtzenkirchen 
 **/
class PagesPattern {

    private Pattern pattern;

    /** Mapping from MODS Element name to group number in the pattern */
    private Map<String, Integer> mods2group = new LinkedHashMap<>();

    PagesPattern(String regularExpression) {
        pattern = Pattern.compile(regularExpression);
    }

    /**
     * Add a mapping from MODS Element name to group number in the pattern.
     * 
     * @param modsElement the name of the MODS element mapped, e.g. "start"
     * @param groupNumber number of the group in the regular expression pattern
     **/
    void addMapping(String modsElement, int groupNumber) {
        mods2group.put(modsElement, groupNumber);
    }

    /**
     * Returns the mappings from MODS Element name to group number
     */
    Set<Entry<String, Integer>> getMappings() {
        return mods2group.entrySet();
    }

    /**
     * Returns a matcher for the given input text
     */
    PagesMatcher matcher(String input) {
        return new PagesMatcher(this, pattern.matcher(input));
    }

}

/**
 * Represents a matcher for a given input text containing pages information,
 * associated with a PagesPattern that possibly matches the text.
 * 
 * @author Frank L\u00FCtzenkirchen 
 **/
class PagesMatcher {

    PagesPattern pattern;

    Matcher matcher;

    PagesMatcher(PagesPattern pattern, Matcher matcher) {
        this.pattern = pattern;
        this.matcher = matcher;
    }

    boolean matches() {
        return matcher.matches();
    }

    /**
     * If matches(), adds MODS elements mapped to the matching groups in the pattern.
     * Note that matches() MUST be called first!
     * 
     * @param extent the mods:extent element to add new MODS elements to
     */
    void addMODSto(Element extent) {
        for (Entry<String, Integer> mapping : pattern.getMappings()) {
            int group = mapping.getValue();
            String name = mapping.getKey();
            Element mods = new Element(name, MCRConstants.MODS_NAMESPACE);
            String value = matcher.group(group);
            mods.setText(value);
            extent.addContent(mods);
        }
    }
}
