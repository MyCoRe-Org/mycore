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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Commands for temporarily testing CLI log and command queue processing.
 *
 * <p>Enable these commands only while debugging by adding this class to
 * {@code MCR.CLI.Classes.Internal}. Large values can exhaust server or client resources.</p>
 */
@MCRCommandGroup(name = "CLI Test Commands")
public class MCRCLITestCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(
        syntax = "cli test {0} log lines",
        help = "Generate {0} numbered log lines as fast as possible.",
        order = 10)
    public static void generateLogLines(int numberOfLogLines) {
        requireNonNegative(numberOfLogLines);
        for (int i = 1; i <= numberOfLogLines; i++) {
            LOGGER.info("CLI test log line {} of {}", i, numberOfLogLines);
        }
    }

    @MCRCommand(
        syntax = "cli test {0} subcommands",
        help = "Generate {0} numbered no-op subcommands.",
        order = 20)
    public static List<String> generateSubcommands(int numberOfSubcommands) {
        requireNonNegative(numberOfSubcommands);
        List<String> subcommands = new ArrayList<>(numberOfSubcommands);
        for (int i = 1; i <= numberOfSubcommands; i++) {
            subcommands.add("cli test subcommand " + i);
        }
        return subcommands;
    }

    @MCRCommand(
        syntax = "cli test subcommand {0}",
        help = "Execute a numbered no-op subcommand.",
        order = 30)
    public static void executeSubcommand(int counter) {
        // intentionally left blank
    }

    private static void requireNonNegative(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must not be negative: " + count);
        }
    }
}
