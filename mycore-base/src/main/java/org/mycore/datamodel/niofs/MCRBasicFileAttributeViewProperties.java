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

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A utility class for working with file attributes using a specified {@link BasicFileAttributeView}.
 *
 * @param <V> the type of the {@link BasicFileAttributeView}.
 */
public class MCRBasicFileAttributeViewProperties<V extends BasicFileAttributeView> {

    /**
     * Enum representing the various file attributes that can be accessed.
     */
    private enum Attribute {
        ALL("*"),
        SIZE_NAME("size"),
        CREATION_TIME_NAME("creationTime"),
        LAST_ACCESS_TIME_NAME("lastAccessTime"),
        LAST_MODIFIED_TIME_NAME("lastModifiedTime"),
        FILE_KEY_NAME("fileKey"),
        IS_DIRECTORY_NAME("isDirectory"),
        IS_REGULAR_FILE_NAME("isRegularFile"),
        IS_SYMBOLIC_LINK_NAME("isSymbolicLink"),
        IS_OTHER_NAME("isOther");

        private final String name;

        Attribute(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * @deprecated use {@link #fromName(String)} instead
         */
        @Deprecated
        public static Attribute ofName(String name) {
            return fromName(name);
        }

        public static Attribute fromName(String name) {
            return Arrays.stream(values())
                .filter(attribute -> attribute.getName().equals(name))
                .findAny()
                .orElse(null);
        }

    }

    /**
     * Set of allowed attributes that can be accessed.
     */
    public static final Set<String> ALLOWED_ATTRIBUTES = Arrays.stream(Attribute.values())
        .map(Attribute::getName)
        .collect(Collectors.toSet());

    private final V view;

    /**
     * Constructs a new {@code MCRBasicFileAttributeViewProperties} with the specified view.
     *
     * @param view the {@link BasicFileAttributeView} to be used.
     */
    public MCRBasicFileAttributeViewProperties(V view) {
        this.view = view;
    }

    /**
     * Gets the underlying view.
     *
     * @return the {@link BasicFileAttributeView}.
     */
    public V getView() {
        return view;
    }

    /**
     * Retrieves a map of file attributes.
     *
     * @param attributes the attributes to be retrieved. Use "*" to retrieve all attributes.
     * @return a map of attribute names to their values.
     * @throws IOException if an I/O error occurs.
     */
    public Map<String, Object> getAttributeMap(String... attributes) throws IOException {
        Set<String> allowed = getAllowedAttributes();
        boolean copyAll = false;
        for (String attr : attributes) {
            if (!allowed.contains(attr)) {
                throw new IllegalArgumentException("'" + attr + "' not recognized");
            }
            if (Objects.equals(attr, Attribute.ALL.getName())) {
                copyAll = true;
            }
        }
        Set<String> requested = copyAll ? allowed : Set.of(attributes);
        return buildMap(requested);
    }

    /**
     * Builds a map of requested attributes and their values.
     *
     * @param requested the set of attributes to be retrieved.
     * @return a map of attribute names to their values.
     * @throws IOException if an I/O error occurs.
     */
    protected Map<String, Object> buildMap(Set<String> requested) throws IOException {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Object> map = new HashMap<>();
        BasicFileAttributes attrs = view.readAttributes();
        for (String attributeName : requested) {
            Attribute attribute = Attribute.fromName(attributeName);
            switch (attribute) {
                case SIZE_NAME -> map.put(attributeName, attrs.size());
                case CREATION_TIME_NAME -> map.put(attributeName, attrs.creationTime());
                case LAST_ACCESS_TIME_NAME -> map.put(attributeName, attrs.lastAccessTime());
                case LAST_MODIFIED_TIME_NAME -> map.put(attributeName, attrs.lastModifiedTime());
                case FILE_KEY_NAME -> map.put(attributeName, attrs.fileKey());
                case IS_DIRECTORY_NAME -> map.put(attributeName, attrs.isDirectory());
                case IS_REGULAR_FILE_NAME -> map.put(attributeName, attrs.isRegularFile());
                case IS_SYMBOLIC_LINK_NAME -> map.put(attributeName, attrs.isSymbolicLink());
                case IS_OTHER_NAME -> map.put(attributeName, attrs.isOther());
                default -> {
                }
            }
        }
        return map;
    }

    /**
     * Sets the value of the specified attribute.
     *
     * @param name the name of the attribute.
     * @param value the new value of the attribute.
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException  if the attribute is read-only.
     */
    public void setAttribute(String name, Object value) throws IOException {
        Set<String> allowed = getAllowedAttributes();
        if (Objects.equals(name, Attribute.ALL.getName()) || !allowed.contains(name)) {
            throw new IllegalArgumentException("'" + name + "' not recognized");
        }
        Attribute attribute = Attribute.fromName(name);
        switch (attribute) {
            case CREATION_TIME_NAME -> view.setTimes(null, null, (FileTime) value);
            case LAST_ACCESS_TIME_NAME -> view.setTimes(null, (FileTime) value, null);
            case LAST_MODIFIED_TIME_NAME -> view.setTimes((FileTime) value, null, null);
            case SIZE_NAME,
                FILE_KEY_NAME,
                IS_DIRECTORY_NAME,
                IS_REGULAR_FILE_NAME,
                IS_SYMBOLIC_LINK_NAME,
                IS_OTHER_NAME -> throw new IllegalArgumentException(
                    "'" + name + "' is a read-only attribute.");
            default -> {
                //ignored
            }
        }
    }

    /**
     * Gets the set of allowed attributes.
     *
     * @return the set of allowed attributes.
     */
    protected Set<String> getAllowedAttributes() {
        return ALLOWED_ATTRIBUTES;
    }

}
