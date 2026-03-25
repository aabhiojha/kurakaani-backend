package com.abhishekojha.kurakanimonolith.modules.friendRequest.mapper;

import com.abhishekojha.kurakanimonolith.modules.friendRequest.dto.FriendShipDto;
import com.abhishekojha.kurakanimonolith.modules.friendRequest.model.Friendship;
import com.abhishekojha.kurakanimonolith.modules.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FriendShipMapper {

    @Mapping(source = "requester", target = "requesterId", qualifiedByName = "toRequesterId")
    @Mapping(source = "recipient", target = "recipientId", qualifiedByName = "toRecipientId")
    FriendShipDto toDto(Friendship friendship);

    List<FriendShipDto> toListDto(List<Friendship> friendship);

    @Named("toRequesterId")
    default Long mapToRequesterId(User requester) {
        return requester.getId();
    }

    @Named("toRecipientId")
    default Long mapToRecipientId(User recipient) {
        return recipient.getId();
    }
}
