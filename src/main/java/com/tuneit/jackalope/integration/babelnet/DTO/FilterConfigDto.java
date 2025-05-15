package com.tuneit.jackalope.integration.babelnet.DTO;

import it.uniroma1.lcl.jlt.util.Language;

public class FilterConfigDto {
    private Language requiredLanguage;
    private int minEdges;
    private String requiredDomain;
    private int minGlossWords;

    public Language getRequiredLanguage() {
        return requiredLanguage;
    }

    public void setRequiredLanguage(Language requiredLanguage) {
        this.requiredLanguage = requiredLanguage;
    }

    public int getMinEdges() {
        return minEdges;
    }

    public void setMinEdges(int minEdges) {
        this.minEdges = minEdges;
    }

    public String getRequiredDomain() {
        return requiredDomain;
    }

    public void setRequiredDomain(String requiredDomain) {
        this.requiredDomain = requiredDomain;
    }

    public int getMinGlossWords() {
        return minGlossWords;
    }

    public void setMinGlossWords(int minGlossWords) {
        this.minGlossWords = minGlossWords;
    }
}