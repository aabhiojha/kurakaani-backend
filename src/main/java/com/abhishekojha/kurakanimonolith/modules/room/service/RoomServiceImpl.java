package com.abhishekojha.kurakanimonolith.modules.room.service;

import com.abhishekojha.kurakanimonolith.common.security.SecurityUtils;
import com.abhishekojha.kurakanimonolith.modules.room.dto.CreateRoomRequestDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.RoomDto;
import com.abhishekojha.kurakanimonolith.modules.room.mapper.RoomMapper;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room.repository.RoomRepository;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public RoomDto createRoom(@Valid CreateRoomRequestDto createRoomRequestDto) {
        // user
        AppUser requestUser = securityUtils.getRequestUser();

        if (requestUser == null) {
            return null;
        }

        log.info("The request user is retrieved: {}", requestUser.getEmail());

        // convert the createroomdto into room object
        Room room = new Room();
        room.setName(createRoomRequestDto.getName());
        room.setDescription(createRoomRequestDto.getDescription());
        room.setType(createRoomRequestDto.getType());
        room.setCreatedBy(requestUser);

        Room save = roomRepository.save(room);
        log.info("The room is created {}", save);
        return roomMapper.toDto(save);
    }
}
