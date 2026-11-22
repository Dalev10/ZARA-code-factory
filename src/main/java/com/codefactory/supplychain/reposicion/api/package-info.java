/**
 * API pública del módulo reposicion: lo único que otros módulos pueden usar de él.
 * Todo lo demás del módulo es interno y Spring Modulith hace fallar el build si
 * alguien lo usa.
 *
 * Solo contiene contratos que algún módulo consume hoy (plan Sprint 3, sección 4):
 * por ahora {@code consultarOrden} para logistica (HU-46, HU-47, HU-49). Las
 * transiciones de la orden de la sección 4.2 se agregan en la historia que las consume.
 * La creación de órdenes no se publica: ocurre solo al aprobar una recomendación (D-80).
 */
@NamedInterface("api")
package com.codefactory.supplychain.reposicion.api;

import org.springframework.modulith.NamedInterface;
