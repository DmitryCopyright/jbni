package com.tuneit.jackalope.integration.babelnet.DTO;

import it.uniroma1.lcl.babelnet.data.BabelPointer;

public class EdgeDto {
    private BabelPointer pointer;
    private double weight;
    private String sourceLemma;
    private String targetLemma;

    public BabelPointer getPointer() {
        return pointer;
    }
    public void setPointer(BabelPointer pointer) {
        this.pointer = pointer;
    }

    public double getWeight() {
        return weight;
    }
    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getSourceLemma() {
        return sourceLemma;
    }
    public void setSourceLemma(String sourceLemma) {
        this.sourceLemma = sourceLemma;
    }

    public String getTargetLemma() {
        return targetLemma;
    }
    public void setTargetLemma(String targetLemma) {
        this.targetLemma = targetLemma;
    }
}