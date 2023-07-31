package com.cyrus822.messaging.asb.services;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
//import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.cyrus822.messaging.asb.domains.ASBSenderWorkerObject;
import com.cyrus822.messaging.asb.workers.ASBSenderWorker;

@Component
public class SenderSvc implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderSvc.class);

    // ASB setting
    @Value("${spring.cloud.azure.servicebus.credential.client-id}")
    private String clientId;
    @Value("${spring.cloud.azure.servicebus.credential.client-secret}")
    private String clientSecret;
    @Value("${spring.cloud.azure.servicebus.profile.tenant-id}")
    private String tenantId;
    @Value("${spring.cloud.azure.servicebus.namespace}")
    private String namespace;
    @Value("${asb.topic.name}")
    private String topic;

    // App Setting
    @Value("${app.threadCnt}")
    private int threadCnt;
    @Value("${app.msgCntPerThread}")
    private int msgCntPerThread;
    @Value("${app.clientPerThread}")
    private int clientPerThread;
    @Value("${app.enableSession}")
    private boolean enableSession;

    @Override
    public void run(String... args) throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);

            ASBSenderWorkerObject obj = new ASBSenderWorkerObject();
            obj.setMsgCntPerThread(msgCntPerThread);
            obj.setEnableSession(enableSession);

            for (int cnt = 1; cnt <= threadCnt; cnt++) {
                // Create Clients
                ServiceBusSenderClient[] clients = createClients();
                if (clients.length == 0) {
                    break;
                }
                obj.setClients(clients);
                obj.setLatch(latch);

                new Thread(new ASBSenderWorker(cnt, obj)).start();
            } // loop to generate the thread

            latch.countDown();
        } catch (Exception e) {
            LOGGER.error("Error in SenderSvc", e);
        } // end of try-catch
    }// end of run()

    private ServiceBusSenderClient[] createClients() {
        ServiceBusSenderClient[] clients = new ServiceBusSenderClient[clientPerThread];
        try {
            for (int i = 0; i < clientPerThread; i++) {
                // Step-1 : prepare the TokenCredential
                LOGGER.info("[Start::SenderSvc::createClients()::Step-1]");
                TokenCredential credential = new ClientSecretCredentialBuilder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .tenantId(tenantId)
                        .build();

                // TokenCredential credential = new DefaultAzureCredentialBuilder().build();

                AmqpRetryOptions opt = new AmqpRetryOptions();
                opt.setDelay(Duration.ofMillis(100));
                opt.setMaxRetries(100);
                opt.setMode(AmqpRetryMode.FIXED);
                opt.setTryTimeout(Duration.ofSeconds(10));

                // Step-2 : prepare the ServiceBusSenderClient
                LOGGER.info("[Start::SenderSvc::createClients()::Step-2]");
                try {
                    clients[i] = new ServiceBusClientBuilder()
                            .credential(namespace, credential)
                            .retryOptions(opt)
                            .sender()
                            .topicName(topic)
                            .buildClient();
                } catch (AmqpException ae) {
                    LOGGER.error("AmqpException found in bulding client", ae);
                    return new ServiceBusSenderClient[0];
                } // end of try-catch
            }
        } catch (Exception e) {
            LOGGER.error("Error in SenderSvc", e);
        } // end of try-catch
        return clients;
    }
}