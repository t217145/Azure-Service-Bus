package com.manulife.asb.demo.sdk.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class SdkSender implements CommandLineRunner{

    private static final Logger LOGGER = LoggerFactory.getLogger(SdkSender.class);

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

    private BufferedReader br;

    @Override
    public void run(String... args) throws Exception {
        try{
            br = new BufferedReader(new InputStreamReader(System.in));

            //Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::SdkSender::run()::Step-1]");  
            TokenCredential credential = new ClientSecretCredentialBuilder()
                                            .clientId(clientId)
                                            .clientSecret(clientSecret)
                                            .tenantId(tenantId)
                                            .build();     
                                                  
            //Step-2 : prepare the ServiceBusSenderClient
            LOGGER.info("[Start::SdkSender::run()::Step-2]");  
            ServiceBusSenderClient client = new ServiceBusClientBuilder()
                                            .credential(namespace, credential)
                                            .sender()
                                            .topicName(topic)
                                            .buildClient();

            do{
                //Step-3 : wait for user input
                LOGGER.info("[Start::SdkSender::run()::Step-3]");
                String input = GetuserInput("Please input something : ");
                if(input.isEmpty()){
                    break;
                }

                //Step-4 : prepare the ServiceBusMessage
                LOGGER.info("[Start::SdkSender::run()::Step-4]");   
                ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(input.getBytes(UTF_8)));

                //Step-5 : send out the message
                LOGGER.info("[Start::SdkSender::run()::Step-5]");   
                client.sendMessage(msg);                
            }while(true);

            //Step-6 : close the client and br
            LOGGER.info("[Start::SdkSender::run()::Step-6]");    
            client.close();
            br.close();              
        }catch (Exception e){
            LOGGER.error("Error in SdkSender::run()::", e);
        }
    }
    
    private String GetuserInput(String msg){
        String input = "";
        try{        
            System.out.print(msg);
            input = br.readLine();
            if(input == null){
                input = "";
            }
        }catch (Exception e){
            LOGGER.error("Error in SdkSender::run()::", e);
        }
        return input.trim();
    }
}
