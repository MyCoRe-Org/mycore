/*
 * $RCSfile$
 * $Revision$ $Date$
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

package org.mycore.backend.cm8;

import org.mycore.common.MCRConfiguration;

/**
 * This class implements a main program to show the CM8 content of an item.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCM8DeleteItem {
    // The configuration
    static MCRConfiguration conf = null;

    static String database = "";

    static String userid = "";

    static String password = "";

    static String itemtype = "";

    static String select = "";

    public static void main(String[] argv) throws DKException, Exception {
        // Read the arguments
        if ((argv.length != 1) && (argv.length != 2)) {
            System.out.println("The number of argument(s) is not 1 or 2 .");
            System.out.println();
        }

        itemtype = argv[0];

        if (argv.length == 2) {
            select = argv[1];
        }

        // read the configuration
        conf = MCRConfiguration.instance();
        database = conf.getString("MCR.persistence_cm8_library_server");
        userid = conf.getString("MCR.persistence_cm8_user_id");
        password = conf.getString("MCR.persistence_cm8_password");

        // Open connection
        DKDatastoreICM dsICM = new DKDatastoreICM(); // Create new datastore
        // object.

        dsICM.connect(database, userid, password, ""); // Connect to the
        // datastore.

        String query = "/" + itemtype + select;
        System.out.println("Delete all items with " + query + ", server " + database);

        // Specify Search / Query Options
        DKNVPair[] options = new DKNVPair[3];
        options[0] = new DKNVPair(DKConstant.DK_CM_PARM_MAX_RESULTS, "0"); // No
        // Maximum
        // (Default)

        options[1] = new DKNVPair(DKConstant.DK_CM_PARM_RETRIEVE, new Integer(DKConstant.DK_CM_CONTENT_YES));
        options[2] = new DKNVPair(DKConstant.DK_CM_PARM_END, null);

        DKResults results = (DKResults) dsICM.evaluate(query, DKConstantICM.DK_CM_XQPE_QL_TYPE, options);
        dkIterator iter = results.createIterator();
        System.out.println("Number of datas to delete " + results.cardinality());

        while (iter.more()) {
            System.out.print("Delete data ...");

            DKDDO ddo = (DKDDO) iter.next(); // Move pointer to next element
                                                // and
            // obtain that object.

            ddo.del();
            System.out.println(" done.");
        }

        dsICM.disconnect();
    }
}
