package org.rongeb.poc.springintegration.mail.dto;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MailMessageDto {

    public MailMessageDto(String messageId, String raw) {
        this.messageId = messageId;
        this.raw = raw;
    }

    private String messageId;
    private String raw;
    private String subject;
    private String sender;
    private Set<String> recipientsToSet = new HashSet<>();
    private Set<String> recipientsCcSet = new HashSet<>();
    private Set<String> recipientsBccSet = new HashSet<>();
    private String bodyStripped;
    private String body;
    private Date emailDate;
    private List<AttachmentDto> attachments;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Set<String> getRecipientsToSet() {
        return recipientsToSet;
    }

    public void setRecipientsToSet(Set<String> recipientsToSet) {
        this.recipientsToSet = recipientsToSet;
    }

    public Set<String> getRecipientsCcSet() {
        return recipientsCcSet;
    }

    public void setRecipientsCcSet(Set<String> recipientsCcSet) {
        this.recipientsCcSet = recipientsCcSet;
    }

    public Set<String> getRecipientsBccSet() {
        return recipientsBccSet;
    }

    public void setRecipientsBccSet(Set<String> recipientsBccSet) {
        this.recipientsBccSet = recipientsBccSet;
    }

    public String getBodyStripped() {
        return bodyStripped;
    }

    public void setBodyStripped(String bodyStripped) {
        this.bodyStripped = bodyStripped;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Date getEmailDate() {
        return emailDate;
    }

    public void setEmailDate(Date emailDate) {
        this.emailDate = emailDate;
    }

    public List<AttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDto> attachments) {
        this.getAttachments().addAll(attachments);
    }
}
