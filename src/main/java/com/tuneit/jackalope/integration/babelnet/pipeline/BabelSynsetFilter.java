package com.tuneit.jackalope.integration.babelnet.pipeline;

import com.tuneit.jackalope.integration.babelnet.DTO.BabelSynsetDto;
import com.tuneit.jackalope.integration.babelnet.DTO.FilterConfigDto;
import it.uniroma1.lcl.jlt.util.Language;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class BabelSynsetFilter {

    public boolean check(BabelSynsetDto dto, FilterConfigDto config) {
        if (!checkLanguage(dto, config.getRequiredLanguage())) {
            return false;
        }
        if (!checkEdgesCount(dto, config.getMinEdges())) {
            return false;
        }
        if (!checkDomain(dto, config.getRequiredDomain())) {
            return false;
        }
        if (!checkGloss(dto, config.getMinGlossWords())) {
            return false;
        }
        return true;
    }

    private boolean checkLanguage(BabelSynsetDto dto, Language requiredLang) {
        if (requiredLang == null) return true;
        if (dto.getActualLanguage() == null) return false;
        return dto.getActualLanguage().equals(requiredLang.name());
    }

    private boolean checkEdgesCount(BabelSynsetDto dto, int minEdges) {
        return dto.getEdgesCount() >= minEdges;
    }

    private boolean checkDomain(BabelSynsetDto dto, String requiredDomain) {
        if (requiredDomain == null || requiredDomain.isEmpty()) return true;
        if (dto.getDomain() == null) return false;
        return dto.getDomain().equalsIgnoreCase(requiredDomain);
    }

    private boolean checkGloss(BabelSynsetDto dto, int minGlossWords) {
        if (minGlossWords <= 0) return true;
        List<String> glosses = dto.getGlosses();
        if (glosses == null || glosses.isEmpty()) return false;
        int wordsCount = glosses.stream()
                .mapToInt(g -> g.split("\\s+").length)
                .sum();
        return wordsCount >= minGlossWords;
    }
}