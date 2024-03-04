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

package org.mycore.ocfl.metadata.migration;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;

import java.io.IOException;
import java.util.Date;

public abstract class MCROCFLRevision {

    private final MCROCFLVersionType type;

    MCROCFLMigration.ContentSupplier contentSupplier;

    String user;

    Date date;

    MCRObjectID objectID;

    MCROCFLRevision(MCROCFLVersionType type, MCROCFLMigration.ContentSupplier contentSupplier, String user, Date date,
                    MCRObjectID objectID) {
        this.type = type;
        this.contentSupplier = contentSupplier;
        this.user = user;
        this.date = date;
        this.objectID = objectID;
    }

    public abstract void execute(MCROCFLXMLMetadataManager metadataManager) throws IOException;

    public MCROCFLMigration.ContentSupplier getContentSupplier() {
        return contentSupplier;
    }

    public Date getDate() {
        return date;
    }

    public MCRObjectID getObjectID() {
        return objectID;
    }

    public String getUser() {
        return user;
    }

    public MCROCFLVersionType getType() {
        return type;
    }


    @Override
    public String toString() {
        return "MCROCFLRevision{" +
                "type=" + type +
                ", user='" + user + '\'' +
                ", date=" + date +
                ", objectID=" + objectID +
                '}';
    }
}
