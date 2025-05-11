package jp.ecuacion.util.commandapi.web.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Provides app config.
 */
@Configuration
@ComponentScan(basePackages = "jp.ecuacion.splib.core.config"
    + ",jp.ecuacion.splib.rest.config"
    + ",jp.ecuacion.splib.jpa.config")
public class AppConfig {
  
}
