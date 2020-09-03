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

import org.mycore.common.config.MCRConfigurationException;

import de.undercouch.citeproc.CSL;

public class MCRCSLTransformerInstance implements AutoCloseable {

    private final AutoCloseable closeable;

    private final CSL citationProcessor;

    private final MCRItemDataProvider dataProvider;

    public MCRCSLTransformerInstance(String style, String format, AutoCloseable closeable,
        MCRItemDataProvider dataProvider) {
        this.closeable = closeable;
        this.dataProvider = dataProvider;
        try {
            this.citationProcessor = new CSL(this.dataProvider, style);
        } catch (IOException e) {
            throw new MCRConfigurationException("Error while creating CSL with Style " + style, e);
        }
        this.citationProcessor.setOutputFormat(format);

    }

    public CSL getCitationProcessor() {
        return citationProcessor;
    }

    public MCRItemDataProvider getDataProvider() {
        return dataProvider;
    }

    @Override
    public void close() throws Exception {
        this.closeable.close();
    }
}
