/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

/**
 * TestCase for Topological Sort
 * from: Cormen et.all "Introduction to Algorithms Section 22.4"
 *       Topological Sort, S. 550
 */
@MyCoReTest
public class MCRTopologicalSortTest {

    @Test
    public void dressOK() {
        MCRTopologicalSort<String> ts = new MCRTopologicalSort<>();
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
        assertNotNull(order, "Possible Dress Order created");
    }

    /**
     * TestCase for Topological Sort
     * from: Cormen et.all "Introduction to Algorithms Section 22.4"
     *       Topological Sort, S. 550
     */
    @Test
    public void dressWrong() {
        MCRTopologicalSort<String> ts = new MCRTopologicalSort<>();
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
        ts.addEdge(ts.getNodeID("shoes"), ts.getNodeID("socks"));

        int[] order = ts.doTopoSort();
        assertNull(order, "Dress Order was wrong");
        assertTrue(ts.toString().contains("[socks->shoes]"), "Circle list contains [socks->shoes]");
        assertTrue(ts.toString().contains("[shoes->socks]"), "Circle list contains [shoes->socks]");
    }
}
