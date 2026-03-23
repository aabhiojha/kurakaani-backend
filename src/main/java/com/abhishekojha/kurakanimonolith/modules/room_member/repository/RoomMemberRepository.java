package com.abhishekojha.kurakanimonolith.modules.room_member.repository;

import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
