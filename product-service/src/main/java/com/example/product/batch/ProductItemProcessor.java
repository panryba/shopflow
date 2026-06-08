package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class ProductItemProcessor implements ItemProcessor<ProductCsv, Product> {

    @Override
    public Product process(ProductCsv item) {
        if (item.getArtist() == null || item.getArtist().isBlank())
            throw new IllegalArgumentException("Artist is required");
        if (item.getTitle() == null || item.getTitle().isBlank())
            throw new IllegalArgumentException("Title is required");

        if (item.getPrice() == null || item.getPrice().isBlank())
            throw new IllegalArgumentException("Price is required");
        BigDecimal price;
        try {
            price = new BigDecimal(item.getPrice().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price: \"" + item.getPrice().trim() + "\"");
        }

        return Product.builder()
                .id(UUID.randomUUID())
                .artist(item.getArtist().trim())
                .title(item.getTitle().trim())
                .price(price)
                .imageUrl(item.getImageUrl() != null ? item.getImageUrl().trim() : null)
                .createdAt(Instant.now())
                .build();
    }
}