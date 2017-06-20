package org.mycore.sword.application;

public interface MCRSwordLifecycle {
    void init(MCRSwordLifecycleConfiguration lifecycleConfiguration);

    void destroy();
}
