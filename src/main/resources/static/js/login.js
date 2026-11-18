document.getElementById("login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const errBox = document.querySelector('[data-testid="login-error"]');
    const okBox = document.querySelector('[data-testid="login-success"]');
    errBox.hidden = true; okBox.hidden = true;

    try {
        const resp = await apiRequest("/auth/login", {
            method: "POST",
            auth: false,
            body: { email, password }
        });
        setToken(resp.accessToken);
        mostrarMensaje(okBox, "success", "Sesión iniciada. Redirigiendo…");
        setTimeout(() => { window.location.href = "/templates.html"; }, 400);
    } catch (err) {
        mostrarMensaje(errBox, "error", err.status === 401
            ? "Credenciales inválidas."
            : (err.message || "No se pudo iniciar sesión."));
    }
});
