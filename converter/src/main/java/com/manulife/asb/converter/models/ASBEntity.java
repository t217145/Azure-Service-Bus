package com.manulife.asb.converter.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBEntity {
    private String entityType;
    private String ObjectName;
    private String entityName;
    private String namespace;
    private String topicName;
    private String MaxDelivery;
    private String ttl;
    private String lock;
    private String forwardTo;
    private String dedip;
    private String session;
    private String ttlExpired;
    private String filter;
}
