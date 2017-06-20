package org.mycore.mets.model.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MCRMetsPage {
    private String id = UUID.randomUUID().toString();

    private String orderLabel;

    private String contentIds;

    private Boolean hidden;

    private List<MCRMetsFile> fileList;

    public MCRMetsPage(String orderLabel, String contentIds) {
        this();
        this.orderLabel = orderLabel;
        this.contentIds = contentIds;
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
