package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ProductImportJob {

    private static final int CHUNK_SIZE = 5;
    private static final int SKIP_LIMIT = 3;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductRepository productRepository;
    private final ProductItemProcessor processor;
    private final ProductItemWriter writer;
    private final ProductSkipListener skipListener;

    @Bean
    public Job importProductsJob() {
        return new JobBuilder("importProductsJob", jobRepository)
                .start(clearStep())
                .next(importStep())
                .build();
    }

    @Bean
    public Step clearStep() {
        Tasklet deleteAll = (_, _) -> {
            productRepository.deleteAll();
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("clearStep", jobRepository)
                .tasklet(deleteAll, transactionManager)
                .build();
    }

    @Bean
    public Step importStep() {
        return new StepBuilder("importStep", jobRepository)
                .<ProductCsv, Product>chunk(CHUNK_SIZE)
                .reader(csvReader(""))
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(SKIP_LIMIT)
                .listener(skipListener)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsv> csvReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<ProductCsv>()
                .name("productCsvReader")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .names("artist", "title", "price", "imageUrl")
                .targetType(ProductCsv.class)
                .linesToSkip(1)
                .build();
    }
}