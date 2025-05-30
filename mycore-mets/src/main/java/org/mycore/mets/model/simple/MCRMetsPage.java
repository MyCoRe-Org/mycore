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

package org.mycore.mets.model.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public MCRMetsPage() {
        this.fileList = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<MCRMetsFile> getFileList() {
        return fileList;
    }

    public String getOrderLabel() {
        return orderLabel;
    }

    public void setOrderLabel(String orderLabel) {
        this.orderLabel = Objects.equals(orderLabel, "") ? null : orderLabel;
    }

    public String getContentIds() {
        return contentIds;
    }

    public void setContentIds(String contentIds) {
        this.contentIds = Objects.equals(contentIds, "") ? null : contentIds;

    }

    public Boolean isHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }
}
