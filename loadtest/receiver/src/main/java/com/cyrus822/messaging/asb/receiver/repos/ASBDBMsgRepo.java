package com.cyrus822.messaging.asb.receiver.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import com.cyrus822.messaging.asb.receiver.domains.ASBDBMsg;

public interface ASBDBMsgRepo extends JpaRepository<ASBDBMsg, Integer> {

}
