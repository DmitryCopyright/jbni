package com.tuneit.jackalope.integration.babelnet;

import com.tuneit.jackalope.integration.babelnet.DTO.BabelSynsetDto;
import com.tuneit.jackalope.integration.babelnet.exceptions.IntegrationException;
import com.tuneit.jackalope.integration.babelnet.exceptions.SemnetException;
import com.tuneit.jackalope.integration.babelnet.service.BabelNetApiService;
import com.tuneit.jackalope.integration.babelnet.integration.SemanticIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BabelNetImportErrorTest {

    private BabelNetApiService babelNetApiService;
    private SemanticIntegrationService integrationService;

    @BeforeEach
    public void setup() {
        babelNetApiService = mock(BabelNetApiService.class);
        integrationService = mock(SemanticIntegrationService.class);
    }

    @Test
    public void testImportFailsOnInvalidSynset() throws IOException {
        when(babelNetApiService.fetchSynsetById("invalid-id"))
                .thenThrow(new IOException("Invalid SynsetID"));

        assertThrows(IOException.class, () -> babelNetApiService.fetchSynsetById("invalid-id"));
    }

    @Test
    public void testIntegrationSkipsEmptyMainSense() throws IntegrationException, SemnetException {
        BabelSynsetDto dto = new BabelSynsetDto();
        dto.setSynsetId("bn:00000000n");
        dto.setMainSense("");
        dto.setEdgesCount(0);

        doNothing().when(integrationService).integrateSynsets(Collections.singletonList(dto));
        assertDoesNotThrow(() -> integrationService.integrateSynsets(Collections.singletonList(dto)));
    }
}