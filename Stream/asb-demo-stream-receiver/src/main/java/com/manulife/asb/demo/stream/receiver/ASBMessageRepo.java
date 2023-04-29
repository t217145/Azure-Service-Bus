package com.manulife.asb.demo.stream.receiver;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ASBMessageRepo extends JpaRepository<ASBMessage, Integer> {
}
