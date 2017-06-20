package org.mycore.datamodel.common;

import java.util.stream.Stream;

public enum MCRISO8601Format {
    YEAR("UUUU"), YEAR_MONTH("UUUU-MM"), COMPLETE("UUUU-MM-DD"), COMPLETE_HH_MM(
        "UUUU-MM-DDThh:mmTZD"), COMPLETE_HH_MM_SS("UUUU-MM-DDThh:mm:ssTZD"), COMPLETE_HH_MM_SS_SSS(
            "UUUU-MM-DDThh:mm:ss.sTZD"), YEAR_ERA("YYYY"), YEAR_MONTH_ERA("YYYY-MM"), COMPLETE_ERA(
                "YYYY-MM-DD"), COMPLETE_HH_MM_ERA(
                    "YYYY-MM-DDThh:mmTZD"), COMPLETE_HH_MM_SS_ERA("YYYY-MM-DDThh:mm:ssTZD"), COMPLETE_HH_MM_SS_SSS_ERA(
                        "YYYY-MM-DDThh:mm:ss.sTZD");

    private String format;

    private MCRISO8601Format(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }

    public static MCRISO8601Format getFormat(String format) {
        return Stream.of(values())
            .filter(f -> f.format.equals(format))
            .findAny()
            .orElse(null);
    }

}
