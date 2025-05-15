package com.tuneit.jackalope.integration.babelnet.integration;

import com.tuneit.jackalope.ltf.semnetmanager.entry.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PointerToRefTypeMapper {
    Logger logger = LoggerFactory.getLogger(getClass());


    public ReferenceType mapPointer(String pointer) {
        switch (pointer.toLowerCase()) {
            case "holonym":
            case "@":
                return ReferenceType.HOLONYM;
            case "meronym":
            case "+":
                return ReferenceType.MERONYM;
            case "hypernym":
            case "~":
                return ReferenceType.HYPERNYM;
            case "hyponym":
            case "!":
                return ReferenceType.HYPONYM;
            case "synonym":
            case "=":
            case "derivationally_related_form":
            case "gloss_related_form_(disambiguated)":
            case "gdis":
                return ReferenceType.SYNONYM;
            case "antonym":
            case "^":
                return ReferenceType.ANTONYM;
            default:
                logger.warn("Неизвестный тип указателя: '{}', используем PROPERTY", pointer);
                return ReferenceType.PROPERTY;
        }
    }

    public ReferenceType complementary(ReferenceType type) {
        switch (type) {
            case HOLONYM:
                return ReferenceType.MERONYM;
            case MERONYM:
                return ReferenceType.HOLONYM;
            case HYPERNYM:
                return ReferenceType.HYPONYM;
            case HYPONYM:
                return ReferenceType.HYPERNYM;
            default:
                return ReferenceType.PROPERTY;
        }
    }
}