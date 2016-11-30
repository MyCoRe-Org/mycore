package org.mycore.frontend.classeditor.mocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategoryLink;

public class CategoryLinkServiceMock implements MCRCategLinkService {

    @Override
    public Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category) {
        List<MCRCategory> categories;
        if (category == null) {
            categories = MCRCategoryDAOFactory.getInstance().getRootCategories();
        } else {
            categories = category.getChildren();
        }

        Map<MCRCategoryID, Boolean> linkMap = new HashMap<MCRCategoryID, Boolean>();
        int i = 0;
        for (MCRCategory mcrCategory : categories) {
            boolean haslink = false;
            if (i++ % 2 == 0) {
                haslink = true;
            }

            linkMap.put(mcrCategory.getId(), haslink);
        }
        return linkMap;
    }

    @Override
    public boolean hasLink(MCRCategory classif) {
        return false;
    }

    @Override
    public Map<MCRCategoryID, Number> countLinks(MCRCategory category, boolean childrenOnly) {
        return null;
    }

    @Override
    public Map<MCRCategoryID, Number> countLinksForType(MCRCategory category, String type, boolean childrenOnly) {
        return null;
    }

    @Override
    public Collection<String> getLinksFromCategory(MCRCategoryID id) {
        return null;
    }

    @Override
    public Collection<String> getLinksFromCategoryForType(MCRCategoryID id, String type) {
        return null;
    }

    @Override
    public void setLinks(MCRCategLinkReference objectReference, Collection<MCRCategoryID> categories) {
    }

    @Override
    public void deleteLinks(Collection<MCRCategLinkReference> ids) {
    }

    @Override
    public void deleteLink(MCRCategLinkReference id) {
    }

    @Override
    public boolean isInCategory(MCRCategLinkReference reference, MCRCategoryID id) {
        return false;
    }

    @Override
    public Collection<MCRCategoryID> getLinksFromReference(MCRCategLinkReference reference) {
        return null;
    }

    @Override
    public Collection<MCRCategLinkReference> getReferences(String type) {
        return null;
    }

    @Override
    public Collection<String> getTypes() {
        return null;
    }

    @Override
    public Collection<MCRCategoryLink> getLinks(String type) {
        return null;
    }
}
