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

package org.mycore.pi.util;

public record MCRResultOrException<T, E extends Exception>(T result, E exception) {
    public static <T, E extends Exception> MCRResultOrException<T, E> ofResult(T result) {
        return new MCRResultOrException<>(result, null);
    }

    public static <T, E extends Exception> MCRResultOrException<T, E> ofException(E exception) {
        return new MCRResultOrException<>(null, exception);
    }

    public T getResultOrThrow() throws E {
        if (exception != null) {
            throw exception();
        }
        return result;
    }
}
