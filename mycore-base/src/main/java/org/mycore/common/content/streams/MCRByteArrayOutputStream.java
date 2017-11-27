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

package org.mycore.common.content.streams;

import java.io.ByteArrayOutputStream;

/**
 * A extension of {@link ByteArrayOutputStream} that allows access to internal buffer.
 * @author Thomas Scheffler (yagee)
 */
public class MCRByteArrayOutputStream extends ByteArrayOutputStream {

    /**
     * Initital buffer size is 4k.
     */
    public MCRByteArrayOutputStream() {
        this(4 * 1024);
    }

    public MCRByteArrayOutputStream(int i) {
        super(i);
    }

    public byte[] getBuffer() {
        return super.buf;
    }
}
