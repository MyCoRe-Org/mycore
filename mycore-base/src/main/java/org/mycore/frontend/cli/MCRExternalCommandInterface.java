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

package org.mycore.frontend.cli;

import java.util.List;

/**
 * This interface is designed to incude external application commands.
 *
 * @author Jens Kupferschmidt
 */
public interface MCRExternalCommandInterface {
    /**
     * The method return the list of possible commands of this class. Each
     * command has TWO Strings, a String of the user command syntax and a String
     * of the called method.
     *
     * @return a command pair ArrayList
     */
    List<MCRCommand> getPossibleCommands();

    /**
     * Returns the display name of the external commands. If the display name
     * has not been set the simple class name is returned
     *
     * @return the display name of the external commands
     */
    String getDisplayName();

    /**
     * Sets the display name.
     */
    void setDisplayName(String s);
}
