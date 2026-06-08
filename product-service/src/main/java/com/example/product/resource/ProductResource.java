package com.example.product.resource;

import com.example.product.domain.Product;
import com.example.product.dto.ImportResult;
import com.example.product.service.ProductImportService;
import com.example.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductResource {

    private final ProductService productService;
    private final ProductImportService importService;

    @GetMapping
    public List<Product> getAll() {
        return productService.findAll();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importProducts(@RequestParam("file") MultipartFile file) {
        return importService.importCsv(file);
    }
}