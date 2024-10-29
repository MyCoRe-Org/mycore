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

package org.mycore.common.log;

import java.util.List;

/**
 * A {@link MCRListMessage} is a list-like data structure that holds string values in
 * its entries that can be rendered as a comprehensible representation of that list.
 * <p>
 * Intended to create log messages that reveal linear data.
 * <p>
 * Example output:
 * <pre>
 * ├─ Foo: foo
 * ├─ Bar: bar
 * └─ Baz: baz
 * </pre>
 */
public final class MCRListMessage {

    private final MCRTreeMessage message = new MCRTreeMessage();

    public void add(String key, String value) {
        message.add(key, value);
    }

    public String logMessage(String introduction) {
        return message.logMessage(introduction);
    }

    public List<String> listLines() {
        return message.treeLines();
    }

}
