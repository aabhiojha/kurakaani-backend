package com.abhishekojha.kurakanimonolith.modules.message.repository;

import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

}
