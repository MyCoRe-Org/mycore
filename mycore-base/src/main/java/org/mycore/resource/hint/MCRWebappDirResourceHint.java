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

package org.mycore.resource.hint;

import java.io.File;
import java.util.Optional;

import org.mycore.common.events.MCRServletContextHolder;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

public final class MCRWebappDirResourceHint implements MCRHint<File> {

    @Override
    public MCRHintKey<File> key() {
        return MCRResourceHintKeys.WEBAPP_DIR;
    }

    @Override
    public Optional<File> value() {
        return MCRServletContextHolder.getInstance().get().map(context -> context.getRealPath("/")).map(File::new);
    }

}
