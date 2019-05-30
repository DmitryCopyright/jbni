package com.tuneit.jackalope.integration.babelnet.config;

import com.tuneit.jackalope.api.Jackalope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JackalopeConfiguration {

    @Bean
    public Jackalope getJackalope() {
        Jackalope jack = Jackalope.getInstance();
        // ToDo open jackalope dump
        return jack;
    }

}
