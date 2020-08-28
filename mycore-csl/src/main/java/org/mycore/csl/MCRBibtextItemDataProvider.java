/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.csl;

import java.io.IOException;
import java.io.InputStream;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.ParseException;
import org.mycore.common.content.MCRContent;

import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.bibtex.BibTeXItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;

/**
 * Wrapper around BibTeXItemDataProvider to make it reusable
 */
public class MCRBibtextItemDataProvider extends MCRItemDataProvider {

    private BibTeXItemDataProvider wrappedProvider = new BibTeXItemDataProvider();

    @Override
    public CSLItemData retrieveItem(String id) {
        return wrappedProvider.retrieveItem(id);
    }

    @Override
    public String[] getIds() {
        return wrappedProvider.getIds();
    }

    @Override
    public void addContent(MCRContent bibTeX) throws IOException {
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

    @Override
    public void reset() {
        wrappedProvider = new BibTeXItemDataProvider();
    }
}
