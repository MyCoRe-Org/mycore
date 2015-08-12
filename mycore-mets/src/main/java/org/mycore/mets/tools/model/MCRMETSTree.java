package org.mycore.mets.tools.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

/**
 * Contains all fields that are necessary for the serialization of the Tree
 * 
 * @author Sebastian Hofmann
 */
public class MCRMETSTree {

    protected MCRMETSTree() {
        identifier = "id";
        label = "name";
        items = new ArrayList<MCRMETSNode>();
    }

    protected String identifier;

    protected String label;

    protected List<MCRMETSNode> items;

    /**
     * Serializes the whole tree including children with {@link Gson}.
     * @return the generated JSON.
     */
    public String toJSon() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }

    public void add(MCRMETSNode item) {
        items.add(item);
    }
}
