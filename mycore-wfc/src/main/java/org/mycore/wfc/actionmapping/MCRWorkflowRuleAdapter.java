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

package org.mycore.wfc.actionmapping;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;

/**
 * A JAXB XML Adapter that parses String to MCRCondition an back.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRWorkflowRuleAdapter extends XmlAdapter<String, MCRCondition<?>> {

    private MCRBooleanClauseParser parser;

    public MCRWorkflowRuleAdapter() {
        parser = getClauseParser();
    }

    /**
     * Returns a parser instance that is used in {@link #unmarshal(String)}.
     * @return instance of {@link MCRWorkflowRuleParser}
     */
    protected MCRBooleanClauseParser getClauseParser() {
        return new MCRWorkflowRuleParser();
    }

    @Override
    public MCRCondition<?> unmarshal(final String v) throws Exception {
        return parser.parse(v);
    }

    @Override
    public String marshal(final MCRCondition<?> v) throws Exception {
        return v.toString();
    }

}
