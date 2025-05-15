package com.tuneit.jackalope.integration.babelnet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        restTemplate.getMessageConverters().clear();

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();

        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
        jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);

        restTemplate.getMessageConverters().add(jacksonConverter);

        restTemplate.setInterceptors(Collections.singletonList(new LoggingRequestInterceptor()));

        return restTemplate;
    }

    public static class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request, byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws IOException {

            logRequest(request, body);
            org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
            logResponse(response);
            return response;
        }

        private void logRequest(org.springframework.http.HttpRequest request, byte[] body) {
            System.out.println("REQUEST METHOD: " + request.getMethod());
            System.out.println("REQUEST URI: " + request.getURI());
            System.out.println("REQUEST HEADERS: " + request.getHeaders());
            System.out.println("REQUEST BODY: " + new String(body, StandardCharsets.UTF_8));
        }

        private void logResponse(org.springframework.http.client.ClientHttpResponse response) throws IOException {
            String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            System.out.println("RESPONSE STATUS: " + response.getStatusCode());
            System.out.println("RESPONSE HEADERS: " + response.getHeaders());
            System.out.println("RESPONSE BODY: " + responseBody);
        }
    }
}