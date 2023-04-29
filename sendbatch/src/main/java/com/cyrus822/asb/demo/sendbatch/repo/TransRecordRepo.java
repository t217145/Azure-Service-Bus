package com.cyrus822.asb.demo.sendbatch.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cyrus822.asb.demo.sendbatch.domains.TransRecord;

public interface TransRecordRepo extends JpaRepository<TransRecord, Integer> {
    
}
