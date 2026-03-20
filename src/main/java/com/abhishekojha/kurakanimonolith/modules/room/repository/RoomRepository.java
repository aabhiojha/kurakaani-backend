package com.abhishekojha.kurakanimonolith.modules.room.repository;

import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.ResponseBody;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
}
