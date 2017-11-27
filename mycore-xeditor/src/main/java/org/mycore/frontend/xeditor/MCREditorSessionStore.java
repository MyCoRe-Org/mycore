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

package org.mycore.frontend.xeditor;

import java.util.concurrent.atomic.AtomicInteger;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSessionStore {

    public static final String XEDITOR_SESSION_PARAM = "_xed_session";

    private MCRCache<String, MCREditorSession> cachedSessions;

    private AtomicInteger idGenerator = new AtomicInteger(0);

    MCREditorSessionStore() {
        int maxEditorsInSession = MCRConfiguration.instance().getInt("MCR.XEditor.MaxEditorsInSession", 50);
        cachedSessions = new MCRCache<>(maxEditorsInSession, "Stored XEditor Sessions");
    }

    public void storeSession(MCREditorSession session) {
        String id = String.valueOf(idGenerator.incrementAndGet());
        cachedSessions.put(id, session);
        session.setID(id);
    }

    public MCREditorSession getSession(String id) {
        return cachedSessions.get(id);
    }
}
