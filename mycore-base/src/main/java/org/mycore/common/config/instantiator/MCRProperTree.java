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

package org.mycore.common.config.instantiator;

import java.util.Map;
import java.util.stream.Stream;

import org.mycore.common.log.MCRTreeMessage;

/**
 * A {@link MCRProperTree} is an immutable tree of configuration properties with dot-separated keys
 * that is used by {@link MCRInstantiator} during instantiation of configured classes.
 * <p>
 * The value of a tree node can be obtained with {@link MCRProperTree#value()}. Nested tree nodes
 * can be obtained with {@link MCRProperTree#nested(String)} and {@link MCRProperTree#deeplyNested(String)}.
 * The first method expects a single-level-key like <code>Foo</code>, while the latter method supports nested
 * keys like <code>Foo.Bar.Baz</code>.
 */
public interface MCRProperTree {

    String value();

    Stream<String> keys();

    MCRProperTree nested(String key);

    MCRProperTree deeplyNested(String key);

    MCRProperTree excludeKey(String classKey);

    Map<String, String> toProperties();

    MCRTreeMessage toTreeMessage();

}
