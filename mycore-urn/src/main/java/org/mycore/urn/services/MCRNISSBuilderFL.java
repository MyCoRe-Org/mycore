package org.mycore.urn.services;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Builds a new, unique NISS based on the current date and time expressed
 * in seconds. The resulting NISS is non-speaking, but unique and somewhat
 * optimized for the nbn:de checksum algorithm. Only one NISS per second 
 * will be generated.
 * 
 * @author Frank Lützenkirchen
 */
@Deprecated
public class MCRNISSBuilderFL implements MCRNISSBuilder {
    private String last;

    public void init(String configID) {
    }

    public synchronized String buildNISS() {
        Calendar now = new GregorianCalendar();
        int yyy = 2268 - now.get(Calendar.YEAR);
        int ddd = 500 - now.get(Calendar.DAY_OF_YEAR);
        int hh = now.get(Calendar.HOUR_OF_DAY);
        int mm = now.get(Calendar.MINUTE);
        int ss = now.get(Calendar.SECOND);
        int sss = 99999 - (hh * 3600 + mm * 60 + ss);

        String DDDDD = String.valueOf(yyy * 366 + ddd);

        String niss = String.valueOf(DDDDD.charAt(4)) + DDDDD.charAt(2) + DDDDD.charAt(1) + DDDDD.charAt(3)
            + DDDDD.charAt(0)
            + sss;

        if (niss.equals(last)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            return buildNISS();
        } else {
            last = niss;
            return niss;
        }
    }
}
