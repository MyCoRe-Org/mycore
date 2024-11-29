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

package org.mycore.solr.cloud.configsets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.MCRResourceResolver;

/**
 * This class represents a file that is part of a Solr config set stored in the resources directory as part of the
 * classpath.
 */
public class MCRSolrResourceConfigSetProvider extends MCRSolrConfigSetProvider {

    private String filesString;

    private String base;

    public String getFilesString() {
        return filesString;
    }

    @MCRProperty(name = "Files", required = true)
    public void setFilesString(String filesString) {
        this.filesString = filesString;
    }

    public String getBase() {
        return base;
    }

    @MCRProperty(name = "Base", required = true)
    public void setBase(String base) {
        this.base = base;
    }

    @Override
    public Supplier<InputStream> getStreamSupplier() {
        return () -> {
            HashSet<String> createdDirs = new HashSet<>();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                try (ZipOutputStream outputStream = new ZipOutputStream(baos)) {
                    List<String> files = MCRConfiguration2.splitValue(this.getFilesString()).toList();
                    for (String file : files) {
                        Optional<URL> resolve = MCRResourceResolver.instance()
                            .resolve(MCRResourcePath.ofPath(base + file));
                        if (resolve.isEmpty()) {
                            throw new MCRException("File not found: " + base + file);
                        }
                        addNotExistingDirs(file, createdDirs, outputStream);
                        outputStream.putNextEntry(new ZipEntry(file));
                        try (InputStream is = resolve.get().openStream()) {
                            is.transferTo(outputStream);
                        }
                        outputStream.closeEntry();
                    }
                }
                return new ByteArrayInputStream(baos.toByteArray());
            } catch (IOException e) {
                throw new MCRException("Error creating zip file", e);
            }
        };
    }

    /**
     * Adds all directories that are not already in the zip file. This is necessary because the zip file must contain
     * all directories that are part of the file paths. Adds the directories to existingDirs.
     * @param file The file to add.
     * @param existingDirs The directories that are already in the zip file.
     * @param outputStream The output stream to write to.
     * @throws IOException If an error occurs while writing to the output stream.
     */
    private static void addNotExistingDirs(String file, Set<String> existingDirs, ZipOutputStream outputStream)
        throws IOException {
        String[] dirs = file.split("/");
        StringBuilder dir = new StringBuilder();
        for (int i = 0; i < dirs.length - 1; i++) {
            dir.append(dirs[i]);
            dir.append('/');
            if (!existingDirs.contains(dir.toString())) {
                outputStream.putNextEntry(new ZipEntry(dir.toString()));
                outputStream.closeEntry();
                existingDirs.add(dir.toString());
            }
        }
    }
}
