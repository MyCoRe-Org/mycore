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

package org.mycore.ocfl.niofs;

import java.io.IOException;

/**
 * An interface for a component that calculates a digest for a given path or byte array.
 *
 * @param <P> The type of the path object.
 * @param <D> The type of the digest object.
 */
public interface MCROCFLDigestCalculator<P, D> {

    /**
     * Calculates the digest for the given byte array.
     * This is the preferred method when the content is already in memory.
     *
     * @param bytes The content to be digested.
     * @return The calculated digest.
     * @throws IOException If an I/O error occurs (though less likely for byte arrays).
     */
    D calculate(byte[] bytes) throws IOException;

    /**
     * Calculates the digest for the given path by reading its content.
     *
     * @param path the path to the content to be digested.
     * @return the calculated digest.
     * @throws java.io.IOException if an I/O error occurs while reading the path.
     */
    D calculate(P path) throws IOException;

}
