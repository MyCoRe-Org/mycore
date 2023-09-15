package org.mycore.tools;


import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.config.MCRConfiguration2;

public class MCRLanguageOrientationHelper {

    static Set<String> RTL_LANGUAGES = getRTLLanguages();

    private static Set<String> getRTLLanguages() {
        return MCRConfiguration2.getString("MCR.I18N.RtlLanguageList")
            .map(MCRConfiguration2::splitValue)
            .orElse(Stream.empty())
            .collect(Collectors.toSet());
    }


    public static boolean isRTL(String language) {
        return RTL_LANGUAGES.contains(language);
    }

}
