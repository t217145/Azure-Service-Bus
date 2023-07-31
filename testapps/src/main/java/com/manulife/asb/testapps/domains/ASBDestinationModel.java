package com.manulife.asb.testapps.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBDestinationModel {
    private String namespace;
    private String type;
    private String destination;
    private String topicName;
}
