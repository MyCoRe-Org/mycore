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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;

/**
 * Stores metadata files or file collections containing files and directories in
 * a persistent store implemented using a local filesystem.
 * 
 * For better filesystem performance, the store can build slot subdirectories
 * (containing other subdirectories and so on) so that not all objects are
 * stored in the same filesystem directory. Directories containing a very large
 * number of files typically show bad performance.
 * 
 * The slot layout of the store defines the usage of subdirectories within the
 * base directory. A layout of "8" would mean no subdirectories will be used,
 * the maximum ID size is 8 digits, and therefore up to 99999999 objects can be
 * stored all in the same base directory. A layout of "2-2-4" would mean data is
 * stored using two levels of subdirectories, where the first subdirectory
 * contains up to 100 (00-99) subdirectories, the second subdirectory level
 * below contains up to 100 subdirectories, too, and below the data is stored,
 * with up to 10000 data objects in the subdirectory. Using this slot layout,
 * the data of ID 10485 would be stored in the file object "/00/01/00010485",
 * for example. Using layout "4-2-2", data would be stored in
 * "/0001/04/00010485", and so on.
 * 
 * The slot file name itself may optionally have a prefix and suffix. With
 * prefix "derivate-", the slot name would be "derivate-00010485". With prefix
 * "DocPortal_document_" and suffix ".xml", the slot name would be
 * "DocPortal_document_00010485.xml" for example.
 * 
 * MCR.IFS2.Store.ID.Class=org.mycore.datamodel.ifs2.MCRFileStore
 * MCR.IFS2.Store.ID.BaseDir=/foo/bar
 * MCR.IFS2.Store.ID.SlotLayout=4-2-2
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRStore {

    /**
     * Indicates ascending order when listing IDs
     */
    public static final boolean ASCENDING = true;

    /**
     * Indicates descending order when listing IDs
     */
    public static final boolean DESCENDING = false;

    /** The ID of the store */
    protected String id;

    /** The base directory containing the stored data */
    protected Path baseDirectory;

    /** The maximum length of IDs **/
    protected int idLength;

    /**
     * The slot subdirectory layout, which is the number of digits used at each
     * subdirectory level to build the filename.
     */
    protected int[] slotLength;

    /** The prefix of slot names */
    protected String prefix = "";

    /** The suffix of slot names */
    protected String suffix = "";

    private MCRStoreConfig storeConfig;

    private Function<String, String> toNativePath;

    /**
     * Offset to add to the maximum ID found in the store to build the new ID.
     * This is normally 1, but initially higher to avoid reassigning the same ID
     * after system restarts. Consider the following example:
     * 
     * 1) User creates new document, ID assigned is 10. 2) User deletes document
     * 10. 3) Web application is restarted. 4) User creates new document, ID
     * assigned is 20. If offset would always be 1, ID assigned would have been
     * 10 again, and that is not nice, because we can not distinguish the two
     * creates easily.
     */
    protected int offset = 11; // Sicherheitsabstand, initially 11, later 1

    /**
     * The last ID assigned by this store.
     */
    protected int lastID = 0;

    public static final Logger LOGGER = LogManager.getLogger();

    /**
     * Deletes the data stored under the given ID from the store
     * 
     * @param id
     *            the ID of the document to be deleted
     */
    public void delete(final int id) throws IOException {
        delete(getSlot(id));
    }

    /**
     * Returns true if data for the given ID is existing in the store.
     * 
     * @param id
     *            the ID of the data
     * @return true, if data for the given ID is existing in the store.
     */
    public boolean exists(final int id) throws IOException {
        return Files.exists(getSlot(id));
    }

    public synchronized int getHighestStoredID() {
        try {
            String max = findMaxID(baseDirectory, 0);
            if (max != null) {
                return slot2id(max);
            }
        } catch (final IOException e) {
            LOGGER.error("Error while getting highest stored ID in " + baseDirectory, e);
        }
        return 0;
    }

    /**
     * Returns the ID of this store
     */
    public String getID() {
        return getStoreConfig().getID();
    }

    /**
     * Returns the next free ID that can be used to store data. Call as late as
     * possible to avoid that another process, for example from batch import, in
     * the meantime already used that ID.
     * 
     * @return the next free ID that can be used to store data
     */
    public synchronized int getNextFreeID() {
        lastID = Math.max(getHighestStoredID(), lastID);
        lastID += lastID > 0 ? offset : 1;
        offset = 1;
        return lastID;
    }

    public boolean isEmpty() {
        try (Stream<Path> streamBaseDirectory = Files.list(baseDirectory)) {
            return streamBaseDirectory.findAny().isEmpty();
        } catch (final IOException e) {
            LOGGER.error("Error while checking if base directory is empty: " + baseDirectory, e);
            return false;
        }
    }

    /**
     * @return all Ids of this store
     */
    public IntStream getStoredIDs() {
        int characteristics = Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED;
        return StreamSupport
            .stream(() -> Spliterators
                .spliteratorUnknownSize(listIDs(ASCENDING), characteristics),
                characteristics,
                false)
            .mapToInt(Integer::intValue);
    }

    /**
     * Lists all IDs currently used in the store, in ascending or descending
     * order
     * 
     * @see #ASCENDING
     * @see #DESCENDING
     * 
     * @param order
     *            the order in which IDs should be returned.
     * @return all IDs currently used in the store
     */
    public Iterator<Integer> listIDs(final boolean order) {
        return new Iterator<Integer>() {
            /**
             * List of files or directories in store not yet handled
             */
            List<Path> files = new ArrayList<>();

            /**
             * The next ID to return, when 0, all IDs have been returned
             */
            int nextID;

            /**
             * The last ID that was returned
             */
            int lastID;

            /**
             * The order in which the IDs should be returned, ascending or
             * descending
             */
            boolean order;

            @Override
            public boolean hasNext() {
                return nextID > 0;
            }

            @Override
            public Integer next() {
                if (nextID < 1) {
                    throw new NoSuchElementException();
                }

                lastID = nextID;
                nextID = findNextID();
                return lastID;
            }

            @Override
            public void remove() {
                if (lastID == 0) {
                    throw new IllegalStateException();
                }
                try {
                    MCRStore.this.delete(lastID);
                } catch (final Exception ex) {
                    throw new MCRException("Could not delete " + MCRStore.this.getID() + " " + lastID, ex);
                }
                lastID = 0;
            }

            /**
             * Initializes the enumeration and searches for the first ID to
             * return
             * 
             * @param order
             *            the return order, ascending or descending
             */
            Iterator<Integer> init(final boolean order) {
                this.order = order;
                try {
                    addChildren(baseDirectory);
                } catch (final IOException e) {
                    LOGGER.error("Error while iterating over children of " + baseDirectory, e);
                }
                nextID = findNextID();
                return this;
            }

            /**
             * Adds children of the given directory to the list of files to
             * handle next. Depending on the return sort order, ascending or
             * descending file name order is used.
             * 
             * @param dir
             *            the directory thats children should be added
             * @throws IOException
             */
            private void addChildren(final Path dir) throws IOException {
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> steamDir = Files.list(dir)) {
                        final Path[] children = steamDir.toArray(Path[]::new);
                        Arrays.sort(children, new MCRPathComparator());

                        for (int i = 0; i < children.length; i++) {
                            files.add(order ? i : 0, children[i]);
                        }
                    }
                }
            }

            /**
             * Finds the next ID used in the store.
             * 
             * @return the next ID, or 0 if there is no other ID any more
             */
            private int findNextID() {
                if (files.isEmpty()) {
                    return 0;
                }

                final Path first = files.remove(0);
                // checks basename length against prefix (projectId_typeId), file suffix (.xml) and configured id length
                // if they match it should be a parseable id
                String fileName = first.getFileName().toString();
                if (fileName.length() == idLength + prefix.length() + suffix.length()) {
                    return MCRStore.this.slot2id(fileName);
                }

                try {
                    addChildren(first);
                } catch (final IOException e) {
                    LOGGER.error("Error while finding next id.", e);
                }
                return findNextID();
            }
        }.init(order);
    }

    /**
     * Deletes the data stored in the given file object from the store
     *
     * @see <a href="https://stackoverflow.com/questions/39628328/trying-to-create-a-directory-immediately-after-a-successful-deleteifexists-throw">stackoverflow</a>
     * @param path
     *            the file object to be deleted
     */
    void delete(final Path path) throws IOException {
        if (!path.startsWith(baseDirectory)) {
            throw new IllegalArgumentException(path + " is not in the base directory " + baseDirectory);
        }
        Path current = path;
        Path parent = path.getParent();
        Files.walkFileTree(path, MCRRecursiveDeleter.instance());

        while (!Files.isSameFile(baseDirectory, parent)) {

            // Prevent access denied error in windows with closing the stream correctly
            try (Stream<Path> streamParent = Files.list(parent)) {
                if (streamParent.findAny().isPresent()) {
                    break;
                }
                current = parent;
                parent = current.getParent();
                Files.delete(current);
            }
        }
    }

    /**
     * @return the absolute path of the local base directory
     */
    public Path getBaseDirectory() {
        return baseDirectory.toAbsolutePath();
    }

    /**
     * Returns the absolute path of the local base directory
     * 
     * @return the base directory storing the data
     */
    String getBaseDirURI() {
        return baseDirectory.toAbsolutePath().toUri().toString();
    }

    /** Returns the maximum length of any ID stored in this store */
    int getIDLength() {
        return idLength;
    }

    /**
     * Returns the relative path used to store data for the given id within the
     * store base directory
     * 
     * @param id
     *            the id of the data
     * @return the relative path storing that data
     */
    String getSlotPath(final int id) {
        final String[] paths = getSlotPaths(id);
        return paths[paths.length - 1];
    }

    /**
     * Returns the paths of all subdirectories and the slot itself used to store
     * data for the given id relative to the store base directory
     * 
     * @param id
     *            the id of the data
     * @return the directory and file names of the relative path storing that
     *         data
     */
    String[] getSlotPaths(final int id) {
        final String paddedId = createIDWithLeadingZeros(id);

        final String[] paths = new String[slotLength.length + 1];
        final StringBuilder path = new StringBuilder();
        int offset = 0;
        for (int i = 0; i < paths.length - 1; i++) {
            path.append(paddedId, offset, offset + slotLength[i]);
            paths[i] = path.toString();
            path.append("/");
            offset += slotLength[i];
        }
        path.append(prefix).append(paddedId).append(suffix);
        paths[paths.length - 1] = path.toString();
        return paths;
    }

    /**
     * Extracts the numerical ID contained in the slot filename.
     * 
     * @param slot
     *            the file name of the slot containing the data
     * @return the ID of that data
     */
    int slot2id(String slot) {
        slot = slot.substring(prefix.length());
        slot = slot.substring(0, idLength);
        return Integer.parseInt(slot);
    }

    /**
     * Returns the slot file object used to store data for the given id. This
     * may be a file or directory, depending on the subclass of MCRStore that is
     * used.
     * 
     * @param id
     *            the id of the data
     * @return the file object storing that data
     */
    protected Path getSlot(final int id) {
        String slotPath = getSlotPath(id);
        return baseDirectory.resolve(toNativePath.apply(slotPath));
    }

    protected MCRStoreConfig getStoreConfig() {
        return storeConfig;
    }

    protected void init(final MCRStoreConfig config) {
        setStoreConfig(config);

        idLength = 0;

        final StringTokenizer st = new StringTokenizer(getStoreConfig().getSlotLayout(), "-");
        slotLength = new int[st.countTokens() - 1];

        int i = 0;
        while (st.countTokens() > 1) {
            slotLength[i] = Integer.parseInt(st.nextToken());
            idLength += slotLength[i++];
        }
        idLength += Integer.parseInt(st.nextToken());
        prefix = config.getPrefix();

        try {
            try {
                URI uri = new URI(getStoreConfig().getBaseDir());
                if (uri.getScheme() != null) {
                    baseDirectory = Paths.get(uri);
                }
            } catch (URISyntaxException e) {
                //not a uri, handle as relative path
            }
            if (baseDirectory == null) {
                baseDirectory = Paths.get(getStoreConfig().getBaseDir());
            }

            String separator = baseDirectory.getFileSystem().getSeparator();
            if (separator.equals("/")) {
                toNativePath = s -> s;
            } else {
                toNativePath = s -> {
                    if (s.contains("/")) {
                        if (s.contains(separator)) {
                            throw new IllegalArgumentException(
                                s + " may not contain both '/' and '" + separator + "'.");
                        }
                        return s.replace("/", separator);
                    }
                    return s;
                };
            }

            try {
                BasicFileAttributes attrs = Files.readAttributes(baseDirectory, BasicFileAttributes.class);
                if (!attrs.isDirectory()) {
                    final String msg = "Store " + getStoreConfig().getBaseDir() + " is not a directory";
                    throw new MCRConfigurationException(msg);
                }

                if (!Files.isReadable(baseDirectory)) {
                    final String msg = "Store directory " + getStoreConfig().getBaseDir() + " is not readable";
                    throw new MCRConfigurationException(msg);
                }
            } catch (IOException e) {
                //does not exist;
                Files.createDirectories(baseDirectory);
            }
        } catch (final IOException e) {
            LOGGER.error("Could not initialize store " + config.getID() + " correctly.", e);
        }
    }

    /**
     * Initializes a new store instance
     */
    protected void init(final String id) {
        init(new MCRStoreDefaultConfig(id));
    }

    protected void setStoreConfig(final MCRStoreConfig storeConfig) {
        this.storeConfig = storeConfig;
    }

    private String createIDWithLeadingZeros(final int id) {
        final NumberFormat numWithLeadingZerosFormat = NumberFormat.getIntegerInstance(Locale.ROOT);
        numWithLeadingZerosFormat.setMinimumIntegerDigits(idLength);
        numWithLeadingZerosFormat.setGroupingUsed(false);
        return numWithLeadingZerosFormat.format(id);
    }

    /**
     * Recursively searches for the highest ID, which is the greatest slot file
     * name currently used in the store.
     * 
     * @param dir
     *            the directory to search
     * @param depth
     *            the subdirectory depth level of the dir
     * @return the highest slot file name / ID currently stored
     */
    private String findMaxID(final Path dir, final int depth) throws IOException {

        final Path[] children;

        try (Stream<Path> streamDirectory = Files.list(dir)) {
            children = streamDirectory.toArray(Path[]::new);
        }

        if (children.length == 0) {
            return null;
        }

        Arrays.sort(children, new MCRPathComparator());

        if (depth == slotLength.length) {
            return children[children.length - 1].getFileName().toString();
        }

        for (int i = children.length - 1; i >= 0; i--) {
            final Path child = children[i];

            try (Stream<Path> streamChild = Files.list(child)) {
                if (!Files.isDirectory(child) || streamChild.findAny().isEmpty()) {
                    continue;
                }
            }

            final String found = findMaxID(child, depth + 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public interface MCRStoreConfig {
        String getBaseDir();

        String getID();

        String getPrefix();

        String getSlotLayout();
    }
}
