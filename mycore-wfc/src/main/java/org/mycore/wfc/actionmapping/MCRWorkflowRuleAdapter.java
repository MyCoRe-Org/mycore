/*
 * $Id$
 * $Revision: 5697 $ $Date: 15.03.2012 $
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
