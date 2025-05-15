package com.tuneit.jackalope.integration.babelnet.DTO;

import lombok.Data;

import java.util.List;

@Data
public class BabelSynsetDto {
    private String synsetId;
    private String mainSense;
    private List<String> glosses;
    private List<String> translations;
    private String domain;
    private int edgesCount;
    private String actualLanguage;

    public String getSynsetId() {
        return synsetId;
    }

    public void setSynsetId(String synsetId) {
        this.synsetId = synsetId;
    }

    public String getMainSense() {
        return mainSense;
    }

    public void setMainSense(String mainSense) {
        this.mainSense = mainSense;
    }

    public List<String> getGlosses() {
        return glosses;
    }

    public void setGlosses(List<String> glosses) {
        this.glosses = glosses;
    }

    public List<String> getTranslations() {
        return translations;
    }

    public void setTranslations(List<String> translations) {
        this.translations = translations;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getEdgesCount() {
        return edgesCount;
    }

    public void setEdgesCount(int edgesCount) {
        this.edgesCount = edgesCount;
    }

    public String getActualLanguage() {
        return actualLanguage;
    }

    public void setActualLanguage(String actualLanguage) {
        this.actualLanguage = actualLanguage;
    }
}
