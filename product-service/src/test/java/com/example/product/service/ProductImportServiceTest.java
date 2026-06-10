package com.example.product.service;

import com.example.product.batch.ProductSkipListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceTest {

    @Mock JobOperator jobOperator;
    @Mock Job importProductsJob;
    @Mock ProductSkipListener skipListener;

    ProductImportService service;

    @BeforeEach
    void setUp() {
        service = new ProductImportService(jobOperator, importProductsJob, skipListener, new SimpleMeterRegistry());
    }

    @Test
    void wrongExtension_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain",
                "artist,title,price,imageUrl\n".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.importCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only .csv");
    }

    @Test
    void wrongHeader_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv",
                "wrong,header,format,here\n".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.importCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CSV header");
    }

    @Test
    void correctFile_doesNotThrow() throws Exception {
        String content = "artist,title,price,imageUrl\nPink Floyd,The Wall,29.99,\n";
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(execution.getStepExecutions()).thenReturn(List.of());
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(execution);
        when(skipListener.getSkippedRecords()).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.importCsv(file));
    }
}