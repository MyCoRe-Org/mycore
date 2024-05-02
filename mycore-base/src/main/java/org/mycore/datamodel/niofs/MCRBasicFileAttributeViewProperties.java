package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

public class MCRBasicFileAttributeViewProperties<V extends BasicFileAttributeView> {

    private static final String ALL = "*";

    private static final String SIZE_NAME = "size";

    private static final String CREATION_TIME_NAME = "creationTime";

    private static final String LAST_ACCESS_TIME_NAME = "lastAccessTime";

    private static final String LAST_MODIFIED_TIME_NAME = "lastModifiedTime";

    private static final String FILE_KEY_NAME = "fileKey";

    private static final String IS_DIRECTORY_NAME = "isDirectory";

    private static final String IS_REGULAR_FILE_NAME = "isRegularFile";

    private static final String IS_SYMBOLIC_LINK_NAME = "isSymbolicLink";

    private static final String IS_OTHER_NAME = "isOther";

    public static final HashSet<String> ALLOWED_ATTRIBUTES = Sets.newHashSet(ALL, SIZE_NAME, CREATION_TIME_NAME,
        LAST_ACCESS_TIME_NAME, LAST_MODIFIED_TIME_NAME, FILE_KEY_NAME, IS_DIRECTORY_NAME, IS_REGULAR_FILE_NAME,
        IS_SYMBOLIC_LINK_NAME, IS_OTHER_NAME);

    private final V view;

    public MCRBasicFileAttributeViewProperties(V view) {
        this.view = view;
    }

    public V getView() {
        return view;
    }

    public Map<String, Object> getAttributeMap(String... attributes)
        throws IOException {
        Set<String> allowed = getAllowedAttributes();
        boolean copyAll = false;
        for (String attr : attributes) {
            if (!allowed.contains(attr)) {
                throw new IllegalArgumentException("'" + attr + "' not recognized");
            }
            if (Objects.equals(attr, ALL)) {
                copyAll = true;
            }
        }
        Set<String> requested = copyAll ? allowed : Sets.newHashSet(attributes);
        return buildMap(requested);
    }

    protected Map<String, Object> buildMap(Set<String> requested) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        BasicFileAttributes attrs = view.readAttributes();
        for (String attr : requested) {
            switch (attr) {
                case SIZE_NAME -> map.put(attr, attrs.size());
                case CREATION_TIME_NAME -> map.put(attr, attrs.creationTime());
                case LAST_ACCESS_TIME_NAME -> map.put(attr, attrs.lastAccessTime());
                case LAST_MODIFIED_TIME_NAME -> map.put(attr, attrs.lastModifiedTime());
                case FILE_KEY_NAME -> map.put(attr, attrs.fileKey());
                case IS_DIRECTORY_NAME -> map.put(attr, attrs.isDirectory());
                case IS_REGULAR_FILE_NAME -> map.put(attr, attrs.isRegularFile());
                case IS_SYMBOLIC_LINK_NAME -> map.put(attr, attrs.isSymbolicLink());
                case IS_OTHER_NAME -> map.put(attr, attrs.isOther());
                default -> {
                }
                //ignored
            }
        }
        return map;
    }

    public void setAttribute(String name, Object value) throws IOException {
        Set<String> allowed = getAllowedAttributes();
        if (Objects.equals(name, ALL) || !allowed.contains(name)) {
            throw new IllegalArgumentException("'" + name + "' not recognized");
        }
        switch (name) {
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

    protected Set<String> getAllowedAttributes() {
        return ALLOWED_ATTRIBUTES;
    }

}
