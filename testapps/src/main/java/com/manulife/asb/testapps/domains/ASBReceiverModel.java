package com.manulife.asb.testapps.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBReceiverModel {
    private ASBAuthModel auth;

    private ASBDestinationModel dest;

    private String sessionId;
}