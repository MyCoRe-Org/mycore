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

package mycore.common;

import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.*;
import mycore.editor.*;

/**
 * This class is a simple basic mailer class for mycore.
 *
 * @author marc schluepmann
 * @version $Revision$ $Date$
 **/
public class MCRMailer {
    protected MCRConfiguration milConfig = MCRConfiguration.instance();
    protected Properties sysProps = System.getProperties();
    protected Session mailsess;

    public MCRMailer() {
	sysProps.setProperty( "mail.smtp.host", 
			      milConfig.getString( "MCR.mail.server" ) 
			      );
	sysProps.setProperty( "mail.transport.protocol", 
			      milConfig.getString( "MCR.mail.protocol", "smtp" ) 
			      );
	mailsess = Session.getDefaultInstance( sysProps, null );
	mailsess.setDebug( milConfig.getBoolean( "MCR.mail.debug", false ) );
    }

    /**
     * Returns the mail session for this class.
     *
     * @return the current mail session
     **/
    protected javax.mail.Session getSession() {
	return mailsess;
    }

    /**
     * Internal method for sending a message with given session.
     * 
     * @param msg the message to be sent
     * @throws Exception a problem occured
     **/
    protected void send( javax.mail.Message msg ) throws Exception {
	getSession().getTransport().send( msg );
    }

    /**
     * This method sends a simple plaintext email with the given parameters.
     *
     * @param sender the sender of the email
     * @param recipient the recipient of the email
     * @param subject the subject of the email
     * @param body the textbody of the email
     * @throws Exception something dangerous occored
     **/ 
    protected void send( String sender, String recipient, String subject, String body ) throws Exception {
	MimeMessage msg = new MimeMessage( getSession() );
	msg.setFrom( new InternetAddress( sender ) );
	msg.addRecipient( Message.RecipientType.TO,
			  new InternetAddress( recipient )
			  );
	msg.setSubject( subject );
	msg.setSentDate( new Date() );
	msg.setText( body );
	send( msg );
    }
}
