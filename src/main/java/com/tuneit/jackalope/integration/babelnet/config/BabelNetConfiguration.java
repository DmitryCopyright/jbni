package com.tuneit.jackalope.integration.babelnet.config;

import it.uniroma1.lcl.babelnet.BabelNet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BabelNetConfiguration {

    @Bean
    public BabelNet babelnetInstance() {
        return BabelNet.getInstance();
    }

}
