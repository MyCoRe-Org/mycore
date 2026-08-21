package org.mycore.common.date;

import java.time.Instant;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

@MCRConfigurationProxy(proxyClass = MCRMockDateFormatter.Factory.class)
public class MCRMockDateFormatter extends MCRInstantFormatterBase {

    public static final String VALUE_KEY = "Value";

    private final String value;

    public MCRMockDateFormatter(String value) {
        this.value = value;
    }

    @Override
    public String format(Instant instant) {
        return value;
    }

    public static final class Factory implements Supplier<MCRMockDateFormatter> {

        @MCRProperty(name = VALUE_KEY)
        public String value;

        @Override
        public MCRMockDateFormatter get() {
            return new MCRMockDateFormatter(value);
        }

    }

}
