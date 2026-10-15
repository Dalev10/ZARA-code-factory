/**
 * API pública del módulo inventario: lo único que otros módulos pueden usar de él
 * (Open Host Service + Published Language, ver ARQUITECTURA.md). Todo lo demás del
 * módulo es interno y Spring Modulith hace fallar el build si alguien lo usa.
 *
 * Solo contiene contratos que algún módulo consume hoy: no se agrega nada "por si acaso".
 */
@NamedInterface("api")
package com.codefactory.supplychain.inventario.api;

import org.springframework.modulith.NamedInterface;
