/**
 *
 */
package org.mycore.common.log4j2;

import org.apache.logging.log4j.ThreadContext;
import org.mycore.common.MCRSession;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

/**
 * Adds MCRSession information to the current {@link ThreadContext}.
 *
 * <dl>
 * <dt>loginId</dt><dd>current user id</dd>
 * <dt>ipAddress</dt><dd>see {@link MCRSession#getCurrentIP()}</dd>
 * <dt>mcrSession</dt><dd>see {@link MCRSession#getID()}</dd>
 * <dt>language</dt><dd>see {@link MCRSession#getCurrentLanguage()}</dd>
 * </dl>
 *
 * @author Thomas Scheffler (yagee)
 * @see <a href="https://logging.apache.org/log4j/2.x/manual/thread-context.html">ThreadContext description</a>
 * @see ThreadContext
 */
public class MCRSessionThreadContext implements MCRSessionListener {

    @Override
    public void sessionEvent(MCRSessionEvent event) {
        switch (event.getType()) {
            case activated:
                ThreadContext.put("ipAddress", event.getSession().getCurrentIP());
                ThreadContext.put("loginId", event.getSession().getUserInformation().getUserID());
                ThreadContext.put("mcrSession", event.getSession().getID());
                ThreadContext.put("language", event.getSession().getCurrentLanguage());
                break;
            case passivated:
                ThreadContext.clearMap();
                break;

            default:
                break;
        }
    }

}
