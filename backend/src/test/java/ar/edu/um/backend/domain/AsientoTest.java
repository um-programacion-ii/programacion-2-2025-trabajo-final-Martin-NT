package ar.edu.um.backend.domain;

import static ar.edu.um.backend.domain.AsientoTestSamples.*;
import static ar.edu.um.backend.domain.EventoTestSamples.*;
import static ar.edu.um.backend.domain.VentaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AsientoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Asiento.class);
        Asiento asiento1 = getAsientoSample1();
        Asiento asiento2 = new Asiento();
        assertThat(asiento1).isNotEqualTo(asiento2);

        asiento2.setId(asiento1.getId());
        assertThat(asiento1).isEqualTo(asiento2);

        asiento2 = getAsientoSample2();
        assertThat(asiento1).isNotEqualTo(asiento2);
    }

    @Test
    void eventoTest() {
        Asiento asiento = getAsientoRandomSampleGenerator();
        Evento eventoBack = getEventoRandomSampleGenerator();

        asiento.setEvento(eventoBack);
        assertThat(asiento.getEvento()).isEqualTo(eventoBack);

        asiento.evento(null);
        assertThat(asiento.getEvento()).isNull();
    }

    @Test
    void ventasTest() {
        Asiento asiento = getAsientoRandomSampleGenerator();
        Venta ventaBack = getVentaRandomSampleGenerator();

        asiento.addVentas(ventaBack);
        assertThat(asiento.getVentas()).containsOnly(ventaBack);
        assertThat(ventaBack.getAsientos()).containsOnly(asiento);

        asiento.removeVentas(ventaBack);
        assertThat(asiento.getVentas()).doesNotContain(ventaBack);
        assertThat(ventaBack.getAsientos()).doesNotContain(asiento);

        asiento.ventas(new HashSet<>(Set.of(ventaBack)));
        assertThat(asiento.getVentas()).containsOnly(ventaBack);
        assertThat(ventaBack.getAsientos()).containsOnly(asiento);

        asiento.setVentas(new HashSet<>());
        assertThat(asiento.getVentas()).doesNotContain(ventaBack);
        assertThat(ventaBack.getAsientos()).doesNotContain(asiento);
    }
}
