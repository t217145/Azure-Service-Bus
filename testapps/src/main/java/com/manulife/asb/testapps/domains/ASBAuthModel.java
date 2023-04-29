package com.manulife.asb.testapps.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBAuthModel {
    private String clientId;
    private String clientSecret;
    private String tenantId;
}
