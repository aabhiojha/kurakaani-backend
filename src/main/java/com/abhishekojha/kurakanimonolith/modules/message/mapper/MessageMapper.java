package com.abhishekojha.kurakanimonolith.modules.message.mapper;

import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageDto;
import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {

    @Mapping(source = "sender", target = "senderId")
    @Mapping(source = "room", target = "roomId")
    MessageDto toDto(Message message);

    List<MessageDto> toDtoList(List<Message> messages);

    default Long mapToSenderId(User sender){
        return sender.getId();
    }

    default Long mapToRoomId(Room room){
        return room.getId();
    }
}
