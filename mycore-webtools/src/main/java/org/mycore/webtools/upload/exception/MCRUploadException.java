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

package org.mycore.webtools.upload.exception;

import org.mycore.services.i18n.MCRTranslation;

import java.io.Serial;

public class MCRUploadException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public MCRUploadException(String messageKey) {
        super(MCRTranslation.translate(messageKey));
    }

    public MCRUploadException(String messageKey, String... translationParams) {
        super(MCRTranslation.translate(messageKey, (Object[]) translationParams));
    }

    public MCRUploadException(String messageKey, Throwable throwable) {
        super(MCRTranslation.translate(messageKey), throwable);
    }
}
