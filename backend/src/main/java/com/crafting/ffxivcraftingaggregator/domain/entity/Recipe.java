package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A crafting recipe: what it produces, how many, and what it consumes.
 *
 * <p>{@code job} stores XIVAPI's CraftType ("Smithing") rather than the job that performs it
 * ("Blacksmith"), because that is what the source data provides. {@code level} may be 0 where
 * the source has none.
 *
 * <p>{@code resultQuantity} is the yield, and is what makes cost non-linear: a recipe making 3 at
 * a time satisfies a request for 1 with a single craft and two left over.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "xivapi_id",nullable = false, unique = true)
    private int xivapiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_item_id")
    private Item resultItem;

    @Column(nullable = false)
    private int resultQuantity;

    @Column(nullable = false)
    private String job;

    @Column(nullable = false)
    private int level;

    @Builder.Default
    @OneToMany(mappedBy = "recipe",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeMaterials> recipeIngredients = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "recipe")
    private List<SavedCraftRecipes> savedCraftRecipes = new ArrayList<>();
}
