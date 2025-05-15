package com.tuneit.jackalope.integration.babelnet.DTO;

import it.uniroma1.lcl.babelnet.data.BabelPointer;

public class EdgeFilterConfigDto {
    private double minWeight;
    private BabelPointer requiredPointer;

    public double getMinWeight() {
        return minWeight;
    }

    public void setMinWeight(double minWeight) {
        this.minWeight = minWeight;
    }

    public BabelPointer getRequiredPointer() {
        return requiredPointer;
    }

    public void setRequiredPointer(BabelPointer requiredPointer) {
        this.requiredPointer = requiredPointer;
    }
}
