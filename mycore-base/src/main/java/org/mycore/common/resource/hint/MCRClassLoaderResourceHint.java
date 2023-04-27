package org.mycore.common.resource.hint;

import java.util.Optional;

import org.mycore.common.MCRClassTools;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

public final class MCRClassLoaderResourceHint implements MCRHint<ClassLoader> {

    @Override
    public MCRHintKey<ClassLoader> key() {
        return MCRResourceHintKeys.CLASS_LOADER;
    }

    @Override
    public Optional<ClassLoader> value() {
        return Optional.ofNullable(MCRClassTools.getClassLoader());
    }

}
