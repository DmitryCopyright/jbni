package com.tuneit.jackalope.integration.babelnet.DTO;

import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;

public class SynsetEdgeHolderDto {
    private final BabelSynset syn;
    private final BabelSynsetRelation edge;

    public SynsetEdgeHolderDto(BabelSynset syn, BabelSynsetRelation edge) {
        this.syn = syn;
        this.edge = edge;
    }

    public BabelSynset getSyn() {
        return syn;
    }

    public BabelSynsetRelation getEdge() {
        return edge;
    }
}

