package org.fipe;

import org.fipe.config.FipeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FipeProperties.class)
public class TabelaFipeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TabelaFipeApplication.class, args);
    }
}
