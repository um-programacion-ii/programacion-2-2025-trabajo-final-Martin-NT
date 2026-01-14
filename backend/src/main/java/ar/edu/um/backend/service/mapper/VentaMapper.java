package ar.edu.um.backend.service.mapper;

import ar.edu.um.backend.domain.Venta;
import ar.edu.um.backend.service.dto.VentaDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Venta} and its DTO {@link VentaDTO}.
 */
@Mapper(componentModel = "spring", uses = { EventoMapper.class, AsientoMapper.class })
public interface VentaMapper extends EntityMapper<VentaDTO, Venta> {

    @Override
    @Mapping(target = "evento", source = "evento")
    @Mapping(target = "asientos", source = "asientos")
    VentaDTO toDto(Venta s);

    @Override
    @Mapping(target = "asientos", ignore = true) // normalmente no construís ventas desde el DTO completo
    Venta toEntity(VentaDTO ventaDTO);
}
