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
package org.mycore.datamodel.classifications2;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface MCRTransactionalDAO {

    /**
     * Starts a new transaction.
     * 
     * Transactions are bound to the currentThread and must be closed either by
     * commitTransaction() or rollBackTransaction(). Not more than one
     * uncommitted transaction is allowed per thread.
     */
    public abstract void beginTransaction();

    /**
     * Commits the changes to the underlaying persistence layer.
     */
    public abstract void commitTransaction();

    /**
     * Forces the underlaying transaction to roll back.
     * 
     * Every changes made since begin of the transaction
     * 
     */
    public abstract void rollBackTransaction();

}