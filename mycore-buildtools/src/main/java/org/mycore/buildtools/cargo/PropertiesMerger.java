/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.buildtools.cargo;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;

import org.codehaus.cargo.module.merge.MergeException;
import org.codehaus.cargo.module.merge.MergeProcessor;
import org.mycore.buildtools.common.MCRSortedProperties;

/**
 * This class implements a merger for property files The properties in the base
 * file will be overwritten or complemented by the properties of the delta file.
 * It will be used for the goal cargo:uberwar of the Cargo Maven2 Plugin
 * (http://cargo.codehaus.org/Maven2+plugin) where different property files are
 * merged together. It works also for i18n language files (like
 * messages_de.properties). The result is a properties file alphabetically
 * sorted by keys
 * 
 * 
 * @see MergeProcessor
 * 
 * @author Robert Stephan
 */
public class PropertiesMerger implements MergeProcessor {
    private ArrayList<ByteArrayInputStream> propsList;

    /**
     * the constructor
     */
    public PropertiesMerger() {
        propsList = new ArrayList<ByteArrayInputStream>();

    }

    /**
     * adds an other property file to be merged as ByteArrayInputStream
     * 
     * see @ByteArrayInputStream 
     */
    public void addMergeItem(Object o) throws MergeException {
        propsList.add((ByteArrayInputStream) o);

    }

    /**
     * merges the property files as described above
     */
    @Override
    public Object performMerge() throws MergeException {
        if (propsList.size() == 0) {
            return null;
        }
        try {
            MCRSortedProperties baseProps = new MCRSortedProperties();
            baseProps.load(propsList.get(0));

            for (int i = 1; i < this.propsList.size(); i++) {
                Properties deltaProps = new Properties();
                deltaProps.load(this.propsList.get(i));
                baseProps.putAll(deltaProps);
            }
            StringWriter sw = new StringWriter();
            baseProps.store(sw, "These Properties were merged by org.mycore.buildtools.cargo.PropertiesMerger");

            //property files should always be ISO-8859-1 encoded in Java
            return new ByteArrayInputStream(sw.toString().getBytes("ISO-8859-1"));
        } catch (Exception e) {
            throw new MergeException("Error merging properties", e);
        }
    }
}
