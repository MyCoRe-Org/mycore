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

package mycore.user;

import org.jdom.Element;

/**
 * This class defines a privilege of the MyCoRe user management system.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRPrivilege
{
  /** The name of the privilege */
  private String privName;

  /** The description of the privilege */
  private String privDescription;

  /** constructor */
  public MCRPrivilege (String name, String description)
  {
    privName = name;
    privDescription = description;
  }

  /** @return returns the name of the privilege */
  public String getName()
  { return privName; }

  /** @return returns the description of the privilege */
  public String getDescription()
  { return privDescription; }

  /**
   * @return
   *   This method returns the privilege object as a JDOM element. This is needed if
   *   one wants to get a representation of several privileges in one xml document.
   */
  public Element toJDOMElement() throws Exception
  {
    Element priv = new Element("privilege").setAttribute("name", privName);
    Element Description = new Element("privilege.descrition").setText(privDescription);

    // Aggregate privilege element
    priv.addContent(Description);
    return priv;
  }
}
