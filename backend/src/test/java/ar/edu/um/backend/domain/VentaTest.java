package ar.edu.um.backend.domain;

import static ar.edu.um.backend.domain.AsientoTestSamples.*;
import static ar.edu.um.backend.domain.EventoTestSamples.*;
import static ar.edu.um.backend.domain.VentaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VentaTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Venta.class);
        Venta venta1 = getVentaSample1();
        Venta venta2 = new Venta();
        assertThat(venta1).isNotEqualTo(venta2);

        venta2.setId(venta1.getId());
        assertThat(venta1).isEqualTo(venta2);

        venta2 = getVentaSample2();
        assertThat(venta1).isNotEqualTo(venta2);
    }

    @Test
    void eventoTest() {
        Venta venta = getVentaRandomSampleGenerator();
        Evento eventoBack = getEventoRandomSampleGenerator();

        venta.setEvento(eventoBack);
        assertThat(venta.getEvento()).isEqualTo(eventoBack);

        venta.evento(null);
        assertThat(venta.getEvento()).isNull();
    }

    @Test
    void asientosTest() {
        Venta venta = getVentaRandomSampleGenerator();
        Asiento asientoBack = getAsientoRandomSampleGenerator();

        venta.addAsientos(asientoBack);
        assertThat(venta.getAsientos()).containsOnly(asientoBack);

        venta.removeAsientos(asientoBack);
        assertThat(venta.getAsientos()).doesNotContain(asientoBack);

        venta.asientos(new HashSet<>(Set.of(asientoBack)));
        assertThat(venta.getAsientos()).containsOnly(asientoBack);

        venta.setAsientos(new HashSet<>());
        assertThat(venta.getAsientos()).doesNotContain(asientoBack);
    }
}
