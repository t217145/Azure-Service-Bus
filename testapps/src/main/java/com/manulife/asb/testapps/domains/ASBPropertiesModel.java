package com.manulife.asb.testapps.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBPropertiesModel {
    private String key;
    private String value;
}
