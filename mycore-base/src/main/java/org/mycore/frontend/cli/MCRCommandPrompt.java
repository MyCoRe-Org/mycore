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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Reads the next command entered at the stdin prompt.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRCommandPrompt {

    private BufferedReader console;

    private String systemName;

    public MCRCommandPrompt(String systemName) {
        this.systemName = systemName;
        this.console = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
    }

    public String readCommand() {
        String line = "";
        do {
            line = readLine();
        } while (line.isEmpty());
        return line;
    }

    private String readLine() {
        System.out.print(systemName + "> ");

        try {
            return console.readLine().trim();
        } catch (IOException ignored) {
            return "";
        }
    }

}
