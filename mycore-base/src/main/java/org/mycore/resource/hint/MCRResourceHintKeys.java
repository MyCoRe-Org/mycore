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

import java.io.File;

import org.mycore.common.hint.MCRHintKey;
import org.mycore.resource.MCRResourceResolver;

import jakarta.servlet.ServletContext;

/**
 * A utility class that provides {@link MCRHintKey} used by {@link MCRResourceResolver#defaultHints()}.
 */
public final class MCRResourceHintKeys {

    public static final MCRHintKey<File> CONFIG_DIR = new MCRHintKey<>(
        File.class,
        "CONFIG_DIR",
        File::getAbsolutePath
    );

    public static final MCRHintKey<ClassLoader> CLASS_LOADER = new MCRHintKey<>(
        ClassLoader.class,
        "CLASS_LOADER",
        ClassLoader::getName
    );

    public static final MCRHintKey<ServletContext> SERVLET_CONTEXT = new MCRHintKey<>(
        ServletContext.class,
        "SERVLET_CONTEXT",
        ServletContext::getServletContextName
    );

    public static final MCRHintKey<File> WEBAPP_DIR = new MCRHintKey<>(
        File.class,
        "WEBAPP_DIR",
        File::getAbsolutePath
    );

    private MCRResourceHintKeys(){
    }

}
