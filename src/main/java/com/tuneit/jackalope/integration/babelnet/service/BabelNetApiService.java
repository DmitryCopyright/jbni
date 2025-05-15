package com.tuneit.jackalope.integration.babelnet.service;

import com.tuneit.jackalope.integration.babelnet.integration.SemnetClient;
import com.tuneit.jackalope.integration.babelnet.DTO.BabelSynsetDto;
import it.uniroma1.lcl.babelnet.*;
import it.uniroma1.lcl.babelnet.data.BabelGloss;
import it.uniroma1.lcl.babelnet.data.BabelImage;
import it.uniroma1.lcl.jlt.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BabelNetApiService {
    private static final Logger log = LoggerFactory.getLogger(SemnetClient.class);

    @Autowired
    private BabelNet babelNet;

    @Autowired
    private BabelNetApiService babelNetApiService;

    public List<BabelSynset> fetchSynsets(String lemma, Language lang) throws IOException {
        return babelNet.getSynsets(lemma, lang);
    }

    public BabelSynsetDto fetchExtendedSynsetData(BabelSynset synset, Language lang) {
        BabelSynsetDto dto = new BabelSynsetDto();
        dto.setSynsetId(synset.getId().toString());
        dto.setActualLanguage(lang != null ? lang.name() : null);
        dto.setEdgesCount(synset.getEdges() != null ? synset.getEdges().size() : 0);

        try {
            String humanMainSense = babelNetApiService.getMainSenseOrFallback(synset, lang);

            if (humanMainSense == null || humanMainSense.isEmpty() || "N/A".equals(humanMainSense)) {
                log.warn("Пропущен синсет без корректной леммы: {}", synset.getId());
                return null;
            }

            dto.setMainSense(humanMainSense);
        } catch (Exception e) {
            log.error("Ошибка получения MainSense для синсета: {} - {}", synset.getId(), e.getMessage(), e);
            return null;
        }

        return dto;
    }

    public BabelSynset fetchSynsetById(String id) throws IOException {
        if (!id.startsWith("bn:")) {
            id = "bn:" + id;
        }

        try {
            BabelSynsetID synsetID = new BabelSynsetID(id);
            return babelNet.getSynset(synsetID);
        } catch (InvalidSynsetIDException e) {
            LoggerFactory.getLogger(getClass()).error("Некорректный SynsetID: {}", id, e);
            throw new IOException("Invalid SynsetID: " + id, e);
        }
    }

    public String getMainSenseOrFallback(BabelSynset synset, Language language) {
        try {
            Optional<BabelSense> mainSenseOpt = synset.getMainSense(language);
            if (mainSenseOpt.isPresent()) {
                BabelSense mainSense = mainSenseOpt.get();
                if (mainSense != null && mainSense.getLemma() != null && !mainSense.getFullLemma().isEmpty()) {
                    return mainSense.getFullLemma();
                }
            }

            List<BabelSense> senses = synset.getSenses(language);
            if (senses != null && !senses.isEmpty()) {
                for (BabelSense sense : senses) {
                    if (sense.getFullLemma() != null && !sense.getFullLemma().isEmpty()) {
                        return sense.getFullLemma();
                    }
                }
            }

            Optional<BabelGloss> glossOpt = synset.getMainGloss();
            if (glossOpt.isPresent()) {
                return glossOpt.get().getGloss();
            }

            log.warn("MainSense и глосс не найдены для синсета: {}", synset.getId());
            return "";
        } catch (Exception e) {
            log.error("Ошибка при получении MainSense: {}", e.getMessage());
            return "";
        }
    }

}