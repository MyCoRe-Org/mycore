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

package org.mycore.orcid.user;

public enum MCRORCIDPublicationStatus {
    NO_ORCID_USER, // the user is not connected to an ORCID profile, we don't know his ORCID iD
    NOT_MINE, // the user has an ORCID profile, but this publication is not from him (his name identifiers do not occur)
    IN_MY_ORCID_PROFILE, // this publication is in the user's ORCID works section
    NOT_IN_MY_ORCID_PROFILE; // this is a publication of the user, but is not in the user's ORCID works section
}
