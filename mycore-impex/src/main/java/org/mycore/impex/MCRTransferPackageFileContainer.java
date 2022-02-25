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

package org.mycore.impex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Container for derivate files.
 * 
 * @author Silvio Hermann
 * @author Matthias Eichner
 */
public class MCRTransferPackageFileContainer {

    private MCRObjectID derivateId;

    private List<MCRPath> fileList;

    public MCRTransferPackageFileContainer(MCRObjectID derivateId) {
        if (derivateId == null) {
            throw new IllegalArgumentException("The derivate parameter must not be null.");
        }
        this.derivateId = derivateId;
    }

    /**
     * @return the name of this file container
     */
    public String getName() {
        return this.derivateId.toString();
    }

    public MCRObjectID getDerivateId() {
        return derivateId;
    }

    /**
     * @return the list of files hold by this container
     */
    public List<MCRPath> getFiles() throws IOException {
        if (fileList == null) {
            this.createFileList();
        }
        return this.fileList;
    }

    private void createFileList() throws IOException {
        final MCRPath derRoot = MCRPath.getPath(this.derivateId.toString(), "/");
        try (Stream<Path> files = Files.find(derRoot, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())) {
            this.fileList = files.map(MCRPath::toMCRPath)
                .sorted(Comparator.comparing(MCRPath::getNameCount)
                    .thenComparing(MCRPath::toString))
                .collect(Collectors.toList());
        }
    }

    @Override
    public String toString() {
        return this.getName() + " (" + this.fileList.size() + ")";
    }
}
