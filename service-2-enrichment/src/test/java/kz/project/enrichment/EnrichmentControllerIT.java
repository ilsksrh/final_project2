package kz.project.enrichment;

import kz.project.enrichment.dto.EnrichmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class EnrichmentControllerIT {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ReactiveRedisTemplate<String, EnrichmentResponse> redisTemplate;

    @Test
    void shouldReturnDataFromRedisCache() {
        EnrichmentResponse cachedResponse = new EnrichmentResponse();
        cachedResponse.setActiveCategories(List.of("urgent", "credit"));
        cachedResponse.setTotalActiveAppeals(2);

        redisTemplate.opsForValue()
                .set("extended:client:123", cachedResponse)
                .block();

        webTestClient.get()
                .uri("/extended-info/123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalActiveAppeals").isEqualTo(2)
                .jsonPath("$.activeCategories[0]").isEqualTo("urgent")
                .jsonPath("$.activeCategories[1]").isEqualTo("credit");
    }
}
