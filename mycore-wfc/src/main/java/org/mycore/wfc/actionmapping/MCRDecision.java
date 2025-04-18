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

package org.mycore.wfc.actionmapping;

import org.mycore.parsers.bool.MCRCondition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@XmlRootElement(name = "when")
@XmlAccessorType(XmlAccessType.NONE)
public class MCRDecision {

    @XmlAttribute
    private String url;

    @XmlAttribute
    @XmlJavaTypeAdapter(MCRWorkflowRuleAdapter.class)
    private MCRCondition<?> condition;

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the condition
     */
    public MCRCondition<?> getCondition() {
        return condition;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(MCRCondition<?> condition) {
        this.condition = condition;
    }

}
