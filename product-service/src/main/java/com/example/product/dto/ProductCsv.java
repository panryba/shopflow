package com.example.product.dto;

import lombok.Data;

@Data
public class ProductCsv {
    private String artist;
    private String title;
    private String price;
    private String imageUrl;
    private int lineNumber;
}