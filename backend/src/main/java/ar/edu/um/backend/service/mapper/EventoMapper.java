package ar.edu.um.backend.service.mapper;

import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.service.dto.EventoDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Evento} and its DTO {@link EventoDTO}.
 */
@Mapper(componentModel = "spring")
public interface EventoMapper extends EntityMapper<EventoDTO, Evento> {
}
