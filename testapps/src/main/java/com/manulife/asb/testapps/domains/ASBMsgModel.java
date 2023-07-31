package com.manulife.asb.testapps.domains;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBMsgModel {
    private String body;
    private String correlationId;
    private String contentType;
    private String subject;
    private String messageId;
    private String sessionId;
    private String replyTo;
    private String replyToSessionId;
    private String to;
    private Map<String, Object> applicationProperties;
    private long deliveryCount;
}
