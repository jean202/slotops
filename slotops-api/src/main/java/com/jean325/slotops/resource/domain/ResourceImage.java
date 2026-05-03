package com.jean325.slotops.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource_images")
public class ResourceImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "resource_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_resource_image_resource")
    )
    private Resource resource;

    @Column(nullable = false)
    private String imageUrl;

    private Integer sortOrder;

    protected ResourceImage() {}

    public ResourceImage(Resource resource, String imageUrl, Integer sortOrder) {
        this.resource = resource;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public Long getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
