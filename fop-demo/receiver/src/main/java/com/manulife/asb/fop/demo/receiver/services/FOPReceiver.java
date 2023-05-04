package com.manulife.asb.fop.demo.receiver.services;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.manulife.asb.fop.demo.receiver.domains.FOPMsg;
import com.manulife.asb.fop.demo.receiver.repo.FOPMsgRepo;

@Service
public class FOPReceiver implements CommandLineRunner{
    private static final Logger LOGGER = LoggerFactory.getLogger(FOPReceiver.class);

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

    @Value("${asb.topic.subscription.isSync}")
    private String isSync;

    @Value("${asb.topic.subscription.sync}")
    private String sync;
    
    @Value("${asb.topic.subscription.batch}")
    private String batch;    

    @Value("${fop.processor.batchsize}")
    private int batchsize;

    @Value("${fop.processor.id}")
    private String pId;    

    @Autowired
    private FOPMsgRepo repo;

    @Override
    public void run(String... args) {
        try{
            //Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::FOPReceiver::run()::Step-1]");
            TokenCredential credential = new ClientSecretCredentialBuilder()
                                            .clientId(clientId)
                                            .clientSecret(clientSecret)
                                            .tenantId(tenantId)
                                            .build();

            //Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::FOPReceiver::run()::Step-1]");
            boolean isSyncMode = isSync(credential);

            if(isSyncMode){
                processSyncMode(credential);
            } else {
                processBatchMode(credential);
            }
        }catch (Exception e){
            LOGGER.error("Error in DemoReceiver::run()::", e);
        }
    }

    private void processSyncMode(TokenCredential credential){
        LOGGER.info("[Start::FOPReceiver::processSyncMode()::Step-1]");
        ServiceBusSessionReceiverClient sessionClient = new ServiceBusClientBuilder()
                                                            .credential(namespace, credential)
                                                            .sessionReceiver()
                                                            .topicName(topic)
                                                            .subscriptionName(sync)
                                                            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                                            .buildClient();

        //Step-2 : Triage the logic
        String[] ctxs = {"high", "medium", "low"};
        int[] waitSeconds = {5, 3, 1};
        int[] processMilliSeconds = {100, 300, 500};

        LOGGER.info("[Start::FOPReceiver::processSyncMode()::Step-2]");
        for(int i=0; i<ctxs.length; i = (i == ctxs.length - 1) ? 0 : i+1){
            processSessionMsg(sessionClient, ctxs[i], waitSeconds[i], processMilliSeconds[i]);
        }
    }

    private void processBatchMode(TokenCredential credential){
        LOGGER.info("[Start::FOPReceiver::processBatchMode()::Step-1]");
        ServiceBusReceiverClient client = new ServiceBusClientBuilder()
                                                .credential(namespace, credential)
                                                .receiver()
                                                .topicName(topic)
                                                .subscriptionName(batch)
                                                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                                .buildClient();

        LOGGER.info("[Start::FOPReceiver::processBatchMode()::Step-2]");
        do{
            for(ServiceBusReceivedMessage message : client.receiveMessages(1)){
                try {
                    processMessage(message, "batch", 3000);
                    //ack the message
                    client.complete(message);                 
                } catch (Exception e) {
                    client.abandon(message);
                    LOGGER.error("Start::FOPReceiver::processBatchMode()::Step-3::Error when sleep", e);
                }                
            }
        }while(!isSync(credential));//check again

        client.close();

        processSyncMode(credential);
    }

    private boolean isSync(TokenCredential credential){
        ServiceBusReceiverClient isSyncClient;

        try{
            //Step-1 : prepare the Service Bus Client
            LOGGER.info("[Start::FOPReceiver::isSync()::Step-1]");                      
            ServiceBusSessionReceiverClient sessionClient = new ServiceBusClientBuilder()
                                                                .credential(namespace, credential)
                                                                .sessionReceiver()
                                                                .topicName(topic)
                                                                .subscriptionName(isSync)
                                                                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                                                .buildClient();
            try{
                //Step-2 : Try get sync session
                LOGGER.info("[Start::FOPReceiver::isSync()::Step-2]::Accept Session"); 
                isSyncClient = sessionClient.acceptSession("sync");
            } catch (Exception sbe) {
                LOGGER.info("[Start::FOPReceiver::isSync()::Step-2a]::Sync mode already acquired by other processor"); 
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("[Start::FOPReceiver::isSync()::Step-4]::Exception", e);
            return false;
        }    

        try{
            //Step-3 : maintain the session lock
            OffsetDateTime odt = isSyncClient.renewSessionLock();
            long renewPeriod = Duration.between(odt, OffsetDateTime.now()).getSeconds();
            new Timer().schedule(new TimerTask() {
                public void run(){
                    isSyncClient.renewSessionLock();
                }
            }, (renewPeriod - 1) * 1000, (renewPeriod - 1) * 1000);
        } catch (Exception e) {
            LOGGER.error("[Start::FOPReceiver::isSync()::Step-4]::Exception", e);
            return true;
        }                                  
        
        LOGGER.info("[Start::FOPReceiver::isSync()::Step-4]::No processor handling sync mode"); 
        return true;
    }

    private void processSessionMsg(ServiceBusSessionReceiverClient sessionClient, String ctx, int waitSeconds, int processMilliSeconds){
        //Step-3 : Get the message
        LOGGER.info("[Start::FOPReceiver::processSessionMsg()::Step-3]::Accept Session::{}", ctx); 
        ServiceBusReceiverClient client;
        try{
            client = sessionClient.acceptSession(ctx);
        } catch (Exception sbe) {
            LOGGER.info("[Start::FOPReceiver::processSessionMsg()::Step-3]::Session already acquired by other processor::{}", ctx); 
            return;
        }

        //Step-4 : Get the message
        LOGGER.info("[Start::FOPReceiver::processSessionMsg()::Step-4]");
        try {
            for(ServiceBusReceivedMessage message : client.receiveMessages(batchsize, Duration.ofSeconds(waitSeconds))){
                try {
                    processMessage(message, "sync", processMilliSeconds);
                    //ack the message
                    client.complete(message);                 
                } catch (Exception e) {
                    client.abandon(message);
                    LOGGER.error("Start::FOPReceiver::processSessionMsg()::Step-4::Error when sleep", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Start::FOPReceiver::processSessionMsg()::Step-4::Error when sleep", e);
        }
    
        //Step-4 : stop the processor
        LOGGER.info("[Start::FOPReceiver::processSessionMsg()::Step-5]");                                             
        client.close();        
    }

    private void processMessage(ServiceBusReceivedMessage message, String mode, int processSeconds) throws Exception{
        String msg = new String(message.getBody().toBytes());
        LOGGER.info("Received Message {}}", msg);

        //pretending working on it
        if(processSeconds != 0){
            Thread.sleep(processSeconds);
        }

        //Save to DB to view the effect
        repo.save(new FOPMsg(0, message.getMessageId(), message.getSessionId(), msg, mode, pId));                
    }
}
