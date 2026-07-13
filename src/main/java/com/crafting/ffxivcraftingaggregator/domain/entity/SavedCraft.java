package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Entity
@Table(name = "crafts")
public class Craft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "item_name",nullable = false)
    private String name;

    @Column(name = "item_id",nullable = false)
    private Integer itemId;

    @Column(name = "data_center",nullable = false)
    private String dataCenter;

    @Column(name = "world", nullable = false)
    private String world;

    @Column(name = "created",updatable = false,nullable = false)
    private Instant created;

    @Column(name = "updated", nullable = false)
    private Instant updated;

}
