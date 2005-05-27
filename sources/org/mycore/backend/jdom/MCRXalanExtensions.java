/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.backend.jdom;

import org.mycore.common.MCRUtils;

/**
 * This class implements Xalan extension functions for the JDOM search.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRXalanExtensions {
    /**
     * This method implements a XSLT function to compare two date values.
     * 
     * @param date_one
     *            the date that should be compared with date_two
     * @param date_two
     *            the data that date_one is compared with
     * @param compare_operator
     *            The operator to compare in the form 'date_one compare_operator
     *            date_two'
     * @return true if the test is correct, else return false
     */
    public static boolean compareDates(String date_one, String date_two,
            String compare_operator) {
        String date1iso = MCRUtils.covertDateToISO(date_one);
        String date2iso = MCRUtils.covertDateToISO(date_two);

        if ((date1iso == null) || (date2iso == null)
                || (date1iso.length() == 0) || (date2iso.length() == 0))
            return false;
        if ((compare_operator == null)
                || ((compare_operator = compare_operator.trim()).length() == 0)) {
            return false;
        }

        int result = date1iso.compareTo(date2iso);

        if (compare_operator.equals("="))
            return (result == 0);
        else if (compare_operator.equals("!="))
            return (result != 0);
        else if (compare_operator.equals("<="))
            return (result <= 0);
        else if (compare_operator.equals(">="))
            return (result >= 0);
        else if (compare_operator.equals("<"))
            return (result < 0);
        else if (compare_operator.equals(">"))
            return (result > 0);
        else
            return false;
    }
}

