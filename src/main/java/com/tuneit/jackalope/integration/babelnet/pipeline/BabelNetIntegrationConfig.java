package com.tuneit.jackalope.integration.babelnet.pipeline;


import com.tuneit.jackalope.integration.babelnet.DTO.BabelRequestDto;
import com.tuneit.jackalope.integration.babelnet.DTO.BabelSynsetDto;
import com.tuneit.jackalope.integration.babelnet.exceptions.IntegrationException;
import com.tuneit.jackalope.integration.babelnet.integration.SemanticIntegrationService;
import com.tuneit.jackalope.integration.babelnet.exceptions.SemnetException;
import com.tuneit.jackalope.integration.babelnet.service.BabelNetApiService;
import it.uniroma1.lcl.babelnet.BabelSynset;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class BabelNetIntegrationConfig {

    @Autowired
    private BabelNetApiService babelNetApiService;

    @Autowired
    private SemanticIntegrationService semanticIntegrationService;

    @Bean
    public MessageChannel inputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel outputChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow babelNetFlow() {
        return IntegrationFlows.from(inputChannel())
                .handle((payload, headers) -> {
                    BabelRequestDto request = (BabelRequestDto) payload;
                    try {
                        List<BabelSynset> synsets = babelNetApiService.fetchSynsets(request.getLemma(), request.getLanguage());

                        List<BabelSynsetDto> validSynsets = synsets.stream()
                                .map(synset -> babelNetApiService.fetchExtendedSynsetData(synset, request.getLanguage()))
                                .filter(Objects::nonNull)
                                .filter(dto -> dto.getMainSense() != null && !dto.getMainSense().isEmpty())
                                .collect(Collectors.toList());

                        if (validSynsets.isEmpty()) {
                            LoggerFactory.getLogger(getClass()).warn("Все полученные синсеты для запроса '{}' пустые или некорректные.", request.getLemma());
                            return Collections.emptyList();
                        }

                        return validSynsets;
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при получении синсетов из BabelNet", e);
                    }
                })
                .aggregate()
                .handle((payload, headers) -> {
                    List<BabelSynsetDto> synsets = (List<BabelSynsetDto>) payload;
                    try {
                        if (synsets == null || synsets.isEmpty()) {
                            LoggerFactory.getLogger(getClass()).warn("Пропущена интеграция пустого списка синсетов.");
                            return null;
                        }

                        semanticIntegrationService.integrateSynsets(synsets);
                        return synsets;
                    } catch (IntegrationException e) {
                        throw new RuntimeException("Ошибка интеграции синсетов в семантическую сеть", e);
                    } catch (SemnetException e) {
                        throw new RuntimeException(e);
                    }
                })
                .channel(outputChannel())
                .get();
    }
}
