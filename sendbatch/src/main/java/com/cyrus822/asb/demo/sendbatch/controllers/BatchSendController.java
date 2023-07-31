package com.cyrus822.asb.demo.sendbatch.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusTransactionContext;
import com.cyrus822.asb.demo.sendbatch.domains.MsgModel;
import com.cyrus822.asb.demo.sendbatch.domains.SendModel;
import com.cyrus822.asb.demo.sendbatch.domains.TransRecord;
import com.cyrus822.asb.demo.sendbatch.repo.TransRecordRepo;
import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class BatchSendController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSendController.class);

    @Autowired
    private TransRecordRepo repo;

    @PostMapping("/sendbatch")
    public String sendBatch(@RequestBody SendModel model){
        //Step-1 : prepare the TokenCredential
        LOGGER.info("[Start::BatchSendController::sendBatch()::Step-1]");  
        TokenCredential credential = new ClientSecretCredentialBuilder()
                                        .clientId(model.getCred().getClientId())
                                        .clientSecret(model.getCred().getSecret())
                                        .tenantId(model.getCred().getTenantId())
                                        .build();
                                            
        //Step-2 : prepare the ServiceBusSenderClient
        LOGGER.info("[Start::BatchSendController::sendBatch()::Step-2]");  
        
        ServiceBusSenderClient client;
        if(model.getDest().getDestType().equals("queue")){
            client = new ServiceBusClientBuilder()
            .credential(model.getDest().getNamespace(), credential)
            .sender()
            .queueName(model.getDest().getDestName())
            .buildClient();
        } else {
            client = new ServiceBusClientBuilder()
            .credential(model.getDest().getNamespace(), credential)
            .sender()
            .topicName(model.getDest().getDestName())
            .buildClient();
        }

        ServiceBusTransactionContext ctx = client.createTransaction();

        try{
            //Step-3 : prepare the ServiceBusMessage
            LOGGER.info("[Start::BatchSendController::sendBatch()::Step-3]");   
    
            for (MsgModel msgModel : model.getMsgs()) {
                LOGGER.info("[Start::BatchSendController::sendBatch()::Step-3a]");
                ServiceBusMessage asbMsg = new ServiceBusMessage(BinaryData.fromBytes(msgModel.getMsg().getBytes(UTF_8)));
                asbMsg.setMessageId(msgModel.getMessageId());
                asbMsg.setSessionId(msgModel.getSessionId());

                LOGGER.info("[Start::BatchSendController::sendBatch()::Step-3b]");
                // Below line will trigger exception, duplicate sessionId, causing save to db failed
                repo.save(new TransRecord(0, msgModel.getSessionId(), msgModel.getMessageId(), msgModel.getMsg()));

                LOGGER.info("[Start::BatchSendController::sendBatch()::Step-3c]");
                //currentBatch.tryAddMessage(asbMsg);
                client.sendMessage(asbMsg, ctx);
            }
    
            //Step-4 : send out the message
            LOGGER.info("[Start::BatchSendController::sendBatch()::Step-4]");
            //client.sendMessages(currentBatch, ctx);
    
            client.commitTransaction(ctx);
        } catch (Exception e) {
            client.rollbackTransaction(ctx);
            return e.getMessage();
        }
        
        //Step-5 : close the client and br
        LOGGER.info("[Start::BatchSendController::sendBatch()::Step-5]");    
        client.close(); 
        return "success";
    }
}