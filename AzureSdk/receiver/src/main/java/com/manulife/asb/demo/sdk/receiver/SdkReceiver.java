package com.manulife.asb.demo.sdk.receiver;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

@Service
public class SdkReceiver implements CommandLineRunner{

    private static final Logger LOGGER = LoggerFactory.getLogger(SdkReceiver.class);

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

    @Value("${asb.topic.subscription.name}")
    private String subscription;

    @Override
    public void run(String... args) {
        try{
            //Step-1 : prepare the processor
            LOGGER.info("[Start::SdkReceiver::run()::Step-1]");
            Consumer<ServiceBusReceivedMessageContext> processor = prepareProcessor();
            if(processor == null){
                throw new Exception("[SdkReceiver::run()::Step-1] : create processor failed!");
            }

            //Step-2 : prepare the error processor
            LOGGER.info("[Start::SdkReceiver::run()::Step-2]");
            Consumer<ServiceBusErrorContext> errProcessor = errorContext -> {
                System.err.println("SdkReceiver::run()::Error occurred while receiving message:" + errorContext.getException());
            };

            //Step-3 : prepare the TokenCredential
            LOGGER.info("[Start::SdkReceiver::run()::Step-3]");
            TokenCredential credential = new ClientSecretCredentialBuilder()
                                            .clientId(clientId)
                                            .clientSecret(clientSecret)
                                            .tenantId(tenantId)
                                            .build();

            //Step-4 : prepare the Service Bus Processor Client
            LOGGER.info("[Start::SdkReceiver::run()::Step-4]");                      
            ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                                                            .credential(namespace, credential)
                                                            .processor()
                                                            .topicName(topic)
                                                            .subscriptionName(subscription)
                                                            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                                                            .processMessage(processor)
                                                            .processError(errProcessor)
                                                            .buildProcessorClient();
    
            //Step-5 : start the processor
            LOGGER.info("[Start::SdkReceiver::run()::Step-5]");                                             
            processorClient.start();
        }catch (Exception e){
            LOGGER.error("Error in SdkReceiver::run()::", e);
        }
    }//end of run()

    private Consumer<ServiceBusReceivedMessageContext> prepareProcessor(){
        try{
             return context -> {
                final ServiceBusReceivedMessage message = context.getMessage();
                try {
                    String msg = new String(message.getBody().toBytes());
                    LOGGER.info("Received Message {}}", msg);
                } catch (Exception completionError) {
                    context.abandon();
                    LOGGER.error(String.format("Completion of the message %s failed%n", message.getMessageId()), completionError);
                    completionError.printStackTrace();
                }
            };
        }catch (Exception e){
            LOGGER.error("Error in SdkReceiver::prepareProcessor()", e);
            return null;
        }        
    }
}