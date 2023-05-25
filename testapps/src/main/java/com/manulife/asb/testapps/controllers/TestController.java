package com.manulife.asb.testapps.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.manulife.asb.testapps.domains.ASBReceiverModel;
import com.manulife.asb.testapps.domains.ASBSenderModel;
import com.manulife.asb.testapps.domains.ASBMsgModel;
import com.manulife.asb.testapps.domains.ASBPropertiesModel;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
    public List<ASBMsgModel> receive(ModelMap m, @RequestBody ASBReceiverModel model) {
        List<ASBMsgModel> rtn = new ArrayList<>();
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
            int cnt = (model.getCnt() > 0) ? model.getCnt() : 1;
            if (model.isHasSession()) {
                rtn = handleSessionReceive(cnt, credential, model);
            } else {
                rtn = handleNonSessionReceive(cnt, credential, model);
            }
        } catch (Exception e) {
            LOGGER.error("[Start::TestController::receive()::Error] : {}", e.getMessage());
        }
        return rtn;
    }

    private List<ASBMsgModel> handleSessionReceive(int cnt, TokenCredential credential, ASBReceiverModel model){
        List<ASBMsgModel> rtn = new ArrayList<>();
        if (model.getDest().getType().equals("subscription")) {
            rtn = getMessageFromSessionSubscription(cnt, credential, model);
        } else {
            rtn = getMessageFromSessionQueue(cnt, credential, model);
        }
        return rtn;
    }

    private List<ASBMsgModel> handleNonSessionReceive(int cnt, TokenCredential credential, ASBReceiverModel model){
        List<ASBMsgModel> rtn = new ArrayList<>();
        if (model.getDest().getType().equals("subscription")) {
            rtn = getMessageFromSubscription(cnt, credential, model);
        } else {
            rtn = getMessageFromQueue(cnt, credential, model);
        }
        return rtn;
    }

    private List<ASBMsgModel> getMessageFromQueue(int cnt, TokenCredential credential, ASBReceiverModel model) {
        List<ASBMsgModel> rtn = new ArrayList<>();
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
            LOGGER.info("[Start::TestController::getMessageFromSessionQueue()::Step-3]");
            rtn = processSessionMessage(cnt, client);
        } catch (Exception e) {
            LOGGER.error("[Start::TestController::getMessageFromQueue()::Error] : {}", e.getMessage());
        }
        return rtn;
    }

    private List<ASBMsgModel> getMessageFromSessionQueue(int cnt, TokenCredential credential, ASBReceiverModel model) {
        List<ASBMsgModel> rtn = new ArrayList<>();
        try {
            // Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::TestController::getMessageFromSessionQueue()::Step-1]");
            ServiceBusSessionReceiverClient sessionClient = new ServiceBusClientBuilder()
                    .credential(model.getDest().getNamespace(), credential)
                    .retryOptions(new AmqpRetryOptions().setMaxRetries(1).setTryTimeout(Duration.ofSeconds(15)))
                    .sessionReceiver()
                    .queueName(model.getDest().getDestination())
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                    .buildClient();

            // Step-2 : Triage whether session id is provided
            LOGGER.info("[Start::TestController::getMessageFromSessionQueue()::Step-2]");
            if(model.getSessionId() == null || model.getSessionId().isEmpty()){
                do{
                    try{
                        // Step-3 : Get the message
                        LOGGER.info("[Start::TestController::getMessageFromSessionQueue()::Step-3]");
                        rtn.addAll(processSessionMessage(cnt, sessionClient.acceptNextSession()));                    
                    }catch(AmqpException e){
                        LOGGER.info("No more session to accept");
                        break;
                    } catch (Exception e){
                        LOGGER.error("[Start::TestController::getMessageFromSessionQueue()::Error] : {}", e.getMessage());
                        break;
                    }
                }while(true);
            } else {
                // Step-3 : Get the message
                LOGGER.info("[Start::TestController::getMessageFromSessionQueue()::Step-3]");
                rtn = processSessionMessage(cnt, sessionClient.acceptSession(model.getSessionId()));
            }
        } catch (Exception e) {
            LOGGER.error("[Start::TestController::getMessageFromSessionQueue()::Error] : {}", e.getMessage());
        }
        return rtn;
    }


    private List<ASBMsgModel> getMessageFromSubscription(int cnt, TokenCredential credential, ASBReceiverModel model) {
        List<ASBMsgModel> rtn = new ArrayList<>();
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
            LOGGER.info("[Start::TestController::getMessageFromSubscription()::Step-2]");
            rtn = processSessionMessage(cnt, client);
        } catch (Exception e) {
            LOGGER.error("[Start::TestController::getMessageFromSubscription()::Error] : {}", e.getMessage());
        }
        return rtn;
    }

    private List<ASBMsgModel> getMessageFromSessionSubscription(int cnt, TokenCredential credential, ASBReceiverModel model){
        List<ASBMsgModel> rtn = new ArrayList<>();
        try {
            // Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::TestController::getMessageFromSessionSubscription()::Step-1]");
            ServiceBusSessionReceiverClient sessionClient = new ServiceBusClientBuilder()
                    .retryOptions(new AmqpRetryOptions().setMaxRetries(1).setTryTimeout(Duration.ofSeconds(15)))
                    .credential(model.getDest().getNamespace(), credential)
                    .sessionReceiver()
                    .topicName(model.getDest().getTopicName())
                    .subscriptionName(model.getDest().getDestination())
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                    .buildClient();

            // Step-2 : Triage whether session id is provided
            LOGGER.info("[Start::TestController::getMessageFromSessionSubscription()::Step-2]");
            if(model.getSessionId() == null || model.getSessionId().isEmpty()){
                do{
                    try{
                        // Step-3 : Get the message
                        LOGGER.info("[Start::TestController::getMessageFromSessionSubscription()::Step-3]");
                        rtn.addAll(processSessionMessage(cnt, sessionClient.acceptNextSession()));                    
                    }catch(AmqpException e){
                        LOGGER.info("No more session to accept");
                        break;
                    } catch (Exception e){
                        LOGGER.error("[Start::TestController::getMessageFromSessionSubscription()::Error] : {}", e.getMessage());
                        break;
                    }
                }while(true);
            } else {
                // Step-3 : Get the message
                LOGGER.info("[Start::TestController::getMessageFromSessionSubscription()::Step-3]");
                rtn = processSessionMessage(cnt, sessionClient.acceptSession(model.getSessionId()));
            }
        } catch (Exception e) {
            LOGGER.error("[Start::TestController::getMessageFromSessionSubscription()::Error] : {}", e.getMessage());
        }
        return rtn;
    }

    private List<ASBMsgModel> processSessionMessage(int cnt, ServiceBusReceiverClient client){
        List<ASBMsgModel> rtn = new ArrayList<>();
        try {
            // Step-1 : Get the message
            LOGGER.info("[Start::TestController::processSessionMessage()::Step-1]");
            
            for(ServiceBusReceivedMessage message : client.peekMessages(cnt)){
                String msg = new String(message.getBody().toBytes());
                ASBMsgModel rtnMsg =  new ASBMsgModel(
                    new String(message.getBody().toBytes()),
                    message.getCorrelationId(),
                    message.getContentType(),
                    message.getSubject(),
                    message.getMessageId(),
                    message.getSessionId(),
                    message.getReplyTo(),
                    message.getReplyToSessionId(),
                    message.getTo(),
                    message.getApplicationProperties(),
                    message.getDeliveryCount()
                );
                rtn.add(rtnMsg);
                //client.complete(message);
                LOGGER.info("Received Message {}}", msg);
            }

            // Step-2 : stop the processor
            LOGGER.info("[Start::TestController::processSessionMessage()::Step-2]");
            client.close();
        } catch (Exception e) {
            LOGGER.error("[Start::TestController::processSessionMessage()::Error] : {}", e.getMessage());
        }
        return rtn;
    }    
}