package com.wiseflow.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "articles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(length = 100)
    private String author;

    @Column(length = 1000)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_images")
    private List<String> images = new ArrayList<>();

    private LocalDateTime crawlTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "category_name", length = 50)
    private String categoryName;

    @Column(nullable = false)
    private boolean synced = false;

    private LocalDateTime syncTime;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updateCategoryName();
    }

    @PreUpdate
    protected void onUpdate() {
        updateCategoryName();
    }

    private void updateCategoryName() {
        this.categoryName = category != null ? category.getName() : "未分类";
    }

    public void setCategory(Category category) {
        this.category = category;
        if (category != null) {
            this.categoryName = category.getName();
        }
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "未分类";
    }

    public void addImage(String imageUrl) {
        if (images == null) {
            images = new ArrayList<>();
        }
        if (StringUtils.hasText(imageUrl) && !images.contains(imageUrl)) {
            images.add(imageUrl);
        }
    }

    public void setSummaryFromContent() {
        if (StringUtils.hasText(content)) {
            this.summary = content.length() > 200 ? 
                content.substring(0, 200) + "..." : content;
        }
    }
} 