package com.abhishekojha.kurakanimonolith.modules.message.service;

import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageRequest;
import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageDto;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface MessageService {
    void sendMessageToRoom(
            Long roomId,
            MessageRequest request,
            Principal principal
    );

    MessageDto sendMediaMessageToRoom(
            Long roomId,
            MultipartFile file,
            String content,
            Principal principal
    );
}
