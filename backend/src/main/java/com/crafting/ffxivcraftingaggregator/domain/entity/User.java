package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "email",nullable = false,unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "username",nullable = false, unique = true)
    private String username;

    @Column(name = "created_at",updatable = false,nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "default_data_center",nullable = false)
    private String defaultDataCenter;

    @Column(name = "default_world", nullable = false)
    private String defaultWorld;

    @Builder.Default
    @OneToMany(mappedBy = "user",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavedCraft> savedCrafts = new ArrayList<>();

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false)
    @Builder.Default
    private Role role = Role.USER;
}
