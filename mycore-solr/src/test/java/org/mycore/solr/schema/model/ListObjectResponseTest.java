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

package org.mycore.solr.schema.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class ListObjectResponseTest extends TestCase {

    @Parameterized.Parameter(0)
    public String json;
    
    @Parameterized.Parameter(1)
    public ListObjectResponse expected;

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                """
                    {
                      "configSets":["_default"]
                    }
                    """, new ListObjectResponse(List.of("_default")),
            },
            {
                """
                    {
                      "configSets":["mycore_main", "mycore_classifications"]
                    }
                    """, new ListObjectResponse(List.of("mycore_main", "mycore_classifications")),
            },
        });
    }

    @Test
    public void testConfigSets() {
        ListObjectResponse actual = new Gson().fromJson(json, ListObjectResponse.class);
        assertEquals(expected, actual);
    }

}
