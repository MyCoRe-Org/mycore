package org.mycore.parsers.bool;

import org.mycore.access.mcrimpl.MCRAccessData;

public interface MCRIPCondition extends MCRCondition<MCRAccessData> {
    public void set(String ip);
}
