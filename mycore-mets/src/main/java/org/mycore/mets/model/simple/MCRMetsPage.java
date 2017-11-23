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
