package com.manulife.asb.demo.stream.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.util.function.Supplier;

@Configuration
public class ServiceProducerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProducerConfiguration.class);

    @Bean
    public Sinks.Many<Message<String>> many() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<String>>> supply(Sinks.Many<Message<String>> many) {
        return () -> many.asFlux()
                .doOnNext(m -> LOGGER.info("Manually sending message {}", m))
                .doOnError(t -> LOGGER.error("Error encountered", t));
    }
//    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProducerConfiguration.class);
//
//    private int i = 0;
//
//    @Bean
//    public Supplier<Message<String>> supply() {
//        return () -> {
//            LOGGER.info("Sending message, sequence " + i);
//            return MessageBuilder.withPayload("Hello world, " + i++).build();
//        };
//    }
}