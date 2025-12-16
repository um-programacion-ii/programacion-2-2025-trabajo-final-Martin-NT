package ar.edu.um.backend.service.mapper;

import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.service.dto.AsientoDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Asiento} and its DTO {@link AsientoDTO}.
 */
@Mapper(componentModel = "spring", uses = { EventoMapper.class })
public interface AsientoMapper extends EntityMapper<AsientoDTO, Asiento> {

    @Override
    //@Mapping(target = "evento", source = "evento") // Incluir evento dentro del asiento
    @Mapping(target = "evento", ignore = true) // no incluir evento dentro del asiento
    @Mapping(target = "ventas", ignore = true) // evitamos grafo infinito Venta -> Asiento -> Venta...
    AsientoDTO toDto(Asiento s);

    @Override
    @Mapping(target = "ventas", ignore = true)
    Asiento toEntity(AsientoDTO asientoDTO);
}
