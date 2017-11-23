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

package org.mycore.common.processing;

import java.util.EventListener;

/**
 * Base interface to listen to {@link MCRProgressable} changes.
 * 
 * @author Matthias Eichner
 */
public interface MCRProgressableListener extends EventListener {

    /**
     * Is fired when the progress of the {@link MCRProgressable} has changed.
     * 
     * @param source the source {@link MCRProgressable}
     * @param oldProgress the old progress
     * @param newProgress the new progress
     */
    void onProgressChange(MCRProgressable source, Integer oldProgress, Integer newProgress);

    /**
     * Is fired when the progress text of the {@link MCRProgressable} has changed.
     * 
     * @param source the source {@link MCRProgressable}
     * @param oldProgressText the old progress text
     * @param newProgressText the new progress text
     */
    void onProgressTextChange(MCRProgressable source, String oldProgressText, String newProgressText);

}
