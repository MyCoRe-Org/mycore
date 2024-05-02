package org.mycore.datamodel.niofs;

import java.io.IOException;
import java.nio.file.attribute.FileTime;

import org.mycore.common.MCRException;
import org.mycore.common.digest.MCRDigest;

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
                    try {
                        this.proxy = loadProxy();
                    } catch (IOException ioException) {
                        throw new MCRException("Unable to load basic file attributes for '" + path + "'.", ioException);
                    }
                }
            }
        }
        return this.proxy;
    }

    protected abstract MCRFileAttributes<T> loadProxy() throws IOException;

    public P getPath() {
        return path;
    }

}
