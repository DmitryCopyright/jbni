package com.tuneit.jackalope.integration.babelnet;

import com.tuneit.jackalope.integration.babelnet.config.BabelNetConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(new Class<?> []{
                App.class,
                BabelNetConfiguration.class
        }, args);
    }
}
