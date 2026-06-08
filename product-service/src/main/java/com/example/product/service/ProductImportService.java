package com.example.product.service;

import com.example.product.batch.ProductSkipListener;
import com.example.product.dto.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private final JobOperator jobOperator;
    private final Job importProductsJob;
    private final ProductSkipListener skipListener;

    public ImportResult importCsv(MultipartFile file) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("product-import-", ".csv");
            file.transferTo(tempFile);

            skipListener.reset();

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempFile.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobOperator.start(importProductsJob, params);

            long written = execution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();

            return ImportResult.builder()
                    .imported((int) written)
                    .skipped(skipListener.getSkippedRecords().size())
                    .skippedRecords(skipListener.getSkippedRecords())
                    .build();

        } catch (Exception e) {
            log.error("Product import failed", e);
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
        }
    }
}
