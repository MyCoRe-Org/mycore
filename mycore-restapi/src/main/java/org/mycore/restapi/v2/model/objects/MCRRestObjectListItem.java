package org.mycore.restapi.v2.model.objects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.restapi.v2.model.MCRRestLink;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = MCRRestObjectListItemJsonSerializer.class)
public class MCRRestObjectListItem {
    private String id;
    private Instant modified;

    private List<MCRRestLink> links = new ArrayList<MCRRestLink>();

    public MCRRestObjectListItem(MCRObjectIDDate idDate) {
        id = idDate.getId();
        modified = idDate.getLastModified().toInstant();
        links.add(new MCRRestLink("self", MCRFrontendUtil.getBaseURL() + "api/v2/objects/" + id));
        // TODO make configurable
        links.add(new MCRRestLink("html", MCRFrontendUtil.getBaseURL() + "receive/" + id));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public List<MCRRestLink> getLinks() {
        return links;
    }
}
