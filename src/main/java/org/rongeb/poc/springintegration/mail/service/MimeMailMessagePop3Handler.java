package org.rongeb.poc.springintegration.mail.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import java.io.Closeable;
import java.io.IOException;

public class MimeMailMessagePop3Handler implements MessageHandler {

    Logger log = LoggerFactory.getLogger(MimeMailMessagePop3Handler.class);

    @Autowired
    private MimeMessageProcessor processor;

    public MimeMailMessagePop3Handler() { }

    @Override
    public void handleMessage(final Message<?> message) throws MessagingException {
        log.debug("received message {}", message);

        MimeMessage mimeMessage = (MimeMessage) message.getPayload();
        processor.extractAndProcessMessage(mimeMessage);
        try {
            mimeMessage.setFlags(mimeMessage.getFlags(), false);
            mimeMessage.setFlag(Flags.Flag.SEEN, true);
            mimeMessage.setFlag(Flags.Flag.DELETED, true);
            Closeable closeableResource = StaticMessageHeaderAccessor.getCloseableResource(message);
            if (closeableResource != null) {
                try {
                    closeableResource.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        } catch (javax.mail.MessagingException e) {
           log.error(e.getMessage());
        }

    }
}
