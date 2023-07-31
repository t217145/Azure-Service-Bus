package com.cyrus822.messaging.asb.receiver.domains;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBMsg {
    private String ctx;
    private int msgId;
    private Date preparationDtm;
    private String msg;
}
