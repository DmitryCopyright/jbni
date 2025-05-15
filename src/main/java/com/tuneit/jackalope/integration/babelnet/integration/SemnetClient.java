package com.tuneit.jackalope.integration.babelnet.integration;

import com.tuneit.jackalope.integration.babelnet.DTO.CreateSenseRequestDto;
import com.tuneit.jackalope.integration.babelnet.exceptions.SemnetException;
import com.tuneit.jackalope.ltf.semnetmanager.dto.*;
import com.tuneit.jackalope.ltf.semnetmanager.entry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import java.util.*;

@Service
public class SemnetClient {
    private static final Logger log = LoggerFactory.getLogger(SemnetClient.class);
    private static final String BASE_URL = "http://127.0.0.1:4242/manager";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SemanticIntegrationService semanticIntegrationService;

    public WordformEntry findWordform(long wfid) throws SemnetException {
        try {
            return restTemplate.postForObject(BASE_URL + "/find_wordform", new FindWordformByIdRequestDTO(wfid), WordformEntry.class);
        } catch (RestClientException e) {
            throw new SemnetException("Ошибка при поиске wordform", e);
        }
    }

    public LexPoolEntry findLexeme(String value) throws SemnetException {
        try {
            FindLexemeByTextRequestODO request = new FindLexemeByTextRequestODO(value);
            return restTemplate.postForObject(BASE_URL + "/find_lexeme", request, LexPoolEntry.class);
        } catch (RestClientException e) {
            throw new SemnetException("Ошибка при поиске лексемы", e);
        }
    }

    public LexPoolEntry createLexeme(String value) {
        CreateLexemeRequestODO request = new CreateLexemeRequestODO(value);
        return restTemplate.postForObject(BASE_URL + "/create_lexeme", request, LexPoolEntry.class);
    }

    public ReferenceEntry findReference(long src, long dst, ReferenceType type) throws SemnetException {
        try {
            log.debug("Поиск Reference между src={} и dst={} с типом {}", src, dst, type);

            if (src <= 0 || dst <= 0) {
                log.error("Некорректные идентификаторы: src={}, dst={}", src, dst);
                return null;
            }

            if (type == null) {
                log.error("Тип Reference не может быть null");
                return null;
            }

            FindReferenceRequestDTO req = new FindReferenceRequestDTO(src, dst, type);
            ReferenceEntry response = restTemplate.postForObject(
                    BASE_URL + "/find_reference",
                    req,
                    ReferenceEntry.class
            );

            if (response != null) {
                log.debug("Reference найден: {}", response);
                return response;
            } else {
                log.debug("Reference не найден: src={}, dst={}, type={}", src, dst, type);
                return null;
            }

        } catch (RestClientException e) {
            log.error("Ошибка при поиске Reference: src={}, dst={}, type={}", src, dst, type, e);
            throw new SemnetException("Ошибка при поиске Reference", e);
        }
    }

    public Object findReferenceSource(long src, ReferenceType type) throws SemnetException {
        try {
            FindReferenceSourceRequestDTO req = new FindReferenceSourceRequestDTO(src, type);
            return restTemplate.postForObject(BASE_URL + "/find_references_source", req, Object.class);
        } catch (RestClientException e) {
            throw new SemnetException("Ошибка при поиске источника Reference", e);
        }
    }

    public String findPath(String higher, String lower) throws SemnetException {
        try {
            FindPathRequestODO request = new FindPathRequestODO(higher, lower);
            return restTemplate.postForObject(BASE_URL + "/find_path", request, String.class);
        } catch (RestClientException e) {
            throw new SemnetException("Error in findPath", e);
        }
    }

    public long sense_no(long senseId) {
        return (senseId >> 40) & 0x3FFFFF;
    }

    public SenseEntry createSense(SenseEntry sense) throws SemnetException {
        try {
            String namedId = cleanLemma(sense.getNamed_id());
            if (namedId == null) {
                log.warn("Пустой или некорректный namedId при создании Sense.");
                return null;
            }

            String canonicalWordform = sense.getCanonical() != null ? cleanLemma(sense.getCanonical().getLexeme()) : null;
            if (canonicalWordform == null) {
                log.warn("Пустая каноническая форма при создании Sense: '{}'", namedId);
                return null;
            }

            CreateSenseRequestDto request = new CreateSenseRequestDto();
            request.setNamed_id(namedId);
            request.setCanonical_wordform(canonicalWordform);
            request.setEditor(null);

            log.debug("Создаем sense: {}", request);
            SenseEntry createdSense = restTemplate.postForObject(
                    BASE_URL + "/create_sense",
                    request,
                    SenseEntry.class
            );

            if (createdSense == null || !semanticIntegrationService.isSenseValid(createdSense)) {
                log.error("Sense создан, но содержит пустой или некорректный named_id: {}", namedId);
                return null;
            }

            return createdSense;
        } catch (RestClientException e) {
            log.error("Ошибка при создании sense: {}", sense, e);
            throw new SemnetException("Ошибка при создании sense", e);
        }
    }


    public String cleanLemma(String lemma) {
        if (lemma == null) {
            LoggerFactory.getLogger(getClass()).warn("Лемма не может быть null, возвращаем null.");
            return null;
        }

        lemma = lemma.trim();
        if (lemma.isEmpty()) {
            LoggerFactory.getLogger(getClass()).warn("Лемма пуста после очистки.");
            return null;
        }

        if (lemma.contains(":")) {
            int index = lemma.lastIndexOf(":");
            lemma = lemma.substring(index + 1).trim();
        }

        return lemma.isEmpty() ? null : lemma;
    }

    public SenseEntry findSense(String namedId) throws SemnetException {
        try {
            if (namedId.matches("^[0-9a-zA-Z]+$")) {
                LexPoolEntry lexeme = findLexeme(namedId);
                if (lexeme != null) {
                    namedId = lexeme.getValue();
                }
            }

            FindSenseByTextRequestODO request = new FindSenseByTextRequestODO(namedId);
            return restTemplate.postForObject(BASE_URL + "/find_sense", request, SenseEntry.class);
        } catch (RestClientException e) {
            log.error("Ошибка поиска sense с namedId={}", namedId, e);
            throw new SemnetException("Ошибка поиска sense", e);
        }
    }

    public ReferenceEntry createReference(String sourceNamedId, String targetNamedId, ReferenceType refType) throws SemnetException {
        try {
            if (refType == ReferenceType.PROPERTY) {
                log.info("Пропуск создания ссылки типа PROPERTY между {} и {}", sourceNamedId, targetNamedId);
                return null;
            }

            if (sourceNamedId == null || targetNamedId == null || refType == null) {
                throw new SemnetException("Некорректные данные для создания Reference: source=" + sourceNamedId + ", target=" + targetNamedId + ", type=" + refType);
            }

            Map<String, Object> request = new HashMap<>();
            request.put("src_named_id", sourceNamedId);
            request.put("dst_named_id", targetNamedId);
            request.put("type", refType.name());

            log.debug("Создаем Reference: {}", request);

            ReferenceEntry response = restTemplate.postForObject(
                    BASE_URL + "/create_reference",
                    request,
                    ReferenceEntry.class
            );

            if (response != null) {
                log.debug("Reference успешно создан: {}", response);
            } else {
                log.warn("Не удалось создать Reference между {} и {} с типом {}", sourceNamedId, targetNamedId, refType);
            }

            return response;
        } catch (RestClientException e) {
            log.error("Ошибка при создании Reference: {}", e.getMessage());
            throw new SemnetException("Ошибка создания Reference", e);
        }
    }

}
