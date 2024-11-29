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

package org.mycore.common.events;

import java.util.Optional;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;

public class MCRServletContextHolder implements ServletContextListener {

    private static final MCRServletContextHolder SINGLETON = new MCRServletContextHolder();

    private Optional<ServletContext> context = Optional.empty();

    private MCRServletContextHolder() {
    }

    public static MCRServletContextHolder instance() {
        return SINGLETON;
    }

    public Optional<ServletContext> get() {
        return context;
    }

    void set(ServletContext context) {
        this.context = Optional.ofNullable(context);
    }

}
