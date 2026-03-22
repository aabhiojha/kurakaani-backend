package com.abhishekojha.kurakanimonolith.modules.message.dto;

import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {

    private Long roomId;
    private String content;

}
