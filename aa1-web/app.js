const API_BASE = "/api";
const SESSION_KEY = "gestiondb-session";
const CART_KEY = "gestiondb-cart";

const PAGE_TITLES = {
  login: "Acceso",
  resumen: "Resumen",
  ventas: "Ventas",
  productos: "Productos",
  clientes: "Clientes",
  inventario: "Inventario",
  credito: "Credito",
  facturas: "Facturas",
  cortes: "Cortes",
  empleados: "Empleados"
};

const PAGE_AREAS = {
  resumen: "Tablero",
  ventas: "Operacion",
  productos: "Catalogo",
  clientes: "Relacion comercial",
  inventario: "Existencias",
  credito: "Cartera",
  facturas: "Documentos",
  cortes: "Caja",
  empleados: "Administracion"
};

const EMPLOYEE_ALLOWED_PAGES = new Set(["ventas", "clientes", "inventario", "credito", "facturas", "cortes"]);

const monthNames = [
  "",
  "Enero",
  "Febrero",
  "Marzo",
  "Abril",
  "Mayo",
  "Junio",
  "Julio",
  "Agosto",
  "Septiembre",
  "Octubre",
  "Noviembre",
  "Diciembre"
];

const page = document.body.dataset.page || "login";
const state = {
  session: loadSession(),
  cart: loadCart(),
  products: [],
  categories: [],
  clients: [],
  saleClientMatches: [],
  selectedSaleClient: null,
  saleClientSearchLoading: false,
  inventoryProductMatches: [],
  selectedInventoryCode: 0,
  methods: [],
  credits: [],
  payments: [],
  invoices: [],
  employees: [],
  selectedClientId: 0,
  selectedProductCode: 0,
  selectedCreditId: 0,
  selectedInvoiceId: 0,
  selectedEmployeeId: 0
};

function byId(id) {
  return document.getElementById(id);
}

function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY)) || null;
  } catch (_error) {
    return null;
  }
}

function saveSession(session) {
  state.session = session;
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function loadCart() {
  try {
    return JSON.parse(localStorage.getItem(CART_KEY)) || [];
  } catch (_error) {
    return [];
  }
}

function saveCart() {
  localStorage.setItem(CART_KEY, JSON.stringify(state.cart));
}

async function api(path, options = {}) {
  const request = {
    method: options.method || "GET",
    headers: {
      "Content-Type": "application/json"
    }
  };

  if (state.session?.employeeId) {
    request.headers["X-Employee-Id"] = String(state.session.employeeId);
  }

  if (options.body) {
    request.body = JSON.stringify(options.body);
  }

  const response = await fetch(`${API_BASE}${path}`, request);
  const data = await response.json();
  if (!response.ok || data.ok === false) {
    throw new Error(data.message || "No se pudo completar la operacion");
  }
  return data;
}

function redirectTo(pageName) {
  window.location.href = hrefForPage(pageName);
}

function hrefForPage(pageName) {
  const target = pageName === "login" ? "index.html" : `${pageName}.html`;
  return `./${target}`;
}

function ensureSession() {
  if (page !== "login" && !state.session) {
    redirectTo("login");
    return;
  }

  if (page !== "login" && state.session && !canAccessPage(page)) {
    redirectTo(defaultPageForSession());
  }
}

function isManager() {
  return Number(state.session?.roleId) === 1 || String(state.session?.role || "").toLowerCase() === "gerente";
}

function canAccessPage(pageName) {
  return isManager() || EMPLOYEE_ALLOWED_PAGES.has(pageName);
}

function defaultPageForSession() {
  return isManager() ? "resumen" : "ventas";
}

function formatMoney(value) {
  return new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value || 0));
}

function formatNumber(value, unit = "kg") {
  const number = Number(value || 0);
  return `${number.toFixed(2)} ${unit}`;
}

function round2(value) {
  return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
}

function formatDate(dateText) {
  if (!dateText) {
    return "-";
  }
  const date = new Date(`${String(dateText).slice(0, 10)}T12:00:00`);
  if (Number.isNaN(date.getTime())) {
    return dateText;
  }
  return new Intl.DateTimeFormat("es-CO", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(date);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function showToast(message, tone = "ok") {
  const toast = byId("toast");
  if (!toast) {
    return;
  }

  toast.textContent = message;
  toast.classList.remove("error", "show");
  if (tone === "error") {
    toast.classList.add("error");
  }

  window.clearTimeout(showToast.timer);
  requestAnimationFrame(() => toast.classList.add("show"));
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 3000);
}

function accessiblePageTrail() {
  const pages = [];
  document.querySelectorAll("[data-page-link]").forEach((link) => {
    const pageName = link.dataset.pageLink;
    if (!pageName || pages.includes(pageName) || !canAccessPage(pageName)) {
      return;
    }
    pages.push(pageName);
  });
  return pages;
}

function syncFlowBreadcrumb() {
  const headerContent = document.querySelector(".workspace-header > div");
  if (!headerContent) {
    return;
  }

  headerContent.classList.add("workspace-heading");

  const trail = accessiblePageTrail();
  const currentIndex = Math.max(trail.indexOf(page), 0);
  const totalSteps = trail.length || 1;
  const currentTitle = PAGE_TITLES[page] || "Panel";
  const areaLabel = PAGE_AREAS[page] || "Modulo";
  const homePage = defaultPageForSession();
  const breadcrumb = byId("flowBreadcrumb") || document.createElement("div");

  breadcrumb.id = "flowBreadcrumb";
  breadcrumb.className = "flow-breadcrumb-shell";
  breadcrumb.innerHTML = `
    <div class="flow-breadcrumb-meta">
      <span class="flow-badge">Ventana ${currentIndex + 1} de ${totalSteps}</span>
    </div>
    <nav class="flow-breadcrumb" aria-label="Ubicacion actual">
      <a class="flow-link" href="${hrefForPage(homePage)}">Inicio</a>
      <span class="flow-separator" aria-hidden="true">/</span>
      <span class="flow-area">${escapeHtml(areaLabel)}</span>
      <span class="flow-separator" aria-hidden="true">/</span>
      <span class="flow-current" aria-current="page">${escapeHtml(currentTitle)}</span>
    </nav>
  `;

  headerContent.appendChild(breadcrumb);
}

function syncShell() {
  const currentSection = byId("currentSection");
  const todayLabel = byId("todayLabel");
  const operatorLabel = byId("operatorLabel");

  if (currentSection) {
    currentSection.textContent = PAGE_TITLES[page] || "Panel";
  }

  if (todayLabel) {
    todayLabel.textContent = new Intl.DateTimeFormat("es-CO", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric"
    }).format(new Date());
  }

  if (operatorLabel) {
    const role = state.session?.role ? ` - ${state.session.role}` : "";
    operatorLabel.textContent = `${state.session?.name || "Operador"}${role}`;
  }

  document.querySelectorAll("[data-page-link]").forEach((link) => {
    const allowed = canAccessPage(link.dataset.pageLink);
    link.classList.toggle("hidden", !allowed);
    link.classList.toggle("active", link.dataset.pageLink === page);
  });

  syncFlowBreadcrumb();
}

function getProduct(code) {
  return state.products.find((product) => String(product.code) === String(code));
}

