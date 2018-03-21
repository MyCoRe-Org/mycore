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

package org.mycore.webtools.processing.socket.impl;

import java.util.Map;
import java.util.stream.Collectors;

import org.mycore.common.MCRUtils;
import org.mycore.common.processing.MCRProcessable;

/**
 * @author Sebastian Hofmann
 */
class MCRProcessableMessage extends MCRWebSocketMessage {

    public Integer id;

    public Integer collectionId;

    public String name;

    public String user;

    public String status;

    public Long createTime;

    public Long startTime;

    public Long endTime;

    public Long took;

    public String errorMessage;

    public String stackTrace;

    public Integer progress;

    public String progressText;

    public Map<String, String> properties;

    MCRProcessableMessage(MCRProcessable processable, Integer processableId,
        Integer collectionId) {
        super(MCRMessageType.updateProcessable);

        this.id = processableId;
        this.collectionId = collectionId;

        this.name = processable.getName();
        this.user = processable.getUserId();
        this.status = processable.getStatus().toString();
        this.createTime = processable.getCreateTime().toEpochMilli();

        if (!processable.isCreated()) {
            this.startTime = processable.getStartTime().toEpochMilli();
            if (processable.isDone()) {
                this.endTime = processable.getEndTime().toEpochMilli();
                this.took = processable.took().toMillis();
            }
        }
        if (processable.isFailed()) {
            Throwable error = processable.getError();
            this.errorMessage = error.getMessage();
            this.stackTrace = MCRUtils.getStackTraceAsString(error);
        }
        if (processable.getProgress() != null) {
            this.progress = processable.getProgress();
        }
        if (processable.getProgressText() != null) {
            this.progressText = processable.getProgressText();
        }
        this.properties = processable.getProperties().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toString()));
    }

}
