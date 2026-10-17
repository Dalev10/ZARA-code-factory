/**
 * API pública del módulo inventario: lo único que otros módulos pueden usar de él
 * (Open Host Service + Published Language, ver ARQUITECTURA.md). Todo lo demás del
 * módulo es interno y Spring Modulith hace fallar el build si alguien lo usa.
 *
 * Solo contiene contratos que algún módulo consume hoy: no se agrega nada "por si acaso".
 * Los puertos para dependencias que Inventario consume viven en application/port/out
 * y sus adaptadores en infrastructure/adapter/out/<proveedor>; ese es el SPI de
 * Inventario, aunque no se publique como parte de esta API.
 */
@NamedInterface("api")
package com.codefactory.supplychain.inventario.api;

import org.springframework.modulith.NamedInterface;
