package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @OneToMany(mappedBy = "recipe")
    private List<RecipeMaterials> recipeIngredients = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "recipe")
    private List<SavedCraftRecipes> savedCraftRecipes = new ArrayList<>();
}
