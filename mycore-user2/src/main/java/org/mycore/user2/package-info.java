/** new MyCoRe user system */
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(type = java.util.Date.class, value = org.mycore.user2.utils.MCRDateXMLAdapter.class) })
package org.mycore.user2;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
