package com.cyrus822.asb.demo.sendbatch.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DestModel {
    private String namespace;
    private String destType;
    private String destName;
    private String topicName;
}
