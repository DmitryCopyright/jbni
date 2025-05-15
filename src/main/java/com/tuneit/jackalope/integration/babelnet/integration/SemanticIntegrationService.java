package com.tuneit.jackalope.integration.babelnet.integration;

import com.tuneit.jackalope.integration.babelnet.exceptions.IntegrationException;
import com.tuneit.jackalope.integration.babelnet.exceptions.SemnetException;
import com.tuneit.jackalope.integration.babelnet.DTO.BabelSynsetDto;
import com.tuneit.jackalope.integration.babelnet.DTO.EdgeDto;
import com.tuneit.jackalope.integration.babelnet.service.BabelNetApiService;
import com.tuneit.jackalope.ltf.semnetmanager.entry.*;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.jlt.util.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Service
public class SemanticIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(SemanticIntegrationService.class);

    @Autowired
    private BabelNetApiService babelNetApiService;

    @Autowired
    private SemnetClient semnetClient;

    @Autowired
    private PointerToRefTypeMapper pointerToRefTypeMapper;

    public void integrateEdge(EdgeDto edgeDto) throws IntegrationException {
        try {
            logger.info("Начало интеграции ребра: {}", edgeDto);

            String sourceLemma = edgeDto.getSourceLemma();
            String targetLemma = edgeDto.getTargetLemma();

            if (sourceLemma == null || sourceLemma.isEmpty()) {
                sourceLemma = semnetClient.cleanLemma(edgeDto.getSourceLemma());
            }
            if (targetLemma == null || targetLemma.isEmpty()) {
                targetLemma = semnetClient.cleanLemma(edgeDto.getTargetLemma());
            }

            if (sourceLemma == null || targetLemma == null) {
                logger.warn("Пропуск создания ребра с пустой леммой: {} -> {}", sourceLemma, targetLemma);
                return;
            }

            SenseEntry source = findOrCreateSense(sourceLemma);
            if (source == null || !checkSenseConnectivity(source)) {
                logger.info("Порог связности не пройден для узла source: {}", getDisplayLabel(source));
                return;
            }

            SenseEntry target = findOrCreateSense(targetLemma);
            if (target == null || !checkSenseConnectivity(target)) {
                logger.info("Порог связности не пройден для узла target: {}", getDisplayLabel(target));
                return;
            }

            ReferenceType refType = pointerToRefTypeMapper.mapPointer(edgeDto.getPointer().toString());
            ReferenceEntry existing = semnetClient.findReference(source.getId(), target.getId(), refType);

            String sourceLabel = getDisplayLabel(source);
            String targetLabel = getDisplayLabel(target);

            if (existing == null) {
                createReference(source, target, refType);
                logger.info("Создана связь {} между {} и {}", refType, sourceLabel, targetLabel);

                ReferenceType comp = pointerToRefTypeMapper.complementary(refType);
                if (comp != ReferenceType.PROPERTY) {
                    createReference(target, source, comp);
                    logger.info("Создана симметричная связь {} между {} и {}", comp, targetLabel, sourceLabel);
                }
            } else {
                logger.info("Связь уже существует между {} и {}: {}", sourceLabel, targetLabel, existing);
            }

        } catch (Exception e) {
            logger.error("Ошибка интеграции ребра {}: {}", edgeDto, e.toString(), e);
            throw new IntegrationException("Error integrating edge " + edgeDto, e);
        }
    }

    public void integrateSynset(String synsetId, String mainSense, List<String> translations) throws IntegrationException {
        try {
            logger.info("Начало интеграции синсета: {}", synsetId);

            String readableSynsetId = convertToReadable(synsetId);

            LexPoolEntry lexeme = semnetClient.findLexeme(mainSense);
            if (lexeme == null) {
                lexeme = semnetClient.createLexeme(mainSense);
                logger.info("Создана новая лексема: {}", mainSense);
                if (lexeme == null) {
                    throw new IntegrationException("Lexeme creation failed for " + mainSense, null);
                }
            }

            SenseEntry sense = semnetClient.findSense(readableSynsetId);
            if (sense == null) {
                sense = createSenseFromId(readableSynsetId);
                logger.info("Создан новый смысл для синсета: {}", readableSynsetId);
                if (sense == null) {
                    throw new IntegrationException("Sense creation failed for " + readableSynsetId, null);
                }
            }
            logger.info("Начало интеграции синсета: {}", getDisplayLabel(sense));

            if (!checkSenseConnectivity(sense)) {
                logger.info("Порог связности не пройден для синсета: {}", getDisplayLabel(sense));
                return;
            }

            for (String translation : translations) {
                String readableTranslation = convertToReadable(translation);
                SenseEntry relatedSense = findOrCreateSense(readableTranslation);
                if (!checkSenseConnectivity(relatedSense)) {
                    logger.info("Порог связности не пройден для связанного синсета: {}", relatedSense.getNamed_id());
                    continue;
                }

                long senseNo = semnetClient.sense_no(sense.getId());
                long relatedSenseNo = semnetClient.sense_no(relatedSense.getId());
                String path = semnetClient.findPath(String.valueOf(senseNo), String.valueOf(relatedSenseNo));
                if (path == null || path.isEmpty()) {
                    logger.warn("Путь не найден между {} и {}", sense.getNamed_id(), relatedSense.getNamed_id());
                    throw new IntegrationException("Path not found between " + sense.getNamed_id() + " and " + relatedSense.getNamed_id(), null);
                }
                logger.info("Найден путь между {} и {}: {}", sense.getNamed_id(), relatedSense.getNamed_id(), path);
            }
        } catch (Exception e) {
            logger.error("Ошибка интеграции синсета {}: {}", synsetId, e.toString(), e);
            throw new IntegrationException("Error integrating synset " + synsetId, e);
        }
    }

    public void integrateSynsets(List<BabelSynsetDto> dtos) throws IntegrationException, SemnetException {
        for (BabelSynsetDto dto : dtos) {
            try {
                String readableSynsetId = convertToReadable(dto.getSynsetId());
                String mainSense = babelNetApiService.getMainSenseOrFallback(
                        babelNetApiService.fetchSynsetById(dto.getSynsetId()), Language.EN);
                integrateSynset(readableSynsetId, mainSense, dto.getTranslations());
            } catch (Exception e) {
                logger.error("Ошибка интеграции синсета {}: {}", dto.getSynsetId(), e.getMessage());
            }
        }
    }

    SenseEntry findOrCreateSense(String namedId) throws IntegrationException {
        try {
            if (namedId == null || namedId.isEmpty()) {
                logger.warn("Недопустимый namedId для создания/поиска Sense: '{}'", namedId);
                return null;
            }

            String readableId = convertToReadable(namedId);
            logger.debug("Поиск Sens: {}", readableId);

            SenseEntry sense = semnetClient.findSense(readableId);
            if (sense != null && isSenseValid(sense)) {
                logger.info("Найден существующий Sense: '{}'", readableId);
                return sense;
            }

            logger.info("Sense не найден, создаём новый для '{}'", readableId);
            SenseEntry newSense = createSenseFromId(readableId);
            if (newSense == null || !isSenseValid(newSense)) {
                logger.error("Ошибка создания Sense: {}", readableId);
                return null;
            }

            logger.info("Новый Sense успешно создан: {}", newSense.getNamed_id());
            return newSense;

        } catch (Exception e) {
            logger.error("Ошибка при создании Sense для '{}': {}", namedId, e.getMessage());
            throw new IntegrationException("Error in findOrCreateSense for " + namedId, e);
        }
    }

    boolean isSenseValid(SenseEntry sense) {
        return sense != null && sense.getNamed_id() != null && !sense.getNamed_id().isEmpty();
    }

    private SenseEntry createSenseFromId(String senseId) throws IntegrationException {
        try {
            if (senseId == null || senseId.isEmpty()) {
                logger.warn("Недопустимый senseId для создания: '{}'", senseId);
                return null;
            }

            String readableId = convertToReadable(senseId);
            SenseEntry sense = new SenseEntry();
            sense.setNamed_id(readableId);

            LexPoolEntry lexeme = semnetClient.findLexeme(readableId);
            if (lexeme == null) {
                lexeme = semnetClient.createLexeme(readableId);
                if (lexeme == null) {
                    throw new IntegrationException("Ошибка создания лексемы для " + readableId, null);
                }
            }

            WordformEntry canonical = new WordformEntry();
            canonical.setId(lexeme.getId());
            canonical.setLexeme(lexeme.getValue());
            sense.setCanonical(canonical);

            return semnetClient.createSense(sense);
        } catch (Exception e) {
            throw new IntegrationException("Ошибка работы createSenseFromId для " + senseId, e);
        }
    }


    private String convertToReadable(String namedId) throws SemnetException {
        if (namedId.startsWith("bn:")) {
            namedId = namedId.substring(3);
        }
        if (namedId.matches("^[0-9a-zA-Z]+$")) {
            LexPoolEntry lexeme = semnetClient.findLexeme(namedId);
            if (lexeme != null) {
                return lexeme.getValue();
            }
        }
        return namedId;
    }

    private boolean checkSenseConnectivity(SenseEntry sense) throws IntegrationException {
        try {
            int total = 0;
            for (ReferenceType type : ReferenceType.values()) {
                Object refsObj = semnetClient.findReferenceSource(sense.getId(), type);
                if (refsObj instanceof List) {
                    total += ((List<?>) refsObj).size();
                }
            }
            logger.info("Узел {} имеет {} связей", sense.getNamed_id(), total);
            return total <= 15;
        } catch (Exception e) {
            throw new IntegrationException("Ошибка проверки sense connectivity для " + sense.getNamed_id(), e);
        }
    }

    private void createReference(SenseEntry source, SenseEntry target, ReferenceType refType) throws IntegrationException {
        try {
            if (refType == ReferenceType.PROPERTY) {
                LoggerFactory.getLogger(getClass()).info("Скрытие Reference типа PROPERTY между {} и {}", source.getNamed_id(), target.getNamed_id());
                return;
            }

            logger.debug("Проверка наличия Reference между {} и {} с типом {}", source.getNamed_id(), target.getNamed_id(), refType);

            ReferenceEntry reference = semnetClient.findReference(source.getId(), target.getId(), refType);
            if (reference != null) {
                logger.info("Reference уже существует: {}", reference);
                return;
            }

            logger.info("Reference не найден, создаём новый между {} и {}", source.getNamed_id(), target.getNamed_id());
            reference = semnetClient.createReference(source.getNamed_id(), target.getNamed_id(), refType);
            if (reference != null) {
                logger.info("Reference успешно создан: {}", reference);
            } else {
                logger.warn("Не удалось создать Reference между {} и {}", source.getNamed_id(), target.getNamed_id());
            }

        } catch (Exception e) {
            logger.error("Ошибка при создании Reference между {} и {}", source.getNamed_id(), target.getNamed_id(), e);
            throw new IntegrationException("Ошибка создания Reference между " + source.getNamed_id() + " и " + target.getNamed_id(), e);
        }
    }


    public String getDisplayLabel(SenseEntry sense) {
        try {
            String namedId = sense.getNamed_id();
            logger.info("Получение Label для: {}", namedId);

            if (!namedId.startsWith("bn:") && !namedId.matches("\\d+")) {
                logger.info("Лемма напрямую используется как Label: {}", namedId);
                return namedId;
            }

            BabelSynset babelSynset = babelNetApiService.fetchSynsetById(namedId);
            if (babelSynset == null) {
                logger.warn("Не удалось найти BabelSynset для: {}", namedId);
                return namedId;
            }

            String readableMainSense = babelNetApiService.getMainSenseOrFallback(babelSynset, Language.EN);
            if (!readableMainSense.isEmpty()) {
                logger.info("Успешно получен Label для леммы: {}, метка: {}", namedId, readableMainSense);
                return readableMainSense;
            }

            logger.warn("MainSense отсутствует, возвращаем исходное значение: {}", namedId);
            return namedId;
        } catch (Exception e) {
            logger.warn("Ошибка при получении леммы для: {}", sense.getNamed_id(), e);
            return sense.getNamed_id();
        }
    }

    public void handleImportEdges(String param) throws Exception {
        String lemma = parseParamValue(param, "lemma");
        if (lemma == null) {
            logger.warn("Укажите лемму синсета, например: lemma=computer");
            return;
        }
        logger.info("Импорт рёбер для синсета с леммой: {}", lemma);

        List<BabelSynset> synsets = babelNetApiService.fetchSynsets(lemma, Language.EN);
        if (synsets == null || synsets.isEmpty()) {
            logger.info("Синсеты не найдены для леммы: {}", lemma);
            return;
        }

        BabelSynset synset = synsets.get(0);
        List<BabelSynsetRelation> edges = synset.getEdges();
        logger.info("Найдено рёбер: {}", edges.size());

        for (BabelSynsetRelation relation : edges) {
            EdgeDto edgeDto = new EdgeDto();

            String sourceLemma = babelNetApiService.getMainSenseOrFallback(synset, Language.EN);
            String targetSynsetId = relation.getBabelSynsetIDTarget().toString();
            BabelSynset targetSynset;
            try {
                targetSynset = babelNetApiService.fetchSynsetById(targetSynsetId);
            } catch (IOException e) {
                logger.warn("Не удалось получить целевой синсет по ID: {}. Ошибка: {}", targetSynsetId, e.getMessage());
                continue;
            }

            String targetLemma = semnetClient.cleanLemma(babelNetApiService.getMainSenseOrFallback(targetSynset, Language.EN));

            edgeDto.setSourceLemma(sourceLemma);
            edgeDto.setTargetLemma(targetLemma);
            edgeDto.setPointer(relation.getPointer());
            edgeDto.setWeight(relation.getWeight());

            logger.info("Ребро: {} -> {} с типом {}", sourceLemma, targetLemma, relation.getPointer());

            integrateEdge(edgeDto);

            logger.info("Ребро обработано: {} -> {}", sourceLemma, targetLemma);
        }
    }

    private String parseParamValue(String param, String key) {
        int eqIndex = param.indexOf('=');
        if (eqIndex < 0) return null;
        String k = param.substring(0, eqIndex).trim();
        String v = param.substring(eqIndex + 1).trim();
        return k.equalsIgnoreCase(key) ? v : null;
    }
}
