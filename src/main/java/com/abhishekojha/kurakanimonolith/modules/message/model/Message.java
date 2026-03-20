package com.abhishekojha.kurakanimonolith.modules.message.model;

import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.user.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
@DynamicInsert
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "room_id")
    private Room room;

    private String content;

    @Column(name = "is_edited",columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isEdited;

    @Column(name = "is_deleted",columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

    @JoinColumn(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JoinColumn(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
