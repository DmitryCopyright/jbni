package com.tuneit.jackalope.integration.babelnet.pipeline;

import com.tuneit.jackalope.integration.babelnet.DTO.*;
import com.tuneit.jackalope.integration.babelnet.integration.*;
import com.tuneit.jackalope.integration.babelnet.service.BabelNetApiService;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.jlt.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;


import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
public class EdgesFlowConfig {
    private static final Logger log = LoggerFactory.getLogger(SemnetClient.class);

    @Autowired
    private BabelNetApiService babelNetApiService;

    @Autowired
    private BabelSynsetFilter babelSynsetFilter;

    @Autowired
    private BabelEdgeFilter edgeFilter;

    @Autowired
    private SemnetClient semnetClient;

    @Bean
    public MessageChannel edgesInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel validSynsetsChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel outputEdgesChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow validSynsetsFlow() {
        FilterConfigDto filterConfigDTO = new FilterConfigDto();
        filterConfigDTO.setRequiredLanguage(null);
        filterConfigDTO.setMinEdges(1);
        filterConfigDTO.setRequiredDomain(null);
        filterConfigDTO.setMinGlossWords(3);

        return IntegrationFlows
                .from(edgesInputChannel())
                .handle((payload, headers) -> {
                    BabelRequestDto req = (BabelRequestDto) payload;
                    try {
                        return babelNetApiService.fetchSynsets(req.getLemma(), req.getLanguage());
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка получения синсетов: " + req, e);
                    }
                })
                .split(new DefaultMessageSplitter())
                .transform(syn -> babelNetApiService.fetchExtendedSynsetData((BabelSynset) syn, null))
                .filter(Objects::nonNull)
                .filter(BabelSynsetDto.class, dto -> babelSynsetFilter.check(dto, filterConfigDTO))
                .aggregate()
                .channel(validSynsetsChannel())
                .get();
    }

    @Bean
    public IntegrationFlow edgesFlow() {
        EdgeFilterConfigDto edgeConfig = new EdgeFilterConfigDto();
        edgeConfig.setMinWeight(0.5);
        edgeConfig.setRequiredPointer(null);

        return IntegrationFlows
                .from(validSynsetsChannel())
                .split()
                .transform(dto -> {
                    BabelSynsetDto synDto = (BabelSynsetDto) dto;
                    try {
                        BabelSynset synset = babelNetApiService.fetchSynsetById(synDto.getSynsetId());
                        if (synset == null) {
                            LoggerFactory.getLogger(getClass()).warn("Пропущен синсет без леммы: {}", synDto.getSynsetId());
                            return null;
                        }

                        String mainSense = babelNetApiService.getMainSenseOrFallback(synset, Language.EN);
                        if (mainSense == null || mainSense.isEmpty() || "N/A".equals(mainSense)) {
                            LoggerFactory.getLogger(getClass()).warn("Пропущен синсет без MainSense: {}", synDto.getSynsetId());
                            return null;
                        }

                        return synset;
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка получения синсета по ID: " + synDto.getSynsetId(), e);
                    }
                })
                .filter(Objects::nonNull)
                .split(new AbstractMessageSplitter() {
                    @Override
                    protected Object splitMessage(Message<?> message) {
                        BabelSynset syn = (BabelSynset) message.getPayload();
                        return syn.getEdges().stream()
                                .map(edge -> new SynsetEdgeHolderDto(syn, edge))
                                .collect(Collectors.toList());
                    }
                })
                .filter(SynsetEdgeHolderDto.class, holder -> edgeFilter.checkEdge(holder.getEdge(), edgeConfig))
                .transform(holder -> {
                    SynsetEdgeHolderDto h = (SynsetEdgeHolderDto) holder;
                    BabelSynsetRelation relation = h.getEdge();
                    BabelSynset syn = h.getSyn();

                    String sourceLemma = semnetClient.cleanLemma(babelNetApiService.getMainSenseOrFallback(syn, Language.EN));
                    String targetLemma = null;

                    try {
                        BabelSynset targetSynset = babelNetApiService.fetchSynsetById(relation.getBabelSynsetIDTarget().toString());
                        targetLemma = semnetClient.cleanLemma(babelNetApiService.getMainSenseOrFallback(targetSynset, Language.EN));

                        if (sourceLemma == null || sourceLemma.isEmpty() ||
                                targetLemma == null || targetLemma.isEmpty()) {
                            LoggerFactory.getLogger(getClass()).warn("Пропуск создания связи с пустой или невалидной леммой: {} -> {}", sourceLemma, targetLemma);
                            return null;
                        }

                        EdgeDto edgeDto = new EdgeDto();
                        edgeDto.setSourceLemma(sourceLemma);
                        edgeDto.setTargetLemma(targetLemma);
                        edgeDto.setPointer(relation.getPointer());
                        edgeDto.setWeight(relation.getWeight());

                        return edgeDto;

                    } catch (IOException e) {
                        log.warn("Ошибка получения целевого синсета по ID: {}. Ошибка: {}", relation.getBabelSynsetIDTarget(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .get();
    }

}