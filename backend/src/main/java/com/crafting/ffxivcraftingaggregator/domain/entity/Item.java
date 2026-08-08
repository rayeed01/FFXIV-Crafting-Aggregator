package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A game item.
 *
 * <p>Carries both this application's UUID and the game's {@code xivapiId}, which is unique and is
 * what Universalis and the sync speak in.
 *
 * <p>{@code canBeCrafted} is denormalised - it duplicates "some recipe produces this item" - and is
 * set in bulk at the end of a sync rather than maintained per row. It exists so a search result
 * can be flagged as craftable without joining to recipes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false,unique = true)
    private int xivapiId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String iconUrl;

    @Column(nullable = false)
    private boolean canBeCrafted;

    @Builder.Default
    @OneToMany(mappedBy = "item")
    private List<RecipeMaterials> usedInRecipe = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resultItem")
    private List<Recipe> recipes = new ArrayList<>();

}
