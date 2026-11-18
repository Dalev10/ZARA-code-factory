const toast = document.querySelector('[data-testid="tpl-toast"]');
const tbody = document.getElementById("tpl-tbody");
const categoriaSelect = document.getElementById("tpl-categoria");

function notify(kind, text) {
    toast.hidden = false;
    toast.textContent = text;
    toast.dataset.kind = kind;
    toast.classList.remove("error", "success");
    toast.classList.add(kind);
}

document.getElementById("logout").addEventListener("click", () => {
    setToken(null);
    window.location.href = "/index.html";
});

async function cargarCategorias() {
    const data = await apiRequest("/categorias?page=0&size=100");
    const rows = Array.isArray(data) ? data : (data.content || []);
    categoriaSelect.innerHTML = '<option value="">— Elegir —</option>';
    for (const c of rows) {
        const opt = document.createElement("option");
        opt.value = c.id;
        opt.textContent = c.nombre;
        categoriaSelect.appendChild(opt);
    }
}

async function cargarTemplates() {
    const data = await apiRequest("/templates?page=0&size=100");
    const rows = Array.isArray(data) ? data : (data.content || []);
    tbody.innerHTML = "";
    for (const t of rows) {
        const tr = document.createElement("tr");
        tr.dataset.testid = "tpl-row-" + t.id;
        tr.innerHTML = `
            <td data-field="nombre">${t.nombre || ""}</td>
            <td data-field="temporada">${t.temporada || ""}</td>
            <td data-field="proveedor">${t.proveedor || ""}</td>
            <td data-field="precio">${t.precioBase ?? ""}</td>
            <td>
                <button type="button" class="secondary" data-testid="tpl-edit-${t.id}">Editar</button>
                <button type="button" class="danger" data-testid="tpl-delete-${t.id}">Eliminar</button>
            </td>
        `;
        tr.querySelector(`[data-testid="tpl-delete-${t.id}"]`).addEventListener("click", () => eliminarTemplate(t.id));
        tr.querySelector(`[data-testid="tpl-edit-${t.id}"]`).addEventListener("click", () => editarTemplate(t));
        tbody.appendChild(tr);
    }
}

async function eliminarTemplate(id) {
    try {
        await apiRequest("/templates/" + id, { method: "DELETE" });
        notify("success", "Producto eliminado.");
        cargarTemplates();
    } catch (ex) {
        notify("error", ex.message || "No se pudo eliminar.");
    }
}

async function editarTemplate(t) {
    const nuevoPrecio = prompt("Nuevo precio base para " + t.nombre, t.precioBase ?? "0");
    if (nuevoPrecio == null) return;
    try {
        await apiRequest("/templates/" + t.id, {
            method: "PUT",
            body: {
                nombre: t.nombre,
                temporada: t.temporada,
                proveedor: t.proveedor,
                precioBase: Number(nuevoPrecio),
                categoriaId: t.categoriaId
            }
        });
        notify("success", "Producto actualizado.");
        cargarTemplates();
    } catch (ex) {
        notify("error", ex.message || "No se pudo actualizar.");
    }
}

document.getElementById("tpl-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const body = {
        nombre: document.getElementById("tpl-nombre").value.trim(),
        temporada: document.getElementById("tpl-temporada").value.trim() || null,
        proveedor: document.getElementById("tpl-proveedor").value.trim() || null,
        precioBase: document.getElementById("tpl-precio").value
            ? Number(document.getElementById("tpl-precio").value) : null,
        categoriaId: document.getElementById("tpl-categoria").value || null
    };
    try {
        await apiRequest("/templates", { method: "POST", body });
        notify("success", "Producto creado: " + body.nombre);
        e.target.reset();
        cargarTemplates();
    } catch (ex) {
        notify("error", ex.message || ("Error " + ex.status));
    }
});

(async function init() {
    if (!getToken()) { window.location.href = "/index.html"; return; }
    try {
        await cargarCategorias();
        await cargarTemplates();
    } catch (ex) {
        if (ex.status === 401) { setToken(null); window.location.href = "/index.html"; return; }
        notify("error", ex.message || "No se pudo cargar el catálogo.");
    }
})();
