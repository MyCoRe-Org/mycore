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

package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.util.concurrent.MCRReadWriteGuard;
import org.xml.sax.SAXException;

/**
 * Represents a set of files and directories belonging together, that are stored
 * in a persistent MCRFileStore. A FileCollection has a unique ID within the
 * store, it is the root folder of all files and directories in the collection.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileCollection extends MCRDirectory {

    /**
     * The logger
     */
    private static final Logger LOGGER = LogManager.getLogger(MCRFileCollection.class);

    private static final String dataFile = "mcrdata.xml";

    /**
     * The store this file collection is stored in.
     */
    private MCRStore store;

    /**
     * The ID of this file collection
     */
    private int id;

    /**
     * Guard for additional data
     *
     * MCR-1869
     */
    private MCRReadWriteGuard dataGuard;

    /**
     * Creates a new file collection in the given store, or retrieves an
     * existing one.
     *
     * @see MCRFileStore
     *
     * @param store
     *            the store this file collection is stored in
     * @param id
     *            the ID of this file collection
     */
    protected MCRFileCollection(MCRStore store, int id) throws IOException {
        super(null, store.getSlot(id), new Element("collection"));
        this.store = store;
        this.id = id;
        this.dataGuard = new MCRReadWriteGuard();
        if (Files.exists(path)) {
            readAdditionalData();
        } else {
            Files.createDirectories(path);
            writeData(Document::new);
            saveAdditionalData();
        }
    }

    MCRReadWriteGuard getDataGuard() {
        return dataGuard;
    }

    private void readAdditionalData() throws IOException {
        Path src = path.resolve(dataFile);
        if (!Files.exists(src)) {
            LOGGER.warn("Metadata file is missing, repairing metadata...");
            writeData(e -> {
                e.detach();
                e.setName("collection");
                e.removeContent();
                new Document(e);
            });
            repairMetadata();
        }
        try {
            Element parsed = new MCRPathContent(src).asXML().getRootElement();
            writeData(e -> {
                e.detach();
                e.setName("collection");
                e.removeContent();
                List<Content> parsedContent = new ArrayList<>(parsed.getContent());
                parsedContent.forEach(Content::detach);
                e.addContent(parsedContent);
                new Document(e);
            });
        } catch (JDOMException | SAXException e) {
            throw new IOException(e);
        }
    }

    protected void saveAdditionalData() throws IOException {
        Path target = path.resolve(dataFile);
        try {
            readData(e -> {
                try {
                    boolean needsUpdate = true;
                    while (needsUpdate) {
                        try {
                            new MCRJDOMContent(e.getDocument()).sendTo(target, StandardCopyOption.REPLACE_EXISTING);
                            needsUpdate = false;
                        } catch (FileAlreadyExistsException ex) {
                            //other process writes to the same file, let us win
                        } catch (IOException | RuntimeException ex) {
                            throw ex;
                        }
                    }
                    return null;
                } catch (IOException e1) {
                    throw new UncheckedIOException(e1);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public Stream<MCRNode> getChildren() throws IOException {
        return super.getChildren()
            .filter(f -> !dataFile.equals(f.getName()));
    }

    /**
     * Deletes this file collection with all its data and children
     */
    public void delete() throws IOException {
        writeData(Element::removeContent);
        Files.walkFileTree(path, MCRRecursiveDeleter.instance());
        store.delete(id);
    }

    /**
     * Throws a exception, because a file collection's name is always the empty
     * string and therefore can not be renamed.
     */
    @Override
    public void renameTo(String name) {
        throw new UnsupportedOperationException("File collections can not be renamed");
    }

    /**
     * Returns the store this file collection is stored in.
     * 
     * @return the store this file collection is stored in.
     */
    public MCRStore getStore() {
        return store;
    }

    /**
     * Returns the ID of this file collection
     * 
     * @return the ID of this file collection
     */
    public int getID() {
        return id;
    }

    /**
     * Returns this object, because the FileCollection instance is the root of
     * all files and directories contained in the collection.
     * 
     * @return this
     */
    @Override
    public MCRFileCollection getRoot() {
        return this;
    }

    @Override
    public MCRNode getChild(String name) {
        if (dataFile.equals(name)) {
            return null;
        } else {
            return super.getChild(name);
        }
    }

    @Override
    public boolean hasChildren() throws IOException {
        return getUsableChildSream().findAny().isPresent();
    }

    private Stream<Path> getUsableChildSream() throws IOException {
        return Files.list(path)
            .filter(p -> !dataFile.equals(p.getFileName().toString()));
    }

    @Override
    public int getNumChildren() throws IOException {
        return Math.toIntExact(getUsableChildSream().count());
    }

    @Override
    public String getName() {
        return "";
    }

    /**
     * Repairs additional metadata stored for all files and directories in this
     * collection
     */
    @Override
    public void repairMetadata() throws IOException {
        super.repairMetadata();
        writeData(e -> {
            e.setName("collection");
            e.removeAttribute("name");
        });
        saveAdditionalData();
    }

    /**
     * Returns additional metadata stored for all files and directories in this
     * collection
     */
    Document getMetadata() {
        return new Document(readData(Element::clone));
    }

}
