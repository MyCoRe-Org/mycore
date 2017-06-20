/*
 * $Revision$ 
 * $Date$
 * 
 * This file is part of   M y C o R e 
 * See http://www.mycore.de/ for details.
 * 
 * This program is free software; you can use it, redistribute it and / or modify it under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation; either version 2 of the License or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program, in a file called gpl.txt or license.txt. If not, write to the Free
 * Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.frontend.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Collects statistics on number of invocations and total time needed for each command invoked.
 * 
 * @see org.mycore.frontend.cli.MCRBasicCommands for integration in MyCoRe CLI
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRCommandStatistics {

    private static Map<MCRCommand, StatisticsEntry> entries = new HashMap<MCRCommand, StatisticsEntry>();

    private static StatisticsEntry getEntry(MCRCommand command) {
        StatisticsEntry entry = entries.get(command);
        if (entry == null) {
            entry = new StatisticsEntry();
            entries.put(command, entry);
        }
        return entry;
    }

    public static void commandInvoked(MCRCommand command, long timeNeeded) {
        StatisticsEntry entry = getEntry(command);
        entry.numInvocations++;
        entry.totalTimeNeeded += timeNeeded;
    }

    /**
     * Shows statistics on number of invocations and time needed for each
     * command successfully executed.
     */
    public static void showCommandStatistics() {
        System.out.println();
        for (Entry<MCRCommand, StatisticsEntry> entry : entries.entrySet()) {
            System.out.println(entry.getKey().getSyntax());
            System.out.println(entry.getValue().toString());
        }
    }
}

class StatisticsEntry {
    /** Stores total number of invocations for each command */
    int numInvocations = 0;

    /** Stores total time needed for all executions of the given command */
    long totalTimeNeeded = 0;

    long getAverage() {
        return totalTimeNeeded / numInvocations;
    }

    public String toString() {
        return "  total=" + totalTimeNeeded + " ms, average=" + getAverage() + " ms, " + numInvocations
            + " invocations.";
    }
}
