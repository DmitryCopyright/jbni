package com.tuneit.jackalope.integration.babelnet.pipeline;

import com.tuneit.jackalope.integration.babelnet.DTO.EdgeFilterConfigDto;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import org.springframework.stereotype.Component;

@Component
public class BabelEdgeFilter {
    public boolean checkEdge(BabelSynsetRelation edge, EdgeFilterConfigDto config) {
        if (edge == null) return false;
        if (edge.getWeight() < config.getMinWeight()) {
            return false;
        }
        if (config.getRequiredPointer() != null && !edge.getPointer().equals(config.getRequiredPointer())) {
            return false;
        }
        return true;
    }
}
