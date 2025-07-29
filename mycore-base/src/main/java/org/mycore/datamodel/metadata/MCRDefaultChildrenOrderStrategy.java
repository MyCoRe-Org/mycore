package org.mycore.datamodel.metadata;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mycore.access.MCRAccessException;

public class MCRDefaultChildrenOrderStrategy implements MCRChildrenOrderStrategy {

    @Override
    public List<MCRObjectID> getChildOrder(MCRObject parent) {
        List<MCRObjectID> childrenOrder = parent.getStructure().getChildrenOrder();
        Map<MCRObjectID, Integer> childrenOrderMap = new HashMap<>();

        for (int i = 0; i < childrenOrder.size(); i++) {
            childrenOrderMap.put(childrenOrder.get(i), i);
        }

        return MCRMetadataManager.getChildren(parent.getId())
            .stream()
            .sorted(Comparator.comparing((MCRObjectID o) -> childrenOrderMap.getOrDefault(o, Integer.MAX_VALUE))
                .thenComparing(MCRObjectID::compareTo))
            .toList();
    }

    @Override
    public void setChildOrder(MCRObject parent, List<MCRObjectID> newOrder) throws MCRAccessException {
        parent.getStructure().setChildrenOrder(newOrder);
        MCRMetadataManager.update(parent);
    }
}
