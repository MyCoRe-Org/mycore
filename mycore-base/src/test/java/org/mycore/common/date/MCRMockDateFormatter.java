package org.mycore.common.date;

import java.time.Instant;

public class MCRMockDateFormatter extends MCRInstantFormatterBase {

    private final ThreadLocal<String> lastFormattedDateHolder = new ThreadLocal<>();

    @Override
    public String format(Instant instant) {
        String formatted = Long.toString(instant.getEpochSecond());
        lastFormattedDateHolder.set(formatted);
        return formatted;
    }

    public String lastFormattedDate() {
        return lastFormattedDateHolder.get();
    }

}
