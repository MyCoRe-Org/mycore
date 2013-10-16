/*
 * 
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

package org.mycore.common;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This class provides methods to send emails from within a MyCoRe application.
 * 
 * @author Marc Schluepmann
 * @author Frank L\u00FCtzenkirchen
 * @author Werner Greßhoff
 * 
 * @version $Revision$ $Date$
 */
public class MCRMailer extends MCRServlet {

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String goTo = job.getRequest().getParameter("goto");
        String xsl = job.getRequest().getParameter("xsl");

        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        Document input = sub != null ? sub.getXML() : (Document) (job.getRequest().getAttribute("MCRXEditorSubmission"));
        MCRMailer.sendMail(input, xsl);

        job.getResponse().sendRedirect(goTo);
    }

    private static final Logger LOGGER = Logger.getLogger(MCRMailer.class);

    private static Session mailSession;

    protected static final String encoding;

    /** How often should MCRMailer try to send mail? */
    protected static int numTries;

    /** Initializes the class */
    static {
        MCRConfiguration config = MCRConfiguration.instance();
        encoding = config.getString("MCR.Mail.Encoding", "ISO-8859-1");

        Properties mailProperties = new Properties();

        try {
            numTries = config.getInt("MCR.Mail.NumTries", 1);
            mailProperties.setProperty("mail.smtp.host", config.getString("MCR.Mail.Server"));
            mailProperties.setProperty("mail.transport.protocol", config.getString("MCR.Mail.Protocol", "smtp"));
            mailSession = Session.getDefaultInstance(mailProperties, null);
            mailSession.setDebug(config.getBoolean("MCR.Mail.Debug", false));
        } catch (MCRConfigurationException mcrx) {
            String msg = "Missing e-mail configuration data.";
            LOGGER.fatal(msg, mcrx);
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
    public static void send(String sender, String recipient, String subject, String body) {
        LOGGER.debug("Called plaintext send method with single recipient.");

        ArrayList<String> recipients = new ArrayList<String>();
        recipients.add(recipient);
        send(sender, null, recipients, null, subject, body, null);
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
    public static void send(String sender, List<String> recipients, String subject, String body, boolean bcc) {
        LOGGER.debug("Called plaintext send method with multiple recipients.");

        List<String> bccList = null;

        if (bcc) {
            bccList = new ArrayList<String>();
            bccList.add(sender);
        }

        send(sender, null, recipients, bccList, subject, body, null);
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
    public static void send(String sender, String recipient, String subject, String body, List<String> parts) {
        LOGGER.debug("Called multipart send method with single recipient.");

        ArrayList<String> recipients = new ArrayList<String>();
        recipients.add(recipient);
        send(sender, null, recipients, null, subject, body, parts);
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
     *            if true, sender will also get a copy as bcc recipient
     */
    public static void send(String sender, List<String> recipients, String subject, String body, List<String> parts, boolean bcc) {
        LOGGER.debug("Called multipart send method with multiple recipients.");

        List<String> bccList = null;

        if (bcc) {
            bccList = new ArrayList<String>();
            bccList.add(sender);
        }

        send(sender, null, recipients, bccList, subject, body, parts);
    }

    /**
     * Send email from a given XML document. See the sample mail below:
     * 
     * <email><from>bingo@bongo.com</from>
     *   <to>jim.knopf@lummerland.de</to>
     *   <bcc>frau.waas@lummerland.de</bcc>
     *   <subject>Grüße aus der Stadt der Drachen</subject>
     *   <body>Es ist recht bewölkt. Alles Gute, Jim.</body>
     *   <part>http://upload.wikimedia.org/wikipedia/de/f/f7/JimKnopf.jpg</part>
     * </email>
     *
     * @param email the email as JDOM element.
     */
    public static void send(Element email) {
        String from = email.getChildTextTrim("from");

        @SuppressWarnings("unchecked")
        List<Element> rptList = email.getChildren("replyTo");
        List<String> replyTo = new ArrayList<String>();

        for (Element reply : rptList) {
            replyTo.add(reply.getTextTrim());
        }

        @SuppressWarnings("unchecked")
        List<Element> toList = email.getChildren("to");
        List<String> to = new ArrayList<String>();

        for (Element toElement : toList) {
            to.add(toElement.getTextTrim());
        }

        @SuppressWarnings("unchecked")
        List<Element> bccList = email.getChildren("bcc");
        List<String> bcc = new ArrayList<String>();

        for (Element bccElement : bccList) {
            bcc.add(bccElement.getTextTrim());
        }

        String subject = email.getChildTextTrim("subject");
        String body = email.getChildTextTrim("body");

        @SuppressWarnings("unchecked")
        List<Element> partsList = email.getChildren("part");
        List<String> parts = new ArrayList<String>();

        for (Element partsElement : partsList) {
            parts.add(partsElement.getTextTrim());
        }

        send(from, replyTo, to, bcc, subject, body, parts);
    }

    /**
     * Sends email. When sending email fails (for example, outgoing mail server
     * is not responding), sending will be retried after five minutes. This is
     * done up to 10 times.
     * 
     * 
     * @param from
     *            the sender of the email
     * @param replyTo
     *            the reply-to addresses as a List of Strings, may be null
     * @param to
     *            the recipients of the email as a List of Strings
     * @param bcc
     *            the bcc recipients of the email as a List of Strings, may be
     *            null
     * @param subject
     *            the subject of the email
     * @param body
     *            the text of the email
     * @param parts
     *            a List of URL strings which should be added as parts, may be
     *            null
     */
    public static void send(final String from, final List<String> replyTo, final List<String> to, final List<String> bcc,
            final String subject, final String body, final List<String> parts) {
        if (to == null || to.size() == 0) {
            StringBuilder sb = new StringBuilder("No receiver defined for mail\n");
            sb.append("Subject: ").append(subject).append('\n');
            sb.append("Body:\n").append(body).append('\n');
            sb.append("Parts: ").append(parts).append('\n');
            throw new MCRException(sb.toString());
        }
        try {
            if (numTries > 0) {
                trySending(from, replyTo, to, bcc, subject, body, parts);
            }
        } catch (Exception ex) {
            LOGGER.info("Sending e-mail failed: ", ex);
            if (numTries < 2) {
                return;
            }

            Thread t = new Thread(new Runnable() {
                public void run() {
                    for (int i = numTries - 1; i > 0; i--) {
                        LOGGER.info("Retrying in 5 minutes...");
                        try {
                            Thread.sleep(300000);
                        } // wait 5 minutes
                        catch (InterruptedException ignored) {
                        }

                        try {
                            trySending(from, replyTo, to, bcc, subject, body, parts);
                            LOGGER.info("Successfully resended e-mail.");
                            break;
                        } catch (Exception ex) {
                            LOGGER.info("Sending e-mail failed: ", ex);
                        }
                    }
                }
            });
            t.start(); // Try to resend mail in separate thread
        }
    }

    private static void trySending(String from, List<String> replyTo, List<String> to, List<String> bcc, String subject, String body,
            List<String> parts) throws Exception {
        MimeMessage msg = new MimeMessage(mailSession);
        msg.setFrom(buildAddress(from));

        if (replyTo != null) {
            InternetAddress[] adrs = new InternetAddress[replyTo.size()];

            for (int i = 0; i < replyTo.size(); i++) {
                adrs[i] = buildAddress(replyTo.get(i));
            }

            msg.setReplyTo(adrs);
        }

        for (String aTo : to) {
            msg.addRecipient(Message.RecipientType.TO, buildAddress(aTo));
        }

        if (bcc != null) {
            for (String aBcc : bcc) {
                msg.addRecipient(Message.RecipientType.BCC, buildAddress(aBcc));
            }
        }

        msg.setSentDate(new Date());
        msg.setSubject(subject, encoding);

        if (parts == null || parts.size() == 0) {
            msg.setText(body, encoding);
        } else {
            // Create the message part
            MimeBodyPart messagePart = new MimeBodyPart();

            // Fill the message
            messagePart.setText(body, encoding);

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messagePart);

            for (String part : parts) {
                messagePart = new MimeBodyPart();

                URL url = new URL(part);
                DataSource source = new URLDataSource(url);
                messagePart.setDataHandler(new DataHandler(source));

                String fileName = url.getPath();
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                } else if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                messagePart.setFileName(fileName);

                multipart.addBodyPart(messagePart);
            }

            // Put parts in message
            msg.setContent(multipart);
        }

        LOGGER.info("Sending e-mail to " + to.get(0));
        Transport.send(msg);
    }

    /**
     * Builds email address from a string. The string may be a single email
     * address or a combination of a personal name and address, like "John Doe"
     * <john@doe.com>
     */
    private static InternetAddress buildAddress(String s) throws Exception {
        if (!s.endsWith(">")) {
            return new InternetAddress(s.trim());
        }

        String name = s.substring(0, s.lastIndexOf("<")).trim();
        String addr = s.substring(s.lastIndexOf("<") + 1, s.length() - 1).trim();

        if (name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }

        return new InternetAddress(addr, name);
    }

    /**
     * Generates e-mail from the given input document by transforming it with an xsl stylesheet,
     * and sends the e-mail afterwards.
     * 
     * @param input the xml input document
     * @param stylesheet the xsl stylesheet that will generate the e-mail, without the ending ".xsl" 
     * @param parameters the optionally empty table of xsl parameters
     * @return the generated e-mail
     * 
     * @see org.mycore.common.MCRMailer
     */
    public static Element sendMail(Document input, String stylesheet, Map<String, String> parameters) throws Exception {
        LOGGER.info("Generating e-mail from " + input.getRootElement().getName() + " using " + stylesheet + ".xsl");
        if (LOGGER.isDebugEnabled())
            debug(input.getRootElement());

        Element eMail = transform(input, stylesheet, parameters).getRootElement();
        if (LOGGER.isDebugEnabled())
            debug(eMail);

        if (eMail.getChildren("to").isEmpty())
            LOGGER.warn("Will not send e-mail, no 'to' address specified");
        else {
            LOGGER.info("Sending e-mail to " + eMail.getChildText("to") + ": " + eMail.getChildText("subject"));
            MCRMailer.send(eMail);
        }

        return eMail;
    }

    /**
     * Generates e-mail from the given input document by transforming it with an xsl stylesheet,
     * and sends the e-mail afterwards.
     * 
     * @param input the xml input document
     * @param stylesheet the xsl stylesheet that will generate the e-mail, without the ending ".xsl" 
     * @return the generated e-mail
     * 
     * @see org.mycore.common.MCRMailer
     */
    public static Element sendMail(Document input, String stylesheet) throws Exception {
        return sendMail(input, stylesheet, Collections.<String, String> emptyMap());
    }

    /** 
     * Transforms the given input element using xsl stylesheet.
     * 
     * @param input the input document to transform.
     * @param stylesheet the name of the xsl stylesheet to use, without the ".xsl" ending.
     * @param parameters the optionally empty table of xsl parameters
     * @return the output document generated by the transformation process
     */
    private static Document transform(Document input, String stylesheet, Map<String, String> parameters) throws Exception {
        MCRJDOMContent source = new MCRJDOMContent(input);
        MCRXSL2XMLTransformer transformer = MCRXSL2XMLTransformer.getInstance("xsl/" + stylesheet + ".xsl");
        MCRParameterCollector parameterCollector = MCRParameterCollector.getInstanceFromUserSession();
        parameterCollector.setParameters(parameters);
        MCRContent result = transformer.transform(source, parameterCollector);
        return result.asXML();
    }

    private final static String delimiter = "\n--------------------------------------\n";

    /** Outputs xml to the LOGGER for debugging */
    private static void debug(Element xml) {
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        LOGGER.debug(delimiter + xout.outputString(xml) + delimiter);
    }
}
