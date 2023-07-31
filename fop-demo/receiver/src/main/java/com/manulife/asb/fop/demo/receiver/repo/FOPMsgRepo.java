package com.manulife.asb.fop.demo.receiver.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.manulife.asb.fop.demo.receiver.domains.FOPMsg;

public interface FOPMsgRepo extends JpaRepository<FOPMsg, Integer>{
    
}
