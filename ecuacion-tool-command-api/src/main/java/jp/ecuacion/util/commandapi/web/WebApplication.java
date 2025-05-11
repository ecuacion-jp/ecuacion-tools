package jp.ecuacion.util.commandapi.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Provides SpringApplication function.
 */
@SpringBootApplication
public class WebApplication extends SpringBootServletInitializer {
  
  /**
   * Provides main method.
   * 
   * @param args args
   */
  public static void main(String[] args) {
    SpringApplication.run(WebApplication.class, args);
  }

  /** 既存tomcatにwarとして配置するために必要. */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(WebApplication.class);
  }
}
