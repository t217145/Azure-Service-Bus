package com.cyrus822.asb.demo.sendbatch.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CredModel {
    private String tenantId;
    private String clientId;
    private String secret;
}
