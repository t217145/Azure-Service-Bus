package com.manulife.asb.fop.demo.sender.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusTransactionContext;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class FOPController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FOPController.class);

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
    
    @GetMapping({"", "/", "/index"})
    public String index(){
        sendBatch();
        return "success";
    }

    @GetMapping("single")
    public String single(@RequestParam("type") String type, @RequestParam("msgId") String msgId){
        try{
            //Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::FOPController::single()::Step-1]");  
            TokenCredential credential = new ClientSecretCredentialBuilder()
                                            .clientId(clientId)
                                            .clientSecret(clientSecret)
                                            .tenantId(tenantId)
                                            .build();
                                                
            //Step-2 : prepare the ServiceBusSenderClient
            LOGGER.info("[Start::FOPController::single()::Step-2]");  

            ServiceBusSenderClient client = new ServiceBusClientBuilder()
                                                .credential(namespace, credential)
                                                .sender()
                                                .topicName(topic)
                                                .buildClient();
            
            sendMsg(client, null, type, msgId);
        } catch(Exception e){
            LOGGER.error("[Start::BatchSendController::single()::Error]", e);    
        }
        return "success";
    }

    private void sendBatch(){
        try{
            //Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::FOPController::SendBatch()::Step-1]");  
            TokenCredential credential = new ClientSecretCredentialBuilder()
                                            .clientId(clientId)
                                            .clientSecret(clientSecret)
                                            .tenantId(tenantId)
                                            .build();
                                                
            //Step-2 : prepare the ServiceBusSenderClient
            LOGGER.info("[Start::FOPController::SendBatch()::Step-2]");  

            ServiceBusSenderClient client = new ServiceBusClientBuilder()
                                                .credential(namespace, credential)
                                                .sender()
                                                .topicName(topic)
                                                .buildClient();

            ServiceBusTransactionContext ctx = client.createTransaction();

            try{
                //Step-3 : prepare the ServiceBusMessage
                LOGGER.info("[Start::FOPController::SendBatch()::Step-3]");
                String[] batch = {"low","low","low","medium","low","high","medium","low","high","low","low"};
                int i = 0 ;

                for (String type : batch) {
                    LOGGER.info("[Start::FOPController::SendBatch()::Step-3a]");
                    sendMsg(client, ctx, type, String.format("m%d", ++i));
                }

                LOGGER.info("[Start::FOPController::SendBatch()::Step-4]");
                client.commitTransaction(ctx);
            } catch (Exception e) {
                client.rollbackTransaction(ctx);
            }

            //Step-5 : close the client and br
            LOGGER.info("[Start::BatchSendController::sendBatch()::Step-5]");    
            client.close(); 
        }catch(Exception e){
            LOGGER.error("[Start::BatchSendController::sendBatch()::Error]", e);    
        }
    }

    private void sendMsg(ServiceBusSenderClient client, ServiceBusTransactionContext ctx, String type, String msgId){
        LOGGER.info("[Start::FOPController::SendMsg()::Step-3b]");
        ServiceBusMessage asbMsg = new ServiceBusMessage(BinaryData.fromBytes(String.format("I am a %s message", type).getBytes(UTF_8)));
        asbMsg.setMessageId(msgId);
        asbMsg.setSessionId(type);

        LOGGER.info("[Start::FOPController::SendMsg()::Step-3c]");
        if(ctx == null){
            client.sendMessage(asbMsg);
        } else {
            client.sendMessage(asbMsg, ctx);
        }
    }
}
