package com.tuneit.jackalope.integration.babelnet.DTO;

import it.uniroma1.lcl.jlt.util.Language;
import lombok.Data;


@Data
public class BabelRequestDto {
    private String lemma;
    private Language language;

    public BabelRequestDto() {
    }

    public BabelRequestDto(String lemma, Language language) {
        this.lemma = lemma;
        this.language = language;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}