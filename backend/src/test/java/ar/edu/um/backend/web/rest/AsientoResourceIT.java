package ar.edu.um.backend.web.rest;

import static ar.edu.um.backend.domain.AsientoAsserts.*;
import static ar.edu.um.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.um.backend.IntegrationTest;
import ar.edu.um.backend.domain.Asiento;
import ar.edu.um.backend.domain.Evento;
import ar.edu.um.backend.domain.enumeration.AsientoEstado;
import ar.edu.um.backend.repository.AsientoRepository;
import ar.edu.um.backend.service.AsientoService;
import ar.edu.um.backend.service.dto.AsientoDTO;
import ar.edu.um.backend.service.mapper.AsientoMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AsientoResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AsientoResourceIT {

    private static final Integer DEFAULT_FILA = 1;
    private static final Integer UPDATED_FILA = 2;

    private static final Integer DEFAULT_COLUMNA = 1;
    private static final Integer UPDATED_COLUMNA = 2;

    private static final AsientoEstado DEFAULT_ESTADO = AsientoEstado.LIBRE;
    private static final AsientoEstado UPDATED_ESTADO = AsientoEstado.BLOQUEADO;

    private static final String DEFAULT_PERSONA_ACTUAL = "AAAAAAAAAA";
    private static final String UPDATED_PERSONA_ACTUAL = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/asientos";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AsientoRepository asientoRepository;

    @Mock
    private AsientoRepository asientoRepositoryMock;

    @Autowired
    private AsientoMapper asientoMapper;

    @Mock
    private AsientoService asientoServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAsientoMockMvc;

    private Asiento asiento;

    private Asiento insertedAsiento;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Asiento createEntity(EntityManager em) {
        Asiento asiento = new Asiento()
            .fila(DEFAULT_FILA)
            .columna(DEFAULT_COLUMNA)
            .estado(DEFAULT_ESTADO)
            .personaActual(DEFAULT_PERSONA_ACTUAL);
        // Add required entity
        Evento evento;
        if (TestUtil.findAll(em, Evento.class).isEmpty()) {
            evento = EventoResourceIT.createEntity();
            em.persist(evento);
            em.flush();
        } else {
            evento = TestUtil.findAll(em, Evento.class).get(0);
        }
        asiento.setEvento(evento);
        return asiento;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Asiento createUpdatedEntity(EntityManager em) {
        Asiento updatedAsiento = new Asiento()
            .fila(UPDATED_FILA)
            .columna(UPDATED_COLUMNA)
            .estado(UPDATED_ESTADO)
            .personaActual(UPDATED_PERSONA_ACTUAL);
        // Add required entity
        Evento evento;
        if (TestUtil.findAll(em, Evento.class).isEmpty()) {
            evento = EventoResourceIT.createUpdatedEntity();
            em.persist(evento);
            em.flush();
        } else {
            evento = TestUtil.findAll(em, Evento.class).get(0);
        }
        updatedAsiento.setEvento(evento);
        return updatedAsiento;
    }

    @BeforeEach
    void initTest() {
        asiento = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAsiento != null) {
            asientoRepository.delete(insertedAsiento);
            insertedAsiento = null;
        }
    }

    @Test
    @Transactional
    void createAsiento() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);
        var returnedAsientoDTO = om.readValue(
            restAsientoMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AsientoDTO.class
        );

        // Validate the Asiento in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAsiento = asientoMapper.toEntity(returnedAsientoDTO);
        assertAsientoUpdatableFieldsEquals(returnedAsiento, getPersistedAsiento(returnedAsiento));

        insertedAsiento = returnedAsiento;
    }

    @Test
    @Transactional
    void createAsientoWithExistingId() throws Exception {
        // Create the Asiento with an existing ID
        asiento.setId(1L);
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAsientoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkFilaIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        asiento.setFila(null);

        // Create the Asiento, which fails.
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        restAsientoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkColumnaIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        asiento.setColumna(null);

        // Create the Asiento, which fails.
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        restAsientoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEstadoIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        asiento.setEstado(null);

        // Create the Asiento, which fails.
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        restAsientoMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAsientos() throws Exception {
        // Initialize the database
        insertedAsiento = asientoRepository.saveAndFlush(asiento);

        // Get all the asientoList
        restAsientoMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(asiento.getId().intValue())))
            .andExpect(jsonPath("$.[*].fila").value(hasItem(DEFAULT_FILA)))
            .andExpect(jsonPath("$.[*].columna").value(hasItem(DEFAULT_COLUMNA)))
            .andExpect(jsonPath("$.[*].estado").value(hasItem(DEFAULT_ESTADO.toString())))
            .andExpect(jsonPath("$.[*].personaActual").value(hasItem(DEFAULT_PERSONA_ACTUAL)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAsientosWithEagerRelationshipsIsEnabled() throws Exception {
        when(asientoServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAsientoMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(asientoServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAsientosWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(asientoServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAsientoMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(asientoRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAsiento() throws Exception {
        // Initialize the database
        insertedAsiento = asientoRepository.saveAndFlush(asiento);

        // Get the asiento
        restAsientoMockMvc
            .perform(get(ENTITY_API_URL_ID, asiento.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(asiento.getId().intValue()))
            .andExpect(jsonPath("$.fila").value(DEFAULT_FILA))
            .andExpect(jsonPath("$.columna").value(DEFAULT_COLUMNA))
            .andExpect(jsonPath("$.estado").value(DEFAULT_ESTADO.toString()))
            .andExpect(jsonPath("$.personaActual").value(DEFAULT_PERSONA_ACTUAL));
    }

    @Test
    @Transactional
    void getNonExistingAsiento() throws Exception {
        // Get the asiento
        restAsientoMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAsiento() throws Exception {
        // Initialize the database
        insertedAsiento = asientoRepository.saveAndFlush(asiento);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the asiento
        Asiento updatedAsiento = asientoRepository.findById(asiento.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAsiento are not directly saved in db
        em.detach(updatedAsiento);
        updatedAsiento.fila(UPDATED_FILA).columna(UPDATED_COLUMNA).estado(UPDATED_ESTADO).personaActual(UPDATED_PERSONA_ACTUAL);
        AsientoDTO asientoDTO = asientoMapper.toDto(updatedAsiento);

        restAsientoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, asientoDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO))
            )
            .andExpect(status().isOk());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAsientoToMatchAllProperties(updatedAsiento);
    }

    @Test
    @Transactional
    void putNonExistingAsiento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        asiento.setId(longCount.incrementAndGet());

        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAsientoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, asientoDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAsiento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        asiento.setId(longCount.incrementAndGet());

        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAsientoMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(asientoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAsiento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        asiento.setId(longCount.incrementAndGet());

        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAsientoMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(asientoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAsientoWithPatch() throws Exception {
        // Initialize the database
        insertedAsiento = asientoRepository.saveAndFlush(asiento);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the asiento using partial update
        Asiento partialUpdatedAsiento = new Asiento();
        partialUpdatedAsiento.setId(asiento.getId());

        partialUpdatedAsiento.personaActual(UPDATED_PERSONA_ACTUAL);

        restAsientoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAsiento.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAsiento))
            )
            .andExpect(status().isOk());

        // Validate the Asiento in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAsientoUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedAsiento, asiento), getPersistedAsiento(asiento));
    }

    @Test
    @Transactional
    void fullUpdateAsientoWithPatch() throws Exception {
        // Initialize the database
        insertedAsiento = asientoRepository.saveAndFlush(asiento);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the asiento using partial update
        Asiento partialUpdatedAsiento = new Asiento();
        partialUpdatedAsiento.setId(asiento.getId());

        partialUpdatedAsiento.fila(UPDATED_FILA).columna(UPDATED_COLUMNA).estado(UPDATED_ESTADO).personaActual(UPDATED_PERSONA_ACTUAL);

        restAsientoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAsiento.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAsiento))
            )
            .andExpect(status().isOk());

        // Validate the Asiento in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAsientoUpdatableFieldsEquals(partialUpdatedAsiento, getPersistedAsiento(partialUpdatedAsiento));
    }

    @Test
    @Transactional
    void patchNonExistingAsiento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        asiento.setId(longCount.incrementAndGet());

        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAsientoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, asientoDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(asientoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAsiento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        asiento.setId(longCount.incrementAndGet());

        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAsientoMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(asientoDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAsiento() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        asiento.setId(longCount.incrementAndGet());

        // Create the Asiento
        AsientoDTO asientoDTO = asientoMapper.toDto(asiento);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAsientoMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(asientoDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Asiento in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAsiento() throws Exception {
        // Initialize the database
        insertedAsiento = asientoRepository.saveAndFlush(asiento);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the asiento
        restAsientoMockMvc
            .perform(delete(ENTITY_API_URL_ID, asiento.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return asientoRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Asiento getPersistedAsiento(Asiento asiento) {
        return asientoRepository.findById(asiento.getId()).orElseThrow();
    }

    protected void assertPersistedAsientoToMatchAllProperties(Asiento expectedAsiento) {
        assertAsientoAllPropertiesEquals(expectedAsiento, getPersistedAsiento(expectedAsiento));
    }

    protected void assertPersistedAsientoToMatchUpdatableProperties(Asiento expectedAsiento) {
        assertAsientoAllUpdatablePropertiesEquals(expectedAsiento, getPersistedAsiento(expectedAsiento));
    }
}
