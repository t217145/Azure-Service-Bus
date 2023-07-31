package com.manulife.asb.demo.stream.receiver;

import com.azure.spring.messaging.checkpoint.Checkpointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import java.util.function.Consumer;
import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	@Autowired
	private ASBMessageRepo repo;

	@Bean
	public Consumer<Message<String>> consume() {
		return message -> {
			Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);
			if(checkpointer != null){
				LOGGER.info("New message received: '{}'", message.getPayload());
				checkpointer.success()
						.doOnSuccess(s -> {
							LOGGER.info("Message '{}' successfully checkpointed", message.getPayload());
							ASBMessage msg = new ASBMessage();
							msg.setMsg(message.getPayload());
							repo.save(msg);
						})
						.doOnError(e -> LOGGER.error("Error found", e))
						.block();
			}
		};
	}

}
