/*
 * 
 * $Revision: 28695 $ 
 * $Date: 2013-12-19 09:38:31 +0100 (Do, 19 Dez 2013) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

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
public class MCRTopologicalSort {
    private static final Logger LOGGER = LogManager.getLogger(MCRTopologicalSort.class);

    /** store the edges as adjacent list
     *  for each target node a list of corresponding source node is stored 
     */
    Map<Integer, TreeSet<Integer>> edgeSources = new TreeMap<>();

    BiMap<Integer, String> nodes = HashBiMap.create();

    boolean dirty = false;

    /**
     * executes the example code
     */
    public static void main(String[] args) {
        example1();
        example2();
        example3();
    }

    // Example: Cormen et.all "Introduction to Algorithms Section 22.4"
    // Topological Sort, S. 550
    private static void example1() {
        MCRTopologicalSort ts = new MCRTopologicalSort();
        ts.addNode("belt");
        ts.addNode("jacket");
        ts.addNode("pants");
        ts.addNode("shirt");
        ts.addNode("shoes");
        ts.addNode("socks");
        ts.addNode("tie");
        ts.addNode("undershorts");
        ts.addNode("watch");

        ts.addEdge(ts.getNodeID("undershorts"), ts.getNodeID("pants"));
        ts.addEdge(ts.getNodeID("undershorts"), ts.getNodeID("shoes"));
        ts.addEdge(ts.getNodeID("socks"), ts.getNodeID("shoes"));
        ts.addEdge(ts.getNodeID("pants"), ts.getNodeID("belt"));
        ts.addEdge(ts.getNodeID("belt"), ts.getNodeID("jacket"));
        ts.addEdge(ts.getNodeID("shirt"), ts.getNodeID("belt"));
        ts.addEdge(ts.getNodeID("shirt"), ts.getNodeID("tie"));
        ts.addEdge(ts.getNodeID("tie"), ts.getNodeID("jacket"));

        int[] order = ts.doTopoSort();
        if (order == null) {
            System.out.println("An error occured!");
        } else {
            for (int x : order) {
                System.out.print(ts.getNodeName(x) + " <- ");
            }
        }
        System.out.println();
    }

    //Example: random document IDs with random parent connections
    private static void example2() {
        MCRTopologicalSort ts = new MCRTopologicalSort();
        int count = 0;
        for (int i = 1; i < 100000; i++) {
            String from = "Docportal_document_" + String.format(Locale.ROOT, "%10d", i);
            if (i > 10 && Math.random() * 20d < 1d) {
                ++count;
                String to = "Docportal_document_"
                    + String.format(Locale.ROOT, "%010d", Math.round((Math.random() * i / 20)) + 1);
                ts.addNode(from);
                ts.addNode(to);

                ts.addEdge(ts.getNodeID(from), ts.getNodeID(to));
            } else {
                ts.addNode(from);
            }
        }
        System.out.println(count);
        long start = System.currentTimeMillis();
        int[] order = ts.doTopoSort();
        if (order == null) {
            System.out.println("An error occured!");
        } else {
            for (int i = 0; i < order.length; i++) {
                // System.out.print(order[i] + " <- ");
            }
        }
        System.out.println();
        System.out.println();
        System.out.println("Runtime: " + (System.currentTimeMillis() - start) / 1000 + " s");
    }

    //example with real MyCoRe XML files
    private static void example3() {
        File baseDir = new File("c:\\temp\\rosdok_data");
        String[] files = baseDir.list();
        MCRTopologicalSort ts = new MCRTopologicalSort();
        long start = System.currentTimeMillis();
        ts.prepareData(files, baseDir);
        System.out.println("Preparation time: " + (System.currentTimeMillis() - start) / 1000 + " s");

        start = System.currentTimeMillis();
        int[] order = ts.doTopoSort();
        System.out.println("Runtime: " + (System.currentTimeMillis() - start) / 1000 + " s");
        System.out.println("Array-length:" + files.length + " / " + order.length);
        if (order != null) {
            for (int i : order) {
                System.out.println(String.format(Locale.ROOT, "%04d", i) + ": " + files[i]);
            }
        }
    }

    /**
     * parses MCRObject xml files for parent links
     * and creates the graph
     *
     * uses StAX cursor API (higher performance)
     */
    public void prepareData(String[] files, File dir) {
        nodes = HashBiMap.create(files.length);
        edgeSources.clear();

        String file = null;
        Map<Integer, List<String>> parentNames = new HashMap<Integer, List<String>>();
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        for (int i = 0; i < files.length; i++) {
            file = files[i];

            try (FileInputStream fis = new FileInputStream(new File(dir, file))) {
                XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(fis);
                while (xmlStreamReader.hasNext()) {
                    switch (xmlStreamReader.getEventType()) {
                        case XMLStreamConstants.START_ELEMENT:
                            if (xmlStreamReader.getLocalName().equals("mycoreobject")) {
                                nodes.forcePut(i, xmlStreamReader
                                    .getAttributeValue(null, "ID"));
                            } else {
                                String href = xmlStreamReader
                                    .getAttributeValue("http://www.w3.org/1999/xlink", "href");
                                if (xmlStreamReader.getLocalName().equals("parent")) {
                                    List<String> dependencyList = parentNames.computeIfAbsent(i,
                                        e -> new ArrayList<String>());
                                    dependencyList.add(
                                        href);
                                } else if (xmlStreamReader.getLocalName().equals("relatedItem")) {
                                    if (MCRObjectID.isValid(
                                        href)) {
                                        List<String> dependencyList = parentNames
                                            .computeIfAbsent(i, e -> new ArrayList<String>());
                                        dependencyList.add(
                                            href);
                                    }
                                } else if (xmlStreamReader.getLocalName().equals("metadata")) {
                                    break;
                                }
                            }
                            break;

                        case XMLStreamConstants.END_ELEMENT:
                            if (xmlStreamReader.getLocalName().equals("parents")) {
                                break;
                            } else if (xmlStreamReader.getLocalName().equals("relatedItem")) {
                                break;
                            }
                            break;
                    }
                    xmlStreamReader.next();
                }

            } catch (XMLStreamException | IOException e) {
                e.printStackTrace();
            }
        }

        //build edges
        for (int source : parentNames.keySet()) {
            parentNames.get(source)
                .stream()
                .map(nodes.inverse()::get)
                .filter(Objects::nonNull)
                .forEach(target -> addEdge(source, target));
        }

        dirty = false;
    }

    /**
     * reads MCRObjectIDs, retrieves parent links from MCRLinkTableManager
     * and creates the graph
     *
     * uses StAX cursor API (higher performance)
     */
    public void prepareMCRObjects(String[] mcrids) {
        nodes = HashBiMap.create(mcrids.length);
        edgeSources.clear();

        for (int i = 0; i < mcrids.length; i++) {
            nodes.forcePut(i, mcrids[i]);
        }
        for (int i = 0; i < mcrids.length; i++) {
            Collection<String> parents = MCRLinkTableManager.instance().getDestinationOf(mcrids[i], "parent");
            for (String p : parents) {
                Integer target = nodes.inverse().get(p);
                if (target != null) {
                    addEdge(i, target);
                }
            }
            Collection<String> refs = MCRLinkTableManager.instance().getDestinationOf(mcrids[i], "reference");
            for (String r : refs) {
                Integer target = nodes.inverse().get(r);
                if (target != null) {
                    addEdge(i, target);
                }
            }
        }
        dirty = false;
    }

    /**
     * add a node to the graph
     * @param name - the node name
     */

    public void addNode(String name) {
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
    public Integer getNodeID(String name) {
        return nodes.inverse().get(name);
    }

    /**
     * return the name of the given node
     * @param id - the node id
     * @return the node name
     */
    public String getNodeName(Integer id) {
        return nodes.get(id);
    }

    /**
     *  add an edge to the graph
     * @param from - the source node
     * @param to - the target node
     */
    public void addEdge(Integer from, Integer to) {
        TreeSet<Integer> ts = edgeSources.get(to);
        if (ts == null) {
            ts = new TreeSet<Integer>();
            edgeSources.put(to, ts);
        }
        ts.add(from);
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
                "The data of this instance is inconsistent. Please call prepareData() again or start with a new instance!");
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
            for (Integer to : new TreeSet<Integer>(edgeSources.keySet())) {
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
            LOGGER.error("The input contained circular dependencies: \n" + toString());
            return null;
            // else return L (a topologically sorted order)
        } else {
            return result;
        }
    }

    /**
     * @return a string representation of the underlying graph
     */
    public String toString() {
        StringBuffer result = new StringBuffer("[");
        for (Integer to : edgeSources.keySet()) {
            for (Integer from : edgeSources.get(to)) {
                result.append("[" + nodes.get(from) + "->" + nodes.get(to) + "]");
            }
        }
        result.append("]");
        return result.toString();
    }
}
