/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.user2;

import jakarta.xml.bind.annotation.XmlEnum;

/**
 * This enum represents different hash type for user passwords.
 * Allows lazy migration of users from different sources.
 * <ul>
 * <li>{@link #CRYPT} is used in the old MyCoRe user system
 * <li>{@link #MD5} is used in the old miless user system
 * <li>{@link #SHA_1} was the default hash type of mycore-user2
 * <li>{@link #SHA_256} is the default hash type of mycore-user2
 * </ul>
 * @author Thomas Scheffler (yagee)
 *
 */
@XmlEnum
@Deprecated
public enum MCRPasswordHashType {

    CRYPT, MD5, SHA_1, SHA_256

}
