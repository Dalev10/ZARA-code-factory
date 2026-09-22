-- ============================================================================
-- V4__seed_admin_inicial.sql
-- Bootstrap: como HU-02 exige que solo un admin pueda registrar usuarios, pero
-- todavía no existe ningún usuario en el sistema, se crea aquí un admin inicial
-- por datos semilla en vez de por la API.
--
-- Contraseña temporal del admin semilla: "CambiarInmediatamente2026!"
-- (cumple la política de HU-02: mínimo 12 caracteres). El hash de abajo es
-- BCrypt de esa contraseña. Cámbiala apenas tengas login (HU-03) funcionando —
-- todavía no existe un flujo de "forzar cambio en primer login" en el alcance
-- actual de las HU.
--
-- No se asigna ningún scope todavía: el modelo de scopes y su asignación a
-- roles es responsabilidad de HU-09/HU-10, que no se han construido aún.
-- ============================================================================

INSERT INTO rol (nombre, descripcion)
VALUES ('ADMIN', 'Administrador con acceso total al sistema');

INSERT INTO usuario (email, nombre_completo, password_hash, estado)
VALUES (
    'admin@supplychain.local',
    'Administrador Inicial',
    '$2a$10$WpZmtcIaDE5vLu5T6Mu7KuMSfODo0cD9UoKw7.9lf8/DDVGbe3X5i',
    'ACTIVO'
);

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuario u, rol r
WHERE u.email = 'admin@supplychain.local' AND r.nombre = 'ADMIN';
