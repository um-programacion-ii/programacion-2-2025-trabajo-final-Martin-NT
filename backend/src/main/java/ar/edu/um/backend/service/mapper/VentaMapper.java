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
 * Mapper for the entity {@link Venta} and its DTO {@link VentaDTO}.
 */
@Mapper(componentModel = "spring")
public interface VentaMapper extends EntityMapper<VentaDTO, Venta> {
    @Mapping(target = "evento", source = "evento", qualifiedByName = "eventoTitulo")
    @Mapping(target = "asientos", source = "asientos", qualifiedByName = "asientoIdSet")
    VentaDTO toDto(Venta s);

    @Mapping(target = "removeAsientos", ignore = true)
    Venta toEntity(VentaDTO ventaDTO);

    @Named("eventoTitulo")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "titulo", source = "titulo")
    EventoDTO toDtoEventoTitulo(Evento evento);

    @Named("asientoId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    AsientoDTO toDtoAsientoId(Asiento asiento);

    @Named("asientoIdSet")
    default Set<AsientoDTO> toDtoAsientoIdSet(Set<Asiento> asiento) {
        return asiento.stream().map(this::toDtoAsientoId).collect(Collectors.toSet());
    }
}
