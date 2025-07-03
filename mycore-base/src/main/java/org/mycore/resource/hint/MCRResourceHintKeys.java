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

package org.mycore.resource.hint;

import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;

import org.mycore.common.config.MCRComponent;
import org.mycore.common.hint.MCRCollectionHintKey;
import org.mycore.common.hint.MCRHintKey;
import org.mycore.resource.MCRResourceResolver;

import jakarta.servlet.ServletContext;

/**
 * A utility class that provides {@link MCRHintKey} used by {@link MCRResourceResolver#defaultHints()}.
 */
public final class MCRResourceHintKeys {

    public static final MCRHintKey<Path> CONFIG_DIR = new MCRHintKey<>(
        Path.class,
        MCRResourceHintKeys.class,
        "CONFIG_DIR",
        path -> path.toAbsolutePath().toString());

    public static final MCRHintKey<ClassLoader> CLASS_LOADER = new MCRHintKey<>(
        ClassLoader.class,
        MCRResourceHintKeys.class,
        "CLASS_LOADER",
        ClassLoader::getName);

    public static final MCRHintKey<SortedSet<MCRComponent>> COMPONENTS = new MCRCollectionHintKey<>(
        MCRComponent.class,
        MCRResourceHintKeys.class,
        "COMPONENTS",
        Object::toString,
        List.of(collection -> {
            if (collection.size() > 1 && collection.getFirst().getPriority() < collection.getLast().getPriority()) {
                throw new IllegalArgumentException("Components must be ordered from highest to lowest priority");
            }
        }));

    public static final MCRHintKey<ServletContext> SERVLET_CONTEXT = new MCRHintKey<>(
        ServletContext.class,
        MCRResourceHintKeys.class,
        "SERVLET_CONTEXT",
        ServletContext::getServletContextName);

    public static final MCRHintKey<Path> WEBAPP_DIR = new MCRHintKey<>(
        Path.class,
        MCRResourceHintKeys.class,
        "WEBAPP_DIR",
        path -> path.toAbsolutePath().toString());

    private MCRResourceHintKeys() {
    }

}
