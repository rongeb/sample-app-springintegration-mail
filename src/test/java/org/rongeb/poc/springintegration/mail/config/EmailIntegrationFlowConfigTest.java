package org.rongeb.poc.springintegration.mail.config;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.SearchTerm;

import java.io.Closeable;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringJUnitConfig
@DirtiesContext
public class EmailIntegrationFlowConfigTest {
    private static GreenMail mailServer;

    @Autowired
    private PollableChannel imapChannel;

    @Autowired
    private PollableChannel pop3Channel;

    @BeforeAll
    public static void setup() {
        ServerSetup imap = ServerSetupTest.IMAP.createCopy();
        imap.setServerStartupTimeout(10000);
        ServerSetup pop3 = ServerSetupTest.POP3.createCopy();
        mailServer = new GreenMail(new ServerSetup[]{ imap, pop3 });
        mailServer.setUser("popuser", "pw");
        mailServer.setUser("imapuser", "pw");
        mailServer.start();
    }

    @AfterAll
    static void tearDown() {
        mailServer.stop();
    }


    @Test
    public void testImap() throws Exception {
        MimeMessage mimeMessage =
                GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "Test Email", "<div>foo</div>", mailServer.getImap().getServerSetup());
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.CC, "a@b, c@d");
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.BCC, "e@f, g@h");
        mailServer.getManagers().getUserManager().getUser("imapuser").deliver(mimeMessage);
        Message<?> message = this.imapChannel.receive(10000);
        assertThat(message).isNotNull();
        MimeMessage mm = (MimeMessage) message.getPayload();
        assertThat(mm.getRecipients(javax.mail.Message.RecipientType.TO)[0].toString()).isEqualTo("Foo <foo@bar>");
        assertThat(mm.getFrom()[0].toString()).isEqualTo("Bar <bar@baz>");
        assertThat(mm.getSubject()).isEqualTo("Test Email");
        assertThat(mm.getContent()).isEqualTo("<div>foo</div>");
        assertThat(message.getHeaders().containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE)).isTrue();
        message.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class).close();
    }


    @Test
    public void testPop3() throws Exception {
        MimeMessage mimeMessage =
                GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>, Bar2 <bar2@baz>", "Test Email", "foo\r\n", mailServer.getPop3().getServerSetup());
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.CC, "a@b, c@d");
        mimeMessage.setRecipients(javax.mail.Message.RecipientType.BCC, "e@f, g@h");
        mailServer.getManagers().getUserManager().getUser("popuser").deliver(mimeMessage);

        Message<?> message = this.pop3Channel.receive(10000);
        assertThat(message).isNotNull();
        MimeMessage mm = (MimeMessage) message.getPayload();
        MessageHeaders headers = message.getHeaders();
        assertThat(mm.getRecipients(javax.mail.Message.RecipientType.TO)[0].toString()).isEqualTo("Foo <foo@bar>");
        assertThat(mm.getFrom()[0].toString()).isEqualTo("Bar <bar@baz>");
        assertThat(mm.getSubject()).isEqualTo("Test Email");
        assertThat(mm.getContent()).isEqualTo("foo\r\n");
        assertThat(message.getHeaders().containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE)).isTrue();
        message.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class).close();
    }


    @Configuration
    @EnableIntegration
    public static class ContextConfiguration {

        private static final String MAIL_DEBUG = "mail.debug";
        private static final String IS_MAIL_DEBUG = "true";
        private static final Integer IMAP_POLL_DELAY = 1000;

        private Properties imap4JavaMailProperties() {
            Properties javaMailProperties = new Properties();
            javaMailProperties.setProperty(MAIL_DEBUG, IS_MAIL_DEBUG);

            return javaMailProperties;
        }

        private Properties pop3JavaMailProperties() {
            Properties javaMailProperties = new Properties();
            javaMailProperties.setProperty(MAIL_DEBUG, IS_MAIL_DEBUG);
            return javaMailProperties;
        }

        @Bean
        public IntegrationFlow imapMailFlow() {
            return IntegrationFlows
                    .from(Mail.imapInboundAdapter("imap://imapuser:pw@localhost:" + mailServer.getImap().getPort() + "/INBOX")
                            .searchTermStrategy(this::fromAndNotSeenTerm)
                            .userFlag("pocImapMailFlow")
                            .shouldMarkMessagesAsRead(true)
                            .shouldDeleteMessages(true)
                            .autoCloseFolder(false)
                            .maxFetchSize(1)
                            .javaMailProperties(imap4JavaMailProperties()), e -> e.autoStartup(true)
                            .poller(p -> p.fixedDelay(IMAP_POLL_DELAY)))
                    //.channel(MessageChannels.queue("imapChannel"))
                    .channel(imapChannel())
                    .get();
        }

        @Bean
        public PollableChannel pop3Channel() {
            return new QueueChannel();
        }

        @Bean
        public PollableChannel imapChannel() {
            return new QueueChannel();
        }

        @Bean
        @InboundChannelAdapter(value = "pop3Channel", poller = @Poller(fixedDelay = "5000"))
        public MessageSource<Object> pop3MailReceiver() {
            Pop3MailReceiver receiver = new Pop3MailReceiver("localhost", mailServer.getPop3().getPort(), "popuser", "pw");
            receiver.setJavaMailProperties(pop3JavaMailProperties());
            receiver.setShouldDeleteMessages(true);
            receiver.setAutoCloseFolder(false);
            receiver.setMaxFetchSize(1);
            var source = new MailReceivingMessageSource(receiver);
            return source;
        }


        private SearchTerm fromAndNotSeenTerm(Flags supportedFlags, Folder folder) {
            SearchTerm searchTerm = null;
            if (supportedFlags != null) {
                if (supportedFlags.contains(Flags.Flag.RECENT)) {
                    searchTerm = new FlagTerm(new Flags(Flags.Flag.RECENT), true);
                }
                if (supportedFlags.contains(Flags.Flag.ANSWERED)) {
                    NotTerm notAnswered = new NotTerm(new FlagTerm(new Flags(Flags.Flag.ANSWERED), true));
                    if (searchTerm == null) {
                        searchTerm = notAnswered;
                    } else {
                        searchTerm = new AndTerm(searchTerm, notAnswered);
                    }
                }
                if (supportedFlags.contains(Flags.Flag.DELETED)) {
                    NotTerm notDeleted = new NotTerm(new FlagTerm(new Flags(Flags.Flag.DELETED), true));
                    if (searchTerm == null) {
                        searchTerm = notDeleted;
                    } else {
                        searchTerm = new AndTerm(searchTerm, notDeleted);
                    }
                }
                if (supportedFlags.contains(Flags.Flag.SEEN)) {
                    NotTerm notSeen = new NotTerm(new FlagTerm(new Flags(Flags.Flag.SEEN), true));
                    if (searchTerm == null) {
                        searchTerm = notSeen;
                    } else {
                        searchTerm = new AndTerm(searchTerm, notSeen);
                    }
                }
            }

            return searchTerm;
        }

    }
}