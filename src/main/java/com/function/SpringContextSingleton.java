package com.function;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.function.models")
@ComponentScan(basePackages = {"com.function", "com.function.config", "com.function.models", "com.function.controllers"})
public class SpringContextSingleton {
    private static ApplicationContext context;

    public static synchronized ApplicationContext getContext() {
        if (context == null) {
            context = SpringApplication.run(SpringContextSingleton.class);
        }
        return context;
    }
}
