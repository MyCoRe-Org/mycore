/*
 * $Id$
 * $Revision: 5697 $ $Date: 01.08.2012 $
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

package org.mycore.webcli.cli;

import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRCommandManager;

/**
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRWebCLICommandManager extends MCRCommandManager {

    public MCRWebCLICommandManager() {
        initCommands();
    }

    @Override
    protected void handleInitException(Exception ex) {
        LogManager.getLogger(getClass()).error("Exception while initializing commands.", ex);
    }

    @Override
    protected void initBuiltInCommands() {
        addAnnotatedCLIClass(MCRBasicWebCLICommands.class);
    }

    public TreeMap<String, List<MCRCommand>> getCommandsMap() {
        return knownCommands;
    }

}
