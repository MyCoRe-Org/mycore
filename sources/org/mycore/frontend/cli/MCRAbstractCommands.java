/*
 * 
 * $Revision$ $Date$
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

package org.mycore.frontend.cli;

import java.util.ArrayList;

import org.mycore.common.MCRConfiguration;

/**
 * This class is an abstract for the implementation of command classes for the
 * MyCoRe commandline system.
 * 
 * @author Jens Kupferschmidt
 * 
 * @version $Revision$ $Date$
 */
public class MCRAbstractCommands implements MCRExternalCommandInterface {
    /** The configuration instance */
    protected static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    /** The array holding all known commands */
    protected ArrayList<MCRCommand> command = null;

    /**
     * The constrctor.
     */
    protected MCRAbstractCommands() {
        command = new ArrayList<MCRCommand>();
    }

    /**
     * The method return the list of possible commands of this class. Each
     * command has TWO Strings, a String of the user command syntax and a String
     * of the called method.
     * 
     * @return a command pair RArrayList
     */
    public final ArrayList<MCRCommand> getPossibleCommands() {
        return command;
    }
}
