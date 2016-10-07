package org.mycore.iiif.presentation.model.basic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFViewingDirection;

public class MCRIIIFManifest extends MCRIIIFPresentationBase {

    public static final String TYPE = "sc:Manifest";

    public List<MCRIIIFSequence> sequences = new ArrayList<>();

    public List<MCRIIIFMetadata> metadata = new ArrayList<>();

    public List<MCRIIIFRange> structures = new ArrayList<>();

    private String label = null;

    private String description = null;

    private MCRIIIFResource thumbnail = null;

    private MCRIIIFViewingDirection viewingDirection = null;

    private String within = null;

    private Date navDate;

    public MCRIIIFManifest() {
        super(TYPE, API_PRESENTATION_2);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MCRIIIFResource getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(MCRIIIFResource thumbnail) {
        this.thumbnail = thumbnail;
    }

    public MCRIIIFViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(MCRIIIFViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

    public String getWithin() {
        return within;
    }

    public void setWithin(String within) {
        this.within = within;
    }

    public Date getNavDate() {
        return navDate;
    }

    public void setNavDate(Date navDate) {
        this.navDate = navDate;
    }

}
