package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
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
@Import({InventarioMapper.class, InventarioPorNodoPersistenceAdapter.class})
class InventarioPorNodoPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventarioPorNodoPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void consultaInventariosDeUnNodoUsandoElMapperOficial() {
        UUID tiendaId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tienda (id, nombre, ubicacion, estado) VALUES (:id, :nombre, :ubicacion, 'ACTIVA')")
                .setParameter("id", tiendaId)
                .setParameter("nombre", "Tienda Lectura " + tiendaId)
                .setParameter("ubicacion", "Cali")
                .executeUpdate();
        UUID nodoId = UUID.randomUUID();
        entityManager.persistAndFlush(NodoEntity.builder()
                .id(nodoId).tipo(TipoNodo.BODEGA_TIENDA).tiendaId(tiendaId).build());

        UUID varianteId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO categoria (id, nombre) VALUES (:id, :nombre)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("nombre", "Categoria Lectura " + varianteId)
                .executeUpdate();
        UUID templateId = UUID.randomUUID();
        UUID categoriaId = (UUID) entityManager.getEntityManager()
                .createNativeQuery("SELECT id FROM categoria WHERE nombre = :nombre")
                .setParameter("nombre", "Categoria Lectura " + varianteId)
                .getSingleResult();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO template (id, categoria_id, nombre) VALUES (:id, :categoria, :nombre)")
                .setParameter("id", templateId)
                .setParameter("categoria", categoriaId)
                .setParameter("nombre", "Template Lectura " + varianteId)
                .executeUpdate();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO variante (id, template_id, sku) VALUES (:id, :template, :sku)")
                .setParameter("id", varianteId)
                .setParameter("template", templateId)
                .setParameter("sku", "SKU-" + varianteId)
                .executeUpdate();

        Inventario inventario = Inventario.crear(nodoId, varianteId);
        inventario.cargar(9, null);
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO inventario (id, nodo_id, variante_id, a_la_mano, disponible_para_uso, entrante, saliente, pronosticado) "
                        + "VALUES (:id, :nodo, :variante, 9, 9, 0, 0, 9)")
                .setParameter("id", inventario.getId())
                .setParameter("nodo", nodoId)
                .setParameter("variante", varianteId)
                .executeUpdate();
        entityManager.flush();

        assertThat(adapter.consultarPorNodo(nodoId))
                .extracting(InventarioResumen::varianteId)
                .containsExactly(varianteId);
        assertThat(adapter.consultarPorNodo(nodoId).getFirst().aLaMano()).isEqualTo(9);
    }
}
