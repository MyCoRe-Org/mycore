package org.mycore.datamodel.common;

public enum MCRISO8601Format {
    YEAR, YEAR_MONTH, COMPLETE, COMPLETE_HH_MM, COMPLETE_HH_MM_SS, COMPLETE_HH_MM_SS_SSS;

    public final static String F_YEAR = "YYYY";

    public final static String F_YEAR_MONTH = "YYYY-MM";

    public final static String F_COMPLETE = "YYYY-MM-DD";

    public final static String F_COMPLETE_HH_MM = "YYYY-MM-DDThh:mmTZD";

    public final static String F_COMPLETE_HH_MM_SS = "YYYY-MM-DDThh:mm:ssTZD";

    public final static String F_COMPLETE_HH_MM_SS_SSS = "YYYY-MM-DDThh:mm:ss.sTZD";

    @Override
    public String toString() {
        switch (this) {
        case YEAR:
            return F_YEAR;
        case YEAR_MONTH:
            return F_YEAR_MONTH;
        case COMPLETE:
            return F_COMPLETE;
        case COMPLETE_HH_MM:
            return F_COMPLETE_HH_MM;
        case COMPLETE_HH_MM_SS:
            return F_COMPLETE_HH_MM_SS;
        case COMPLETE_HH_MM_SS_SSS:
            return F_COMPLETE_HH_MM_SS_SSS;
        }
        // never reached
        return null;
    }

    public static MCRISO8601Format getFormat(String format) {
        if (format == null) {
            return null;
        }
        String fmt = format.intern();
        if (fmt == F_YEAR) {
            return YEAR;
        }
        if (fmt == F_YEAR_MONTH) {
            return YEAR_MONTH;
        }
        if (fmt == F_COMPLETE) {
            return COMPLETE;
        }
        if (fmt == F_COMPLETE_HH_MM) {
            return COMPLETE_HH_MM;
        }
        if (fmt == F_COMPLETE_HH_MM_SS) {
            return COMPLETE_HH_MM_SS;
        }
        if (fmt == F_COMPLETE_HH_MM_SS_SSS) {
            return COMPLETE_HH_MM_SS_SSS;
        }
        // never reached
        return null;
    }

}