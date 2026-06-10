package com.example.product.batch;

import com.example.product.TestcontainersConfiguration;
import com.example.product.domain.Product;
import com.example.product.dto.ImportResult;
import com.example.product.repository.ProductRepository;
import com.example.product.service.ProductImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductImportJobTest {

    @Autowired ProductImportService importService;
    @Autowired ProductRepository productRepository;
    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        jobRepositoryTestUtils.removeJobExecutions();
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "products.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void happyPath_importsAllRows() {
        String csv = """
                artist,title,price,imageUrl
                Pink Floyd,The Wall,29.99,http://example.com/wall.jpg
                Led Zeppelin,IV,24.99,http://example.com/iv.jpg
                Metallica,Master Of Puppets,36.99,http://example.com/mop.jpg
                """;
        ImportResult result = importService.importCsv(csvFile(csv));
        assertThat(result.getImported()).isEqualTo(3);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(productRepository.count()).isEqualTo(3);
    }

    @Test
    void badPriceRow_skipsOneImportsTwo_withCorrectLineNumber() {
        String csv = """
                artist,title,price,imageUrl
                Pink Floyd,The Wall,29.99,
                Led Zeppelin,IV,24.99,
                Metallica,Master Of Puppets,INVALID,
                """;
        ImportResult result = importService.importCsv(csvFile(csv));
        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSkippedRecords().get(0).identifier()).isEqualTo("Line 4");
        assertThat(productRepository.count()).isEqualTo(2);
    }

    @Test
    void clearBeforeImport_replacesExistingCatalogue() {
        for (int i = 1; i <= 5; i++) {
            productRepository.save(Product.builder()
                    .id(UUID.randomUUID())
                    .artist("Artist " + i)
                    .title("Title " + i)
                    .price(BigDecimal.TEN)
                    .createdAt(Instant.now())
                    .build());
        }
        assertThat(productRepository.count()).isEqualTo(5);

        String csv = """
                artist,title,price,imageUrl
                Pink Floyd,The Wall,29.99,
                Led Zeppelin,IV,24.99,
                """;
        importService.importCsv(csvFile(csv));
        assertThat(productRepository.count()).isEqualTo(2);
    }
}