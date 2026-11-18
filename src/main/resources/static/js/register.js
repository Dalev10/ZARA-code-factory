document.getElementById("register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = document.getElementById("reg-email").value.trim();
    const nombreCompleto = document.getElementById("reg-nombre").value.trim();
    const password = document.getElementById("reg-password").value;
    const ok = document.querySelector('[data-testid="reg-success"]');
    const err = document.querySelector('[data-testid="reg-error"]');
    ok.hidden = true; err.hidden = true;

    try {
        const resp = await apiRequest("/usuarios", {
            method: "POST",
            body: { email, nombreCompleto, password }
        });
        mostrarMensaje(ok, "success",
            "Usuario registrado: " + resp.email + " (" + resp.estado + ")");
        e.target.reset();
    } catch (ex) {
        let msg;
        if (ex.status === 401) msg = "Necesitas iniciar sesión como administrador.";
        else if (ex.status === 403) msg = "Tu usuario no tiene permiso para registrar usuarios.";
        else if (ex.status === 409) msg = "Ya existe un usuario con ese correo.";
        else if (ex.status === 400) msg = "Datos inválidos: " + (ex.message || "revisa el formulario.");
        else msg = ex.message || "No se pudo registrar el usuario.";
        mostrarMensaje(err, "error", msg);
    }
});
