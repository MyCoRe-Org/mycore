package org.mycore.common.events;

import org.mycore.common.MCRSession;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRSessionEvent {

    public enum Type {
        activated, created, destroyed, passivated
    }

    private Type type;

    private MCRSession session;

    private int concurrentAccessors;

    public MCRSessionEvent(MCRSession session, Type type, int concurrentAccessors) {
        this.session = session;
        this.type = type;
        this.concurrentAccessors = concurrentAccessors;
    }

    /**
     * Return how many threads accessed the session at time the event occured.
     */
    public int getConcurrentAccessors() {
        return concurrentAccessors;
    }

    /**
     * Return the MCRSession on which this event occured. 
     */
    public MCRSession getSession() {
        return session;
    }

    /**
     * Return the event type of this event. 
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        String sb = "MCRSessionEvent['" + getSession() + "'," + getType() + "," + getConcurrentAccessors() + "]'";
        return sb;
    }

}
