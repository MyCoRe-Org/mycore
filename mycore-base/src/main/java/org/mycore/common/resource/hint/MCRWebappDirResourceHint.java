package org.mycore.common.resource.hint;

import java.io.File;
import java.util.Optional;

import org.mycore.common.events.MCRServletContextHolder;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

public class MCRWebappDirResourceHint implements MCRHint<File> {

    @Override
    public MCRHintKey<File> key() {
        return MCRResourceHintKeys.WEBAPP_DIR;
    }

    @Override
    public Optional<File> value() {
        return MCRServletContextHolder.instance().get().map(context -> context.getRealPath("/")).map(File::new);
    }
}
