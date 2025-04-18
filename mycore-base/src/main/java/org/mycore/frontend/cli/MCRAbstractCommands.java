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

import java.util.ArrayList;
import java.util.List;

/**
 * This class is an abstract for the implementation of command classes for the
 * MyCoRe commandline system.
 *
 * @author Jens Kupferschmidt
 */
public abstract class MCRAbstractCommands implements MCRExternalCommandInterface {
    /** The array holding all known commands */
    protected List<MCRCommand> command;

    private String displayName;

    /**
     * The constructor.
     */
    protected MCRAbstractCommands() {
        init();
    }

    private void init() {
        setCommand(new ArrayList<>());
    }

    /**
     * @param displayName
     *            a human readable name for this collection of commands
     */
    protected MCRAbstractCommands(String displayName) {
        init();
        this.displayName = displayName;
    }

    /**
     * The method return the list of possible commands of this class. Each
     * command has TWO Strings, a String of the user command syntax and a String
     * of the called method.
     *
     * @return a ascending sorted command pair ArrayList
     */
    @Override
    public List<MCRCommand> getPossibleCommands() {
        return this.command;
    }

    @Override
    public String getDisplayName() {
        return this.displayName == null ? this.getClass().getSimpleName() : this.displayName;
    }

    @Override
    public void setDisplayName(String s) {
        this.displayName = s;
    }

    public void addCommand(MCRCommand cmd) {
        if (this.command == null) {
            init();
        }

        this.command.add(cmd);
    }

    private void setCommand(List<MCRCommand> command) {
        this.command = command;
    }
}
