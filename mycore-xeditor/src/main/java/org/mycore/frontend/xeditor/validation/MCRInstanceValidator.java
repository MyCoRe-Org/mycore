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

package org.mycore.frontend.xeditor.validation;

import java.util.Locale;
import java.util.function.Supplier;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.xeditor.MCRBinding;

/**
 * Validates edited xml using a configured instance of type {@link Validator}.
 * Attribute "mode" can be "instance" to use {@link MCRConfiguration2#getInstanceOf(Class, String)}
 * or "shared-instance" (default) to use {@link MCRConfiguration2#getSingleInstanceOf(Class, String)}
 * in order to obtain the validator for each node.
 * <p>
 * Example: &lt;xed:validate instance="MCR.Foo.Bar.MyValidator" ... /&gt;
 *
 * @author Frank Lützenkirchen
 */
public class MCRInstanceValidator extends MCRValidator {

    private static final String ATTR_INSTANCE = "instance";

    private static final String ATTR_MODE = "mode";

    private String name;

    private Mode mode = Mode.SHARED_INSTANCE;

    @Override
    public boolean hasRequiredAttributes() {
        return hasAttributeValue(ATTR_INSTANCE);
    }

    @Override
    public void configure() {
        name = getAttributeValue(ATTR_INSTANCE);
        if (hasAttributeValue(ATTR_MODE)) {
            String modeValue = getAttributeValue(ATTR_MODE);
            mode = Mode.valueOf(modeValue.toUpperCase(Locale.ROOT).replace("-", "_"));
        }
    }

    @Override
    public boolean validateBinding(MCRValidationResults results, MCRBinding binding) {
        Supplier<Validator> validatorSupplier = mode.getValidatorSupplier(name);
        boolean isValid = true; // all nodes must validate
        for (Object node : binding.getBoundNodes()) {
            String absPath = MCRXPathBuilder.buildXPath(node);
            if (results.hasError(absPath)) {
                continue;
            }

            Boolean result = validatorSupplier.get().isValid(node);
            if (result == null) {
                continue;
            }

            results.mark(absPath, result, this);
            isValid = isValid && result;
        }
        return isValid;
    }

    private enum Mode {

        INSTANCE {
            @Override
            public Supplier<Validator> getValidatorSupplier(String name) {
                return () -> MCRConfiguration2.getInstanceOfOrThrow(Validator.class, name);
            }
        },

        SHARED_INSTANCE {
            @Override
            public Supplier<Validator> getValidatorSupplier(String name) {
                Validator validator = MCRConfiguration2.getSingleInstanceOfOrThrow(Validator.class, name);
                return () -> validator;
            }
        };

        public abstract Supplier<Validator> getValidatorSupplier(String name);
    }

    private enum Type {

        NODE,

        ELEMENT,

        STRING;

    }

    public interface Validator {

        Boolean isValid(Object node) throws MCRException;

    }

    public static abstract class AttributeValidatorBase implements Validator {

        @Override
        public final Boolean isValid(Object node) throws MCRException {
            if (node instanceof Attribute attribute) {
                return isValidAttribute(attribute);
            }
            return null;
        }

        public abstract Boolean isValidAttribute(Attribute element);

    }

    public static abstract class ElementValidatorBase implements Validator {

        @Override
        public final Boolean isValid(Object node) throws MCRException {
            if (node instanceof Element element) {
                return isValidElement(element);
            }
            return null;
        }

        public abstract Boolean isValidElement(Element element);

    }

    public static abstract class StringValidatorBase implements Validator {

        @Override
        public final Boolean isValid(Object node) throws MCRException {
            String value = MCRBinding.getValue(node);
            return value.isEmpty() ? null : isValidString(value);
        }

        public abstract boolean isValidString(String value);

    }

}
