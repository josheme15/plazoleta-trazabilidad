package com.plazoleta.trazabilidad;

import com.plazoleta.trazabilidad.domain.api.ITraceServicePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifica que el contexto arranque (el cliente de MongoDB se conecta de forma diferida, no requiere el servidor).
 */
@SpringBootTest
class TrazabilidadApplicationTests {

    @Autowired
    private ITraceServicePort traceServicePort;

    @Test
    void contextLoads() {
        assertNotNull(traceServicePort);
    }
}
