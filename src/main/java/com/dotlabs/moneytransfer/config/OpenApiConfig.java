package com.dotlabs.moneytransfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI moneyTransferOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DotLabs Money Transfer Simulation API")
                        .description("RESTful microservice to simulate money transfers between bank accounts with automated fee calculation, scheduled commission evaluation, and daily transaction summaries.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("DotLabs Candidate")
                                .email("candidate@dotlabs.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
