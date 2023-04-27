package org.mycore.common.resource.hint;

import java.io.File;

import org.mycore.common.hint.MCRHintKey;
import org.mycore.common.resource.MCRResourceResolver;

import jakarta.servlet.ServletContext;

/**
 * A utility class that provides {{@link MCRHintKey}} used by {@link MCRResourceResolver#defaultHints()}.
 */
public final class MCRResourceHintKeys {

    public static final MCRHintKey<File> CONFIG_DIR = new MCRHintKey<>(
        File.class,
        "CONFIG_DIR",
        File::getAbsolutePath
    );

    public static final MCRHintKey<ClassLoader> CLASS_LOADER = new MCRHintKey<>(
        ClassLoader.class,
        "CLASS_LOADER",
        ClassLoader::getName
    );

    public static final MCRHintKey<ServletContext> SERVLET_CONTEXT = new MCRHintKey<>(
        ServletContext.class,
        "SERVLET_CONTEXT",
        ServletContext::getServletContextName
    );

    public static final MCRHintKey<File> WEBAPP_DIR = new MCRHintKey<>(
        File.class,
        "WEBAPP_DIR",
        File::getAbsolutePath
    );

    private MCRResourceHintKeys(){
    }

}
