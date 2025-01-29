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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class MCRCacheTest extends MCRTestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void put() {
        MCRCache<String, String> cache = new MCRCache<>(4, "Small Sample Program");
        LOGGER.info(cache);
        System.out.println(cache);
        cache.put("a", "Anton");
        cache.put("b", "Bohnen");
        cache.put("c", "Cache");
        LOGGER.info(cache);
        cache.get("d");
        cache.get("c");
        cache.put("d", "Dieter");
        cache.put("e", "Egon");
        cache.put("f", "Frank");
        cache.get("c");
        LOGGER.info(cache);
        // TODO @Thomas: Check capacity, size, fill rate and hitrate
    }

}
