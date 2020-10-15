package org.mycore.common.events2;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.UUID;

public class MCREvent {

    protected MCREvent(){
        this.id = UUID.randomUUID();
        this.created = LocalDateTime.now();
        this.type = getClass();
    }

    public UUID getId() {
        return id;
    }

    public Class getType() {
        return type;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    private UUID id;
    private LocalDateTime created;
    private Class type;
}
