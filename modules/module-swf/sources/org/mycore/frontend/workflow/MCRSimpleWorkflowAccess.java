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

// package
package org.mycore.frontend.workflow;

import org.apache.log4j.Logger;

import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * This class holds methods to manage the access part of the simple workflow
 * system of MyCoRe.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRSimpleWorkflowAccess {

    public static final String READ_POOL = "READ";

    public static final String WRITE_POOL = "WRITE";

    public static final String WRITEWF_POOL = "WRITEWF";

    public static final String DELETE_POOL = "DELETE";

    public static final String DELETEWF_POOL = "DELETEWF";

    public static final String COMMIT_POOL = "COMMIT";

    // logger
    static Logger LOGGER = Logger.getLogger(MCRSimpleWorkflowAccess.class.getName());

    /**
     * The method build the access rules for the SimpleWorkflow based on data in
     * the service part of MCRObject or MCRDerivate.
     */
    public static final void getAccessRule(String pool, MCRObjectService serv) {
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return;
        }
        if (!pool.equals(READ_POOL) & !pool.equals(WRITE_POOL) & !pool.equals(WRITEWF_POOL) & !pool.equals(DELETE_POOL) & !pool.equals(DELETEWF_POOL) & !pool.equals(COMMIT_POOL)) {
            return;
        }
        StringBuffer sb1 = new StringBuffer(1024);
        int j = 0;
        for (int i = 0; i < serv.getGroupSize(); i++) {
            if (serv.getGroupType(i).equals(pool)) {
                if (j > 0) {
                    sb1.append(" or ");
                }
                sb1.append(serv.getGroup(i));
                j++;
            }
        }
        for (int i = 0; i < serv.getUserSize(); i++) {
            if (serv.getUserType(i).equals(pool)) {
                if (j > 0) {
                    sb1.append(" or ");
                }
                sb1.append(serv.getUser(i));
                j++;
            }
        }
        StringBuffer sb2 = new StringBuffer(1024);
        int k = 0;
        for (int i = 0; i < serv.getIPSize(); i++) {
            if (serv.getIPType(i).equals(pool)) {
                if (k > 0) {
                    sb2.append(" or ");
                }
                sb2.append(serv.getIP(i));
                k++;
            }
        }
        StringBuffer sb = new StringBuffer(1024);
        if (j > 0 & k > 0) {
            sb.append("( ").append(sb1).append(" ) and ( ").append(sb2).append(" )");
        }
        if (j > 0 & k == 0) {
            sb.append("( ").append(sb1).append(" )");
        }
        if (j == 0 & k > 0) {
            sb.append("( ").append(sb2).append(" )");
        }

        String rule = sb.toString();
        LOGGER.debug("ACCESS RULE : " + rule);
    }
}