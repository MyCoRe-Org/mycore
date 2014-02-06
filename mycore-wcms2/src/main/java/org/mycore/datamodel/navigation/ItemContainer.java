package org.mycore.datamodel.navigation;

import java.util.List;

public interface ItemContainer extends NavigationItem {

    public List<NavigationItem> getChildren();

}
