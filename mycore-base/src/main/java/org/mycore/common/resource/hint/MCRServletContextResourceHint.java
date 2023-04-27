package org.mycore.common.resource.hint;

import java.util.Optional;

import org.mycore.common.events.MCRServletContextHolder;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

import jakarta.servlet.ServletContext;

public class MCRServletContextResourceHint implements MCRHint<ServletContext> {


    @Override
    public MCRHintKey<ServletContext> key() {
        return MCRResourceHintKeys.SERVLET_CONTEXT;
    }

    @Override
    public Optional<ServletContext> value() {
        return MCRServletContextHolder.instance().get();
    }
}