function getClient(id) {
  const clientId = Number(id);
  if (!clientId) {
    return null;
  }
  return state.clients.find((client) => Number(client.id) === clientId)
    || state.saleClientMatches.find((client) => Number(client.id) === clientId)
    || (Number(state.selectedSaleClient?.id) === clientId ? state.selectedSaleClient : null);
}

function getCartTotal() {
  return round2(state.cart.reduce((sum, item) => sum + item.quantity * item.price, 0));
}

function paymentName() {
  return byId("paymentMethod")?.value || "";
}

function renderOverview(data) {
  byId("statVentasDia").textContent = formatMoney(data.salesToday);
  byId("statCredito").textContent = formatMoney(data.totalDebt);
  byId("statBajoStock").textContent = String(data.lowStockCount);
  byId("statFacturas").textContent = String(data.invoiceCount);
  byId("overviewLowStockCount").textContent = `${data.lowStock.length} activos`;

  byId("overviewLowStock").innerHTML = data.lowStock.length
    ? data.lowStock
        .map(
          (product) => `
            <div class="stack-item">
              <div>
                <strong>${escapeHtml(product.name)}</strong>
                <span class="subtle">${escapeHtml(product.code)} - ${escapeHtml(product.category)}</span>
              </div>
              <strong class="tone-danger">${formatNumber(product.stock)}</strong>
            </div>
          `
        )
        .join("")
    : '<div class="stack-item"><strong>Todo el stock esta saludable.</strong></div>';

  byId("overviewInvoices").innerHTML = data.recentInvoices.length
    ? data.recentInvoices
        .map(
          (invoice) => `
            <div class="stack-item">
              <div>
                <strong>FAC-${escapeHtml(invoice.id)}</strong>
                <span class="subtle">${escapeHtml(invoice.client || "Cliente")} - ${formatDate(invoice.date)}</span>
              </div>
              <strong>${formatMoney(invoice.total)}</strong>
            </div>
          `
        )
        .join("")
    : '<div class="stack-item"><strong>No hay facturas registradas.</strong></div>';
}

async function loadOverview() {
  renderOverview(await api("/resumen"));
}

