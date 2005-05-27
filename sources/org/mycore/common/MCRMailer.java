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

package org.mycore.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;

/**
 * This class is a simple basic mailer class for mycore.
 * 
 * @author Marc Schluepmann
 * @author Frank Lützenkirchen
 * @author Werner Greßhoff
 * 
 * @version $Revision$ $Date$
 */
public class MCRMailer {

    /** Logger */
    static Logger logger = Logger.getLogger(MCRMailer.class);

    protected static Properties mailProperties;

    protected static Session mailSession;

    /** Initializes the class */
    static {
        MCRConfiguration config = MCRConfiguration.instance();

        mailProperties = new Properties();

        try {
            mailProperties.setProperty("mail.smtp.host", config
                    .getString("MCR.mail.server"));
            mailProperties.setProperty("mail.transport.protocol", config
                    .getString("MCR.mail.protocol", "smtp"));
            mailSession = Session.getDefaultInstance(mailProperties, null);
            mailSession.setDebug(config.getBoolean("MCR.mail.debug", false));
        } catch (MCRConfigurationException mcrx) {
            String msg = "Missing configuration data.";
            logger.fatal(msg);
        }
    }

    public MCRMailer() {
    }

    /**
     * Returns the mail session for this class.
     * 
     * @return the current mail session
     */
    protected Session getSession() {
        return mailSession;
    }

    /**
     * Internal method for sending a message with given session.
     * 
     * @param msg
     *            the message to be sent
     */
    private static void send(Message msg) {
        try {
            Transport.send(msg);
        } catch (SendFailedException sfe) {
            logger.error(sfe.getMessage());
            throw new MCRException("The message could not be sent.", sfe);
        } catch (MessagingException me) {
            logger.error(me.getMessage());
            throw new MCRException("The message could not be sent.", me);
        }
    }

    /**
     * This method sends a simple plaintext email with the given parameters.
     * 
     * @param sender
     *            the sender of the email
     * @param recipient
     *            the recipient of the email
     * @param subject
     *            the subject of the email
     * @param body
     *            the textbody of the email
     */
    public static void send(String sender, String recipient, String subject,
            String body) {
        logger.debug("Called plaintext send method with single recipient.");
        ArrayList recipients = new ArrayList();
        recipients.add(recipient);
        send(sender, recipients, subject, body, false);
    }

    /**
     * This method sends a simple plaintext email to more than one recipient. If
     * flag BCC is true, the sender will also get the email as BCC recipient.
     * 
     * @param sender
     *            the sender of the email
     * @param recipients
     *            the recipients of the email as a List of Strings
     * @param subject
     *            the subject of the email
     * @param body
     *            the textbody of the email
     * @param bcc
     *            if true, sender will also get a copy as cc recipient
     */
    public static void send(String sender, List recipients, String subject,
            String body, boolean bcc) {
        logger.debug("Called plaintext send method with multiple recipients.");

        MimeMessage msg = new MimeMessage(mailSession);
        try {
            msg.setFrom(new InternetAddress(sender));

            for (int i = 0; i < recipients.size(); i++) {
                String recipient = (String) (recipients.get(i));
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
                        recipient));
            }

            if (bcc) {
                msg.addRecipient(Message.RecipientType.BCC,
                        new InternetAddress(sender));
            }

            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(body);

            Transport.send(msg);
        } catch (AddressException ae) {
            logger.error(ae.getMessage());
            throw new MCRException("The message could not be sent.", ae);
        } catch (SendFailedException sfe) {
            logger.error(sfe.getMessage());
            throw new MCRException("The message could not be sent.", sfe);
        } catch (MessagingException me) {
            logger.error(me.getMessage());
            throw new MCRException("The message could not be sent.", me);
        }
    }

    /**
     * This method sends a multipart email with the given parameters.
     * 
     * @param sender
     *            the sender of the email
     * @param recipient
     *            the recipient of the email
     * @param subject
     *            the subject of the email
     * @param parts
     *            a List of URL strings which should be added as parts
     * @param body
     *            the textbody of the email
     */
    public static void send(String sender, String recipient, String subject,
            String body, List parts) {
        logger.debug("Called multipart send method with single recipient.");
        ArrayList recipients = new ArrayList();
        recipients.add(recipient);
        send(sender, recipients, subject, body, parts, false);
    }

    /**
     * This method sends a multipart email to more than one recipient. If flag
     * BCC is true, the sender will also get the email as BCC recipient.
     * 
     * @param sender
     *            the sender of the email
     * @param recipients
     *            the recipients of the email as a List of Strings
     * @param subject
     *            the subject of the email
     * @param body
     *            the textbody of the email
     * @param parts
     *            a List of URL strings which should be added as parts
     * @param bcc
     *            if true, sender will also get a copy as cc recipient
     */
    public static void send(String sender, List recipients, String subject,
            String body, List parts, boolean bcc) {
        logger.debug("Called multipart send method with multiple recipients.");

        MimeMessage msg = new MimeMessage(mailSession);
        try {
            msg.setFrom(new InternetAddress(sender));

            for (int i = 0; i < recipients.size(); i++) {
                String recipient = (String) (recipients.get(i));
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
                        recipient));
            }

            if (bcc) {
                msg.addRecipient(Message.RecipientType.BCC,
                        new InternetAddress(sender));
            }

            msg.setSubject(subject);
            msg.setSentDate(new Date());

            // Create the message part
            BodyPart messagePart = new MimeBodyPart();

            // Fill the message
            messagePart.setText(body);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);

            for (int i = 0; i < parts.size(); i++) {
                String part = (String) (parts.get(i));
                messagePart = new MimeBodyPart();
                DataSource source = new URLDataSource(new URL(part));
                messagePart.setDataHandler(new DataHandler(source));
                multipart.addBodyPart(messagePart);
            }

            // Put parts in message
            msg.setContent(multipart);

            Transport.send(msg);
        } catch (AddressException ae) {
            logger.error(ae.getMessage());
            throw new MCRException("The message could not be sent.", ae);
        } catch (SendFailedException sfe) {
            logger.error(sfe.getMessage());
            throw new MCRException("The message could not be sent.", sfe);
        } catch (MessagingException me) {
            logger.error(me.getMessage());
            throw new MCRException("The message could not be sent.", me);
        } catch (MalformedURLException mue) {
            logger.error(mue.getMessage());
            throw new MCRException("Error in URL of message parts.", mue);
        }
    }

}