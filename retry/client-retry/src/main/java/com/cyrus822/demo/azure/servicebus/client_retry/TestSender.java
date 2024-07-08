package com.cyrus822.demo.azure.servicebus.client_retry;

import org.springframework.boot.CommandLineRunner;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.BinaryData;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.amqp.AmqpRetryMode;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Date;
import java.time.Duration;

@Component
public class TestSender implements CommandLineRunner{

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSender.class);

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

    @Override
    public void run(String... args) throws Exception {
        try{
            //Step-1 : prepare the TokenCredential
            LOGGER.info("[Start::TestSender::run()::Step-1]");  
            TokenCredential credential = new ClientSecretCredentialBuilder()
                                            .clientId(clientId)
                                            .clientSecret(clientSecret)
                                            .tenantId(tenantId)
                                            .build();   
            
            //Step-2 : prepare the retry option
            LOGGER.info("[Start::TestSender::run()::Step-2]");  
            AmqpRetryOptions option = new AmqpRetryOptions();
            option.setDelay(Duration.ofSeconds(20));
            option.setMaxDelay(Duration.ofSeconds(20));
            option.setMaxRetries(10);
            option.setMode(AmqpRetryMode.FIXED); //EXPONENTIAL
            option.setTryTimeout(Duration.ofSeconds(60));

                                                  
            //Step-3 : prepare the ServiceBusSenderClient
            LOGGER.info("[Start::TestSender::run()::Step-3]");  
            ServiceBusSenderClient client = new ServiceBusClientBuilder()
                                            .credential(namespace, credential)
                                            .retryOptions(option)//assign the option into builder
                                            .sender()
                                            .topicName(topic)
                                            .buildClient();

            do{
                //Step-4 : wait for user input
                LOGGER.info("[Start::TestSender::run()::Step-4]");
                String input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                if(input.isEmpty()){
                    break;
                }

                //Step-5 : prepare the ServiceBusMessage
                LOGGER.info("[Start::TestSender::run()::Step-5]");   
                ServiceBusMessage msg = new ServiceBusMessage(BinaryData.fromBytes(input.getBytes(UTF_8)));

                //Step-6 : send out the message
                LOGGER.info("[Start::TestSender::run()::Step-6]");   
                client.sendMessage(msg);   
                
                Thread.sleep(3000);                   
            }while(true);

            //Step-7 : close the client and br
            LOGGER.info("[Start::TestSender::run()::Step-7]");    
            client.close();             
        }catch (Exception e){
            LOGGER.error("Error in outer TestSender::run()::", e);
        }//end of try catch
    }
}