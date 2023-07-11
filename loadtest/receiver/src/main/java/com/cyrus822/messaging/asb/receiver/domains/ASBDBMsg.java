package com.cyrus822.messaging.asb.receiver.domains;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ASBDBMsg {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String ctxId;
    private int msgId;
    private int processorId;
    private long deliveryCnt;
    private String msg;
    @Temporal(TemporalType.TIMESTAMP)
    private Date enqueuTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date dequeuTime;
}
