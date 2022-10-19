package org.rongeb.poc.springintegration.mail.config;

import org.rongeb.poc.springintegration.mail.config.searchtermstrategy.UnreadSearchTermStrategy;
import org.rongeb.poc.springintegration.mail.service.MimeMailMessageImap4Handler;
import org.rongeb.poc.springintegration.mail.service.MimeMailMessagePop3Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.PollableChannel;

import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

@Configuration
@EnableIntegration
public class EmailIntegrationFlowConfig {

    Logger log = LoggerFactory.getLogger(EmailIntegrationFlowConfig.class);

    @Autowired
    private UnreadSearchTermStrategy unreadSearchTermStrategy;

    @Value("${mail.debug}")
    private String MAIL_DEBUG;

    @Value("${is.mail.debug}")
    private String IS_MAIL_DEBUG = "true";

    @Value("#{new Integer('${first.email.server.port}')}")
    private Integer IMAP_PORT;

    @Value("${first.email.user.credential}")
    private String IMAP_CREDENTIAL_USER;

    @Value("${first.email.user.pwd}")
    private String IMAP_CREDENTIAL_PWD;

    @Value("${first.email.host}")
    private String IMAP_HOST;

    @Value("#{new Integer('${first.email.polling.delay}')}")
    private Integer IMAP_POLL_DELAY;


    @Value("#{new Integer('${second.email.server.port}')}")
    private Integer IMAP_PORT_2;

    @Value("${second.email.user.credential}")
    private String IMAP_CREDENTIAL_USER_2;

    @Value("${second.email.user.pwd}")
    private String IMAP_CREDENTIAL_PWD_2;

    @Value("${second.email.host}")
    private String IMAP_HOST_2;

    @Value("#{new Integer('${second.email.polling.delay}')}")
    private Integer IMAP_POLL_DELAY_2;


    @Value("#{new Integer('${third.email.server.port}')}")
    private Integer POP_PORT;

    @Value("${third.email.user.credential}")
    private String POP_CREDENTIAL_USER;

    @Value("${third.email.user.pwd}")
    private String POP_CREDENTIAL_PWD;

    @Value("${third.email.host}")
    private String POP_HOST;

    private Properties imap4JavaMailProperties() {
        Properties javaMailProperties = new Properties();

        javaMailProperties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.setProperty("mail.imap.socketFactory.fallback", "false");
        javaMailProperties.setProperty("mail.store.protocol", "imaps");
        javaMailProperties.setProperty(MAIL_DEBUG, IS_MAIL_DEBUG);

        return javaMailProperties;
    }

    @Bean
    public IntegrationFlow imapMailFlowOne() throws UnsupportedEncodingException {

        return IntegrationFlows
                .from(Mail.imapInboundAdapter("imap://" + URLEncoder.encode(IMAP_CREDENTIAL_USER, "UTF-8") + ":" + IMAP_CREDENTIAL_PWD + "@" + IMAP_HOST + ":" + IMAP_PORT + "/INBOX")
                        .searchTermStrategy(unreadSearchTermStrategy)
                        .userFlag("pocImapMailFlowOne")
                        .shouldMarkMessagesAsRead(true)
                        .shouldDeleteMessages(true)
                        .maxFetchSize(1)
                        .autoCloseFolder(false)
                        .javaMailProperties(imap4JavaMailProperties()), e -> e.autoStartup(true)
                        .poller(p -> p.fixedDelay(IMAP_POLL_DELAY)))
                .channel(MessageChannels.queue("imapChannelOne"))
                .get();
    }

    @Bean
    public IntegrationFlow imapMailFlowTwo() throws UnsupportedEncodingException {

        return IntegrationFlows
                .from(Mail.imapInboundAdapter("imap://" + URLEncoder.encode(IMAP_CREDENTIAL_USER_2, "UTF-8") + ":" + IMAP_CREDENTIAL_PWD_2 + "@" + IMAP_HOST_2 + ":" + IMAP_PORT_2 + "/INBOX")
                        .searchTermStrategy(unreadSearchTermStrategy)
                        .userFlag("pocImapMailFlowTwo")
                        .shouldMarkMessagesAsRead(true)
                        .shouldDeleteMessages(true)
                        .autoCloseFolder(false)
                        .maxFetchSize(1)
                        .javaMailProperties(imap4JavaMailProperties()), e -> e.autoStartup(true)
                        .poller(p -> p.fixedDelay(IMAP_POLL_DELAY_2)))
                .channel(MessageChannels.queue("imapChannelTwo"))
                .get();
    }

    @Bean
    public HeaderMapper<MimeMessage> mailHeaderMapper() {
        return new DefaultMailHeaderMapper();
    }

    @ServiceActivator(inputChannel = "imapChannelOne")
    @Bean
    public MimeMailMessageImap4Handler imapHandlerOne() {
        return new MimeMailMessageImap4Handler();
    }

    @ServiceActivator(inputChannel = "imapChannelTwo")
    @Bean
    public MimeMailMessageImap4Handler imapHandlerTwo() {
        return new MimeMailMessageImap4Handler();
    }


    @Bean
    public PollableChannel pop3Channel() {
        return new QueueChannel();
    }

    private Properties pop3JavaMailProperties() {
        Properties javaMailProperties = new Properties();

        javaMailProperties.setProperty("mail.pop3.socketFactory.fallback", "false");
        javaMailProperties.setProperty(MAIL_DEBUG, IS_MAIL_DEBUG);
        javaMailProperties.put("mail.pop3.ssl.enable", true);
        javaMailProperties.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.setProperty("mail.pop3.socketFactory.port", POP_PORT.toString());
        return javaMailProperties;
    }

    @Bean
    @InboundChannelAdapter(value = "pop3Channel", poller = @Poller(fixedDelay = "5000"))
    public MessageSource<Object> pop3MailReceiver() {
        Pop3MailReceiver receiver = new Pop3MailReceiver(POP_HOST, POP_PORT, POP_CREDENTIAL_USER, POP_CREDENTIAL_PWD);
        receiver.setJavaMailProperties(pop3JavaMailProperties());
        receiver.setShouldDeleteMessages(true);
        receiver.setAutoCloseFolder(false);
        receiver.setMaxFetchSize(1);
        var source = new MailReceivingMessageSource(receiver);
        return source;
    }

    @ServiceActivator(inputChannel = "pop3Channel")
    @Bean
    public MimeMailMessagePop3Handler pop3Handler() {
        return new MimeMailMessagePop3Handler();
    }
}



