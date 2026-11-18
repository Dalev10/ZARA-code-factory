const API_BASE = "/api/v1";

function getToken() {
    return localStorage.getItem("jwt");
}

function setToken(token) {
    if (token) localStorage.setItem("jwt", token);
    else localStorage.removeItem("jwt");
}

async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
    const headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (auth && getToken()) headers["Authorization"] = "Bearer " + getToken();

    const resp = await fetch(API_BASE + path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined
    });

    const text = await resp.text();
    const payload = text ? JSON.parse(text) : null;
    if (!resp.ok) {
        const err = new Error((payload && (payload.mensaje || payload.message)) || ("HTTP " + resp.status));
        err.status = resp.status;
        err.payload = payload;
        throw err;
    }
    return payload;
}

function mostrarMensaje(target, kind, text) {
    target.hidden = false;
    target.textContent = text;
    target.classList.remove("error", "success");
    target.classList.add(kind);
}
