package org.mycore.pi;

import java.io.Serializable;
import java.util.Date;

public interface MCRPIRegistrationInfo extends Serializable {
    String getIdentifier();

    String getType();

    String getMycoreID();

    String getAdditional();

    String getMcrVersion();

    int getMcrRevision();

    Date getRegistered();

    Date getCreated();

    String getService();
}
