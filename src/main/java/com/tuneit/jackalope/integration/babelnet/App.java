package com.tuneit.jackalope.integration.babelnet;

import it.uniroma1.lcl.babelnet.BabelNet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;

@SpringBootApplication
@IntegrationComponentScan
public class App {

    public static void main(String[] args) {
        SpringApplication.run(new Class<?> []{
                App.class,
        }, args);
    }

    @Bean
    public BabelNet babelNet() {
        return BabelNet.getInstance();
    }
}
