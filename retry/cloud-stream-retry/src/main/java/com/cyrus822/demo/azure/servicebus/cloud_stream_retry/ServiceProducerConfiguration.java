package com.cyrus822.demo.azure.servicebus.cloud_stream_retry;

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
}