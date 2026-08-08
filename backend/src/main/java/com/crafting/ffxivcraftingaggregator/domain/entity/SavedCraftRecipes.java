package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
/**
 * One line of a crafting list: a recipe and how many times it is to be made.
 *
 * <p>A join entity rather than a plain many-to-many, because the quantity has to live somewhere.
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Entity
@Table(name = "saved_craft_recipes", uniqueConstraints =
        {@UniqueConstraint(columnNames = {"recipe_id", "saved_craft_id"})})
public class SavedCraftRecipes {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_craft_id")
    private SavedCraft savedCraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
}
