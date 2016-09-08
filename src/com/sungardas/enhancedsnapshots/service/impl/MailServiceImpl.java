package com.sungardas.enhancedsnapshots.service.impl;

import com.sungardas.enhancedsnapshots.aws.dynamodb.model.MailConfigurationDocument;
import com.sungardas.enhancedsnapshots.aws.dynamodb.model.TaskEntry;
import com.sungardas.enhancedsnapshots.components.ConfigurationMediator;
import com.sungardas.enhancedsnapshots.service.CryptoService;
import com.sungardas.enhancedsnapshots.service.MailService;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger LOG = LogManager.getLogger(MailServiceImpl.class);

    @Value("${enhancedsnapshots.mail.success.template.path}")
    private String successTemplatePath;

    @Value("${enhancedsnapshots.mail.success.subject}")
    private String successSubject;

    @Value("${enhancedsnapshots.mail.error.template.path}")
    private String failTemplatePath;

    @Value("${enhancedsnapshots.mail.error.subject}")
    private String errorSubject;

    @Value("${enhancedsnapshots.mail.info.template.path}")
    private String systemInformationTemplatePath;

    @Value("${enhancedsnapshots.mail.info.subject}")
    private String infoSubject;


    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ConfigurationMediator configurationMediator;

    @Autowired
    private CryptoService cryptoService;

    private Template successTemplate;

    private Template failTemplate;

    private Template infoTemplate;

    private Session session;

    @PostConstruct
    private void init() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setTemplateLoader(new TemplateLoader() {
            @Override
            public Object findTemplateSource(String name) throws IOException {
                Resource resource = applicationContext.getResource(name);
                return resource.exists() ? resource : null;
            }

            @Override
            public long getLastModified(Object templateSource) {
                Resource resource = (Resource) templateSource;
                try {
                    return resource.lastModified();
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public Reader getReader(Object templateSource, String encoding) throws IOException {
                Resource resource = (Resource) templateSource;
                return new InputStreamReader(resource.getInputStream());
            }

            @Override
            public void closeTemplateSource(Object templateSource) throws IOException {
            }
        });

        successTemplate = cfg.getTemplate(successTemplatePath);
        failTemplate = cfg.getTemplate(failTemplatePath);
        infoTemplate = cfg.getTemplate(systemInformationTemplatePath);
        reconnect();
    }


    @Override
    public boolean reconnect() {
        MailConfigurationDocument configuration = configurationMediator.getMailConfiguration();
        session = getSession(configuration);
        return session != null;
    }

    @Override
    public void disconnect() {
        session = null;
    }

    @Override
    public boolean checkConfiguration(MailConfigurationDocument configuration) {
        try {
            return InetAddress.getByName(configuration.getMailSMTPHost()).isReachable(1000);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void notifyAboutSuccess(TaskEntry taskEntry) {
        if (session != null && configurationMediator.getMailConfiguration().getEvents().isSuccess()) {
            Set<String> recipients = configurationMediator.getMailConfiguration().getRecipients();
            if (recipients != null && !recipients.isEmpty()) {
                Map<String, String> data = new HashMap<>();
                data.put("domain", configurationMediator.getDomain());
                data.put("message", "Task has been successfully finished");
                notifyViaEmail(data, successSubject, successTemplate, recipients);
            }
        }
    }

    @Override
    public void notifyAboutError(TaskEntry taskEntry, Exception e) {
        if (session != null && configurationMediator.getMailConfiguration().getEvents().isError()) {
            Set<String> recipients = configurationMediator.getMailConfiguration().getRecipients();
            if (recipients != null && !recipients.isEmpty()) {
                Map<String, String> data = new HashMap<>();
                data.put("domain", configurationMediator.getDomain());
                data.put("message", "Task has been failed");
                notifyViaEmail(data, errorSubject, failTemplate, recipients);
            }
        }
    }

    @Override
    public void notifyAboutSystemStatus(String message) {
        if (session != null && configurationMediator.getMailConfiguration().getEvents().isInfo()) {
            Set<String> recipients = configurationMediator.getMailConfiguration().getRecipients();
            if (recipients != null && !recipients.isEmpty()) {
                Map<String, String> data = new HashMap<>();
                data.put("domain", configurationMediator.getDomain());
                data.put("message", message);
                notifyViaEmail(data, infoSubject, infoTemplate, recipients);
            }
        }
    }

    private void notifyViaEmail(Map<String, String> data, String subject, Template template, Set<String> recipients) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(configurationMediator.getMailConfiguration().getFromMailAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipients)));
            message.setSubject(subject);

            StringWriter stringWriter = new StringWriter();
            template.process(data, stringWriter);

            message.setContent(stringWriter.toString(), "text/html; charset=utf-8");

            Transport.send(message);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private Session getSession(MailConfigurationDocument configuration) {
        if (configuration == null) {
            return null;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", configuration.getMailSMTPHost());

            props.put("mail.smtp.port", configuration.getMailSMTPPort());

            switch (configuration.getConnectionType()) {
                case TLS:
                    props.put("mail.smtp.starttls.enable", "true");
                    break;
                case SSL:
                    props.put("mail.smtp.socketFactory.port", configuration.getMailSMTPPort());
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    break;
            }


            return Session.getDefaultInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(configuration.getUserName(),
                                    cryptoService.decrypt(configurationMediator.getConfigurationId(), configuration.getPassword()));
                        }
                    });
        } catch (RuntimeException e) {
            LOG.error(e);
            session = null;
            return null;
        }
    }
}
