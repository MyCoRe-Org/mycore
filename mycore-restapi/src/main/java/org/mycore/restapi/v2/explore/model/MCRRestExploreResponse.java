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
package org.mycore.restapi.v2.explore.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The root object for the REST /explore response.
 * 
 * @author Robert Stephan
 *
 */
@XmlRootElement(name = "response")
@XmlType(propOrder={"header", "data"})
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso(ArrayList.class)
public class MCRRestExploreResponse {
    
    @XmlElementWrapper(name="data")
    @XmlElement(name="mcrobject")
    @JsonProperty("data")
    private List<MCRRestExploreResponseObject> data = new ArrayList<MCRRestExploreResponseObject>();

    @XmlElement(name="header")
    private MCRRestExploreResponseHeader header = new MCRRestExploreResponseHeader();

    
    public List<MCRRestExploreResponseObject> getData() {
        return data;
    }

    public MCRRestExploreResponseHeader getHeader() {
        return header;
    }
}
