package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A user's named crafting list.
 *
 * <p>{@code world} is nullable: a list priced across a whole data center has no single world, and
 * the derived price scope falls back to the data center in that case.
 *
 * <p>Lines cascade from here, so adding or removing recipes is done through this entity rather
 * than against the join table directly.
 */
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

    @Column(nullable = false)
    private String title;

    @Column(name = "data_center",nullable = false)
    private String dataCenter;

    @Column(name = "world")
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

    @Transient
    public String getPriceScope(){
        return (world == null || world.isBlank()) ? dataCenter : world;
    }

    @PrePersist
    protected void onCreate(){

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void updateAt(){
        this.updatedAt = LocalDateTime.now();
    }
}
