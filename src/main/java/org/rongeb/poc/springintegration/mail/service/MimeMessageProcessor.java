package org.rongeb.poc.springintegration.mail.service;

import org.rongeb.poc.springintegration.mail.dto.AttachmentDto;
import org.rongeb.poc.springintegration.mail.dto.MailMessageDto;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.*;

@Service
public class MimeMessageProcessor {

    @Value("${output.dir}")
    private String outputDir;

    Logger log = LoggerFactory.getLogger(MimeMessageProcessor.class);

    public void extractAndProcessMessage(final MimeMessage message) {
        try {
            String messageId = message.getMessageID();
            List<AttachmentDto> attachmentDtos = new ArrayList<>(0);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            message.writeTo(output);
            String messageRaw = output.toString();

            MailMessageDto mailMessageDto = new MailMessageDto(messageId, messageRaw);
            if(!ObjectUtils.isEmpty(message)) {

                if(StringUtils.isNotBlank(mailMessageDto.getSender())) {
                    mailMessageDto.setSender(message.getSender().toString());
                } else if(!ObjectUtils.isEmpty(message.getFrom()) && message.getFrom().length > 0){
                    mailMessageDto.setSender(message.getFrom()[0].toString());
                }
                if(StringUtils.isNotBlank(message.getSubject())) {
                    mailMessageDto.setSubject(message.getSubject());
                }

                Address[] recipientsTo = message.getRecipients(Message.RecipientType.TO);
                mailMessageDto.getRecipientsToSet().addAll(this.getRecipients(recipientsTo));
                Address[] recipientsCc = message.getRecipients(Message.RecipientType.CC);
                mailMessageDto.getRecipientsCcSet().addAll(this.getRecipients(recipientsCc));
                Address[] recipientsBcc = message.getRecipients(Message.RecipientType.BCC);
                mailMessageDto.getRecipientsBccSet().addAll(this.getRecipients(recipientsBcc));

                mailMessageDto.setEmailDate(message.getReceivedDate());

                if (message.isMimeType("text/plain")) {
                    mailMessageDto.setBody(handleText(message.getContent()));
                } else if (message.isMimeType("text/html")) {
                    mailMessageDto.setBody(handleHtml(message.getContent()));
                } else if (message.isMimeType("multipart/*")) {
                    mailMessageDto.setBody(handleTextFromMimeMultipart((MimeMultipart) message.getContent()));
                    attachmentDtos = this.handleAttachments((MimeMultipart) message.getContent());
                }

                doWhateverYouWant(attachmentDtos);
            }

        } catch (Exception e) {
            log.warn("Error on processing message {}", e.getLocalizedMessage());
        }
    }

    private void doWhateverYouWant(final List<AttachmentDto> attachmentDtos) {

        log.info("Processing message {}", attachmentDtos.toString());
    }


    private Set<String> getRecipients(Address[] addresses) {
        Set<String> addressSet = new HashSet<>();
        if(addresses != null && addresses.length > 0) {
            for(Address address : addresses) {
                if(!ObjectUtils.isEmpty(address)) {
                    addressSet.add(address.toString());
                }
            }
        }
        return addressSet;
    }

    private List<AttachmentDto> handleAttachments(final MimeMultipart mimeMultipart) throws IOException, MessagingException {
        int count = mimeMultipart.getCount();
        List<AttachmentDto> attachmentDtos = new ArrayList<>(0);
        for (int i = 0; i < count; i++) {
            AttachmentDto attachment = null;
            MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(i);
            if(Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                attachment = new AttachmentDto();
                attachment.setExtension(FilenameUtils.getExtension(bodyPart.getFileName()));
                attachment.setFileDate(new Date());
                attachment.setUuid(UUID.randomUUID().toString());
                var fileName = this.renameFile(bodyPart.getFileName(), attachment.getUuid(), attachment.getFileDate().getTime(), attachment.getExtension());
                attachment.setFilename(fileName);
                attachmentDtos.add(attachment);
                var path = outputDir+File.separator+fileName;
                attachment.setPath(path);
                bodyPart.saveFile(outputDir+File.separator+fileName);
            }
        }

        return attachmentDtos;

    }

    private String handleText(Object emailContent) {
            if(!ObjectUtils.isEmpty(emailContent)) {
                return emailContent.toString();
            }
            return "";
    }

    private String handleHtml(Object emailContent) {
        if(!ObjectUtils.isEmpty(emailContent)) {
            String html = emailContent.toString();
            return Jsoup.parse(html).text();
        }
        return "";
    }

    private String handleTextFromMimeMultipart(MimeMultipart mimeMultipart)  throws MessagingException, IOException{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    result = result + handleTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
                }
            }
        }
        return result;
    }

    private String renameFile(final String filename, String uuid, long timestamp, String extension) {
        if(StringUtils.isNotBlank(filename)) {
            var cleanFilename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
            var filenameWithoutExt = cleanFilename.replace("."+extension, "");
            return String.format("%s_%s_%s.%s", filenameWithoutExt, uuid, timestamp, extension);
        }
        return String.format("%s_%s_%s.%s", "no_name_file", uuid, timestamp, extension);
    }

}
