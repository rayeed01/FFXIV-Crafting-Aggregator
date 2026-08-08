package com.crafting.ffxivcraftingaggregator.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

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
