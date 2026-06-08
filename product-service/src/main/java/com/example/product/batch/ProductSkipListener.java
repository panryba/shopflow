package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import com.example.product.dto.SkippedRecord;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class ProductSkipListener implements SkipListener<ProductCsv, Product> {

    private final List<SkippedRecord> skippedRecords = new ArrayList<>();

    @Override
    public void onSkipInRead(@NonNull Throwable t) {
        if (t instanceof FlatFileParseException e) {
            skippedRecords.add(new SkippedRecord(
                    "Line " + e.getLineNumber(),
                    "Malformed CSV: " + e.getInput()
            ));
        }
    }

    @Override
    public void onSkipInProcess(@NonNull ProductCsv item, @NonNull Throwable t) {
        skippedRecords.add(new SkippedRecord(
                item.getArtist() + " – " + item.getTitle(),
                t.getMessage()
        ));
    }

    @Override
    public void onSkipInWrite(@NonNull Product item, @NonNull Throwable t) {
        skippedRecords.add(new SkippedRecord(
                item.getArtist() + " – " + item.getTitle(),
                t.getMessage()
        ));
    }

    public void reset() {
        skippedRecords.clear();
    }
}