package org.mycore.resource.hint;

import java.io.File;
import java.util.Optional;

import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

public final class MCRConfigDirResourceHint implements MCRHint<File> {

    @Override
    public MCRHintKey<File> key() {
        return MCRResourceHintKeys.CONFIG_DIR;
    }

    @Override
    public Optional<File> value() {
        return Optional.ofNullable(MCRConfigurationDir.getConfigurationDirectory());
    }

}
