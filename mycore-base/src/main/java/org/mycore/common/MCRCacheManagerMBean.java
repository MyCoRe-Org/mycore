package org.mycore.common;

public interface MCRCacheManagerMBean {

    long getSize();

    long getRequests();

    long getEvictions();

    long getHits();

    long getCapacity();

    void setCapacity(long size);

    double getHitRate();

    double getFillRate();

    void clear();

}
