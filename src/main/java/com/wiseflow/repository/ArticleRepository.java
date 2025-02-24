package com.wiseflow.repository;

import com.wiseflow.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    @Query("SELECT a.url FROM Article a")
    List<String> findAllUrls();
    
    @Query("SELECT COUNT(a) > 0 FROM Article a WHERE a.url = :url")
    boolean existsByUrl(@Param("url") String url);
    
    @Query("SELECT a FROM Article a ORDER BY a.createdAt DESC")
    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.category.id = :categoryId ORDER BY a.createdAt DESC")
    Page<Article> findByCategoryIdOrderByCreatedAtDesc(@Param("categoryId") Long categoryId, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.createdAt < :date")
    Page<Article> findByCreatedAtBefore(@Param("date") LocalDateTime date, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM Article a WHERE a.category.id = :categoryId")
    long countByCategory(@Param("categoryId") Long categoryId);
    
    @Query("SELECT a FROM Article a WHERE a.author = :author ORDER BY a.createdAt DESC")
    Page<Article> findByAuthorOrderByCreatedAtDesc(@Param("author") String author, Pageable pageable);
    
    @Query(value = "SELECT DATE(a.created_at) as date, COUNT(a.id) as count " +
                   "FROM articles a " +
                   "WHERE a.created_at >= :startDate " +
                   "GROUP BY DATE(a.created_at) " +
                   "ORDER BY date DESC", 
           nativeQuery = true)
    List<Object[]> getArticleStatsByDate(@Param("startDate") LocalDateTime startDate);
    
    List<Article> findBySyncedFalseOrderByCreatedAtAsc();
} 