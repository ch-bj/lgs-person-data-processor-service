package ch.ejpd.lgs.searchindex.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for the SearchIndexClientApplication class.
 */
@ActiveProfiles(profiles = "test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SearchIndexClientApplicationTests {

  @Container static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-alpine");

  @DynamicPropertySource
  static void rabbitmqProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
  }

  @Autowired private TestRestTemplate testRestTemplate;

  static {
    rabbit.start();
  }

  /**
   * Test to ensure that the application context loads successfully.
   */
  @Test
  void contextLoads() {
    assertNotNull(testRestTemplate);
  }
}
