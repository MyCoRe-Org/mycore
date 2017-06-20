/*
 * $Id$
 * $Revision: 5697 $ $Date: 01.02.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2;

import javax.xml.bind.annotation.XmlEnum;

/**
 * This enum represents different hash type for user passwords.
 * Allows lazy migration of users from different sources.
 * <ul>
 * <li>{@link #crypt} is used in the old MyCoRe user system
 * <li>{@link #md5} is used in the old miless user system
 * <li>{@link #sha1} was the default hash type of mycore-user2
 * <li>{@link #sha256} is the default hash type of mycore-user2
 * </ul>
 * @author Thomas Scheffler (yagee)
 *
 */
@XmlEnum
public enum MCRPasswordHashType {

    crypt, md5, sha1, sha256

}
