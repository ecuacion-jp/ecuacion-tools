package jp.ecuacion.util.commandapi.web.config;

import jp.ecuacion.splib.rest.config.SplibRestSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Provides app secutiry config.
 */
@Configuration
@EnableWebSecurity
public class AppRestSecurityConfig extends SplibRestSecurityConfig {

}
