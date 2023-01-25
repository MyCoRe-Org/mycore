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
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.MCRException;

/**
 * Interface to the command line utility <a href="https://pandoc.org">Pandoc</a>,
 * which needs to be installed separately.
 *
 * @author Kai Brandhorst
 */
public class MCRPandocAPI {

    private static final String PANDOC_BASE_COMMAND = MCRConfiguration2.getStringOrThrow("MCR.Pandoc.BaseCommand");

    private static final int BUFFER_SIZE = MCRConfiguration2.getInt("MCR.Pandoc.BufferSize").orElse(64 * 1024);

    private static final String LUA_PATH = MCRConfiguration2.getString("MCR.Pandoc.LuaPath")
        .orElse(Thread.currentThread().getContextClassLoader().getResource("lua").getPath() + "?.lua");

    private enum Action { Reader, Writer };

    /**
     * Raw Pandoc command invocation
     * @param args Command line arguments to Pandoc
     * @param input Input to Pandoc (Stdin)
     * @return String of parsed output from Pandoc (Stdout)
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
     */
    public static Element convertToXML(String content, String importFormat, String outputFormat) {
        String output = "<pandoc>" + convert(content, importFormat, outputFormat) + "</pandoc>";
        try {
            return new SAXBuilder().build(new StringReader(output)).getRootElement().detach();
        } catch (Exception ex) {
            String msg = "Exception converting Pandoc output to XML element";
            throw new MCRException(msg, ex);
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
        Process p;
        try {
            p = initPandoc(args, input);
        } catch(IOException ex) {
            String msg = "Exception invoking Pandoc " + String.join(" ", args);
            throw new MCRException(msg, ex);
        }
        try {
            return readPandocOutput(p);
        } catch(IOException ex) {
            String msg = "Exception reading output from Pandoc" + String.join(" ", args);
            throw new MCRException(msg, ex);
        } catch(InterruptedException ex) {
            String msg = "Exception shutting down Pandoc " + String.join(" ", args);
            throw new MCRException(msg, ex);
        }
    }

    private static Process initPandoc(String[] args, byte[] input) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().put("LUA_PATH", LUA_PATH);
        Process p;
        p = pb.start();
        p.getOutputStream().write(input);
        p.getOutputStream().close();
        return p;
    }

    private static byte[] readPandocOutput(Process p) throws IOException, InterruptedException {
        BufferedInputStream pandocStream = new BufferedInputStream(p.getInputStream());
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] output = new byte[0];
        int readBytes;
        while((readBytes = pandocStream.read(buffer, 0, BUFFER_SIZE)) >= 0) {
            byte[] newOutput = new byte[output.length + readBytes];
            System.arraycopy(output, 0, newOutput, 0, output.length);
            System.arraycopy(buffer, 0, newOutput, output.length, readBytes);
            output = newOutput;
        }
        p.waitFor();
        pandocStream.close();
        return output;
    }
}
