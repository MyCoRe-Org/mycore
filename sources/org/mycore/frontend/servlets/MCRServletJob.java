/**
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
//import java.io.*;
//import java.net.*;
//import java.util.*;
//import org.jdom.*;
import org.mycore.common.*;

/**
 * This class simply is a container for objects needed during a Servlet session like
 * an MCRSession object, the HttpServletRequest etc.
 *
 * ??? The class provids only get-methods
 *     to return the objects set while constructing the job object. ???
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 **/
public class MCRServletJob
{
  /** The session object */
  private MCRSession theSession = null;

  /** The HttpServletRequest object */
  private HttpServletRequest theRequest= null;

  /** The HttpServletResponse object */
  private HttpServletResponse theResponse = null;

  /**
   * The constructor takes the given objects and stores them in private objects.
   *
   * @param theSession  the MCRSession object for this servlet job
   * @param theRequest  the HttpServletRequest object for this servlet job
   * @param theResponse the HttpServletResponse object for this servlet job
   */
  public MCRServletJob(MCRSession theSession,
                       HttpServletRequest theRequest,
                       HttpServletResponse theResponse)
  {
    this.theSession  = theSession;
    this.theRequest  = theRequest;
    this.theResponse = theResponse;
  }

  /** returns the session object */
  public MCRSession getSession()
  { return theSession; }

  /** returns the HttpServletRequest object */
  public HttpServletRequest getRequest()
  { return theRequest; }

  /** returns the HttpServletResponse object */
  public HttpServletResponse getResponse()
  { return theResponse; }
}