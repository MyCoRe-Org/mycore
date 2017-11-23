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

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class MCRStreamUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        //see https://commons.wikimedia.org/wiki/File%3ASorted_binary_tree_preorder.svg for tree image
        String[] nodes = { "F", "B", "A", "D", "C", "E", "G", "I", "H" };
        HashMap<String, String[]> children = new HashMap<>();
        children.put("F", new String[] { "B", "G" });
        children.put("B", new String[] { "A", "D" });
        children.put("D", new String[] { "C", "E" });
        children.put("G", new String[] { "I" });
        children.put("I", new String[] { "H" });
        children.put("A", new String[0]);
        children.put("C", new String[0]);
        children.put("E", new String[0]);
        children.put("H", new String[0]);
        ArrayList<String> sortedNodes = new ArrayList<>(Arrays.asList(nodes));
        sortedNodes.sort(String.CASE_INSENSITIVE_ORDER);
        String[] nodesPreOrder = MCRStreamUtils
            .flatten("F", ((Function<String, String[]>) children::get).andThen(Arrays::asList),
                Collection::parallelStream)
            .collect(Collectors.toList())
            .toArray(new String[nodes.length]);
        assertEquals("Node count differs", nodes.length, nodesPreOrder.length);
        for (int i = 0; i < nodes.length; i++) {
            assertEquals("unexpected node", nodes[i], nodesPreOrder[i]);
        }
    }

}
