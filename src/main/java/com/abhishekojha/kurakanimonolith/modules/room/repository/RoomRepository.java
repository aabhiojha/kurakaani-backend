package com.abhishekojha.kurakanimonolith.modules.room.repository;

import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RecentMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RoomListDto;
import com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage.RoomMessageDto;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByName(String name);

    @Query("""
                SELECT r FROM Room r
                Join fetch  r.members rm
                where rm.user.id = :userId
            """
    )
    List<Room> findByCreatedByAndNameIgnoreCase(@Param("userId") Long userId, String name);


    @Query("""
            Select new com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RoomListDto(
                r.id,
                r.name,
                r.description,
                r.type,
                SIZE(r.members)
               )
                 FROM Room  r
                 join r.members rm
                 where rm.user.id =:userId
           """
    )
    List<RoomListDto> findRoomsForUser(@Param("userId") Long userId);


    @Query("""
            SELECT new com.abhishekojha.kurakanimonolith.modules.room.dto.roomList.RecentMessageDto(
                m.id, m.room.id, m.content, m.messageType, m.createdAt, m.sender.id, m.sender.userName
            )
            FROM Message m
            WHERE m.room.id IN :roomIds
            AND m.createdAt = (
                SELECT MAX(m2.createdAt) FROM Message m2 WHERE m2.room.id = m.room.id
            )
            """)
    List<RecentMessageDto> getRecentMessagesForRooms(@Param("roomIds") List<Long> roomIds);



    @Query("""
    SELECT new com.abhishekojha.kurakanimonolith.modules.room.dto.roomMessage.RoomMessageDto(
        m.id, m.room.id, m.content, m.messageType, m.mediaKey, m.mediaContentType, m.mediaFileName, m.isEdited,m.isDeleted, m.createdAt, m.updatedAt,
        m.sender.id, m.sender.userName, m.sender.profileImageUrl
    )
    from Message m
    where m.room.id = :roomId
""")
    List<RoomMessageDto> getMessagesForRoom(@Param("roomId") Long roomId);
}