function fillProductSelects() {
  const productCategory = byId("productCategory");
  const productFilter = byId("productFilter");
  if (!productCategory || !productFilter) {
    return;
  }

  const currentCategory = productCategory.value;
  const currentFilter = productFilter.value || "Todos";
  productCategory.innerHTML = '<option value="">Seleccionar</option>' +
    state.categories
      .map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`)
      .join("");
  productFilter.innerHTML = '<option value="Todos">Todos</option>' +
    state.categories
      .map((category) => `<option value="${category.id}">${escapeHtml(category.name)}</option>`)
      .join("");
  productCategory.value = currentCategory;
  productFilter.value = currentFilter;
}

function renderProducts() {
  fillProductSelects();
  const filter = byId("productFilter").value;
  const products = filter === "Todos"
    ? state.products
    : state.products.filter((product) => String(product.categoryId) === String(filter));

  byId("productCount").textContent = String(state.products.length);
  byId("productCategoryCount").textContent = String(state.categories.length);
  byId("productLowCount").textContent = String(state.products.filter((product) => product.stock < 10).length);

  byId("productsTable").innerHTML = products.length
    ? products
        .map((product) => {
          const stockClass = product.stock < 10 ? "low" : "good";
          return `
            <tr class="invoice-row ${Number(product.code) === Number(state.selectedProductCode) ? "active" : ""}" data-product-pick="${product.code}">
              <td>${escapeHtml(product.code)}</td>
              <td>
                <strong>${escapeHtml(product.name)}</strong>
                <div class="subtle">${escapeHtml(product.description)}</div>
              </td>
              <td>${escapeHtml(product.category)}</td>
              <td>${formatMoney(product.price)}</td>
              <td><span class="stock-pill ${stockClass}">${formatNumber(product.stock)}</span></td>
            </tr>
          `;
        })
        .join("")
    : '<tr><td colspan="5">No hay productos para este filtro.</td></tr>';
}

function productPayloadFromForm(action) {
  return {
    action,
    code: Number(byId("productCode").value),
    name: byId("productName").value.trim(),
    categoryId: Number(byId("productCategory").value),
    price: Number(byId("productPrice").value),
    stock: Number(byId("productStock").value),
    description: byId("productDescription").value.trim()
  };
}

function validateProductPayload(payload) {
  if (!payload.code || !payload.name || !payload.categoryId || !payload.price || payload.stock < 0 || !payload.description) {
    showToast("Completa todos los datos del producto.", "error");
    return false;
  }
  return true;
}

function resetProductForm() {
  byId("productForm").reset();
  byId("productUnit").value = "kg";
  byId("productCode").disabled = false;
  byId("saveProductButton").textContent = "Agregar producto";
  byId("saveProductButton").disabled = false;
  state.selectedProductCode = 0;
  renderProducts();
}

function loadProductIntoForm(code) {
  const product = getProduct(code);
  if (!product) {
    return;
  }

  state.selectedProductCode = Number(product.code);
  byId("productCode").value = product.code;
  byId("productCode").disabled = true;
  byId("productName").value = product.name;
  byId("productCategory").value = String(product.categoryId);
  byId("productPrice").value = product.price;
  byId("productStock").value = product.stock;
  byId("productUnit").value = product.unit || "kg";
  byId("productDescription").value = product.description;
  byId("saveProductButton").textContent = "Producto seleccionado";
  byId("saveProductButton").disabled = true;
  renderProducts();
}

async function loadProducts() {
  const data = await api("/productos");
  state.products = data.products;
  state.categories = data.categories;
  renderProducts();
}

async function createProductFromForm() {
  const payload = productPayloadFromForm("create");
  if (!validateProductPayload(payload)) {
    return;
  }

  const result = await api("/productos", {
    method: "POST",
    body: payload
  });

  resetProductForm();
  await loadProducts();
  showToast(result.message || "Producto guardado.");
}

async function updateProductFromForm() {
  const payload = productPayloadFromForm("update");
  payload.code = Number(state.selectedProductCode || payload.code);
  if (!validateProductPayload(payload)) {
    return;
  }

  const result = await api("/productos", {
    method: "POST",
    body: payload
  });

  await loadProducts();
  loadProductIntoForm(payload.code);
  showToast(result.message || "Producto actualizado.");
}

function renderClients() {
  const search = byId("clientSearch").value.trim().toLowerCase();
  const visibleClients = state.clients.filter((client) => {
    const haystack = `${client.name} ${client.document}`.toLowerCase();
    return !search || haystack.includes(search);
  });

  if (!visibleClients.some((client) => client.id === state.selectedClientId) && visibleClients.length) {
    state.selectedClientId = visibleClients[0].id;
  }

  byId("clientsList").innerHTML = visibleClients.length
    ? visibleClients
        .map(
          (client) => `
            <button class="client-item ${client.id === state.selectedClientId ? "active" : ""}" type="button" data-client-pick="${client.id}">
              <strong>${escapeHtml(client.name)}</strong>
              <span class="subtle">${escapeHtml(client.document)} - ${escapeHtml(client.phone)}</span>
            </button>
          `
        )
        .join("")
    : '<div class="stack-item"><strong>No hay clientes para esta busqueda.</strong></div>';

  const selected = getClient(state.selectedClientId);
  if (!selected) {
    return;
  }

  byId("clientName").textContent = selected.name;
  byId("clientDocument").textContent = selected.document;
  byId("clientPhone").textContent = selected.phone || "-";
  byId("clientAddress").textContent = selected.address || "-";
  byId("clientDebt").textContent = formatMoney(selected.debt);
  byId("clientCreditState").textContent = selected.creditEnabled ? "Credito habilitado" : "Credito bloqueado";
  byId("toggleCreditButton").textContent = selected.creditEnabled
    ? "Bloquear credito del cliente"
    : "Habilitar credito del cliente";
}

async function loadClients() {
  const data = await api("/clientes");
  state.clients = data.clients;
  if (!state.selectedClientId && state.clients.length) {
    state.selectedClientId = state.clients[0].id;
  }
  renderClients();
}

async function createClient(payload) {
  return api("/clientes", {
    method: "POST",
    body: {
      action: "create",
      ...payload
    }
  });
}

async function updateClient(payload) {
  return api("/clientes", {
    method: "POST",
    body: {
      action: "update",
      ...payload
    }
  });
}

function resetClientForm() {
  const form = byId("clientForm");
  form.reset();
  byId("clientFormId").value = "";
  byId("saveClientButton").textContent = "Guardar cliente";
}

function toggleClientForm(forceOpen) {
  const form = byId("clientForm");
  const button = byId("toggleClientFormButton");
  const open = typeof forceOpen === "boolean" ? forceOpen : form.classList.contains("hidden");
  form.classList.toggle("hidden", !open);
  button.textContent = open ? "Ocultar formulario" : "Agregar nuevo cliente";
  if (!open) {
    resetClientForm();
    return;
  }
  byId("clientFormName").focus();
}

function loadSelectedClientIntoForm() {
  const client = getClient(state.selectedClientId);
  if (!client) {
    showToast("Selecciona un cliente para editar.", "error");
    return;
  }

  toggleClientForm(true);
  byId("clientFormId").value = client.id;
  byId("clientFormName").value = client.name;
  byId("clientFormDocument").value = client.document;
  byId("clientFormPhone").value = client.phone || "";
  byId("clientFormAddress").value = client.address || "";
  byId("clientFormCredit").checked = Boolean(client.creditEnabled);
  byId("saveClientButton").textContent = "Actualizar cliente";
}

function applyClientRouteState() {
  if (page !== "clientes") {
    return;
  }

  const params = new URLSearchParams(window.location.search);
  if (params.get("openCreate") !== "1") {
    return;
  }

  const document = params.get("document") || "";
  toggleClientForm(true);
  resetClientForm();
  byId("clientFormDocument").value = document;

  if (document) {
    byId("clientFormName").focus();
  } else {
    byId("clientFormDocument").focus();
  }
}

async function saveClientFromForm() {
  const clientId = Number(byId("clientFormId").value || 0);
  const payload = {
    clientId,
    name: byId("clientFormName").value.trim(),
    document: byId("clientFormDocument").value.trim(),
    phone: byId("clientFormPhone").value.trim(),
    address: byId("clientFormAddress").value.trim(),
    creditEnabled: byId("clientFormCredit").checked
  };

  if (!payload.document || (clientId && !payload.name)) {
    showToast(clientId
      ? "Completa nombre y documento del cliente."
      : "Completa al menos el documento del cliente.", "error");
    return;
  }

  const result = clientId ? await updateClient(payload) : await createClient(payload);
  if (result.clientId) {
    state.selectedClientId = Number(result.clientId);
  }
  await loadClients();
  toggleClientForm(false);
  showToast(result.message || "Cliente guardado.");
}

async function toggleSelectedClientCredit() {
  const client = getClient(state.selectedClientId);
  if (!client) {
    return;
  }

  const result = await api("/clientes", {
    method: "POST",
    body: {
      action: "toggleCredit",
      clientId: client.id,
      enabled: !client.creditEnabled
    }
  });
  await loadClients();
  showToast(result.message || "Cliente actualizado.");
}

function renderInventory() {
  byId("lowStockTable").innerHTML = state.lowStock?.length
    ? state.lowStock
        .map(
          (product) => `
            <tr>
              <td>${escapeHtml(product.code)}</td>
              <td>${escapeHtml(product.name)}</td>
              <td class="tone-danger">${formatNumber(product.stock)}</td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="3">No hay alertas criticas.</td></tr>';

  const selected = getProduct(state.selectedInventoryCode);
  if (!selected) {
    state.selectedInventoryCode = 0;
    byId("inventoryProductSelect").value = "";
  } else {
    byId("inventoryProductSelect").value = String(selected.code);
  }

  byId("inventoryCode").textContent = selected?.code || "-";
  byId("inventoryCurrentStock").textContent = selected ? formatNumber(selected.stock) : "-";

  byId("inventoryProductMatches").innerHTML = !selected && state.inventoryProductMatches.length
    ? state.inventoryProductMatches
        .map(
          (product) => `
            <button class="client-item" type="button" data-inventory-product-pick="${product.code}">
              <strong>${escapeHtml(product.name)}</strong>
              <span class="subtle">${escapeHtml(product.code)} - ${formatNumber(product.stock)}</span>
            </button>
          `
        )
        .join("")
    : "";

  byId("inventoryMovements").innerHTML = state.movements?.length
    ? state.movements
        .map((movement) => {
          const isSale = String(movement.motive).toLowerCase().includes("venta");
          const raw = Number(movement.quantity || 0);
          const sign = isSale || raw < 0 ? "-" : "+";
          return `
            <tr>
              <td>${escapeHtml(String(movement.date).slice(0, 16))}</td>
              <td>${escapeHtml(movement.productCode)}</td>
              <td>${escapeHtml(movement.product || "Producto")}</td>
              <td>${escapeHtml(movement.motive)}</td>
              <td>${sign}${formatNumber(Math.abs(raw))}</td>
            </tr>
          `;
        })
        .join("")
    : '<tr><td colspan="5">No hay movimientos registrados.</td></tr>';
}

async function loadInventory() {
  const data = await api("/inventario");
  state.products = data.products;
  state.lowStock = data.lowStock;
  state.movements = data.movements;

  if (!state.products.some((product) => Number(product.code) === Number(state.selectedInventoryCode))) {
    state.selectedInventoryCode = 0;
    state.inventoryProductMatches = [];
  }

  renderInventory();
}

async function updateInventory() {
  const selectedCode = Number(byId("inventoryProductSelect").value || state.selectedInventoryCode || 0);
  if (!selectedCode) {
    showToast("Busca y selecciona un producto antes de actualizar stock.", "error");
    return;
  }

  const result = await api("/inventario", {
    method: "POST",
    body: {
      code: selectedCode,
      mode: byId("inventoryMode").value,
      amount: Number(byId("inventoryAmount").value)
    }
  });
  byId("inventoryAmount").value = "";
  await loadInventory();
  showToast(result.message || "Inventario actualizado.");
}

