package com.manulife.demo.asb.session.identifier.services;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;

@Component
@EnableScheduling
public class SessionHandlerSvc implements CommandLineRunner{

    private static final Logger logger = LoggerFactory.getLogger(SessionHandlerSvc.class);

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
    @Value("${asb.subscription.name}")
    private String subscription;

    //In app setting
    private String[] sessionList = {"ctx-1", "ctx-2", "ctx-3", "ctx-4", "ctx-5", "ctx-6"};
    private ServiceBusSessionReceiverClient ctxClient = null;
    private ServiceBusReceiverClient primaryCtxClient = null;

    @Override
    public void run(String... args){
        try{
            // Step-1 : prepare the ServiceBusSessionReceiverClient
            logger.info("[Start::SessionHandlerSvc::run()::Step-1::Prepare the ServiceBusSessionReceiverClient]");            
            createClient();
            if(ctxClient == null){
                logger.warn("Warning in SessionHandlerSvc::run()::Cannot create ServiceBusSessionReceiverClient");
                return;
            }

            // Step-2 : Acquire primary session id
            logger.info("[Start::SessionHandlerSvc::run()::Step-2::Acquire primary session id]");  
            primaryCtxClient = acquireSession();
            if(primaryCtxClient == null){
                logger.warn("Warning in SessionHandlerSvc::run()::Acquire primary session id");
            }

            // Step-3 : Do your logic
        }catch(Exception e){
            logger.error("Error in SessionHandlerSvc::run()", e);
        }//end of try-catch
    }//end of run()

    private void createClient() {
        try {
            // Step-1 : prepare the TokenCredential
            logger.info("[Start::SessionHandlerSvc::createClients()::Step-1]");
            TokenCredential credential = new ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .build();
            // TokenCredential credential = new DefaultAzureCredentialBuilder().build();

            // Step-2 : prepare the AmqpRetryOptions
            logger.info("[Start::SessionHandlerSvc::createClients()::Step-2]");
            AmqpRetryOptions opt = new AmqpRetryOptions();
            opt.setDelay(Duration.ofMillis(100));
            opt.setMaxRetries(100);
            opt.setMode(AmqpRetryMode.FIXED);
            opt.setTryTimeout(Duration.ofSeconds(10));

            // Step-3 : prepare the ServiceBusSessionReceiverClient
            logger.info("[Start::SessionHandlerSvc::createClients()::Step-3]");
            ctxClient = new ServiceBusClientBuilder()
                        .credential(namespace, credential)
                        .retryOptions(opt)
                        .sessionReceiver()
                        .topicName(topic)
                        .subscriptionName(subscription)
                        .buildClient();

        } catch (Exception e) {
            logger.error("Error in SessionHandlerSvc::createClient()", e);
        } // end of try-catch
    }

    /*
     * This method do following stuff
     * 1. Check whether some sessions do not have pods / nodes to handle, so this method will traverse 
     *    all session id and try to acquire. If success, do the business logic you want. e.g. create 
     *    new thread to handle, publish event to alert Ops / Developer
     * 2. Renew the session lock if the session lock has effective duration setup in ASB
    */
    @Scheduled(fixedDelayString = "${fixedDelay}")
    private void renewSchedule(){
        if(primaryCtxClient != null && primaryCtxClient.getSessionId() != null && primaryCtxClient.getSessionId().isBlank()){
            primaryCtxClient.renewSessionLock();
            logger.warn("Warning in SessionHandlerSvc::run::Session Id %s renewed!", primaryCtxClient.getSessionId());
        }
    }//end of renewSchedule()

    @Scheduled(fixedDelayString = "${fixedDelay}")
    private ServiceBusReceiverClient acquireSession(){
        ServiceBusReceiverClient rtn = null;
        for(String ctxId : sessionList){
            try{
                rtn = ctxClient.acceptSession(ctxId);
            } catch(AmqpException ae) {
                logger.warn("Warning in SessionHandlerSvc::acquireSession()::{} already acquired!", ctxId);
                continue;
            }
            logger.info("[Start::SessionHandlerSvc::acquireSession()::Step-2::Acquired Session Id {}!]", ctxId); 
            break;
        }
        return rtn;
    }//end of acquireSession()
    
}//end of class