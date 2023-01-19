package ch.ejpd.lgs.searchindex.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class SearchIndexClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(SearchIndexClientApplication.class);
  }
}
