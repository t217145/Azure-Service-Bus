package com.manulife.asb.testapps.domains;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBSenderModel {
    private ASBAuthModel auth;

    private ASBDestinationModel dest;

    private String msg;
    private String sessionId;

    private List<ASBPropertiesModel> properties;
    private String subject;
}