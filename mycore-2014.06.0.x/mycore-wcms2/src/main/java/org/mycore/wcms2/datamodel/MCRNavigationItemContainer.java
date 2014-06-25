package org.mycore.wcms2.datamodel;

import java.util.List;

public interface MCRNavigationItemContainer extends MCRNavigationBaseItem {

    public List<MCRNavigationBaseItem> getChildren();

}
