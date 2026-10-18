package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.CategoriaEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.TemplateEntity;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity.VarianteEntity;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.NodoEntity;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.mapper.InventarioMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({InventarioMapper.class, InventarioRepositoryAdapter.class})
class InventarioRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventarioRepositoryAdapter inventarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void guardaYRecuperaInventarioPorNodoYVariante() {
        UUID nodoId = persistirNodo();
        UUID varianteId = persistirVariante();
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(12, null);

        Inventario guardado = inventarioRepository.guardar(inventario);
        entityManager.flush();
        entityManager.clear();

        Inventario recuperado = inventarioRepository
                .buscarPorNodoYVariante(nodoId, varianteId)
                .orElseThrow();

        assertThat(guardado.getId()).isEqualTo(inventario.getId());
        assertThat(recuperado.getALaMano()).isEqualTo(12);
        assertThat(recuperado.getDisponible()).isEqualTo(12);
        assertThat(recuperado.getPronosticado()).isEqualTo(12);
        assertThat(recuperado.getVersion()).isZero();
    }

    @Test
    void buscarPorNodoYVarianteDevuelveVacioSiNoExiste() {
        assertThat(inventarioRepository.buscarPorNodoYVariante(UUID.randomUUID(), UUID.randomUUID()))
                .isEmpty();
    }

    private UUID persistirNodo() {
        UUID tiendaId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tienda (id, nombre, ubicacion, estado) VALUES (:id, :nombre, :ubicacion, 'ACTIVA')")
                .setParameter("id", tiendaId)
                .setParameter("nombre", "Tienda Inventario " + tiendaId)
                .setParameter("ubicacion", "Bogotá")
                .executeUpdate();

        UUID nodoId = UUID.randomUUID();
        entityManager.persistAndFlush(NodoEntity.builder()
                .id(nodoId)
                .tipo(TipoNodo.BODEGA_TIENDA)
                .tiendaId(tiendaId)
                .build());
        return nodoId;
    }

    private UUID persistirVariante() {
        CategoriaEntity categoria = entityManager.persistAndFlush(CategoriaEntity.builder()
                .id(UUID.randomUUID())
                .nombre("Categoría Inventario " + UUID.randomUUID())
                .build());
        TemplateEntity template = entityManager.persistAndFlush(TemplateEntity.builder()
                .id(UUID.randomUUID())
                .categoria(categoria)
                .nombre("Template Inventario " + UUID.randomUUID())
                .build());
        return entityManager.persistAndFlush(VarianteEntity.builder()
                .id(UUID.randomUUID())
                .template(template)
                .sku("SKU-" + UUID.randomUUID())
                .build()).getId();
    }
}
