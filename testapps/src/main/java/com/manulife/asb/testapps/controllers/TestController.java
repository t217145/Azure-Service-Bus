package com.manulife.asb.testapps.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.manulife.asb.testapps.domains.ASBReceiverModel;
import com.manulife.asb.testapps.domains.ASBSenderModel;
import com.manulife.asb.testapps.domains.ASBPropertiesModel;
import static java.nio.charset.StandardCharsets.UTF_8;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

@RestController
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    @PostMapping("/send")
    public String send(ModelMap m, @RequestBody ASBSenderModel model) {
        String msg = "";
        try {
            // Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::TestController::send()::Step-1]");
            TokenCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(model.getAuth().getClientId())
                    .clientSecret(model.getAuth().getClientSecret())
                    .tenantId(model.getAuth().getTenantId())
                    .build();

            // Step-2 : prepare the ServiceBusSenderClient
            LOGGER.info("[Start::TestController::send()::Step-2]");
            ServiceBusSenderClient client;
            if (model.getDest().getType().equals("queue")) {
                client = new ServiceBusClientBuilder()
                        .credential(model.getDest().getNamespace(), credential)
                        .sender()
                        .queueName(model.getDest().getDestination())
                        .buildClient();
            } else {
                client = new ServiceBusClientBuilder()
                        .credential(model.getDest().getNamespace(), credential)
                        .sender()
                        .topicName(model.getDest().getDestination())
                        .buildClient();
            }

            // Step-3 : prepare the ServiceBusMessage
            LOGGER.info("[Start::TestController::send()::Step-3]");
            ServiceBusMessage asbMsg = new ServiceBusMessage(BinaryData.fromBytes(model.getMsg().getBytes(UTF_8)));

            if (model.getSessionId() != null && !model.getSessionId().isEmpty()) {
                asbMsg.setSessionId(model.getSessionId());
            }

            // Step-4 : loop for properties
            LOGGER.info("[Start::TestController::send()::Step-4]");
            if (model.getProperties() != null) {
                for (ASBPropertiesModel prop : model.getProperties()) {
                    asbMsg.getApplicationProperties().put(prop.getKey(), prop.getValue());
                }
            }

            // Step-5 : Set the subject
            LOGGER.info("[Start::TestController::send()::Step-5]");
            if (model.getSubject() != null && !model.getSubject().trim().equals("")) {
                asbMsg.setSubject(model.getSubject());
            }

            // Step-6 : send out the message
            LOGGER.info("[Start::TestController::send()::Step-6]");
            client.sendMessage(asbMsg);

            msg = "success";

            // Step-7 : close the client and br
            LOGGER.info("[Start::TestController::send()::Step-7]");
            client.close();
        } catch (Exception e) {
            msg = String.format("Error : %s", e.getMessage());
        }
        return msg;
    }

    @PostMapping("/receive")
    public String receive(ModelMap m, @RequestBody ASBReceiverModel model) {
        String msg = "";
        try {
            // Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::TestController::receive()::Step-1]");
            TokenCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(model.getAuth().getClientId())
                    .clientSecret(model.getAuth().getClientSecret())
                    .tenantId(model.getAuth().getTenantId())
                    .build();

            // Step-2 : Triage the logic
            LOGGER.info("[Start::TestController::receive()::Step-2]");
            if (model.getDest().getType().equals("subscription")) {
                if (model.getSessionId() != null && !model.getSessionId().isEmpty()) {
                    msg = getMessageFromSessionSubscription(credential, model);
                } else {
                    msg = getMessageFromSubscription(credential, model);
                }
            } else {
                msg = getMessageFromQueue(credential, model);
            }
        } catch (Exception e) {
            msg = String.format("Error : %s", e.getMessage());
        }
        return msg;
    }

    private String getMessageFromQueue(TokenCredential credential, ASBReceiverModel model) {
        String msg = "";
        try {
            // Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::TestController::getMessageFromQueue()::Step-1]");
            ServiceBusReceiverClient client = new ServiceBusClientBuilder()
                    .credential(model.getDest().getNamespace(), credential)
                    .receiver()
                    .queueName(model.getDest().getDestination())
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                    .buildClient();

            // Step-3 : Get the message
            LOGGER.info("[Start::TestController::getMessageFromQueue()::Step-2]");
            ServiceBusReceivedMessage message = client.peekMessage();
            msg = new String(message.getBody().toBytes());
            client.complete(message);
            LOGGER.info("Received Message {}}", msg);

            // Step-4 : stop the processor
            LOGGER.info("[Start::TestController::getMessageFromQueue()::Step-3]");
            client.close();
        } catch (Exception e) {
            msg = String.format("Error : %s", e.getMessage());
        }
        return msg;
    }

    private String getMessageFromSubscription(TokenCredential credential, ASBReceiverModel model) {
        String msg = "";
        try {
            // Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::TestController::GetMessageFromSubscription()::Step-1]");
            ServiceBusReceiverClient client = new ServiceBusClientBuilder()
                    .credential(model.getDest().getNamespace(), credential)
                    .receiver()
                    .topicName(model.getDest().getTopicName())
                    .subscriptionName(model.getDest().getDestination())
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                    .buildClient();

            // Step-3 : Get the message
            LOGGER.info("[Start::TestController::GetMessageFromSubscription()::Step-2]");
            ServiceBusReceivedMessage message = client.peekMessage();
            msg = new String(message.getBody().toBytes());
            client.complete(message);
            LOGGER.info("Received Message {}}", msg);

            // Step-4 : stop the processor
            LOGGER.info("[Start::TestController::GetMessageFromSubscription()::Step-3]");
            client.close();
        } catch (Exception e) {
            msg = String.format("Error : %s", e.getMessage());
        }
        return msg;
    }

    private String getMessageFromSessionSubscription(TokenCredential credential, ASBReceiverModel model) {
        String msg = "";
        try {
            // Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::TestController::GetMessageFromSessionSubscription()::Step-1]");
            ServiceBusSessionReceiverClient sessionClient = new ServiceBusClientBuilder()
                    .credential(model.getDest().getNamespace(), credential)
                    .sessionReceiver()
                    .topicName(model.getDest().getTopicName())
                    .subscriptionName(model.getDest().getDestination())
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                    .buildClient();

            // Step-2 : Set the session id
            LOGGER.info("[Start::TestController::GetMessageFromSessionSubscription()::Step-2]");
            sessionClient.acceptSession(model.getSessionId());

            // Step-3 : Get the message
            LOGGER.info("[Start::TestController::GetMessageFromSessionSubscription()::Step-3]");
            ServiceBusReceiverClient client = sessionClient.acceptNextSession();
            ServiceBusReceivedMessage message = client.peekMessage();
            msg = new String(message.getBody().toBytes());
            client.complete(message);
            LOGGER.info("Received Message {}}", msg);

            // Step-4 : stop the processor
            LOGGER.info("[Start::TestController::GetMessageFromSessionSubscription()::Step-4]");
            client.close();
            sessionClient.close();
        } catch (Exception e) {
            msg = String.format("Error : %s", e.getMessage());
        }
        return msg;
    }
}