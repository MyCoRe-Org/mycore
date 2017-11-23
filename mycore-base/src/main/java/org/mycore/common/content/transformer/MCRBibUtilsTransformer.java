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

package org.mycore.common.content.transformer;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.frontend.cli.MCRExternalProcess;

/**
 * Transforms MCRContent by invoking an external BibUtils command.
 * See http://www.scripps.edu/~cdputnam/software/bibutils/
 * The BibUtils commands provide functionality to convert between
 * RIS, Endnote, BibTeX, ISI web of science, Word 2007 bibliography format
 * and MODS. For each transformer instance, the command must be specified,
 * for example
 * 
 * MCR.ContentTransformer.{ID}.Command=cmd.exe /c C:\\Java\\bibutils_4.12\\xml2bib.exe -b -w 
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBibUtilsTransformer extends MCRContentTransformer {

    private static final Logger LOGGER = LogManager.getLogger(MCRBibUtilsTransformer.class);

    /** The external Bibutils command to invoke */
    private String command;

    @Override
    public void init(String id) {
        super.init(id);
        command = MCRConfiguration.instance().getString("MCR.ContentTransformer." + id + ".Command");
    }

    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        File modsFile = File.createTempFile("mods", ".xml");
        source.sendTo(modsFile);
        try {
            MCRContent export = export(modsFile);
            export.setLastModified(source.lastModified());
            export.setMimeType(getMimeType());
            return export;
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IOException(e);
        } finally {
            modsFile.delete();
        }
    }

    private MCRContent export(File modsFile) {
        String[] arguments = buildCommandArguments(modsFile);
        MCRExternalProcess ep = null;
        try {
            ep = new MCRExternalProcess(arguments);
            ep.run();
            String errors = ep.getErrors();
            if (!errors.isEmpty())
                LOGGER.warn(errors);
            return ep.getOutput();
        } catch (Exception ex) {
            String msg = "Exception invoking external command " + command;
            throw new MCRException(msg, ex);
        }
    }

    /**
     * Builds the command arguments to invoke the external BibUtils command 
     */
    private String[] buildCommandArguments(File modsFile) {
        String path = modsFile.getAbsolutePath();

        String[] args1 = command.split(" ");
        String[] args2 = new String[args1.length + 1];
        System.arraycopy(args1, 0, args2, 0, args1.length);
        args2[args1.length] = path;

        LOGGER.info("{} {}", command, path);
        return args2;
    }
}
