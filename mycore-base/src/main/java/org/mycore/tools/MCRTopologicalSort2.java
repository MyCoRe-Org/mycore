/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.tools;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This class implements an algorithm for topological ordering.
 * It can be used to retrieve the order in which MyCoRe object can be imported to be
 * sure that parent objects are imported first.
 *
 * It also checks for circular dependencies and will throw an exception if it occurs.
 *
 * The doTopoSort() method can only be called once, since it processes the internal data.
 * Afterwards prepareData() must be called again or a new object has to be used.
 *
 * For performance reasons each node label will be mapped to an integer (position in node list)
 *
 * The algorithm is described in
 * http://en.wikipedia.org/wiki/Topological_sorting
 *
 * @author Robert Stephan
 * @version $Revision: 28688 $ $Date: 2013-12-18 15:27:20 +0100 (Mi, 18 Dez 2013) $
 *
 */
public class MCRTopologicalSort2<T> {
    private static final Logger LOGGER = LogManager.getLogger(MCRTopologicalSort2.class);

    /** store the edges as adjacent list
     *  for each target node a list of corresponding source node is stored
     */
    Map<Integer, TreeSet<Integer>> edgeSources = new TreeMap<>();

    BiMap<Integer, T> nodes = HashBiMap.create();

    boolean dirty = false;

    /**
     * add a node to the graph
     * @param name - the node name
     */

    public void addNode(T name) {
        if (!nodes.containsValue(name)) {
            nodes.put(nodes.size(), name);
        }
    }

    /**
     * returns a node id for a given node
     *
     * @param name - the node name
     * @return the node id
     */
    public Integer getNodeID(T name) {
        return nodes.inverse().get(name);
    }

    /**
     * return the name of the given node
     * @param id - the node id
     * @return the node name
     */
    public T getNodeName(Integer id) {
        return nodes.get(id);
    }

    /**
     *  add an edge to the graph
     * @param from - the source node
     * @param to - the target node
     */
    public void addEdge(Integer from, Integer to) {
        edgeSources.computeIfAbsent(to, k -> new TreeSet<>()).add(from);
    }

    /**
     * removes an edge from grapn
     *
     * @param from - the source node id
     * @param to - the target node id
     * @return true, if there are no more incoming edges on the [to]-node
     *               ([to] = leaf node)
     */
    public boolean removeEdge(Integer from, Integer to) {
        TreeSet<Integer> ts = edgeSources.get(to);
        if (ts != null) {
            ts.remove(from);
            if (ts.isEmpty()) {
                edgeSources.remove(to);
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * based upon first pseudo code in
     * http://en.wikipedia.org/w/index.php?title=Topological_sorting&amp;oldid=611829125
     *
     * The algorithm will destroy the input data -&gt; the method can only be called once
     *
     * @return an array of ids which define the order
     *         in which the elements have to be retrieved from the given input list
     *         or null if an error occured
     */
    public int[] doTopoSort() {
        if (dirty) {
            LOGGER.error(
                "The data of this instance is inconsistent."
                    + " Please call prepareData() again or start with a new instance!");
            return null;
        }
        dirty = true;
        // L <-array that will contain the sorted elements
        int[] result = new int[nodes.size()];
        // S <- Set of all nodes with no incoming edges
        List<Integer> leafs = nodes.keySet()
            .stream()
            .filter(i -> !edgeSources.containsKey(i))
            .sorted()
            .collect(Collectors.toList());
        int cursor = result.length - 1;

        // while S is non-empty do
        while (!leafs.isEmpty()) {
            // remove a node n from S
            Integer node = leafs.remove(0);
            // add n to tail of L (we use head, because we need an inverted list !!)
            result[cursor--] = node;
            // for each node m with an edge e from n to m do
            for (Integer to : new TreeSet<>(edgeSources.keySet())) {
                Set<Integer> ts = edgeSources.get(to);
                if (ts != null && ts.contains(node)) {
                    // remove edge e from the graph
                    if (removeEdge(node, to)) {
                        // if m has no other incoming edges then insert m  into S
                        leafs.add(to);
                    }
                }
            }
        }
        // if graph has edges then return error (graph has at least one cycle)
        if (!edgeSources.isEmpty()) {
            LOGGER.error("The input contained circular dependencies: \n{}", toString());
            return null;
            // else return L (a topologically sorted order)
        } else {
            return result;
        }
    }

    /**
     * @return a string representation of the underlying graph
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[");
        for (Integer to : edgeSources.keySet()) {
            for (Integer from : edgeSources.get(to)) {
                result.append('[').append(nodes.get(from)).append("->").append(nodes.get(to)).append(']');
            }
        }
        result.append(']');
        return result.toString();
    }
}
