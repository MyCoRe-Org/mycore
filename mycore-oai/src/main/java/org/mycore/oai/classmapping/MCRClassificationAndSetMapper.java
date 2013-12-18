/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.oai.classmapping;

import java.util.Properties;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;


/**
 * This class maps MyCoRe classification names to set names and vice versa
 * in OAI implementation.
 * 
 * The mapping itself is stored in properties
 * e.g. MCR.OAIDataProvider.OAI2.MapSetToClassification.doc-type=diniPublType
 * 
 * @author Robert Stephan
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationAndSetMapper {
    private static MCRConfiguration config = MCRConfiguration.instance();
    private static String PROP_SUFFIX = "MapSetToClassification.";

    /**
     * maps a classification name to an OAI set name
     * @param prefix - the properties prefix of the OAIAdapter
     * @param classid - the classification name
     * @return
     */
    public static String mapClassificationToSet(String prefix, String classid){
        Properties props = config.getProperties(prefix+PROP_SUFFIX);
        String result = classid;
        for(Object key: props.keySet()){
            if(props.get(key).equals(classid)){
                String s = key.toString();
                result = s.substring(s.lastIndexOf(".")+1);
                break;
            }
        }
        return result;
    }

    
    /**
     * maps an OAI set name to a classification name
     * @param prefix - the property prefix for the OAIAdapter
     * @param setid - the set name
     * @return
     */
    public static String mapSetToClassification(String prefix, String setid){
        try{
            return config.getString(prefix+PROP_SUFFIX+setid);
        }
        catch(MCRConfigurationException mce){
            // use the given value of setid
            return setid;

        }
    }
}
