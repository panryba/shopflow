package com.example.product.batch;

import com.example.product.dto.ProductCsv;
import com.example.product.domain.Product;
import lombok.Getter;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class ProductSkipListener implements SkipListener<ProductCsv, Product> {

    private final List<String> skippedRecords = new ArrayList<>();

    @Override
    public void onSkipInRead(@NonNull Throwable t) {
        if (t instanceof FlatFileParseException e) {
            skippedRecords.add("READ: " + e.getInput());
        }
    }

    @Override
    public void onSkipInProcess(@NonNull ProductCsv item, @NonNull Throwable t) {
        skippedRecords.add("PROCESS: " + item);
    }

    @Override
    public void onSkipInWrite(@NonNull Product item, @NonNull Throwable t) {
        skippedRecords.add("WRITE: " + item.getArtist() + " - " + item.getTitle());
    }

    public void reset() {
        skippedRecords.clear();
    }
}