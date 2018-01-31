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

enum MessageType {
    error,
    registry,
    addCollection,
    removeCollection,
    updateProcessable,
    updateCollectionProperty
}

class SocketMessage {

    private MessageType type;

    SocketMessage(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }
}

@SuppressWarnings("unused")
class RemoveCollectionMessage extends SocketMessage {
    private Integer id;

    RemoveCollectionMessage(Integer id) {
        super(MessageType.removeCollection);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}

@SuppressWarnings("unused")
class RegistryMessage extends SocketMessage {

    RegistryMessage() {
        super(MessageType.registry);
    }
}

@SuppressWarnings("unused")
class UpdateCollectionMessage extends SocketMessage {

    UpdateCollectionMessage() {
        super(MessageType.updateCollectionProperty);
    }

}
@SuppressWarnings("unused")
class AddCollectionMessage extends SocketMessage {

    private Integer id;

    private String name;

    private Map<String, Object> properties;

    AddCollectionMessage(Integer id, String name, Map<String, Object> properties) {
        super(MessageType.addCollection);
        this.id = id;
        this.name = name;
        this.properties = properties;
    }

}

@SuppressWarnings("unused")
class ErrorMessage extends SocketMessage {
    private final int error;

    ErrorMessage(int errorCode) {
        super(MessageType.error);
        this.error = errorCode;
    }

}

@SuppressWarnings("unused")
class UpdateCollectionPropertyMessage extends SocketMessage {

    private Integer id;

    private String propertyName;

    private Object propertyValue;

    UpdateCollectionPropertyMessage(Integer id, String propertyName, Object propertyValue) {
        super(MessageType.updateCollectionProperty);
        this.id = id;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

}

@SuppressWarnings("unused")
class ProcessableMessage extends SocketMessage {

    private Integer id;

    private Integer collectionId;

    private String name;

    private String user;

    private String status;

    private Long createTime;

    private Long startTime;

    private Long endTime;

    private Long took;

    private String errorMessage;

    private String stackTrace;

    private Integer progress;

    private String progressText;

    private Map<String, String> properties;

    ProcessableMessage(MCRProcessable processable, Integer processableId,
        Integer collectionId) {
        super(MessageType.updateProcessable);

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
