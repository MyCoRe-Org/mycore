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

package org.mycore.datamodel.niofs;

import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR;
import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR_STRING;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileStore;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.mycore.common.MCRException;

import com.google.common.primitives.Ints;

/**
 *  IFS implementation of the {@link Path} interface.
 *  Absolute path have this form: <code>{owner}':/'{path}</code>
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRPath implements Path {

    protected String root;

    protected String path;

    protected String stringValue;

    protected int[] offsets;

    protected final MCRAbstractFileSystem fileSystem;

    MCRPath(final String root, final String path, MCRAbstractFileSystem fileSystem) {
        this.root = root;
        this.path = normalizeAndCheck(Objects.requireNonNull(path, "path may not be null"));
        this.fileSystem = fileSystem;
        if (root == null || root.isEmpty()) {
            this.root = "";
            stringValue = this.path;
        } else {
            if (!path.isEmpty() && path.charAt(0) != SEPARATOR) {
                final String msg = new MessageFormat("If root is given, path has to start with ''{0}'': {1}",
                    Locale.ROOT).format(new Object[] { SEPARATOR_STRING, path });
                throw new IllegalArgumentException(msg);
            }
            stringValue = this.root + ":" + (this.path.isEmpty() ? SEPARATOR_STRING : this.path);
        }
        initNameComponents();
    }

    public static MCRPath ofPath(final Path other) {
        Objects.requireNonNull(other);
        if (!(other instanceof MCRPath)) {
            throw new ProviderMismatchException("other is not an instance of MCRPath: " + other.getClass());
        }
        return (MCRPath) other;
    }

    public static MCRPath getPath(String owner, String path) {
        Path resolved = MCRPaths.getPath(owner, path);
        return ofPath(resolved);
    }

    /**
     * Returns the root directory for a given derivate.
     *
     * @param owner the file owner (usually the id of a derivate)
     * @return the root path
     */
    public static MCRPath getRootPath(String owner) {
        return getPath(owner, "/");
    }

    /**
     * removes redundant slashes and checks for invalid characters
     * @param uncleanPath path to check
     * @return normalized path
     * @throws InvalidPathException if <code>uncleanPath</code> contains invalid characters
     */
    static String normalizeAndCheck(final String uncleanPath) {
        String unicodeNormalizedUncleanPath = Normalizer.normalize(uncleanPath, Normalizer.Form.NFC);

        char prevChar = 0;
        final boolean afterSeparator = false;
        for (int i = 0; i < unicodeNormalizedUncleanPath.length(); i++) {
            final char c = unicodeNormalizedUncleanPath.charAt(i);
            checkCharacter(unicodeNormalizedUncleanPath, c, afterSeparator);
            if (c == SEPARATOR && prevChar == SEPARATOR) {
                return normalize(unicodeNormalizedUncleanPath, unicodeNormalizedUncleanPath.length(), i - 1);
            }
            prevChar = c;
        }
        if (prevChar == SEPARATOR) {
            //remove final slash
            return normalize(unicodeNormalizedUncleanPath, unicodeNormalizedUncleanPath.length(),
                unicodeNormalizedUncleanPath.length() - 1);
        }
        return unicodeNormalizedUncleanPath;
    }

    private static void checkCharacter(final String input, final char c, final boolean afterSeparator) {
        if (c == '\u0000') {
            throw new InvalidPathException(input, "Nul character is not allowed.");
        }
        if (afterSeparator && c == ':') {
            throw new InvalidPathException(input, "':' is only allowed after owner id.");
        }

    }

    private static String normalize(final String input, final int length, final int offset) {
        if (length == 0) {
            return input;
        }

        int newLength = length;
        while (newLength > 0 && input.charAt(newLength - 1) == SEPARATOR) {
            newLength--;
        }

        if (newLength == 0) {
            return SEPARATOR_STRING;
        }

        final StringBuilder sb = new StringBuilder(input.length());
        boolean afterSeparator = false;

        if (offset > 0) {
            final String prefix = input.substring(0, offset);
            afterSeparator = prefix.contains(SEPARATOR_STRING);
            sb.append(prefix);
        }

        char prevChar = 0;
        for (int i = offset; i < newLength; i++) {
            final char c = input.charAt(i);
            checkCharacter(input, c, afterSeparator);

            if (c == SEPARATOR && prevChar == SEPARATOR) {
                continue;
            }

            sb.append(c);
            afterSeparator = afterSeparator || c == SEPARATOR;
            prevChar = c;
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#compareTo(java.nio.file.Path)
     */
    @Override
    public int compareTo(final Path other) {
        final MCRPath that = (MCRPath) Objects.requireNonNull(other);
        return toString().compareTo(that.toString());
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#endsWith(java.nio.file.Path)
     */
    @Override
    public boolean endsWith(final Path other) {
        Objects.requireNonNull(other, "other Path may not be null.");
        // Check if other path is MCRPath
        if (!(other instanceof MCRPath that)) {
            return false;
        }
        // Early return if both paths are equal
        if (this.equals(that)) {
            return true;
        }
        int thatPathLength = that.path.length();
        int thisPathLength = path.length();
        // thatPath cannot be longer than thisPath
        if (thatPathLength < thisPathLength) {
            if(that.isAbsolute()) {
                return handleAbsoluteEndsWith(that);
            }
            return handleRelativeEndsWith(that, thisPathLength, thatPathLength);
        }
        return false;
    }

    private boolean handleAbsoluteEndsWith(MCRPath that) {
        if((!isAbsolute() || !root.equals(that.root))) {
            return false;
        }
        return Objects.deepEquals(offsets, that.offsets)
            && path.equals(that.path)
            && that.getFileSystem().equals(getFileSystem());
    }

    private boolean handleRelativeEndsWith(MCRPath that, int thisPathLength, int thatPathLength) {
        int thisOffsetStart = createThisOffsetStart(that);
        if (thisOffsetStart != -1) {
            // Check if the remaining characters in the path match
            int thisPos = offsets[thisOffsetStart];
            int thatPos = that.offsets[0];
            if (thisPathLength - thisPos == thatPathLength - thatPos) {
                while (thisPos < thisPathLength) {
                    if (path.charAt(thisPos) != that.path.charAt(thatPos)) {
                        return false;
                    }
                    thisPos++;
                    thatPos++;
                }
                return that.getFileSystem().equals(getFileSystem());
            }
        }
        return false;
    }

    private int createThisOffsetStart(MCRPath that) {
        final int thatOffsetCount = that.offsets.length;
        final int thisOffsetCount = offsets.length;
        final int thatPathLength = that.path.length();
        final int thisPathLength = path.length();
        // If `that` is not absolute, we check the offsets and path characters
        if (thatOffsetCount > thisOffsetCount
            || (thisOffsetCount == thatOffsetCount && thisPathLength != thatPathLength)) {
            return -1;
        }
        int thisOffsetStart = thisOffsetCount - thatOffsetCount;
        for (int i = 0; i < thatOffsetCount; i++) {
            if (that.offsets[i] != offsets[thisOffsetStart + i]) {
                return -1;
            }
        }
        return thisOffsetStart;
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#endsWith(java.lang.String)
     */
    @Override
    public boolean endsWith(final String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    @Override
    public boolean equals(final Object obj) {
        boolean result;
        if (!(obj instanceof MCRPath that) || !getFileSystem().equals(that.getFileSystem())) {
            result = false;
        } else {
            result = stringValue.equals(that.stringValue);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#getFileName()
     */
    @Override
    public Path getFileName() {
        final int nameCount = getNameCount();
        if (nameCount == 0) {
            return null;
        }
        final int lastOffset = offsets[nameCount - 1];
        final String fileName = path.substring(lastOffset);
        return getPath(null, fileName, getFileSystem());
    }

    @Override
    public MCRAbstractFileSystem getFileSystem() {
        return this.fileSystem;
    }

    protected MCRPath getPath(String owner, String path, MCRAbstractFileSystem fs) {
        return fs.provider().getPath(owner, path);
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#getName(int)
     */
    @Override
    public MCRPath getName(final int index) {
        final int nameCount = getNameCount();
        if (index < 0 || index >= nameCount) {
            throw new IllegalArgumentException();
        }
        final String pathElement = getPathElement(index);
        return getPath(null, pathElement, getFileSystem());
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#getNameCount()
     */
    @Override
    public int getNameCount() {
        return offsets.length;
    }

    public String getOwner() {
        return root;
    }

    public String getOwnerRelativePath() {
        return (path.equals("")) ? "/" : path;
    }

    /**
     * returns complete subpath.
     * same as {@link #subpath(int, int)} with '0' and '{@link #getNameCount()}'.
     */
    public MCRPath subpathComplete() {
        return isAbsolute() ? subpath(0, offsets.length) : this;
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#getParent()
     */
    @Override
    public MCRPath getParent() {
        final int nameCount = getNameCount();
        if (nameCount == 0) {
            return null;
        }
        final int lastOffset = offsets[nameCount - 1] - 1;
        if (lastOffset <= 0) {
            if (root.isEmpty()) {
                if (path.startsWith("/")) {
                    //we have root as parent
                    return getPath(root, "/", getFileSystem());
                }
                // path is like "foo" -> no parent
                return null;
            }
            return getRoot();
        }
        return getPath(root, path.substring(0, lastOffset), getFileSystem());
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#getRoot()
     */
    @Override
    public MCRPath getRoot() {
        if (!isAbsolute()) {
            return null;
        }
        if (getNameCount() == 0) {
            return this;
        }
        return getFileSystem().getRootDirectory(root);
    }

    @Override
    public int hashCode() {
        return stringValue.hashCode();
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#isAbsolute()
     */
    @Override
    public boolean isAbsolute() {
        return root == null || !root.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#iterator()
     */
    @Override
    public Iterator<Path> iterator() {
        return new Iterator<>() {
            int i;

            @Override
            public boolean hasNext() {
                return i < getNameCount();
            }

            @Override
            public Path next() {
                if (hasNext()) {
                    final Path result = getName(i);
                    i++;
                    return result;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#normalize()
     */
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity"})
    @Override
    public MCRPath normalize() {
        final int count = getNameCount();
        int remaining = count;
        final boolean[] ignoreSubPath = new boolean[count];

        for (int i = 0; i < count; i++) {
            if (ignoreSubPath[i]) {
                continue;
            }
            final int subPathIndex = offsets[i];
            int subPathLength;
            if (i == offsets.length - 1) {
                subPathLength = path.length() - subPathIndex;
            } else {
                subPathLength = offsets[i + 1] - subPathIndex - 1;
            }
            if (path.charAt(subPathIndex) == '.') {
                if (subPathLength == 1) {
                    ignoreSubPath[i] = true;
                    remaining--;
                } else if (subPathLength == 2 && path.charAt(subPathIndex + 1) == '.') {
                    ignoreSubPath[i] = true;
                    remaining--;
                    //go backward to the last unignored and mark it ignored
                    //can't normalize if all preceding elements already ignored
                    for (int r = i - 1; r > 0; r--) {
                        if (!ignoreSubPath[r]) {
                            ignoreSubPath[r] = true;
                            remaining--;
                            break;
                        }
                    }
                }
            }

        }

        if (count == remaining) {
            return this;
        }
        if (remaining == 0) {
            return isAbsolute() ? getRoot() : getFileSystem().emptyPath();
        }
        final StringBuilder sb = new StringBuilder(path.length());
        if (isAbsolute()) {
            sb.append(SEPARATOR);
        }
        for (int i = 0; i < count; i++) {
            if (ignoreSubPath[i]) {
                continue;
            }
            sb.append(getPathElement(i));
            remaining--;
            if (remaining > 0) {
                sb.append('/');
            }
        }
        return getPath(root, sb.toString(), getFileSystem());
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[])
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[], java.nio.file.WatchEvent.Modifier[])
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#relativize(java.nio.file.Path)
     */
    @Override
    public MCRPath relativize(final Path other) {
        if (equals(Objects.requireNonNull(other, "Cannot relativize against 'null'."))) {
            return getFileSystem().emptyPath();
        }
        if (isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException("'other' must be absolute if and only if this is absolute, too.");
        }
        final MCRPath that = ofPath(other);
        if (!isAbsolute() && isEmpty()) {
            return that;
        }
        URI thisURI;
        URI thatURI;
        try {
            thisURI = new URI(null, null, path, null);
            thatURI = new URI(null, null, that.path, null);
        } catch (URISyntaxException e) {
            throw new MCRException(e);
        }
        final URI relativizedURI = thisURI.relativize(thatURI);
        if (thatURI.equals(relativizedURI)) {
            return that;
        }
        return getPath(null, relativizedURI.getPath(), getFileSystem());
    }

    private static boolean isEmpty(Path test) {
        return test instanceof MCRPath && ((MCRPath) test).isEmpty()
            || (test.getNameCount() == 1 && test.getName(0).toString().isEmpty());
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#resolve(java.nio.file.Path)
     */
    @Override
    public Path resolve(final Path other) {
        if (other.isAbsolute()) {
            return other;
        }
        if (isEmpty(other)) {
            return this;
        }
        String otherStr = toMCRPathString(other);
        final int baseLength = path.length();
        final int childLength = other.toString().length();
        if (isEmpty() || otherStr.charAt(0) == SEPARATOR) {
            return root == null ? other : getPath(root, otherStr, getFileSystem());
        }
        final StringBuilder result = new StringBuilder(baseLength + 1 + childLength);
        if (baseLength != 1 || path.charAt(0) != SEPARATOR) {
            result.append(path);
        }
        result.append(SEPARATOR);
        result.append(otherStr);
        return getPath(root, result.toString(), getFileSystem());
    }

    private String toMCRPathString(final Path other) {
        String otherStr = other.toString();
        String otherSeperator = other.getFileSystem().getSeparator();
        return otherSeperator.equals(SEPARATOR_STRING) ? otherStr : otherStr.replace(otherSeperator, SEPARATOR_STRING);
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#resolve(java.lang.String)
     */
    @Override
    public Path resolve(final String other) {
        return resolve(getFileSystem().getPath(other));
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#resolveSibling(java.nio.file.Path)
     */
    @Override
    public Path resolveSibling(final Path other) {
        Objects.requireNonNull(other);
        final Path parent = getParent();
        return parent == null ? other : parent.resolve(other);
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#resolveSibling(java.lang.String)
     */
    @Override
    public Path resolveSibling(final String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#startsWith(java.nio.file.Path)
     */
    @Override
    public boolean startsWith(final Path other) {
        if (!(Objects.requireNonNull(other, "other Path may not be null.") instanceof MCRPath)) {
            return false;
        }
        final MCRPath that = (MCRPath) other;
        if (this.equals(that)) {
            return true;
        }
        final int thatOffsetCount = that.offsets.length;
        final int thisOffsetCount = offsets.length;

        //checks required by Path.startsWidth()
        if ((thatOffsetCount > thisOffsetCount || that.path.length() > path.length()) ||
            (thatOffsetCount == thisOffsetCount && that.path.length() != path.length()) ||
            (!Objects.deepEquals(root, that.root) || !Objects.deepEquals(getFileSystem(), that.getFileSystem())) ||
            !path.startsWith(that.path)) {
            return false;
        }

        for (int i = 0; i < thatOffsetCount; i++) {
            if (that.offsets[i] != offsets[i]) {
                return false;
            }
        }

        final int thatPathLength = that.path.length();
        // return false if this.path==/foo/bar and that.path==/path
        return thatPathLength <= path.length() || path.charAt(thatPathLength) == SEPARATOR;
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#startsWith(java.lang.String)
     */
    @Override
    public boolean startsWith(final String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#subpath(int, int)
     */
    @Override
    public MCRPath subpath(final int beginIndex, final int endIndex) {
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex may not be negative: " + beginIndex);
        }
        if (beginIndex >= offsets.length) {
            throw new IllegalArgumentException("beginIndex may not be greater or qual to the number of path elements("
                + offsets.length + "): " + beginIndex);
        }
        if (endIndex > offsets.length) {
            throw new IllegalArgumentException("endIndex may not be greater that the number of path elements("
                + offsets.length + "): " + endIndex);
        }
        if (beginIndex >= endIndex) {
            throw new IllegalArgumentException("endIndex must be greater than beginIndex(" + beginIndex + "): "
                + endIndex);
        }
        final int begin = offsets[beginIndex];
        final int end = endIndex == offsets.length ? path.length() : offsets[endIndex] - 1;
        return getPath(null, path.substring(begin, end), getFileSystem());
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#toAbsolutePath()
     */
    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        }
        throw new IOError(new IOException("There is no default directory to resolve " + this + " against to."));
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#toFile()
     */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#toRealPath(java.nio.file.LinkOption[])
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        if (isAbsolute()) {
            final MCRPath normalized = normalize();
            getFileSystem().provider().checkAccess(normalized); //eventually throws IOException
            return normalized;
        }
        throw new IOException("Cannot get real path from relative path.");
    }

    @SuppressWarnings("resource")
    public Path toPhysicalPath() throws IOException {
        if (isAbsolute()) {
            for (FileStore fs : getFileSystem().getFileStores()) {
                if (fs instanceof MCRAbstractFileStore mcrfs) {
                    Path physicalPath = mcrfs.getPhysicalPath(this);
                    if (physicalPath != null) {
                        return physicalPath;
                    }
                }
            }
            return null;
        }
        throw new IOException("Cannot get real path from relative path.");
    }

    @Override
    public String toString() {
        return stringValue;
    }

    /* (non-Javadoc)
     * @see java.nio.file.Path#toUri()
     */
    @Override
    public URI toUri() {
        try {
            if (isAbsolute()) {
                return MCRPaths.getURI(getFileSystem().provider().getScheme(), root, path);
            }
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getPathElement(final int index) {
        final int begin = offsets[index];
        final int end = index == offsets.length - 1 ? path.length() : offsets[index + 1] - 1;
        return path.substring(begin, end);
    }

    private void initNameComponents() {
        final List<Integer> list = new ArrayList<>();
        if (isEmpty()) {
            if (!isAbsolute()) {
                // is empty path but not root component
                // empty path considered to have one name element
                list.add(0);
            }
        } else {
            int start = 0;
            while (start < path.length()) {
                if (path.charAt(start) != SEPARATOR) {
                    break;
                }
                start++;
            }
            int off = start;
            while (off < path.length()) {
                if (path.charAt(off) != SEPARATOR) {
                    off++;
                } else {
                    list.add(start);
                    start = ++off;
                }
            }
            if (start != off) {
                list.add(start);
            }
        }
        offsets = Ints.toArray(list);
    }

    private boolean isEmpty() {
        return path.isEmpty();
    }

}
