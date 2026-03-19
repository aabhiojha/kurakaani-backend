package com.abhishekojha.kurakanimonolith.modules.user;

import com.abhishekojha.kurakanimonolith.modules.message.model.Message;
import com.abhishekojha.kurakanimonolith.modules.room.model.Room;
import com.abhishekojha.kurakanimonolith.modules.room_members.model.RoomMember;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    // last seen at
    private LocalDateTime last_seen_at;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "created_by")
    private List<Room> rooms;

    @OneToMany(mappedBy = "sender")
    private List<Message> messages;

    @OneToMany(mappedBy = "user")
    private List<RoomMember> members;
}
