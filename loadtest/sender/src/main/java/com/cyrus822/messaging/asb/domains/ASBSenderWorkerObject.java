package com.cyrus822.messaging.asb.domains;

import java.util.concurrent.CountDownLatch;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ASBSenderWorkerObject {
    // ASB setting
    private ServiceBusSenderClient[] clients;

    // App Setting
    private boolean enableSession;
    private int msgCntPerThread;
    private CountDownLatch latch;
}
