package com.grameenphone.wipro.task_executor.processors;

import com.grameenphone.wipro.task_executor.util.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailThread implements Runnable {
    private EmailUtil emailUtil = EmailUtil.getInstance();
    private String mailBody;
    private String to;
    private String cc;
    private String subject;

    public MailThread(String mailBody, String to, String cc, String subject) {
        this.mailBody = mailBody;
        this.to = to;
        this.cc = cc;
        this.subject = subject;
    }

    @Override
    public void run() {
        emailUtil.sendMail(mailBody, to, cc, subject);
    }
}
