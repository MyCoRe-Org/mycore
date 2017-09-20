package org.mycore.mets.model.simple;

import java.util.ArrayList;
import java.util.List;

public class MCRMetsPage {
    private String id;

    private String orderLabel;

    private String contentIds;

    private Boolean hidden;

    private List<MCRMetsFile> fileList;

    public MCRMetsPage(String id, String orderLabel, String contentIds) {
        this();
        this.id = id;
        this.orderLabel = orderLabel;
        this.contentIds = contentIds;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public MCRMetsPage() {
        this.fileList = new ArrayList<>();
    }

    public List<MCRMetsFile> getFileList() {
        return fileList;
    }

    public String getOrderLabel() {
        return orderLabel;
    }

    public void setOrderLabel(String orderLabel) {
        if (orderLabel == "") {
            orderLabel = null;
        }
        this.orderLabel = orderLabel;
    }

    public String getContentIds() {
        return contentIds;
    }

    public void setContentIds(String contentIds) {
        if (contentIds == "") {
            contentIds = null;
        }
        this.contentIds = contentIds;
    }

    public Boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
}
