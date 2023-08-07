package org.mycore.tools;


import java.util.Set;

public class MCRLanguageOrientationHelper {

    static Set<String> RTL_LANGUAGES = getRTLLanguages();

    private static Set<String> getRTLLanguages() {
        return Set.of("ar", "dv", "fa", "ha", "he", "iw", "ji", "ps", "sd", "ug", "ur", "yi");
    }


    public static boolean isRTL(String language) {
        return RTL_LANGUAGES.contains(language);
    }

}
