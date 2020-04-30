package org.mycore.csl;

import java.io.IOException;
import java.io.InputStream;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.ParseException;
import org.mycore.common.content.MCRContent;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.bibtex.BibTeXItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;

/** Wrapper around BibTeXItemDataProvider to make it reusable */
public class MCRItemDataProvider implements ItemDataProvider {

    private BibTeXItemDataProvider wrappedProvider = new BibTeXItemDataProvider();

    public void addBibTeX(MCRContent bibTeX) throws IOException {
        InputStream in = bibTeX.getInputStream();
        BibTeXDatabase db;
        try {
            db = new BibTeXConverter().loadDatabase(in);
        } catch (ParseException ex) {
            throw new IOException(ex);
        }
        in.close();

        wrappedProvider.addDatabase(db);
    }

    public void reset() {
        wrappedProvider = new BibTeXItemDataProvider();
    }

    @Override
    public CSLItemData retrieveItem(String id) {
        return wrappedProvider.retrieveItem(id);
    }

    @Override
    public String[] getIds() {
        return wrappedProvider.getIds();
    }

    public void registerCitationItems(CSL citationProcessor) {
        wrappedProvider.registerCitationItems(citationProcessor);
    }
}
