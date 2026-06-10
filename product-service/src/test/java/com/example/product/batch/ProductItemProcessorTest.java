package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductItemProcessorTest {

    private final ProductItemProcessor processor = new ProductItemProcessor();

    private ProductCsv csv(String artist, String title, String price, String imageUrl) {
        ProductCsv item = new ProductCsv();
        item.setArtist(artist);
        item.setTitle(title);
        item.setPrice(price);
        item.setImageUrl(imageUrl);
        return item;
    }

    @Test
    void validRow_mapsToProductWithTrimmedFields() {
        Product result = processor.process(csv(" Pink Floyd ", " The Wall ", " 29.99 ", "http://img.example.com/cover.jpg"));
        assertThat(result.getArtist()).isEqualTo("Pink Floyd");
        assertThat(result.getTitle()).isEqualTo("The Wall");
        assertThat(result.getPrice()).isEqualByComparingTo("29.99");
        assertThat(result.getImageUrl()).isEqualTo("http://img.example.com/cover.jpg");
    }

    @Test
    void blankArtist_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("   ", "The Wall", "29.99", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist");
    }

    @Test
    void blankTitle_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("Pink Floyd", "   ", "29.99", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void invalidPrice_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("Pink Floyd", "The Wall", "not-a-number", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }

    @Test
    void nullImageUrl_preservedAsNull() {
        Product result = processor.process(csv("Pink Floyd", "The Wall", "29.99", null));
        assertThat(result.getImageUrl()).isNull();
    }

    @Test
    void priceWithWhitespace_parsesCorrectly() {
        Product result = processor.process(csv("Pink Floyd", "The Wall", "  29.99  ", null));
        assertThat(result.getPrice()).isEqualByComparingTo("29.99");
    }
}