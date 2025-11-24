package org.mycore.csl;

import de.undercouch.citeproc.csl.internal.SBibliography;
import de.undercouch.citeproc.csl.internal.format.HtmlFormat;
import de.undercouch.citeproc.output.Bibliography;
import de.undercouch.citeproc.output.SecondFieldAlign;

/**
 * Class to enable xml output in the {@link de.undercouch.citeproc.CSL} processor.
 * */
public class MCRCSLXMLOutputFormat extends HtmlFormat {

    @Override
    public Bibliography makeBibliography(String[] entries, SBibliography bibliographyElement) {
        SecondFieldAlign sfa = bibliographyElement.getSecondFieldAlign();
        return new Bibliography(entries, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<div class=\"csl-bib-body\">\n",
            "</div>",
            null, null, null, null, null, null, sfa);
    }

    @Override
    protected String escape(String str) {
        return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    @Override
    public String getName() {
        return "xml";
    }
}
