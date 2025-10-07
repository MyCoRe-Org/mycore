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
package org.mycore.datamodel.classifications2.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRXPathEvaluator;
import org.mycore.datamodel.classifications2.mapping.MCRConditionalXMappingEvaluator.Condition;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * A {@link MCRXPathClassificationMappingCondition} is a {@link Condition} that
 * determines the condition of a MyCoRe object based on a configurable X-Path evaluating the
 * XML representation of the MyCoRe object.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRXPathClassificationMappingCondition#X_PATH_KEY} can be used to
 * specify the X-Path to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.mapping.MCRXPathClassificationMappingCondition
 * [...].XPath=/mycoreobject/@version
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRXPathClassificationMappingCondition.Factory.class)
public final class MCRXPathClassificationMappingCondition implements Condition {

    public static final String X_PATH_KEY = "XPath";

    private final String xPath;

    public MCRXPathClassificationMappingCondition(String xPath) {
        this.xPath = Objects.requireNonNull(xPath, "X-Path must not be null");
    }

    @Override
    public Set<String> evaluate(MCRObject object) {
        MCRXPathEvaluator evaluator = new MCRXPathEvaluator(new HashMap<>(), object.createXML());
        return new HashSet<>(evaluator.evaluateAllAsString(xPath));
    }

    public static class Factory implements Supplier<MCRXPathClassificationMappingCondition> {

        @MCRProperty(name = X_PATH_KEY)
        public String xPath;

        @Override
        public MCRXPathClassificationMappingCondition get() {
            return new MCRXPathClassificationMappingCondition(xPath);
        }

    }

}
