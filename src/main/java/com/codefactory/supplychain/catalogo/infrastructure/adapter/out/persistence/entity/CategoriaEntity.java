package com.codefactory.supplychain.catalogo.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code categoria}, definida en
 * {@code V1__crear_esquema_inicial.sql}.
 * <p>
 * Es independiente del modelo de dominio {@code Categoria}: la
 * conversión entre ambas se realiza en {@code CategoriaPersistenceMapper}.
 */
@Entity
@Table(name = "categoria")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CategoriaEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;
}
