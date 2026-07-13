package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Entity
@Table(name = "saved_crafts")
public class SavedCraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "data_center",nullable = false)
    private String dataCenter;

    @Column(name = "world", nullable = false)
    private String world;

    @Column(name = "created",updatable = false,nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "savedCraft",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SavedCraftRecipes> savedCraftRecipes = new ArrayList<>();

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void updateAt(){
        this.updatedAt = LocalDateTime.now();
    }
}
