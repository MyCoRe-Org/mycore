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

    private static Map<MCRCommand, StatisticsEntry> entries = new HashMap<>();

    private static StatisticsEntry getEntry(MCRCommand command) {
        return entries.computeIfAbsent(command, k -> new StatisticsEntry());
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
            System.out.println(entry.getValue());
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
