package com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shopping_list_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingListItemModel {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_list_id")
    private ShoppingListModel list;

    @Column(name = "food_id", nullable = false)
    private UUID foodId;

    @Column(name = "food_portion_id")
    private UUID foodPortionId;

    @Column(nullable = false, length = 32)
    private String unit;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "item_order", nullable = false)
    private int order;
}
