package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_category_owner_parent_name", columnNames = {"owner_id", "parent_id", "name"})
        },
        indexes = {
                @Index(name = "idx_category_owner", columnList = "owner_id"),
                @Index(name = "idx_category_parent", columnList = "parent_id")
        })
/**
 * Category forms a hierarchical tree (parent-child) for classifying expense or income
 * transactions. depth is stored for faster tree queries; owner null means global/shared
 * category usable by multiple users.
 */
//TODO: Revisit when implementing subcategories
public class Category extends BaseEntity {

    /** Human readable category name (unique among siblings for same owner). */
    @Column(nullable = false, length = 128)
    private String name;

    /** Owner of this category; null denotes a global system category. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner; // null => глобальная категория

    /** Type (expense or income) restricting which transactions can reference it. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CategoryType type;

    /** Parent category (null for root). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** Direct children of this category. */
    @OneToMany(mappedBy = "parent")
    private Set<Category> children = new HashSet<>();

    /** Archive flag to hide from selection but keep historical links. */
    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    /** Cached hierarchical depth (root=0) for efficient querying / sorting. */
    @Column(name = "depth", nullable = false)
    private int depth = 0;

}
