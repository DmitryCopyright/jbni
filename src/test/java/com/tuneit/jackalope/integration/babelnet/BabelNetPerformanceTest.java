package com.tuneit.jackalope.integration.babelnet;
import com.tuneit.jackalope.integration.babelnet.DTO.EdgeDto;
import com.tuneit.jackalope.integration.babelnet.integration.SemanticIntegrationService;
import com.tuneit.jackalope.integration.babelnet.integration.SemnetClient;
import com.tuneit.jackalope.integration.babelnet.service.BabelNetApiService;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.jlt.util.Language;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
public class BabelNetPerformanceTest {

    @Autowired
    private SemanticIntegrationService semanticIntegrationService;

    @Autowired
    private BabelNetApiService babelNetApiService;

    @Autowired
    private SemnetClient semnetClient;

    private static final Logger log = LoggerFactory.getLogger(BabelNetPerformanceTest.class);

    @Test
    public void testEdgeImportPerformance() throws Exception {
        List<String> lemmas = List.of("philosophy", "human", "ending", "stop", "testing");

        int totalEdges = 0;
        long totalStart = System.currentTimeMillis();

        for (String lemma : lemmas) {
            long start = System.currentTimeMillis();

            log.info("Импорт рёбер для леммы: {}", lemma);

            int edges = handleImportEdgesFromTest(lemma);

            long duration = System.currentTimeMillis() - start;
            log.info("Лемма '{}': обработано {} рёбер за {} мс", lemma, edges, duration);

            totalEdges += edges;
        }

        long totalTime = System.currentTimeMillis() - totalStart;
        log.info("Всего обработано {} рёбер за {} мс (среднее: {} мс/ребро)",
                totalEdges, totalTime, totalEdges > 0 ? totalTime / totalEdges : 0);
    }

    public int handleImportEdgesFromTest(String lemma) throws Exception {
        List<BabelSynset> synsets = babelNetApiService.fetchSynsets(lemma, Language.EN);
        if (synsets == null || synsets.isEmpty()) return 0;

        BabelSynset synset = synsets.get(0);
        List<BabelSynsetRelation> edges = synset.getEdges();
        if (edges == null) return 0;

        int count = 0;
        for (BabelSynsetRelation relation : edges) {
            EdgeDto edgeDto = new EdgeDto();

            String sourceLemma = babelNetApiService.getMainSenseOrFallback(synset, Language.EN);
            BabelSynset targetSynset = babelNetApiService.fetchSynsetById(relation.getBabelSynsetIDTarget().toString());
            String targetLemma = semnetClient.cleanLemma(babelNetApiService.getMainSenseOrFallback(targetSynset, Language.EN));

            edgeDto.setSourceLemma(sourceLemma);
            edgeDto.setTargetLemma(targetLemma);
            edgeDto.setPointer(relation.getPointer());
            edgeDto.setWeight(relation.getWeight());

            semanticIntegrationService.integrateEdge(edgeDto);
            count++;
        }

        return count;
    }
}


