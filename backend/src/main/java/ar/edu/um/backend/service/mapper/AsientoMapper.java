package ar.edu.um.backend.service.mapper;

import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.Venta;
import ar.edu.um.backend.service.dto.AsientoDTO;
import ar.edu.um.backend.service.dto.EventoDTO;
import ar.edu.um.backend.service.dto.VentaDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Asiento} and its DTO {@link AsientoDTO}.
 */
@Mapper(componentModel = "spring")
public interface AsientoMapper extends EntityMapper<AsientoDTO, Asiento> {
    @Mapping(target = "evento", source = "evento", qualifiedByName = "eventoTitulo")
    @Mapping(target = "ventas", source = "ventas", qualifiedByName = "ventaIdSet")
    AsientoDTO toDto(Asiento s);

    @Mapping(target = "ventas", ignore = true)
    @Mapping(target = "removeVentas", ignore = true)
    Asiento toEntity(AsientoDTO asientoDTO);

    @Named("eventoTitulo")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "titulo", source = "titulo")
    EventoDTO toDtoEventoTitulo(Evento evento);

    @Named("ventaId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    VentaDTO toDtoVentaId(Venta venta);

    @Named("ventaIdSet")
    default Set<VentaDTO> toDtoVentaIdSet(Set<Venta> venta) {
        return venta.stream().map(this::toDtoVentaId).collect(Collectors.toSet());
    }
}
