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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;

public class MCRExternalProcess {

    private static final Logger LOGGER = LogManager.getLogger(MCRExternalProcess.class);

    private String[] command;

    private int exitValue;

    private MCRStreamSucker output;

    private MCRStreamSucker errors;

    public MCRExternalProcess(String... command) {
        this.command = command;
    }

    public MCRExternalProcess(String command) {
        this.command = command.split("\\s+");
    }

    public int run() throws IOException, InterruptedException {

        if (LOGGER.isDebugEnabled())
            debug(command);

        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();

        output = new MCRStreamSucker(p.getInputStream());
        output.start();

        errors = new MCRStreamSucker(p.getErrorStream());
        errors.start();

        exitValue = p.waitFor();

        output.join(); // wait for the stream suckers in case output is not read fully yet.
        errors.join();

        return exitValue;
    }

    private void debug(String... command) {
        StringBuilder sb = new StringBuilder();
        for (String arg : command)
            sb.append(arg).append(" ");
        LOGGER.debug(sb.toString().trim());
    }

    public int getExitValue() {
        return exitValue;
    }

    public String getErrors() {
        return new String(errors.getOutput(), Charset.defaultCharset());
    }

    public MCRContent getOutput() throws IOException {
        return new MCRByteContent(output.getOutput(), System.currentTimeMillis());
    }
}

class MCRStreamSucker extends Thread {

    private static final Logger LOGGER = LogManager.getLogger(MCRStreamSucker.class);

    /** The input stream to read from */
    private InputStream in;

    /** Collects the output read from the input stream */
    private ByteArrayOutputStream out;

    MCRStreamSucker(InputStream in) {
        this.in = in;
        this.out = new ByteArrayOutputStream();
    }

    /**
     * Reads from the input stream until end.
     */
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int num;
            while ((num = in.read(buffer, 0, buffer.length)) >= 0)
                out.write(buffer, 0, num);
            out.close();
        } catch (IOException ex) {
            LOGGER.warn(ex);
        }
    }

    /**
     * Returns the output read from the input stream
     */
    public byte[] getOutput() {
        return out.toByteArray();
    }
}
