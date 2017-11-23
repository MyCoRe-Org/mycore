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

package org.mycore.common.xsl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.xml.transform.stream.StreamSource;

import org.mycore.common.MCRException;

/**
 * A {@link StreamSource} that offers a lazy initialization to {@link #getInputStream()}.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRLazyStreamSource extends StreamSource {

    private InputStreamSupplier inputStreamSupplier;

    public MCRLazyStreamSource(InputStreamSupplier inputStreamSupplier, String systemId) {
        super(systemId);
        this.inputStreamSupplier = Optional.ofNullable(inputStreamSupplier).orElse(() -> null);
    }

    @Override
    public void setInputStream(InputStream inputStream) {
        inputStreamSupplier = () -> inputStream;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return inputStreamSupplier.get();
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream get() throws IOException;
    }

}
