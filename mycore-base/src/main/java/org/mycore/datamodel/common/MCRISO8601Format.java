package org.mycore.datamodel.common;

import java.util.stream.Stream;

public enum MCRISO8601Format {
    YEAR("YYYY"), YEAR_MONTH("YYYY-MM"), COMPLETE("YYYY-MM-DD"), COMPLETE_HH_MM(
            "YYYY-MM-DDThh:mmTZD"), COMPLETE_HH_MM_SS("YYYY-MM-DDThh:mm:ssTZD"), COMPLETE_HH_MM_SS_SSS(
            "YYYY-MM-DDThh:mm:ss.sTZD"), YEAR_BC("-YYYY"), YEAR_MONTH_BC("-YYYY-MM"), COMPLETE_BC("-YYYY-MM-DD"), COMPLETE_HH_MM_BC(
            "-YYYY-MM-DDThh:mmTZD"), COMPLETE_HH_MM_SS_BC("-YYYY-MM-DDThh:mm:ssTZD"), COMPLETE_HH_MM_SS_SSS_BC(
            "-YYYY-MM-DDThh:mm:ss.sTZD");

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
