package com.abhishekojha.kurakanimonolith.modules.room.mapper;

import com.abhishekojha.kurakanimonolith.modules.message.dto.MessageDto;
import com.abhishekojha.kurakanimonolith.modules.message.mapper.MessageMapper;
import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = MessageMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {

    @Mapping(source = "createdBy", target = "createdById", qualifiedByName = "createdByToCreatedById")
    @Mapping(source = "messages", target = "messageDtos")
    RoomDto toDto(Room room);

    List<RoomDto> toDtoList(List<Room> messages);

    @Named("createdByToCreatedById")
    default Long mapToCreatedById(AppUser createdBy) {
        return createdBy.getId();
    }

}
