package org.camunda.bpm.getstarted;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("org.camunda.bpm.getstarted.mapper")
public class Application {
  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
  }
}
