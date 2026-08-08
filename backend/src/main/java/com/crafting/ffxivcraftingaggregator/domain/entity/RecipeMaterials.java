package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * One ingredient of a recipe: an item and the quantity a single craft consumes.
 *
 * <p>Quantity is per craft rather than per requested unit, so a caller wanting several must scale
 * by the number of crafts, not by the amount wanted.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "recipe_materials", uniqueConstraints =
        {@UniqueConstraint(columnNames = {"recipe_id", "item_id"})})
public class RecipeMaterials {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "quantity", nullable = false)
    private int quantity;

}
