package com.cyrus822.messaging.asb.receiver.services;

import java.time.Duration;
import java.util.Date;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.cyrus822.messaging.asb.receiver.domains.ASBDBMsg;
import com.cyrus822.messaging.asb.receiver.domains.ASBMsg;
import com.cyrus822.messaging.asb.receiver.repos.ASBDBMsgRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ReceiverSvc implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiverSvc.class);

    @Autowired
    private ASBDBMsgRepo repo;

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
    private String topicName;
    @Value("${asb.subscription.name}")
    private String subscription;

    // App Setting
    @Value("${app.threadCnt}")
    private int threadCnt;
    @Value("${app.clientPerThread}")
    private int clientPerThread;
    @Value("${app.enableSession}")
    private boolean enableSession;
    @Value("${app.needSleep}")
    private boolean needSleep;

    @Override
    public void run(String... args) throws Exception {
        try {
            for (int i = 0; i < threadCnt * clientPerThread; i++) {
                createProcessor(i + 1);
            }
        } catch (Exception e) {
            LOGGER.error("Error in ReceiverSvc", e);
        } // end of try-catch
    }// end of run()

    private void createProcessor(int processorId) {
        // Step-1 : prepare the TokenCredential
        LOGGER.info("[Start::SenderSvc::createProcessor()::Step-1]");
        TokenCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        LOGGER.info("[Start::SenderSvc::createProcessor()::Step-2]");
        AmqpRetryOptions opt = new AmqpRetryOptions();
        opt.setDelay(Duration.ofMillis(50));
        opt.setMaxRetries(100);
        opt.setMode(AmqpRetryMode.FIXED);
        opt.setTryTimeout(Duration.ofSeconds(5));

        LOGGER.info("[Start::SenderSvc::createProcessor()::Step-3]");
        Consumer<ServiceBusReceivedMessageContext> processMessage = context -> {
            final ServiceBusReceivedMessage message = context.getMessage();

            String content = new String(message.getBody().toBytes());
            String ctxId = message.getSessionId();
            Date enqueueTime = Date.from(message.getEnqueuedTime().toInstant());
            Date dequeueTime = new Date();
            long deliveryCnt = message.getDeliveryCount();
            ASBMsg asbMsg = new ASBMsg();

            try {
                asbMsg = new ObjectMapper().readValue(content, ASBMsg.class);
            } catch (Exception jsonErr) {
                context.abandon();
                LOGGER.error("Failed to parse JSON", jsonErr);
                jsonErr.printStackTrace();
            }

            if (needSleep) {
                try {
                    Thread.sleep(3000);
                } catch (Exception sleepError) {
                    context.abandon();
                    LOGGER.error("Failed to sleep", sleepError);
                    sleepError.printStackTrace();
                }
            }

            try {
                ASBDBMsg asbDBMsg = new ASBDBMsg(0, ctxId, asbMsg.getMsgId(), processorId, deliveryCnt, asbMsg.getMsg(),
                        enqueueTime,
                        dequeueTime);
                repo.save(asbDBMsg);
                context.complete();
            } catch (Exception err) {
                context.abandon();
                LOGGER.error(String.format("Failed to process the message %s%n", message.getMessageId()), err);
                err.printStackTrace();
            }
        };

        LOGGER.info("[Start::SenderSvc::createProcessor()::Step-4]");
        Consumer<ServiceBusErrorContext> processError = errorContext -> System.err
                .println("Error occurred while receiving message:" + errorContext.getException());

        LOGGER.info("[Start::SenderSvc::createProcessor()::Step-5]");
        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                .credential(credential)
                .fullyQualifiedNamespace(namespace)
                .retryOptions(opt)
                .sessionProcessor()
                .topicName(topicName)
                // .maxConcurrentCalls(10)// need to test this value
                .subscriptionName(subscription)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()
                .processMessage(processMessage)
                .processError(processError)
                .disableAutoComplete()
                .buildProcessorClient();

        LOGGER.info("[Start::SenderSvc::createProcessor()::Step-6]");
        processorClient.start();
    }// end of createProcessor()
}