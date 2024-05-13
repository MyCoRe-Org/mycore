package org.mycore.datamodel.niofs;

import java.nio.file.attribute.FileTime;

import org.mycore.common.digest.MCRDigest;

/**
 * Provides a lazy loading implementation for {@link MCRFileAttributes}.
 *
 * <p>The attributes are loaded only once on the first request and are then cached for subsequent uses.</p>
 *
 * <p>This approach is particularly beneficial in remote filesystems where network latency and data transfer costs
 * are significant. In some scenarios, instances of this proxy might be created without ever accessing the underlying
 * attributes, thus avoiding unnecessary network traffic entirely.</p>
 *
 * @param <T> The type of the file key.
 * @param <P> The type of the {@link MCRPath} associated with these file attributes.
 */
public abstract class MCRBasicFileAttributesProxy<T, P extends MCRPath> implements MCRFileAttributes<T> {

    private volatile MCRFileAttributes<T> proxy;

    private final Object lock = new Object();

    protected final P path;

    public MCRBasicFileAttributesProxy(P path) {
        this.path = path;
    }

    @Override
    public FileTime lastModifiedTime() {
        return proxy().lastModifiedTime();
    }

    @Override
    public FileTime lastAccessTime() {
        return proxy().lastAccessTime();
    }

    @Override
    public FileTime creationTime() {
        return proxy().creationTime();
    }

    @Override
    public boolean isRegularFile() {
        return proxy().isRegularFile();
    }

    @Override
    public boolean isDirectory() {
        return proxy().isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return proxy().isSymbolicLink();
    }

    @Override
    public boolean isOther() {
        return proxy().isOther();
    }

    @Override
    public long size() {
        return proxy().size();
    }

    @Override
    public T fileKey() {
        return proxy().fileKey();
    }

    @Override
    public MCRDigest digest() {
        return proxy().digest();
    }

    protected MCRFileAttributes<T> proxy() {
        if (this.proxy == null) {
            synchronized (lock) {
                if (this.proxy == null) {
                    this.proxy = loadProxy();
                }
            }
        }
        return this.proxy;
    }

    protected abstract MCRFileAttributes<T> loadProxy();

    public P getPath() {
        return path;
    }

}
