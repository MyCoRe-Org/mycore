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

package org.mycore.common.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

/**
 * Can be used to write test against the {@link MCRURIResolver}.
 * Just add MCR.URIResolver.ModuleResolver.YOUR_PREFIX with MCRMockResolver.class.getName() to the overwritten
 * {@link org.mycore.common.MCRTestCase} getTestProperties()
 */
public class MCRMockResolver implements URIResolver {

    private static final List<MCRMockResolverCall> CALLS = new ArrayList<>();

    private static Source resultSource = null;

    public static List<MCRMockResolverCall> getCalls() {
        return Collections.unmodifiableList(CALLS);
    }

    public static void clearCalls() {
        CALLS.clear();
    }

    public static void setResultSource(Source source) {
        resultSource = source;
    }

    public static Source getResultSource() {
        return resultSource;
    }

    @Override
    public Source resolve(String href, String base) {
        final MCRMockResolverCall mcrMockResolverCall = new MCRMockResolverCall(href, base);
        CALLS.add(mcrMockResolverCall);
        return getResultSource();
    }

    public static class MCRMockResolverCall {
        private final String href;
        private final String base;

        private MCRMockResolverCall(String href, String base) {
            this.href = href;
            this.base = base;
        }

        public String getHref() {
            return href;
        }

        public String getBase() {
            return base;
        }

    }

}
