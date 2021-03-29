package io.slgl.api.it;

import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.TypeNodeRequest;
import io.slgl.client.node.WriteRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;

@Slf4j
public class ConcurrencyIT extends AbstractApiTest {

    private static final int THREADS = 10;
    private static final int ITERATIONS = 5;

    private ExecutorService executor;

    @BeforeEach
    public void setup() {
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    public void cleanup() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldAllowConcurrentRequests() {
        // given
        List<Future<?>> tasks = new ArrayList<>();

        // when
        for (int thread = 0; thread < THREADS; thread++) {
            int threadNumber = thread;

            tasks.add(executor.submit(() -> {
                log.info("Thread stated: thread={}", threadNumber);

                for (int i = 0; i < ITERATIONS; i++) {
                    log.info("Thread processing: thread={}, iteration={}", threadNumber, i);

                    NodeResponse node = user.writeNode(NodeRequest.builder()
                            .id(generateUniqueId())
                            .type(TypeNodeRequest.builder()
                                    .anchor("#test")
                                    .permission(allowAllForEveryone())));

                    user.write(WriteRequest.builder()
                            .addRequest(NodeRequest.builder())
                            .addLinkRequest(0, node, "#test"));
                }

                log.info("Thread ended: thread={}", threadNumber);
            }));
        }

        // then
        int successCount = 0;
        for (Future<?> task : tasks) {
            try {
                task.get();
                successCount++;
            } catch (Exception e) {
                log.error("Task failed", e);
            }
        }

        assertThat(successCount).isEqualTo(tasks.size());
    }
}
