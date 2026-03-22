package com.abhishekojha.kurakanimonolith.modules.room.model;

import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.room_member.model.RoomMember;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_createdBy_roomName", columnNames = {"createdBy", "name"}),
                @UniqueConstraint(columnNames = {"name"})
        }
)
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "room")
    private List<RoomMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "room")
    private List<Message> messages = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private RoomType type;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
