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

package org.mycore.pandoc;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Interface to the command line utility <a href="https://pandoc.org">Pandoc</a>,
 * which needs to be installed separately.
 *
 * @author Kai Brandhorst
 */
public class MCRPandocAPI {

    private static final String PANDOC_BASE_COMMAND = MCRConfiguration2.getStringOrThrow("MCR.Pandoc.BaseCommand");

    private static final int BUFFER_SIZE = MCRConfiguration2.getInt("MCR.Pandoc.BufferSize").orElse(64 * 1024);

    private static final int TIMEOUT = MCRConfiguration2.getInt("MCR.Pandoc.Timeout").orElse(5);

    private static final String LUA_PATH = MCRConfiguration2.getString("MCR.Pandoc.LuaPath")
        .orElse(Thread.currentThread().getContextClassLoader().getResource("lua").getPath() + "?.lua");

    private enum Action { Reader, Writer }

    /**
     * Raw Pandoc command invocation
     * @param args Command line arguments to Pandoc
     * @param input Input to Pandoc (Stdin)
     * @return String of parsed output from Pandoc (Stdout)
     * @throws MCRPandocException in case of unsuccessful call to Pandoc
     */
    public static String call(String args, String input) {
        String command = PANDOC_BASE_COMMAND.trim() + " " + args.trim();
        String[] argsArray = command.split(" ");
        byte[] inputByteArray = input.getBytes(StandardCharsets.UTF_8);
        byte[] outputByteArray = callPandoc(argsArray, inputByteArray);
        String output = new String(outputByteArray, StandardCharsets.UTF_8);
        return output;
    }

    /**
     * Convenience method for converting between different formats.
     * If the property MCR.Pandoc.[Reader|Writer].FORMAT and/or MCR.Pandoc.[Reader|Writer].FORMAT.Path
     * are specified, those values get precedence and are interpreted as file paths.
     * Otherwise the importFormat and outputFormat are used as is.
     * @param content Content to be converted (Stdin)
     * @param importFormat Import format (cf. pandoc -f)
     * @param outputFormat Output format (cf. pandoc -t)
     * @return String of output from Pandoc (Stdout)
     * @throws MCRPandocException in case of unsuccessful call to Pandoc
     */
    public static String convert(String content, String importFormat, String outputFormat) {
        String pandocArgs = "-f " + convertFormatToPath(Action.Reader, importFormat)
            + " -t " + convertFormatToPath(Action.Writer, outputFormat);
        return call(pandocArgs, content);
    }

    /**
     * Convenience method for converting between different formats.
     * If the property MCR.Pandoc.[Reader|Writer].FORMAT and/or MCR.Pandoc.[Reader|Writer].FORMAT.Path
     * are specified, those values get precedence and are interpreted as file paths.
     * Otherwise the importFormat and outputFormat are used as is.
     * This method wraps the output of Pandoc in a pandoc-element and parses it as XML.
     * @param content Content to be converted (Stdin)
     * @param importFormat Import format (cf. pandoc -f)
     * @param outputFormat Output format (cf. pandoc -t)
     * @return Element of parsed output from Pandoc (Stdout)
     * @throws MCRPandocException in case of unsuccessful call to Pandoc
     */
    public static Element convertToXML(String content, String importFormat, String outputFormat) {
        String output = "<pandoc>" + convert(content, importFormat, outputFormat) + "</pandoc>";
        try {
            return new SAXBuilder().build(new StringReader(output)).getRootElement().detach();
        } catch (Exception ex) {
            String msg = "Exception converting Pandoc output to XML element";
            throw new MCRPandocException(msg, ex);
        }
    }

    private static String convertFormatToPath(Action action, String format) {
        String property = MCRConfiguration2.getString("MCR.Pandoc." + action + "." + format).orElse("");
        String path = MCRConfiguration2.getString("MCR.Pandoc." + action + "." + format + ".Path").orElse("");
        if(!property.isEmpty()) {
            if(path.isEmpty()) {
                return Thread.currentThread().getContextClassLoader().getResource(property).getFile();
            } else {
                return Paths.get(path).resolve(property).toString();
            }
        } else {
            return format;
        }
    }

    private static byte[] callPandoc(String[] args, byte[] input) {
        class ThreadWrapper implements Runnable {

            private volatile byte[] output;

            private InputStream istream;

            ThreadWrapper(InputStream is) {
                istream = is;
            }

            @Override
            public void run() {
                try {
                    output = readPandocOutput(istream);
                } catch(IOException ex) {
                    String msg = "Exception reading output from Pandoc";
                    throw new MCRPandocException(msg, ex);
                }
            }

            public byte[] getOutput() {
                return output;
            }
        }

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().put("LUA_PATH", LUA_PATH);
        Process p;
        try {
            p = pb.start();
            p.getOutputStream().write(input);
            p.getOutputStream().close();
        } catch(IOException ex) {
            String msg = "Exception invoking Pandoc " + String.join(" ", args);
            throw new MCRPandocException(msg, ex);
        }
        ThreadWrapper outputThread = new ThreadWrapper(p.getInputStream());
        ThreadWrapper errorThread = new ThreadWrapper(p.getErrorStream());
        Thread oThread = new Thread(outputThread);
        Thread eThread = new Thread(errorThread);
        oThread.start();
        eThread.start();
        try {
            if(!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
                p.destroy();
                throw new InterruptedException();
            }
            oThread.join();
            eThread.join();
        } catch(InterruptedException ex) {
            String msg = "Pandoc process " + String.join(" ", args) + " was terminated after reaching a timeout of "
                + TIMEOUT + " seconds";
            throw new MCRPandocException(msg, ex);
        }
        if(p.exitValue() != 0) {
            String msg = "Pandoc process " + String.join(" ", args) + " terminated with error code " + p.exitValue()
                + " and error message \n" + new String(errorThread.getOutput(), StandardCharsets.UTF_8);
            p.destroy();
            throw new MCRPandocException(msg);
        }
        return outputThread.getOutput();
    }

    private static byte[] readPandocOutput(InputStream s) throws IOException {
        BufferedInputStream pandocStream = new BufferedInputStream(s);
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] output = new byte[0];
        int readBytes;
        while((readBytes = pandocStream.read(buffer, 0, BUFFER_SIZE)) >= 0) {
            byte[] newOutput = new byte[output.length + readBytes];
            System.arraycopy(output, 0, newOutput, 0, output.length);
            System.arraycopy(buffer, 0, newOutput, output.length, readBytes);
            output = newOutput;
        }
        pandocStream.close();
        return output;
    }
}
