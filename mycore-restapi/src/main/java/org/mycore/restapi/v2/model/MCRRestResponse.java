package org.mycore.restapi.v2.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.frontend.MCRFrontendUtil;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement
public class MCRRestResponse {

    @XmlElement
    @JsonSerialize(using = MCRRestResponseHeaderJsonSerializer.class)
    private MCRRestResponseHeader header = new MCRRestResponseHeader();

    @XmlElement
    List<Object> data = new ArrayList<>();

    public MCRRestResponse() {
        header.addLink("base", MCRFrontendUtil.getBaseURL() + "api/v2");
    }

    public MCRRestResponseHeader getHeader() {
        return header;
    }

    public List<Object> getData() {
        return data;
    }

    public void addData(Object o) {
        data.add(o);
    }

    public void addAllData(Collection<?> oColl) {
        data.addAll(oColl);
    }

}
