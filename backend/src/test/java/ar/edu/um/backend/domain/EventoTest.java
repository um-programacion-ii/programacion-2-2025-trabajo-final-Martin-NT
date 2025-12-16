package ar.edu.um.backend.domain;

import static ar.edu.um.backend.domain.AsientoTestSamples.*;
import static ar.edu.um.backend.domain.EventoTestSamples.*;
import static ar.edu.um.backend.domain.VentaTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.backend.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Evento.class);
        Evento evento1 = getEventoSample1();
        Evento evento2 = new Evento();
        assertThat(evento1).isNotEqualTo(evento2);

        evento2.setId(evento1.getId());
        assertThat(evento1).isEqualTo(evento2);

        evento2 = getEventoSample2();
        assertThat(evento1).isNotEqualTo(evento2);
    }

    @Test
    void asientosTest() {
        Evento evento = getEventoRandomSampleGenerator();
        Asiento asientoBack = getAsientoRandomSampleGenerator();

        evento.addAsientos(asientoBack);
        assertThat(evento.getAsientos()).containsOnly(asientoBack);
        assertThat(asientoBack.getEvento()).isEqualTo(evento);

        evento.removeAsientos(asientoBack);
        assertThat(evento.getAsientos()).doesNotContain(asientoBack);
        assertThat(asientoBack.getEvento()).isNull();

        evento.asientos(new HashSet<>(Set.of(asientoBack)));
        assertThat(evento.getAsientos()).containsOnly(asientoBack);
        assertThat(asientoBack.getEvento()).isEqualTo(evento);

        evento.setAsientos(new HashSet<>());
        assertThat(evento.getAsientos()).doesNotContain(asientoBack);
        assertThat(asientoBack.getEvento()).isNull();
    }

    @Test
    void ventasTest() {
        Evento evento = getEventoRandomSampleGenerator();
        Venta ventaBack = getVentaRandomSampleGenerator();

        evento.addVentas(ventaBack);
        assertThat(evento.getVentas()).containsOnly(ventaBack);
        assertThat(ventaBack.getEvento()).isEqualTo(evento);

        evento.removeVentas(ventaBack);
        assertThat(evento.getVentas()).doesNotContain(ventaBack);
        assertThat(ventaBack.getEvento()).isNull();

        evento.ventas(new HashSet<>(Set.of(ventaBack)));
        assertThat(evento.getVentas()).containsOnly(ventaBack);
        assertThat(ventaBack.getEvento()).isEqualTo(evento);

        evento.setVentas(new HashSet<>());
        assertThat(evento.getVentas()).doesNotContain(ventaBack);
        assertThat(ventaBack.getEvento()).isNull();
    }
}
