package ar.edu.um.backend.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VentaTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Venta getVentaSample1() {
        return new Venta().id(1L).descripcion("descripcion1").cantidadAsientos(1);
    }

    public static Venta getVentaSample2() {
        return new Venta().id(2L).descripcion("descripcion2").cantidadAsientos(2);
    }

    public static Venta getVentaRandomSampleGenerator() {
        return new Venta()
            .id(longCount.incrementAndGet())
            .descripcion(UUID.randomUUID().toString())
            .cantidadAsientos(intCount.incrementAndGet());
    }
}