function renderCredits() {
  const openCredits = state.credits.filter((credit) => Number(credit.balance) > 0);
  byId("creditClientCount").textContent = String(new Set(openCredits.map((credit) => credit.clientId)).size);
  byId("creditTotalDebt").textContent = formatMoney(openCredits.reduce((sum, credit) => sum + Number(credit.balance), 0));
  byId("creditBlockedCount").textContent = String(state.blockedCount || 0);

  if (!state.selectedCreditId && state.credits.length) {
    state.selectedCreditId = state.credits[0].id;
  }

  byId("creditsTable").innerHTML = state.credits.length
    ? state.credits
        .map(
          (credit) => `
            <tr class="invoice-row ${Number(credit.id) === Number(state.selectedCreditId) ? "active" : ""}" data-credit-pick="${credit.id}">
              <td>
                <strong>${escapeHtml(credit.client)}</strong>
                <div class="subtle">FAC-${escapeHtml(credit.invoiceId)} - vence ${formatDate(credit.endDate)}</div>
              </td>
              <td>${escapeHtml(credit.document)}</td>
              <td>${formatMoney(credit.balance)}</td>
              <td><span class="stock-pill ${credit.state === "PAGADO" ? "good" : "low"}">${escapeHtml(credit.state)}</span></td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="4">No hay creditos registrados.</td></tr>';

  const selector = byId("creditClientSelect");
  const previous = selector.value || state.selectedCreditId;
  selector.innerHTML = state.credits.length
    ? state.credits
        .map(
          (credit) => `<option value="${credit.id}">${escapeHtml(credit.client)} - FAC-${credit.invoiceId} - ${formatMoney(credit.balance)}</option>`
        )
        .join("")
    : '<option value="">Sin creditos</option>';
  selector.value = state.credits.some((credit) => String(credit.id) === String(previous))
    ? previous
    : String(state.credits[0]?.id || "");
  state.selectedCreditId = Number(selector.value || 0);

  const selected = state.credits.find((credit) => Number(credit.id) === Number(state.selectedCreditId));
  const canPay = selected && Number(selected.balance) > 0;
  byId("creditPayment").disabled = !canPay;
  byId("applyCreditButton").disabled = !canPay;
  byId("settleCreditButton").disabled = !canPay;

  const mode = byId("creditHistoryMode")?.value || "credit";
  const search = byId("creditPaymentSearch")?.value.trim().toLowerCase() || "";
  const selectedCredit = state.credits.find((credit) => Number(credit.id) === Number(state.selectedCreditId));
  let history = state.payments;
  if (mode === "credit" && state.selectedCreditId) {
    history = history.filter((payment) => Number(payment.creditId) === Number(state.selectedCreditId));
  }
  if (mode === "client" && selectedCredit) {
    history = history.filter((payment) => Number(payment.clientId) === Number(selectedCredit.clientId));
  }
  if (search) {
    history = history.filter((payment) => {
      const haystack = [
        payment.client,
        payment.document,
        payment.invoiceId,
        payment.creditId,
        payment.paymentId
      ].join(" ").toLowerCase();
      return haystack.includes(search) || `fac-${payment.invoiceId}`.includes(search);
    });
  }
  byId("creditHistoryCount").textContent = `${history.length} movimientos`;
  byId("creditHistoryTable").innerHTML = history.length
    ? history
        .map(
          (payment) => `
            <tr>
              <td>${formatDate(payment.date)}</td>
              <td>${escapeHtml(payment.client)}</td>
              <td>${escapeHtml(payment.document)}</td>
              <td>FAC-${escapeHtml(payment.invoiceId)}</td>
              <td>${formatMoney(payment.amount)}</td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="5">No hay abonos para esta busqueda.</td></tr>';
}

async function loadCredits() {
  const data = await api("/credito");
  state.credits = data.credits;
  state.payments = data.payments || [];
  state.blockedCount = data.blockedCount;
  if (!state.credits.some((credit) => Number(credit.id) === Number(state.selectedCreditId))) {
    state.selectedCreditId = state.credits[0]?.id || 0;
  }
  renderCredits();
}

async function applyCreditPayment(settleFull = false) {
  const creditId = Number(byId("creditClientSelect").value);
  const credit = state.credits.find((item) => Number(item.id) === creditId);
  if (!credit) {
    showToast("Selecciona un credito.", "error");
    return;
  }
  if (Number(credit.balance) <= 0) {
    showToast("Este credito ya esta saldado.", "error");
    return;
  }

  const amount = settleFull ? Number(credit.balance) : Number(byId("creditPayment").value);
  if (!amount || amount <= 0) {
    showToast("Ingresa un abono valido.", "error");
    return;
  }

  const result = await api("/credito", {
    method: "POST",
    body: {
      creditId,
      amount
    }
  });
  byId("creditPayment").value = "";
  await loadCredits();
  showToast(result.message || "Abono registrado.");
}

async function blockOverdueClients() {
  const result = await api("/credito", {
    method: "POST",
    body: {
      action: "refreshOverdue"
    }
  });
  await loadCredits();
  showToast(`${result.updated || 0} creditos vencidos revisados.`);
}

function invoiceFilters() {
  return new URLSearchParams({
    month: byId("invoiceMonth").value,
    year: byId("invoiceYear").value,
    search: byId("invoiceSearch").value.trim(),
    id: state.selectedInvoiceId || ""
  });
}

function renderInvoices(detail) {
  byId("invoiceCountLabel").textContent = String(state.invoices.length);
  byId("invoicesTable").innerHTML = state.invoices.length
    ? state.invoices
        .map(
          (invoice) => `
            <tr class="invoice-row ${invoice.id === state.selectedInvoiceId ? "active" : ""}" data-invoice-pick="${invoice.id}">
              <td>FAC-${escapeHtml(invoice.id)}</td>
              <td>${formatDate(invoice.date)}</td>
              <td>${escapeHtml(invoice.client || "Cliente")}</td>
              <td>${escapeHtml(invoice.itemsCount)}</td>
              <td>${formatMoney(invoice.total)}</td>
              <td>${escapeHtml(invoice.payment)}</td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="6">No hay facturas para este filtro.</td></tr>';

  if (!detail) {
    byId("invoiceDetails").innerHTML = `
      <div class="invoice-card">
        <h3>Sin factura</h3>
        <p class="subtle">Aun no hay informacion para mostrar en el detalle.</p>
      </div>
    `;
    return;
  }

  byId("invoiceDetails").innerHTML = `
    <div class="invoice-card">
      <h3>FAC-${escapeHtml(detail.id)}</h3>
      <div class="subtle">${formatDate(detail.date)} - ${escapeHtml(detail.employee || "Empleado")}</div>
      <div class="subtle">${escapeHtml(detail.client || "Cliente")} - ${escapeHtml(detail.document || "")}</div>
      <div class="invoice-lines">
        ${(detail.items || [])
          .map(
            (item) => `
              <div class="invoice-line">
                <div>
                  <strong>${escapeHtml(item.name)}</strong>
                  <div class="subtle">${formatNumber(item.quantity)} - ${formatMoney(item.price)}</div>
                </div>
                <strong>${formatMoney(item.subtotal)}</strong>
              </div>
            `
          )
          .join("")}
      </div>
      <div class="invoice-lines">
        <div class="invoice-line">
          <span>Metodo de pago</span>
          <strong>${escapeHtml(detail.payment)}</strong>
        </div>
        <div class="invoice-line">
          <span>Total</span>
          <strong>${formatMoney(detail.total)}</strong>
        </div>
        <div class="invoice-line">
          <span>Recibido</span>
          <strong>${formatMoney(detail.total)}</strong>
        </div>
        <div class="invoice-line">
          <span>Cambio</span>
          <strong>${formatMoney(0)}</strong>
        </div>
      </div>
      ${
        isManager()
          ? `<div class="button-row"><button class="remove-button full" type="button" data-annul-invoice="${escapeHtml(detail.id)}">Anular factura</button></div>`
          : ""
      }
    </div>
  `;
}

async function loadInvoices() {
  const data = await api(`/facturas?${invoiceFilters().toString()}`);
  state.invoices = data.invoices;
  state.selectedInvoiceId = data.detail?.id || state.invoices[0]?.id || 0;
  renderInvoices(data.detail);
}

async function annulInvoice(invoiceId) {
  if (!invoiceId) {
    return;
  }
  const confirmed = window.confirm(`Anular FAC-${invoiceId}?`);
  if (!confirmed) {
    return;
  }

  const result = await api("/facturas", {
    method: "POST",
    body: {
      action: "annul",
      invoiceId
    }
  });
  state.selectedInvoiceId = 0;
  await loadInvoices();
  showToast(result.message || "Factura anulada.");
}

function renderCuts(data) {
  const summary = data.summary || {};
  byId("monthlyRevenue").textContent = formatMoney(summary.revenue);
  byId("monthlyTickets").textContent = String(summary.tickets || 0);
  byId("monthlyAverage").textContent = formatMoney(summary.average);
  byId("monthlyCreditSales").textContent = formatMoney(summary.creditSales);

  const maxTotal = Math.max(...(data.payments || []).map((row) => Number(row.total)), 1);
  byId("cutChart").innerHTML = data.payments?.length
    ? data.payments
        .map(
          (row) => `
            <div class="chart-row">
              <header>
                <strong>${escapeHtml(row.payment)}</strong>
                <span>${formatMoney(row.total)}</span>
              </header>
              <div class="chart-track">
                <div class="chart-fill" style="width: ${(Number(row.total) / maxTotal) * 100}%"></div>
              </div>
            </div>
          `
        )
        .join("")
    : '<div class="stack-item"><strong>No hay ventas para el periodo.</strong></div>';

  const insights = [
    {
      title: "Ticket promedio",
      detail: summary.tickets
        ? `El periodo tiene un promedio de ${formatMoney(summary.average)} por factura.`
        : "No hay facturas en el periodo seleccionado."
    },
    {
      title: "Cartera del periodo",
      detail: summary.creditSales
        ? `${formatMoney(summary.creditSales)} del periodo se fue a credito.`
        : "No hubo ventas a credito en el periodo."
    },
    {
      title: "Reposicion",
      detail: data.lowStockCount
        ? `${data.lowStockCount} productos necesitan reposicion.`
        : "El nivel de inventario se ve estable."
    }
  ];

  if (data.topInvoice) {
    insights.push({
      title: "Factura mas alta",
      detail: `FAC-${data.topInvoice.id} para ${data.topInvoice.client || "cliente"} por ${formatMoney(data.topInvoice.total)}.`
    });
  }

  byId("cutInsights").innerHTML = insights
    .map(
      (item) => `
        <div class="stack-item">
          <div>
            <strong>${escapeHtml(item.title)}</strong>
            <span class="subtle">${escapeHtml(item.detail)}</span>
          </div>
        </div>
      `
    )
    .join("");
}

async function loadCuts() {
  const params = new URLSearchParams({
    month: byId("cutMonth").value,
    year: byId("cutYear").value
  });
  renderCuts(await api(`/cortes?${params.toString()}`));
}

function fillSalesSelects() {
  const methodSelect = byId("paymentMethod");
  if (!methodSelect) {
    return;
  }
  const currentMethod = methodSelect.value;
  methodSelect.innerHTML = '<option value="">Seleccionar</option>' +
    state.methods
      .map((method) => `<option value="${escapeHtml(method.name)}">${escapeHtml(method.name)}</option>`)
      .join("");
  methodSelect.value = currentMethod;
}

function saleClientLabel(client) {
  return client ? `${client.name} - ${client.document}` : "";
}

function normalizeSaleClient(client) {
  if (!client) {
    return null;
  }
  return {
    id: Number(client.id || 0),
    name: client.name || "",
    document: client.document || "",
    phone: client.phone || "",
    address: client.address || "",
    creditEnabled: Boolean(client.creditEnabled),
    debt: Number(client.debt || 0)
  };
}

function setSelectedSaleClient(client) {
  state.selectedSaleClient = normalizeSaleClient(client);
  state.saleClientMatches = [];
  state.saleClientSearchLoading = false;
  byId("customerSelect").value = state.selectedSaleClient ? String(state.selectedSaleClient.id) : "";
  byId("customerSearch").value = saleClientLabel(state.selectedSaleClient);
}

function clearSelectedSaleClient(keepQuery = true) {
  state.selectedSaleClient = null;
  byId("customerSelect").value = "";
  if (!keepQuery) {
    byId("customerSearch").value = "";
  }
}

function renderSaleClientLookup() {
  const summary = byId("selectedCustomerSummary");
  const matches = byId("customerMatches");
  if (!summary || !matches) {
    return;
  }

  const selected = state.selectedSaleClient;
  summary.innerHTML = selected
    ? `
        <div class="stack-item">
          <div>
            <strong>${escapeHtml(selected.name || "Cliente seleccionado")}</strong>
            <span class="subtle">${escapeHtml(selected.document)} - ${selected.creditEnabled ? "Credito habilitado" : "Credito bloqueado"}</span>
          </div>
          <strong>${formatMoney(selected.debt)}</strong>
        </div>
      `
    : '<div class="stack-item"><strong>Busca un cliente por nombre o documento para continuar.</strong></div>';

  const query = byId("customerSearch").value.trim();
  if (!query) {
    matches.innerHTML = "";
    return;
  }

  if (query.length < 2) {
    matches.innerHTML = "";
    return;
  }

  if (state.saleClientSearchLoading) {
    matches.innerHTML = '<div class="stack-item"><strong>Buscando clientes...</strong></div>';
    return;
  }

  if (selected && query === saleClientLabel(selected) && !state.saleClientMatches.length) {
    matches.innerHTML = "";
    return;
  }

  matches.innerHTML = state.saleClientMatches.length
    ? state.saleClientMatches
        .map(
          (client) => `
            <button class="client-item" type="button" data-sale-client-pick="${client.id}">
              <strong>${escapeHtml(client.name)}</strong>
              <span class="subtle">${escapeHtml(client.document)} - ${client.creditEnabled ? "Credito habilitado" : "Credito bloqueado"}</span>
            </button>
          `
        )
        .join("")
    : "";
}

async function searchSaleClients(query, requestId) {
  const data = await api(`/clientes?search=${encodeURIComponent(query)}&limit=3`);
  if (requestId !== searchSaleClients.requestId) {
    return;
  }

  state.saleClientMatches = data.clients || [];
  state.saleClientSearchLoading = false;
  renderSales();
}

function scheduleSaleClientSearch() {
  const query = byId("customerSearch").value.trim();
  window.clearTimeout(scheduleSaleClientSearch.timer);
  searchSaleClients.requestId = (searchSaleClients.requestId || 0) + 1;
  const requestId = searchSaleClients.requestId;

  clearSelectedSaleClient(true);
  state.saleClientMatches = [];
  state.saleClientSearchLoading = false;

  if (!query) {
    renderSales();
    return;
  }

  if (query.length < 2) {
    renderSales();
    return;
  }

  state.saleClientSearchLoading = true;
  renderSales();

  scheduleSaleClientSearch.timer = window.setTimeout(() => {
    searchSaleClients(query, requestId).catch((error) => {
      if (requestId === searchSaleClients.requestId && query === byId("customerSearch").value.trim()) {
        state.saleClientSearchLoading = false;
        state.saleClientMatches = [];
        renderSales();
      }
      showToast(error.message, "error");
    });
  }, 220);
}

function inventoryProductLabel(product) {
  return product ? `${product.code} - ${product.name}` : "";
}

function findInventoryProductMatches(query) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return [];
  }

  return state.products
    .map((product) => {
      const code = String(product.code || "").toLowerCase();
      const name = String(product.name || "").toLowerCase();
      let priority = -1;

      if (code === normalized) {
        priority = 0;
      } else if (code.startsWith(normalized)) {
        priority = 1;
      } else if (name.startsWith(normalized)) {
        priority = 2;
      } else if (name.includes(normalized)) {
        priority = 3;
      }

      return priority >= 0 ? { product, priority } : null;
    })
    .filter(Boolean)
    .sort((left, right) => left.priority - right.priority || String(left.product.name).localeCompare(String(right.product.name)))
    .slice(0, 3)
    .map((entry) => entry.product);
}

function setSelectedInventoryProduct(product) {
  state.selectedInventoryCode = Number(product?.code || 0);
  state.inventoryProductMatches = [];
  byId("inventoryProductSelect").value = state.selectedInventoryCode ? String(state.selectedInventoryCode) : "";
  byId("inventoryProductSearch").value = inventoryProductLabel(product);
  renderInventory();
}

function refreshInventoryProductSearch() {
  const query = byId("inventoryProductSearch").value.trim();
  const selected = getProduct(state.selectedInventoryCode);

  if (!query) {
    state.selectedInventoryCode = 0;
    state.inventoryProductMatches = [];
    byId("inventoryProductSelect").value = "";
    renderInventory();
    return;
  }

  if (selected && query === inventoryProductLabel(selected)) {
    state.inventoryProductMatches = [];
    renderInventory();
    return;
  }

  state.selectedInventoryCode = 0;
  byId("inventoryProductSelect").value = "";
  state.inventoryProductMatches = findInventoryProductMatches(query);
  renderInventory();
}

function renderSales() {
  fillSalesSelects();
  renderSaleClientLookup();
  const total = getCartTotal();
  const method = paymentName();
  const selectedClient = state.selectedSaleClient || getClient(Number(byId("customerSelect").value));
  const creditBlocked = method === "Credito" && selectedClient && !selectedClient.creditEnabled;
  const received = method === "Efectivo" ? Number(byId("cashInput").value || 0) : total;
  const change = method === "Efectivo" ? Math.max(received - total, 0) : 0;

  byId("cartItems").textContent = String(state.cart.length);
  byId("cartTotal").textContent = formatMoney(total);
  byId("cartChange").textContent = formatMoney(change);
  byId("paymentTotal").textContent = formatMoney(total);
  byId("paymentReceived").textContent = formatMoney(received);
  byId("paymentChange").textContent = formatMoney(change);
  byId("saleSummaryBadge").textContent = creditBlocked ? "Credito bloqueado" : `${state.cart.length} productos`;
  byId("lastInvoiceLabel").textContent = state.lastInvoiceId ? `FAC-${state.lastInvoiceId}` : "Sin emitir";
  byId("cashWrap").classList.toggle("hidden", method !== "Efectivo");
  byId("checkoutButton").disabled = creditBlocked;
  byId("checkoutButton").title = creditBlocked
    ? "El cliente tiene el credito bloqueado"
    : "";

  byId("productSuggestions").innerHTML = state.products
    .filter((product) => Number(product.stock) > 0)
    .slice(0, 6)
    .map(
      (product) => `
        <button type="button" data-code-pick="${product.code}">
          ${escapeHtml(product.name)}
        </button>
      `
    )
    .join("");

  byId("salesTable").innerHTML = state.cart.length
    ? state.cart
        .map(
          (item) => `
            <tr>
              <td>${escapeHtml(item.code)}</td>
              <td>${escapeHtml(item.name)}</td>
              <td>${formatMoney(item.price)}</td>
              <td>${formatNumber(item.quantity)}</td>
              <td>${formatMoney(round2(item.quantity * item.price))}</td>
              <td>
                <button class="remove-button" type="button" data-remove-cart="${item.code}">
                  Quitar
                </button>
              </td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="6">No hay productos en la venta actual.</td></tr>';
}

async function loadSales() {
  const data = await api("/ventas");
  state.products = data.products;
  state.methods = data.methods;
  state.lastInvoiceId = data.lastInvoiceId;
  renderSales();
}

function addProductToCart(rawQuery, rawQuantity) {
  const query = rawQuery.trim().toLowerCase();
  if (!query) {
    showToast("Escribe un codigo o nombre para agregar el producto.", "error");
    return;
  }

  const product = state.products.find((item) =>
    String(item.code).toLowerCase() === query || String(item.name).toLowerCase().includes(query)
  );
  if (!product) {
    showToast("No se encontro el producto indicado.", "error");
    return;
  }

  const quantity = round2(rawQuantity);
  if (!Number.isFinite(quantity) || quantity <= 0) {
    showToast("Ingresa una cantidad valida.", "error");
    return;
  }

  const reserved = state.cart
    .filter((item) => String(item.code) === String(product.code))
    .reduce((sum, item) => sum + Number(item.quantity), 0);
  if (reserved + quantity > Number(product.stock)) {
    showToast("La cantidad supera el stock disponible.", "error");
    return;
  }

  const existing = state.cart.find((item) => String(item.code) === String(product.code));
  if (existing) {
    existing.quantity = round2(existing.quantity + quantity);
  } else {
    state.cart.push({
      code: product.code,
      name: product.name,
      price: Number(product.price),
      quantity: round2(quantity)
    });
  }

  byId("saleCode").value = "";
  byId("saleQty").value = "1";
  saveCart();
  renderSales();
  showToast(`${product.name} agregado a la venta.`);
}

function clearSale() {
  state.cart = [];
  byId("saleCode").value = "";
  byId("saleQty").value = "1";
  byId("cashInput").value = "";
  byId("paymentMethod").value = "";
  saveCart();
  renderSales();
}

async function checkout() {
  if (!state.cart.length) {
    showToast("Agrega productos antes de facturar.", "error");
    return;
  }

  const clientId = Number(byId("customerSelect").value);
  const payment = paymentName();
  if (!clientId || !payment) {
    showToast("Selecciona cliente y metodo de pago.", "error");
    return;
  }
  const client = getClient(clientId);
  if (payment === "Credito" && client && !client.creditEnabled) {
    showToast("Este cliente tiene el credito bloqueado.", "error");
    return;
  }

  const result = await api("/ventas", {
    method: "POST",
    body: {
      clientId,
      payment,
      employeeId: state.session?.employeeId || 0,
      received: payment === "Efectivo" ? Number(byId("cashInput").value || 0) : getCartTotal(),
      items: state.cart.map((item) => ({
        code: item.code,
        quantity: item.quantity
      }))
    }
  });

  state.lastInvoiceId = Number(result.invoiceId || state.lastInvoiceId || 0);
  clearSale();
  await loadSales();
  showToast(`Factura FAC-${result.invoiceId} creada.`);
}

function redirectToClientCreate() {
  const params = new URLSearchParams();
  params.set("openCreate", "1");

  const typedDocument = byId("customerSearch")?.value.trim() || "";
  if (typedDocument) {
    params.set("document", typedDocument);
  }

  window.location.href = `./clientes.html?${params.toString()}`;
}

function attachSalesEvents() {
  byId("salesForm").addEventListener("submit", (event) => {
    event.preventDefault();
    addProductToCart(byId("saleCode").value, byId("saleQty").value);
  });

  byId("scaleButton").addEventListener("click", () => {
    byId("saleQty").value = (Math.random() * 3.7 + 0.6).toFixed(2);
  });

  byId("productSuggestions").addEventListener("click", (event) => {
    const button = event.target.closest("[data-code-pick]");
    if (!button) {
      return;
    }
    byId("saleCode").value = button.dataset.codePick;
    byId("saleQty").value = "1";
  });

  byId("salesTable").addEventListener("click", (event) => {
    const button = event.target.closest("[data-remove-cart]");
    if (!button) {
      return;
    }
    state.cart = state.cart.filter((item) => String(item.code) !== String(button.dataset.removeCart));
    saveCart();
    renderSales();
  });

  byId("customerSearch").addEventListener("input", scheduleSaleClientSearch);
  byId("customerMatches").addEventListener("click", (event) => {
    const button = event.target.closest("[data-sale-client-pick]");
    if (!button) {
      return;
    }
    const client = state.saleClientMatches.find((item) => String(item.id) === String(button.dataset.saleClientPick));
    if (!client) {
      return;
    }
    setSelectedSaleClient(client);
    renderSales();
  });
  byId("paymentMethod").addEventListener("change", () => {
    const client = state.selectedSaleClient || getClient(Number(byId("customerSelect").value));
    if (paymentName() === "Credito" && client && !client.creditEnabled) {
      showToast("Este cliente no tiene credito habilitado.", "error");
    }
    renderSales();
  });
  byId("cashInput").addEventListener("input", renderSales);
  byId("toggleQuickClientButton").addEventListener("click", redirectToClientCreate);
  byId("checkoutButton").addEventListener("click", () => checkout().catch((error) => showToast(error.message, "error")));
  byId("cancelSaleButton").addEventListener("click", () => {
    clearSale();
    showToast("La venta actual fue cancelada.");
  });
}

function attachProductsEvents() {
  byId("productForm").addEventListener("submit", (event) => {
    event.preventDefault();
    createProductFromForm().catch((error) => showToast(error.message, "error"));
  });
  byId("productFilter").addEventListener("change", renderProducts);
  byId("productsTable").addEventListener("click", (event) => {
    const row = event.target.closest("[data-product-pick]");
    if (!row) {
      return;
    }
    loadProductIntoForm(row.dataset.productPick);
  });
  byId("updateProductButton").addEventListener("click", () => {
    updateProductFromForm().catch((error) => showToast(error.message, "error"));
  });
  byId("clearProductFormButton").addEventListener("click", resetProductForm);
}

function attachClientsEvents() {
  byId("clientSearch").addEventListener("input", renderClients);
  byId("clientsList").addEventListener("click", (event) => {
    const button = event.target.closest("[data-client-pick]");
    if (!button) {
      return;
    }
    state.selectedClientId = Number(button.dataset.clientPick);
    renderClients();
  });
  byId("toggleCreditButton").addEventListener("click", () => {
    toggleSelectedClientCredit().catch((error) => showToast(error.message, "error"));
  });
  byId("toggleClientFormButton").addEventListener("click", () => toggleClientForm());
  byId("loadClientFormButton").addEventListener("click", loadSelectedClientIntoForm);
  byId("cancelClientFormButton").addEventListener("click", () => toggleClientForm(false));
  byId("clientForm").addEventListener("submit", (event) => {
    event.preventDefault();
    saveClientFromForm().catch((error) => showToast(error.message, "error"));
  });
}

function attachInventoryEvents() {
  byId("inventoryProductSearch").addEventListener("input", refreshInventoryProductSearch);
  byId("inventoryProductMatches").addEventListener("click", (event) => {
    const button = event.target.closest("[data-inventory-product-pick]");
    if (!button) {
      return;
    }
    const product = state.products.find((item) => String(item.code) === String(button.dataset.inventoryProductPick));
    if (!product) {
      return;
    }
    setSelectedInventoryProduct(product);
  });
  byId("inventoryForm").addEventListener("submit", (event) => {
    event.preventDefault();
    updateInventory().catch((error) => showToast(error.message, "error"));
  });
}

function attachCreditsEvents() {
  byId("creditClientSelect").addEventListener("change", (event) => {
    state.selectedCreditId = Number(event.target.value || 0);
    renderCredits();
  });
  byId("creditHistoryMode").addEventListener("change", renderCredits);
  byId("creditPaymentSearch").addEventListener("input", renderCredits);
  byId("creditsTable").addEventListener("click", (event) => {
    const row = event.target.closest("[data-credit-pick]");
    if (!row) {
      return;
    }
    state.selectedCreditId = Number(row.dataset.creditPick);
    renderCredits();
  });
  byId("applyCreditButton").addEventListener("click", () => {
    applyCreditPayment(false).catch((error) => showToast(error.message, "error"));
  });
  byId("settleCreditButton").addEventListener("click", () => {
    applyCreditPayment(true).catch((error) => showToast(error.message, "error"));
  });
  byId("blockOverdueButton").addEventListener("click", () => {
    blockOverdueClients().catch((error) => showToast(error.message, "error"));
  });
}

function attachInvoicesEvents() {
  const params = new URLSearchParams(window.location.search);
  state.selectedInvoiceId = Number(params.get("id") || 0);

  byId("invoiceMonth").addEventListener("change", () => {
    state.selectedInvoiceId = 0;
    loadInvoices().catch((error) => showToast(error.message, "error"));
  });
  byId("invoiceYear").addEventListener("change", () => {
    state.selectedInvoiceId = 0;
    loadInvoices().catch((error) => showToast(error.message, "error"));
  });
  byId("invoiceSearch").addEventListener("input", () => {
    state.selectedInvoiceId = 0;
    loadInvoices().catch((error) => showToast(error.message, "error"));
  });
  byId("invoicesTable").addEventListener("click", (event) => {
    const row = event.target.closest("[data-invoice-pick]");
    if (!row) {
      return;
    }
    state.selectedInvoiceId = Number(row.dataset.invoicePick);
    loadInvoices().catch((error) => showToast(error.message, "error"));
  });
  byId("invoiceDetails").addEventListener("click", (event) => {
    const button = event.target.closest("[data-annul-invoice]");
    if (!button) {
      return;
    }
    annulInvoice(Number(button.dataset.annulInvoice)).catch((error) => showToast(error.message, "error"));
  });
}

function attachCutsEvents() {
  byId("cutMonth").addEventListener("change", () => loadCuts().catch((error) => showToast(error.message, "error")));
  byId("cutYear").addEventListener("change", () => loadCuts().catch((error) => showToast(error.message, "error")));
}

function renderEmployees() {
  const search = byId("employeeSearch").value.trim().toLowerCase();
  const employees = state.employees.filter((employee) => {
    const text = `${employee.name} ${employee.document} ${employee.role} ${employee.state}`.toLowerCase();
    return !search || text.includes(search);
  });

  byId("employeeCount").textContent = String(state.employees.length);
  byId("employeeActiveCount").textContent = String(state.employees.filter((employee) => Number(employee.stateId) === 1).length);
  byId("employeeManagerCount").textContent = String(state.employees.filter((employee) => Number(employee.roleId) === 1).length);

  byId("employeesTable").innerHTML = employees.length
    ? employees
        .map(
          (employee) => `
            <tr class="invoice-row ${Number(employee.id) === Number(state.selectedEmployeeId) ? "active" : ""}" data-employee-pick="${employee.id}">
              <td>${escapeHtml(employee.id)}</td>
              <td>
                <strong>${escapeHtml(employee.name)}</strong>
                <div class="subtle">${escapeHtml(employee.document)} - ${escapeHtml(employee.phone || "-")}</div>
              </td>
              <td>${escapeHtml(employee.role)}</td>
              <td>${formatMoney(employee.salary)}</td>
              <td><span class="stock-pill ${Number(employee.stateId) === 1 ? "good" : "low"}">${escapeHtml(employee.state)}</span></td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="5">No hay empleados para esta busqueda.</td></tr>';

  const selected = state.employees.find((employee) => Number(employee.id) === Number(state.selectedEmployeeId));
  byId("dismissEmployeeButton").disabled = !selected || Number(selected.stateId) !== 1 || Number(selected.id) === Number(state.session?.employeeId);
}

async function loadEmployees() {
  const data = await api("/empleados");
  state.employees = data.employees;
  if (!state.selectedEmployeeId && state.employees.length) {
    state.selectedEmployeeId = state.employees[0].id;
  }
  renderEmployees();
}

function resetEmployeeForm() {
  byId("employeeForm").reset();
  byId("employeeRole").value = "2";
}

async function createEmployeeFromForm() {
  const payload = {
    name: byId("employeeName").value.trim(),
    document: byId("employeeDocument").value.trim(),
    phone: byId("employeePhone").value.trim(),
    password: byId("employeePassword").value.trim(),
    salary: Number(byId("employeeSalary").value || 0),
    roleId: Number(byId("employeeRole").value)
  };

  if (!payload.name || !payload.document || !payload.password || !payload.roleId) {
    showToast("Completa nombre, documento, contrasena y cargo.", "error");
    return;
  }

  const result = await api("/empleados", {
    method: "POST",
    body: payload
  });
  resetEmployeeForm();
  state.selectedEmployeeId = Number(result.employeeId || 0);
  await loadEmployees();
  showToast(result.message || "Empleado creado.");
}

async function dismissSelectedEmployee() {
  const employee = state.employees.find((item) => Number(item.id) === Number(state.selectedEmployeeId));
  if (!employee) {
    showToast("Selecciona un empleado.", "error");
    return;
  }

  const result = await api("/empleados", {
    method: "POST",
    body: {
      action: "dismiss",
      employeeId: employee.id
    }
  });
  await loadEmployees();
  showToast(result.message || "Empleado despedido.");
}

function attachEmployeesEvents() {
  byId("employeeSearch").addEventListener("input", renderEmployees);
  byId("employeeForm").addEventListener("submit", (event) => {
    event.preventDefault();
    createEmployeeFromForm().catch((error) => showToast(error.message, "error"));
  });
  byId("employeesTable").addEventListener("click", (event) => {
    const row = event.target.closest("[data-employee-pick]");
    if (!row) {
      return;
    }
    state.selectedEmployeeId = Number(row.dataset.employeePick);
    renderEmployees();
  });
  byId("dismissEmployeeButton").addEventListener("click", () => {
    dismissSelectedEmployee().catch((error) => showToast(error.message, "error"));
  });
  byId("clearEmployeeFormButton").addEventListener("click", resetEmployeeForm);
}

function initLoginPage() {
  const loginForm = byId("loginForm");
  const demoButton = document.querySelector("#demoButton");
  const resetButton = document.querySelector("#resetDemoButton");

  if (state.session) {
    byId("usernameInput").value = state.session.name || "";
  }

  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      const user = byId("usernameInput").value.trim();
      const pass = byId("passwordInput").value.trim();
      const result = await api("/login", {
        method: "POST",
        body: {
          username: user,
          password: pass
        }
      });
      saveSession({
        employeeId: result.employeeId,
        name: result.name,
        roleId: result.roleId,
        role: result.role
      });
      byId("loginError").textContent = "";
      redirectTo(defaultPageForSession());
    } catch (error) {
      byId("loginError").textContent = error.message;
    }
  });

  if (demoButton) {
    demoButton.addEventListener("click", () => {
      byId("usernameInput").value = "PENE";
      byId("passwordInput").value = "1234";
      loginForm.requestSubmit();
    });
  }

  if (resetButton) {
    resetButton.addEventListener("click", () => {
      localStorage.removeItem(SESSION_KEY);
      localStorage.removeItem(CART_KEY);
      state.session = null;
      state.cart = [];
      byId("usernameInput").value = "";
      byId("passwordInput").value = "";
      byId("loginError").textContent = "";
      showToast("Sesion local reiniciada.");
    });
  }
}

async function initWorkspace() {
  ensureSession();
  if (!state.session || !canAccessPage(page)) {
    return;
  }
  syncShell();

  if (page === "resumen") {
    await loadOverview();
  }

  if (page === "ventas") {
    attachSalesEvents();
    await loadSales();
  }

  if (page === "productos") {
    attachProductsEvents();
    await loadProducts();
  }

  if (page === "clientes") {
    attachClientsEvents();
    await loadClients();
    applyClientRouteState();
  }

  if (page === "inventario") {
    attachInventoryEvents();
    await loadInventory();
  }

  if (page === "credito") {
    attachCreditsEvents();
    await loadCredits();
  }

  if (page === "facturas") {
    attachInvoicesEvents();
    await loadInvoices();
  }

  if (page === "cortes") {
    attachCutsEvents();
    await loadCuts();
  }

  if (page === "empleados") {
    attachEmployeesEvents();
    await loadEmployees();
  }
}

async function init() {
  if (page === "login") {
    initLoginPage();
    return;
  }

  try {
    await initWorkspace();
  } catch (error) {
    showToast(error.message, "error");
  }
}

init();
