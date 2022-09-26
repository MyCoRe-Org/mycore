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

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * TestCase for Topological Sort
 * from: Cormen et.all "Introduction to Algorithms Section 22.4"
 *       Topological Sort, S. 550
 */
public class MCRTopologicalSortTest extends MCRTestCase {

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
        Assert.assertNotNull("Possible Dress Order created", order);
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
        Assert.assertNull("Dress Order was wrong", order);
        Assert.assertTrue("Circle list contains [socks->shoes]", ts.toString().contains("[socks->shoes]"));
        Assert.assertTrue("Circle list contains [shoes->socks]", ts.toString().contains("[shoes->socks]"));
    }
}
