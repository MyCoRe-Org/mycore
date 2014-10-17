package org.mycore.frontend.classeditor.mocks;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.classeditor.json.MCRJSONCategory;

public class CategoryDAOMock implements MCRCategoryDAO {
    HashMap<MCRCategoryID, MCRCategory> categMap = null;

    HashMap<MCRCategoryID, MCRCategory> rootCategMap = null;

    private void buildTestCategs() {
        MCRJSONCategory root_01 = createCategory("rootID_01", "", null);
        MCRJSONCategory root_02 = createCategory("rootID_02", "", null);
        MCRJSONCategory categ_01 = createCategory("rootID_01", "categ_01", null);
        MCRJSONCategory categ_02 = createCategory("rootID_01", "categ_02", null);

        List<MCRCategory> children = new ArrayList<MCRCategory>();
        children.add(categ_01);
        children.add(categ_02);
        root_01.setChildren(children);

        rootCategMap.put(root_01.getId(), root_01);
        rootCategMap.put(root_02.getId(), root_02);
        categMap.put(root_01.getId(), root_01);
        categMap.put(root_02.getId(), root_02);
        categMap.put(categ_01.getId(), categ_01);
        categMap.put(categ_02.getId(), categ_02);
    }

    public void init() {
        categMap = new HashMap<MCRCategoryID, MCRCategory>();
        rootCategMap = new HashMap<MCRCategoryID, MCRCategory>();
        buildTestCategs();
    }

    public Set<MCRCategoryID> getIds() {
        return categMap.keySet();
    }

    public Collection<MCRCategory> getCategs() {
        return categMap.values();
    }

    private MCRJSONCategory createCategory(String rootID, String categID, MCRCategoryID parentID) {
        MCRCategoryID id = new MCRCategoryID(rootID, categID);
        Set<MCRLabel> labels = new HashSet<MCRLabel>();
        labels.add(new MCRLabel("de", id + "_text", id + "_descr"));
        labels.add(new MCRLabel("en", id + "_text", id + "_descr"));
        MCRJSONCategory newCategory = new MCRJSONCategory();
        newCategory.setId(id);
        newCategory.setLabels(labels);
        newCategory.setParentID(parentID);
        return newCategory;
    }

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category) {
        categMap.put(category.getId(), category);
        return categMap.get(parentID);
    }

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        categMap.put(category.getId(), category);
        return categMap.get(parentID);
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCRCategory mcrCategory = categMap.get(id);
        for (MCRCategory child : mcrCategory.getChildren()) {
            categMap.remove(child.getId());
        }

        categMap.remove(id);
    }

    @Override
    public boolean exist(MCRCategoryID id) {
        return categMap.containsKey(id);
    }

    @Override
    public List<MCRCategory> getCategoriesByLabel(MCRCategoryID baseID, String lang, String text) {
        return null;
    }

    @Override
    public MCRCategory getCategory(MCRCategoryID id, int childLevel) {
        return categMap.get(id);
    }

    @Override
    public List<MCRCategory> getChildren(MCRCategoryID id) {
        return new ArrayList<MCRCategory>();
    }

    @Override
    public List<MCRCategory> getParents(MCRCategoryID id) {
        return null;
    }

    @Override
    public List<MCRCategoryID> getRootCategoryIDs() {
        return null;
    }

    @Override
    public List<MCRCategory> getRootCategories() {
        return new ArrayList<MCRCategory>(rootCategMap.values());
    }

    @Override
    public MCRCategory getRootCategory(MCRCategoryID baseID, int childLevel) {
        return null;
    }

    @Override
    public boolean hasChildren(MCRCategoryID id) {
        return false;
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID) {
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        return categMap.get(id);
    }

    @Override
    public Collection<MCRCategory> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        if (!categMap.containsKey(newCategory.getId())) {
            throw new IllegalArgumentException();
        }
        categMap.put(newCategory.getId(), newCategory);
        return categMap.values();
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        return categMap.get(id);
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, Set<MCRLabel> labels) {
        return categMap.get(id);
    }

    @Override
    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        return categMap.get(id);
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public List<MCRCategory> getCategoriesByLabel(String lang, String text) {
        return null;
    }

    @Override
    public long getLastModified(String root) {
        return 0;
    }
}
