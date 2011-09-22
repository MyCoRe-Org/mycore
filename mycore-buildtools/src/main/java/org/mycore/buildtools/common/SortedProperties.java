package org.mycore.buildtools.common;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class SortedProperties extends Properties {
    /**
     * Overrides, called by the store method.
     */
    @SuppressWarnings("unchecked")
    public synchronized Enumeration keys() {
       Enumeration keysEnum = super.keys();
       Vector keyList = new Vector();
       while(keysEnum.hasMoreElements()){
         keyList.add(keysEnum.nextElement());
       }
       Collections.sort(keyList);
       return keyList.elements();
    }
}