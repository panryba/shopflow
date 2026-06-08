package com.example.product.productservice;

import com.example.product.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}