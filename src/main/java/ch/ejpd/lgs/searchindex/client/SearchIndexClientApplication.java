package ch.ejpd.lgs.searchindex.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main class for the Search Index Client Application.
 * This class serves as the entry point for the application.
 */
@SpringBootApplication
@EnableTransactionManagement
public class SearchIndexClientApplication {

  /**
   * Main method to start the Search Index Client Application.
   *
   * @param args Command-line arguments passed to the application.
   */
  public static void main(String[] args) {
    SpringApplication.run(SearchIndexClientApplication.class);
  }
}
