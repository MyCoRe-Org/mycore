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

package mycore.cm7;

import mycore.sql.MCRSQLCM7Interface;
import mycore.cm7.MCRCM7ConnectionPool;

/**
 * This class is designed to solve the workaround connection to CM7
 * if this persistence model is used. The interface is only for working
 * with IBM Content Manager 7.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7SQLWorkaround implements MCRSQLCM7Interface
{
  /**
   * An empty constructor.
   **/
  public MCRCM7SQLWorkaround()
  { }

  /**
   * This methode solve the workaround problem for IBM Content Manager 7.
   * The following line is a workaround for CM 7.1 under AIX
   * to prevent error FrnSysInitSharedMem to be thrown by CM:
   * Before connecting to DB2, ensure connect to CM is already done.
   **/
  public void connectToCM7()
  {
  MCRCM7ConnectionPool.releaseConnection(MCRCM7ConnectionPool.getConnection());
  }

}

