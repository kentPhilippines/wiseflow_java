package com.wiseflow.core.storage;

import com.wiseflow.entity.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.net.URL;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class FileStorage implements Storage {
    private static final String STORAGE_DIR = "data/articles";
    private static final String IMAGES_DIR = "data/images";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    public FileStorage() {
        createStorageDirectories();
    }
    
    private void createStorageDirectories() {
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
            Files.createDirectories(Paths.get(IMAGES_DIR));
        } catch (IOException e) {
            log.error("Failed to create storage directories", e);
        }
    }

    @Override
    public void save(Article article) {
        String fileName = generateFileName(article);
        Path filePath = Paths.get(STORAGE_DIR, fileName);
        
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write("标题: " + article.getTitle());
            writer.newLine();
            writer.write("链接: " + article.getUrl());
            writer.newLine();
            writer.write("作者: " + article.getAuthor());
            writer.newLine();
            writer.write("时间: " + article.getCrawlTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.newLine();
            writer.write("分类: " + article.getCategoryName());
            writer.newLine();
            writer.write("摘要: " + article.getSummary());
            writer.newLine();
            
            writer.write("---正文开始---");
            writer.newLine();
            writer.write(article.getContent());
            writer.newLine();
            writer.write("---正文结束---");
            writer.newLine();
            
            if (!article.getImages().isEmpty()) {
                writer.write("图片链接:");
                writer.newLine();
                for (String img : article.getImages()) {
                    writer.write(img);
                    writer.newLine();
                    downloadImage(img, article.getTitle());
                }
            }
            
            log.info("Article saved: {}", fileName);
        } catch (IOException e) {
            log.error("Failed to save article: {}", fileName, e);
        }
    }

    private void downloadImage(String imageUrl, String articleTitle) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(imageUrl);
                String imageName = generateImageFileName(imageUrl, articleTitle);
                Path imagePath = Paths.get(IMAGES_DIR, imageName);
                
                try (InputStream in = url.openStream()) {
                    Files.copy(in, imagePath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Image downloaded: {}", imageName);
                }
            } catch (Exception e) {
                log.error("Failed to download image: {}", imageUrl, e);
            }
        });
    }

    private String generateImageFileName(String imageUrl, String articleTitle) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String extension = getFileExtension(imageUrl);
        String safeTitle = articleTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeTitle.length() > 30) {
            safeTitle = safeTitle.substring(0, 30);
        }
        return timestamp + "_" + safeTitle + extension;
    }

    private String getFileExtension(String url) {
        String extension = url.substring(url.lastIndexOf('.'));
        return extension.length() > 4 ? ".jpg" : extension;
    }

    @Override
    public Article load(String id) {
        Path filePath = Paths.get(STORAGE_DIR, id + ".txt");
        if (!Files.exists(filePath)) {
            return null;
        }
        
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            return parseArticle(lines);
        } catch (IOException e) {
            log.error("Failed to load article: {}", id, e);
            return null;
        }
    }

    @Override
    public List<Article> loadAll() {
        List<Article> articles = new ArrayList<>();
        try {
            Files.list(Paths.get(STORAGE_DIR))
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(path -> {
                    try {
                        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        Article article = parseArticle(lines);
                        if (article != null) {
                            articles.add(article);
                        }
                    } catch (IOException e) {
                        log.error("Failed to load article: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to list articles", e);
        }
        return articles;
    }
    
    private String generateFileName(Article article) {
        String timestamp = article.getCrawlTime().format(DATE_FORMATTER);
        String title = article.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (title.length() > 50) {
            title = title.substring(0, 50);
        }
        return timestamp + "_" + title + ".txt";
    }
    
    private Article parseArticle(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        
        try {
            Article article = new Article();
            StringBuilder content = new StringBuilder();
            List<String> images = new ArrayList<>();
            boolean isContent = false;
            
            for (String line : lines) {
                if (line.startsWith("标题: ")) {
                    article.setTitle(line.substring(4).trim());
                } else if (line.startsWith("链接: ")) {
                    article.setUrl(line.substring(4).trim());
                } else if (line.startsWith("作者: ")) {
                    article.setAuthor(line.substring(4).trim());
                } else if (line.startsWith("时间: ")) {
                    String timeStr = line.substring(4).trim();
                    article.setCrawlTime(LocalDateTime.parse(timeStr,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else if (line.startsWith("摘要: ")) {
                    article.setSummary(line.substring(4).trim());
                } else if (line.equals("---正文开始---")) {
                    isContent = true;
                } else if (line.equals("---正文结束---")) {
                    isContent = false;
                } else if (line.equals("图片链接:")) {
                    // 下一行开始是图片链接
                } else if (isContent) {
                    content.append(line).append("\n");
                } else if (line.startsWith("http")) {
                    article.addImage(line.trim());
                }
            }
            
            article.setContent(content.toString().trim());
            
            return article;
        } catch (Exception e) {
            log.error("Failed to parse article", e);
            return null;
        }
    }

    @Override
    public void delete(Article article) {
        // 删除文章文件
        String fileName = generateFileName(article);
        Path filePath = Paths.get(STORAGE_DIR, fileName);
        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted article file: {}", fileName);
        } catch (IOException e) {
            log.error("Failed to delete article file: {}", fileName, e);
        }

        // 删除相关图片
        if (article.getImages() != null) {
            for (String imageUrl : article.getImages()) {
                try {
                    String imageName = generateImageFileName(imageUrl, article.getTitle());
                    Path imagePath = Paths.get(IMAGES_DIR, imageName);
                    Files.deleteIfExists(imagePath);
                    log.info("Deleted article image: {}", imageName);
                } catch (Exception e) {
                    log.error("Failed to delete article image: {}", imageUrl, e);
                }
            }
        }
    }
} 