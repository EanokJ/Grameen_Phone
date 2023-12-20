package com.grameenphone.wipro.task_executor.util;

import com.sun.mail.smtp.SMTPTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

public class EmailUtil {
    private static final Logger logger = LoggerFactory.getLogger(EmailUtil.class);
    private static final String SMTP_SERVER = PropertyUtil.getProperty("email-notification-host");
    private static final String EMAIL_FROM = PropertyUtil.getProperty("email-notification-from");
    private static EmailUtil service = null;

    public static EmailUtil getInstance() {
        if (service == null) {
            service = new EmailUtil();
        }
        return service;
    }

    public void sendMail(String body, String to, String cc, String subject) {
        Properties prop = System.getProperties();
        prop.put("mail.smtp.auth", "false");
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", SMTP_SERVER);
        prop.put("mail.smtp.port", "587");

        logger.error("Sending email to: " + to + " with subject: " + subject);
        Session session = Session.getInstance(prop);
        Message msg = new MimeMessage(session);
        SMTPTransport smtpTransport = null;

        if(StringUtil.isNullOrEmpty(EMAIL_FROM)) {
            logger.error("Email Sender Not Configured");
            return;
        }

        try {
            msg.setFrom(new InternetAddress(EMAIL_FROM));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc, false));
            msg.setSubject(subject);
            msg.setDataHandler(new DataHandler(new HTMLDataSource(body)));
            msg.setSentDate(new Date());

            smtpTransport = (SMTPTransport) session.getTransport("smtp");
            smtpTransport.connect(SMTP_SERVER, null, null);
            smtpTransport.sendMessage(msg, msg.getAllRecipients());
            logger.debug("Response: " + smtpTransport.getLastServerResponse());
        } catch (Exception e) {
            logger.error("Failed to send email:: " + e.getMessage());
        } finally {
            try {
                smtpTransport.close();
            } catch (Exception e) {
                logger.error("Failed to close SMTP connection:: " + e.getMessage());
            }
        }
    }

    private static class HTMLDataSource implements DataSource {
        private String html;

        public HTMLDataSource(String htmlString) {
            html = htmlString;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (html == null) throw new IOException("html message is null!");
            return new ByteArrayInputStream(html.getBytes());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("This DataHandler cannot write HTML");
        }

        @Override
        public String getContentType() {
            return "text/html";
        }

        @Override
        public String getName() {
            return "HTMLDataSource";
        }
    }
}