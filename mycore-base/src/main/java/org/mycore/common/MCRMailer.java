/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common;

import java.io.IOException;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRMailer.EMail.MessagePart;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRXSL2XMLTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.URLDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * This class provides methods to send emails from within a MyCoRe application.
 *
 * @author Marc Schluepmann
 * @author Frank Lützenkirchen
 * @author Werner Greßhoff
 * @author René Adler (eagle)
 *
 */
public class MCRMailer extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DELIMITER = "\n--------------------------------------\n";

    private static Session mailSession;

    protected static final String ENCODING;

    /** How often should MCRMailer try to send mail? */
    private static int numTries;

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        String goTo = job.getRequest().getParameter("goto");
        String xsl = job.getRequest().getParameter("xsl");

        Document input = (Document) (job.getRequest().getAttribute("MCRXEditorSubmission"));
        sendMail(input, xsl);

        job.getResponse().sendRedirect(goTo);
    }

    static {
        ENCODING = MCRConfiguration2.getStringOrThrow("MCR.Mail.Encoding");

        Properties mailProperties = new Properties();

        try {
            Authenticator auth = null;

            numTries = MCRConfiguration2.getOrThrow("MCR.Mail.NumTries", Integer::parseInt);
            if (MCRConfiguration2.getString("MCR.Mail.User").isPresent()
                && MCRConfiguration2.getString("MCR.Mail.Password").isPresent()) {
                auth = new SMTPAuthenticator();
                mailProperties.setProperty("mail.smtp.auth", Boolean.toString(true));
            }
            String starttsl = MCRConfiguration2.getString("MCR.Mail.STARTTLS").orElse("disabled");
            if (Objects.equals(starttsl, "enabled")) {
                mailProperties.setProperty("mail.smtp.starttls.enabled", Boolean.toString(true));
            } else if (Objects.equals(starttsl, "required")) {
                mailProperties.setProperty("mail.smtp.starttls.enabled", Boolean.toString(true));
                mailProperties.setProperty("mail.smtp.starttls.required", Boolean.toString(true));
            }
            mailProperties.setProperty("mail.smtp.host", MCRConfiguration2.getStringOrThrow("MCR.Mail.Server"));
            mailProperties.setProperty("mail.transport.protocol",
                MCRConfiguration2.getStringOrThrow("MCR.Mail.Protocol"));
            mailProperties.setProperty("mail.smtp.port", MCRConfiguration2.getString("MCR.Mail.Port").orElse("25"));
            mailSession = Session.getDefaultInstance(mailProperties, auth);
            mailSession.setDebug(MCRConfiguration2.getOrThrow("MCR.Mail.Debug", Boolean::parseBoolean));
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

        List<String> recipients = new ArrayList<>();
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
            bccList = new ArrayList<>();
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

        List<String> recipients = new ArrayList<>();
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
    public static void send(String sender, List<String> recipients, String subject, String body, List<String> parts,
        boolean bcc) {
        LOGGER.debug("Called multipart send method with multiple recipients.");

        List<String> bccList = null;

        if (bcc) {
            bccList = new ArrayList<>();
            bccList.add(sender);
        }

        send(sender, null, recipients, bccList, subject, body, parts);
    }

    /**
     * Send email from a given XML document. See the sample mail below:
     * <pre>
     * &lt;email&gt;
     *   &lt;from&gt;bingo@bongo.com&lt;/from&gt;
     *   &lt;to&gt;jim.knopf@lummerland.de&lt;/to&gt;
     *   &lt;bcc&gt;frau.waas@lummerland.de&lt;/bcc&gt;
     *   &lt;subject&gt;Grüße aus der Stadt der Drachen&lt;/subject&gt;
     *   &lt;body&gt;Es ist recht bewölkt. Alles Gute, Jim.&lt;/body&gt;
     *   &lt;body type="html"&gt;Es ist recht bewölkt. Alles Gute, Jim.&lt;/body&gt;
     *   &lt;part&gt;http://upload.wikimedia.org/wikipedia/de/f/f7/JimKnopf.jpg&lt;/part&gt;
     * &lt;/email&gt;
     * </pre>
     * @param email the email as JDOM element.
     */
    public static void send(Element email) {
        try {
            send(email, false);
        } catch (Exception e) {
            LOGGER.error(e::getMessage);
        }
    }

    /**
     * Send email from a given XML document. See the sample mail below:
     * <pre>
     * &lt;email&gt;
     *   &lt;from&gt;bingo@bongo.com&lt;/from&gt;
     *   &lt;to&gt;jim.knopf@lummerland.de&lt;/to&gt;
     *   &lt;bcc&gt;frau.waas@lummerland.de&lt;/bcc&gt;
     *   &lt;subject&gt;Grüße aus der Stadt der Drachen&lt;/subject&gt;
     *   &lt;body&gt;Es ist recht bewölkt. Alles Gute, Jim.&lt;/body&gt;
     *   &lt;body type="html"&gt;Es ist recht bewölkt. Alles Gute, Jim.&lt;/body&gt;
     *   &lt;part&gt;http://upload.wikimedia.org/wikipedia/de/f/f7/JimKnopf.jpg&lt;/part&gt;
     * &lt;/email&gt;
     * </pre>
     * @param email the email as JDOM element.
     * @param allowException allow to throw exceptions if set to <code>true</code>
     */
    public static void send(Element email, Boolean allowException) throws Exception {
        EMail mail = EMail.parseXML(email);

        if (allowException) {
            if (mail.to == null || mail.to.isEmpty()) {
                throw new MCRException("No receiver defined for mail\n" + mail + '\n');
            }

            trySending(mail);
        } else {
            send(mail);
        }
    }

    /**
     * Sends email. When sending email fails (for example, outgoing mail server
     * is not responding), sending will be retried after five minutes. This is
     * done up to 10 times.
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
    public static void send(final String from, final List<String> replyTo, final List<String> to,
        final List<String> bcc, final String subject, final String body, final List<String> parts) {
        EMail mail = new EMail();

        mail.from = from;
        mail.replyTo = replyTo;
        mail.to = to;
        mail.bcc = bcc;
        mail.subject = subject;

        mail.msgParts = new ArrayList<>();
        mail.msgParts.add(new MessagePart(body));

        mail.parts = parts;

        send(mail);
    }

    /**
     * Sends email. When sending email fails (for example, outgoing mail server
     * is not responding), sending will be retried after five minutes. This is
     * done up to 10 times.
     *
     * @param mail the email
     */
    public static void send(EMail mail) {
        if (mail.to == null || mail.to.isEmpty()) {
            throw new MCRException("No receiver defined for mail\n" + mail + '\n');
        }

        try {
            if (numTries > 0) {
                trySending(mail);
            }
        } catch (Exception ex) {
            LOGGER.info("Sending e-mail failed: ", ex);
            if (numTries < 2) {
                return;
            }

            Thread.ofVirtual().start(() -> {
                for (int i = numTries - 1; i > 0; i--) {
                    LOGGER.info("Retrying in 5 minutes...");
                    try {
                        Thread.sleep(300_000); // wait 5 minutes
                    } catch (InterruptedException ignored) {
                    }

                    try {
                        trySending(mail);
                        LOGGER.info("Successfully resended e-mail.");
                        break;
                    } catch (Exception ex1) {
                        LOGGER.info("Sending e-mail failed: ", ex1);
                    }
                }
            });
        }
    }

    private static void trySending(EMail mail) throws Exception {
        MimeMessage msg = new MimeMessage(mailSession);
        msg.setFrom(EMail.buildAddress(mail.from));

        addRecipientOptionals(msg, mail.to, Message.RecipientType.TO);
        addRecipientOptionals(msg, mail.replyTo, null);
        addRecipientOptionals(msg, mail.bcc, Message.RecipientType.BCC);

        msg.setSentDate(new Date());
        msg.setSubject(mail.subject, ENCODING);

        if (mail.parts != null && !mail.parts.isEmpty() || mail.msgParts != null && mail.msgParts.size() > 1) {
            Multipart multipart = new MimeMultipart("mixed");
            // Create the message part
            MimeBodyPart messagePart = new MimeBodyPart();
            MimeMultipart alternative = new MimeMultipart("alternative");
            buildMessageBodyParts(mail, alternative);
            messagePart.setContent(alternative);
            multipart.addBodyPart(messagePart);

            if (mail.parts != null && !mail.parts.isEmpty()) {
                setMessageContent(mail, multipart);
            }
            msg.setContent(multipart);
        } else {
            Optional<MessagePart> plainMsg = mail.getTextMessage();
            if (plainMsg.isPresent()) {
                msg.setText(plainMsg.get().message, ENCODING);
            }
        }
        LOGGER.info("Sending e-mail to {}", mail.to);
        Transport.send(msg);
    }

    private static void addRecipientOptionals(MimeMessage msg, List<String> foo, Message.RecipientType bar)
        throws MessagingException {
        Optional<List<InternetAddress>> list = EMail.buildAddressList(foo);
        if (list.isPresent()) {
            if (bar == null) {
                msg.setReplyTo((list.get().toArray(InternetAddress[]::new)));
            } else {
                msg.addRecipients(bar, list.get().toArray(InternetAddress[]::new));
            }
        }
    }

    private static void buildMessageBodyParts(EMail mail, MimeMultipart alternative)
        throws MessagingException {
        MimeBodyPart messagePart = new MimeBodyPart();
        for (MessagePart m : mail.msgParts) {
            messagePart.setText(m.message, ENCODING, m.type.value());
            alternative.addBodyPart(messagePart);
        }
    }

    private static void setMessageContent(EMail mail, Multipart multipart)
        throws MessagingException, URISyntaxException, MalformedURLException {
        for (String part : mail.parts) {
            MimeBodyPart messagePart = new MimeBodyPart();
            URL url = new URI(part).toURL();
            DataSource source = new URLDataSource(url);
            messagePart.setDataHandler(new DataHandler(source));
            String fileName = url.getPath();
            if (fileName.contains("\\")) {
                fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
            } else if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }
            messagePart.setFileName(fileName);
            multipart.addBodyPart(messagePart);
        }
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
        LOGGER.info("Generating e-mail from {} using {}.xsl", () -> input.getRootElement().getName(), () -> stylesheet);
        if (LOGGER.isDebugEnabled()) {
            debug(input.getRootElement());
        }

        Element eMail = transform(input, stylesheet, parameters).getRootElement();
        if (LOGGER.isDebugEnabled()) {
            debug(eMail);
        }

        if (eMail.getChildren("to").isEmpty()) {
            LOGGER.warn("Will not send e-mail, no 'to' address specified");
        } else {
            LOGGER.info("Sending e-mail to {}: {}", () -> eMail.getChildText("to"),
                () -> eMail.getChildText("subject"));
            send(eMail);
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
        return sendMail(input, stylesheet, Collections.emptyMap());
    }

    /**
     * Transforms the given input element using xsl stylesheet.
     *
     * @param input the input document to transform.
     * @param stylesheet the name of the xsl stylesheet to use, without the ".xsl" ending.
     * @param parameters the optionally empty table of xsl parameters
     * @return the output document generated by the transformation process
     */
    private static Document transform(Document input, String stylesheet, Map<String, String> parameters)
        throws Exception {
        MCRJDOMContent source = new MCRJDOMContent(input);
        final String xslFolder = MCRConfiguration2.getStringOrThrow("MCR.Layout.Transformer.Factory.XSLFolder");
        MCRXSL2XMLTransformer transformer = MCRXSL2XMLTransformer.obtainInstance(xslFolder + "/" + stylesheet + ".xsl");
        MCRParameterCollector parameterCollector = MCRParameterCollector.ofCurrentSession();
        parameterCollector.setParameters(parameters);
        MCRContent result = transformer.transform(source, parameterCollector);
        return result.asXML();
    }

    /** Outputs xml to the LOGGER for debugging */
    private static void debug(Element xml) {
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        LOGGER.debug(() -> DELIMITER + xout.outputString(xml) + DELIMITER);
    }

    @XmlRootElement(name = "email")
    public static class EMail {

        private static final JAXBContext JAXB_CONTEXT = initContext();

        @XmlElement
        public String from;

        @XmlElement
        public List<String> replyTo;

        @XmlElement
        public List<String> to;

        @XmlElement
        public List<String> bcc;

        @XmlElement
        public String subject;

        @XmlElement(name = "body")
        public List<MessagePart> msgParts;

        @XmlElement(name = "part")
        public List<String> parts;

        private static JAXBContext initContext() {
            try {
                return JAXBContext.newInstance(EMail.class.getPackage().getName(), Thread.currentThread()
                    .getContextClassLoader());
            } catch (final JAXBException e) {
                throw new MCRException("Could not instantiate JAXBContext.", e);
            }
        }

        /**
         * Parse a email from given {@link Element}.
         *
         * @param xml the email
         * @return the {@link EMail} object
         */
        public static EMail parseXML(final Element xml) {
            try {
                final Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
                return (EMail) unmarshaller.unmarshal(new JDOMSource(xml));
            } catch (final JAXBException e) {
                throw new MCRException("Exception while transforming Element to EMail.", e);
            }
        }

        /**
         * Builds email address from a string. The string may be a single email
         * address or a combination of a personal name and address, like "John Doe"
         * &lt;john@doe.com&gt;
         *
         * @param s the email address string
         * @return a {@link InternetAddress}
         * @throws Exception throws AddressException or UnsupportedEncodingException
         */
        private static InternetAddress buildAddress(String s) throws Exception {
            if (!s.endsWith(">")) {
                return new InternetAddress(s.trim());
            }

            String name = s.substring(0, s.lastIndexOf('<')).trim();
            String addr = s.substring(s.lastIndexOf('<') + 1, s.length() - 1).trim();

            if (name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1);
            }

            return new InternetAddress(addr, name);
        }

        /**
         * Builds a list of email addresses from a string list.
         *
         * @param addresses the list with email addresses
         * @return a list of {@link InternetAddress}s
         * @see MCRMailer.EMail#buildAddress(String)
         */
        private static Optional<List<InternetAddress>> buildAddressList(final List<String> addresses) {
            return addresses != null ? Optional.of(addresses.stream().map(address -> {
                try {
                    return buildAddress(address);
                } catch (Exception ex) {
                    return null;
                }
            }).collect(Collectors.toList())) : Optional.empty();
        }

        /**
         * Returns the text message part.
         *
         * @return the text message part
         */
        public Optional<MessagePart> getTextMessage() {
            return msgParts != null ? Optional.of(msgParts).get().stream()
                .filter(m -> m.type.equals(MessageType.TEXT)).findFirst() : Optional.empty();
        }

        /**
         * Returns the HTML message part.
         *
         * @return the HTML message part
         */
        public Optional<MessagePart> getHTMLMessage() {
            return msgParts != null ? Optional.of(msgParts).get().stream()
                .filter(m -> m.type.equals(MessageType.HTML)).findFirst() : Optional.empty();
        }

        /**
         * Returns the {@link EMail} as XML.
         *
         * @return the XML
         */
        public Document toXML() {
            final MCRJAXBContent<EMail> content = new MCRJAXBContent<>(JAXB_CONTEXT, this);
            try {
                return content.asXML();
            } catch (final IOException e) {
                throw new MCRException("Exception while transforming EMail to JDOM document.", e);
            }
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            final int maxLen = 10;
            StringBuilder builder = new StringBuilder();
            builder.append("EMail [");
            if (from != null) {
                builder.append("from=");
                builder.append(from);
                builder.append(", ");
            }
            if (replyTo != null) {
                builder.append("replyTo=");
                builder.append(replyTo.subList(0, Math.min(replyTo.size(), maxLen)));
                builder.append(", ");
            }
            if (to != null) {
                builder.append("to=");
                builder.append(to.subList(0, Math.min(to.size(), maxLen)));
                builder.append(", ");
            }
            if (bcc != null) {
                builder.append("bcc=");
                builder.append(bcc.subList(0, Math.min(bcc.size(), maxLen)));
                builder.append(", ");
            }
            if (subject != null) {
                builder.append("subject=");
                builder.append(subject);
                builder.append(", ");
            }
            if (msgParts != null) {
                builder.append("msgParts=");
                builder.append(msgParts.subList(0, Math.min(msgParts.size(), maxLen)));
                builder.append(", ");
            }
            if (parts != null) {
                builder.append("parts=");
                builder.append(parts.subList(0, Math.min(parts.size(), maxLen)));
            }
            builder.append(']');
            return builder.toString();
        }

        @XmlRootElement(name = "body")
        public static class MessagePart {

            @XmlAttribute
            public MessageType type = MessageType.TEXT;

            @XmlValue
            public String message;

            MessagePart() {
            }

            public MessagePart(final String message) {
                this.message = message;
            }

            public MessagePart(final String message, final MessageType type) {
                this.message = message;
                this.type = type;
            }

            /* (non-Javadoc)
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                final int maxLen = 50;
                StringBuilder builder = new StringBuilder();
                builder.append("MessagePart [");
                if (type != null) {
                    builder.append("type=");
                    builder.append(type);
                    builder.append(", ");
                }
                if (message != null) {
                    builder.append("message=");
                    builder.append(message, 0, Math.min(message.length(), maxLen));
                }
                builder.append(']');
                return builder.toString();
            }
        }

        @XmlType(name = "mcrmailer-messagetype")
        @XmlEnum
        public enum MessageType {
            @XmlEnumValue("text")
            TEXT("text"),

            @XmlEnumValue("html")
            HTML("html");

            private final String value;

            MessageType(String v) {
                value = v;
            }

            public String value() {
                return value;
            }

            public static MessageType fromValue(String v) {
                for (MessageType t : values()) {
                    if (t.value.equals(v)) {
                        return t;
                    }
                }
                throw new IllegalArgumentException(v);
            }
        }
    }

    private static final class SMTPAuthenticator extends Authenticator {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(MCRConfiguration2.getStringOrThrow("MCR.Mail.User"),
                MCRConfiguration2.getStringOrThrow("MCR.Mail.Password"));
        }
    }
}
