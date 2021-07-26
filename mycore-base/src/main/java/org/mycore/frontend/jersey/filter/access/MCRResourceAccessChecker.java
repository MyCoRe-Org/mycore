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

package org.mycore.frontend.jersey.filter.access;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public interface MCRResourceAccessChecker {

    boolean isPermitted(ContainerRequestContext request);

    /**
     * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string?answertab=votes#tab-top
     * 
     * @param is the inputstream to read
     * @return the string
     */
    default String convertStreamToString(InputStream is) {
        try (Scanner s = new Scanner(is, StandardCharsets.UTF_8)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

}
