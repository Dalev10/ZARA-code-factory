/**
 * Componentes transversales (excepciones base, seguridad, configuración, DTOs comunes)
 * compartidos por todos los módulos.
 *
 * <p>Módulo abierto para Spring Modulith: los demás módulos pueden usar sus subpaquetes
 * directamente, a diferencia de los módulos de negocio, que solo exponen lo declarado.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.codefactory.supplychain.shared;

import org.springframework.modulith.ApplicationModule;
