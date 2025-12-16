package ar.edu.um.backend.service.mapper;

import static ar.edu.um.backend.domain.AsientoAsserts.*;
import static ar.edu.um.backend.domain.AsientoTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsientoMapperTest {

    private AsientoMapper asientoMapper;

    @BeforeEach
    void setUp() {
        asientoMapper = new AsientoMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAsientoSample1();
        var actual = asientoMapper.toEntity(asientoMapper.toDto(expected));
        assertAsientoAllPropertiesEquals(expected, actual);
    }
}
