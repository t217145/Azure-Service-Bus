package com.cyrus822.demo.azure.servicebus.cloud_stream_retry;

import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class TestSender implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSender.class);

    @Autowired
    private Sinks.Many<Message<String>> many;

    @Override
    public void run(String... args) throws Exception {
        String message = "";
        LOGGER.info("Going to add message {} to Sinks.Many.", message);
        many.emitNext(MessageBuilder.withPayload(message).build(), Sinks.EmitFailureHandler.FAIL_FAST);
    }//end of run()
    
}//end of class TestSender