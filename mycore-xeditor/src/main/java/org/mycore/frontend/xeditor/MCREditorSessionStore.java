/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.xeditor;

import java.util.concurrent.atomic.AtomicInteger;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSessionStore {

    private MCRCache<String, MCREditorSession> cachedSessions;

    private AtomicInteger idGenerator = new AtomicInteger(0);

    MCREditorSessionStore() {
        int maxEditorsInSession = MCRConfiguration.instance().getInt("MCR.XEditor.MaxEditorsInSession", 50);
        cachedSessions = new MCRCache<String, MCREditorSession>(maxEditorsInSession, "Stored XEditor Sessions");
    }

    public void storeSession(MCREditorSession session) {
        String id = String.valueOf(idGenerator.incrementAndGet());
        cachedSessions.put(id, session);
        session.setID(id);
    }

    public MCREditorSession getSession(String id) {
        return cachedSessions.get(id);
    }

    public final static String XEDITOR_SESSION_PARAM = "_xed_session";
}
