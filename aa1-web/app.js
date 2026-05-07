const STORAGE_KEY = "aa1-web-demo-store";
const TODAY = "2026-05-07";

const PAGE_TITLES = {
  login: "Acceso",
  resumen: "Resumen",
  ventas: "Ventas",
  productos: "Productos",
  clientes: "Clientes",
  inventario: "Inventario",
  credito: "Credito",
  facturas: "Facturas",
  cortes: "Cortes"
};

const DEFAULT_STORE = {
  operator: "",
  products: [
    {
      code: "770101",
      name: "Lomo fino",
      category: "Carne de Res",
      description: "Corte premium para parrilla y pedidos especiales.",
      price: 32000,
      stock: 18.5,
      minStock: 10,
      unit: "kg"
    },
    {
      code: "770102",
      name: "Costilla premium",
      category: "Carne de Res",
      description: "Ideal para coccion lenta y parrilla.",
      price: 26800,
      stock: 9.2,
      minStock: 12,
      unit: "kg"
    },
    {
      code: "770201",
      name: "Higado fresco",
      category: "Viceras",
      description: "Producto de alta rotacion en mostrador.",
      price: 15000,
      stock: 7,
      minStock: 5,
      unit: "kg"
    },
    {
      code: "770301",
      name: "Chuleta de cerdo",
      category: "Cerdo",
      description: "Corte clasico para menu diario.",
      price: 21400,
      stock: 11.5,
      minStock: 8,
      unit: "kg"
    },
    {
      code: "770401",
      name: "Pechuga deshuesada",
      category: "Pollo",
      description: "Presentacion limpia para restaurante y hogar.",
      price: 18800,
      stock: 22,
      minStock: 10,
      unit: "kg"
    },
    {
      code: "770501",
      name: "Combo escolar",
      category: "Colegio",
      description: "Paquete practico para despachos institucionales.",
      price: 12000,
      stock: 35,
      minStock: 20,
      unit: "unidad"
    },
    {
      code: "770601",
      name: "Salchicha artesanal",
      category: "Salsamentaria",
      description: "Elaboracion local con buena salida en mostrador.",
      price: 17200,
      stock: 16,
      minStock: 12,
      unit: "unidad"
    },
    {
      code: "770602",
      name: "Morcilla casera",
      category: "Salsamentaria",
      description: "Producto de rotacion alta en fin de semana.",
      price: 14300,
      stock: 5,
      minStock: 8,
      unit: "unidad"
    }
  ],
  clients: [
    {
      id: 1,
      document: "1023948576",
      name: "Martha Rojas",
      phone: "310 449 2811",
      address: "Barrio Centro",
      creditEnabled: true,
      debt: 85000
    },
    {
      id: 2,
      document: "43192817",
      name: "Jose Vargas",
      phone: "312 784 5090",
      address: "San Miguel",
      creditEnabled: false,
      debt: 132000
    },
    {
      id: 3,
      document: "1098823411",
      name: "Lucia Bernal",
      phone: "315 988 4102",
      address: "Villa Esperanza",
      creditEnabled: true,
      debt: 0
    },
    {
      id: 4,
      document: "59877412",
      name: "Carlos Duran",
      phone: "320 400 1255",
      address: "Altos del Sur",
      creditEnabled: true,
      debt: 42000
    },
    {
      id: 5,
      document: "1088457720",
      name: "Nadia Pardo",
      phone: "313 884 9931",
      address: "La Floresta",
      creditEnabled: true,
      debt: 0
    }
  ],
  invoices: [
    {
      id: "FAC-2041",
      date: "2026-05-07",
      employee: "Andres Cruz",
      clientId: 1,
      payment: "Efectivo",
      received: 100000,
      total: 92500,
      change: 7500,
      items: [
        { code: "770101", name: "Lomo fino", quantity: 1.4, unit: "kg", price: 32000 },
        { code: "770601", name: "Salchicha artesanal", quantity: 2, unit: "unidad", price: 17200 }
      ]
    },
    {
      id: "FAC-2038",
      date: "2026-05-06",
      employee: "Andres Cruz",
      clientId: 4,
      payment: "Credito",
      received: 54000,
      total: 54000,
      change: 0,
      items: [
        { code: "770301", name: "Chuleta de cerdo", quantity: 1.5, unit: "kg", price: 21400 },
        { code: "770401", name: "Pechuga deshuesada", quantity: 1.2, unit: "kg", price: 18800 }
      ]
    },
    {
      id: "FAC-2032",
      date: "2026-05-03",
      employee: "Lorena Diaz",
      clientId: 5,
      payment: "Transferencia",
      received: 71000,
      total: 71000,
      change: 0,
      items: [
        { code: "770102", name: "Costilla premium", quantity: 1.1, unit: "kg", price: 26800 },
        { code: "770602", name: "Morcilla casera", quantity: 3, unit: "unidad", price: 14300 }
      ]
    },
    {
      id: "FAC-1988",
      date: "2026-04-27",
      employee: "Lorena Diaz",
      clientId: 3,
      payment: "Tarjeta",
      received: 116000,
      total: 116000,
      change: 0,
      items: [
        { code: "770101", name: "Lomo fino", quantity: 2.5, unit: "kg", price: 32000 },
        { code: "770201", name: "Higado fresco", quantity: 1.2, unit: "kg", price: 15000 }
      ]
    },
    {
      id: "FAC-1924",
      date: "2026-04-14",
      employee: "Andres Cruz",
      clientId: 2,
      payment: "Credito",
      received: 68000,
      total: 68000,
      change: 0,
      items: [
        { code: "770501", name: "Combo escolar", quantity: 3, unit: "unidad", price: 12000 },
        { code: "770401", name: "Pechuga deshuesada", quantity: 1.7, unit: "kg", price: 18800 }
      ]
    }
  ],
  movements: [
    {
      id: 91,
      productCode: "770102",
      product: "Costilla premium",
      quantity: "-3.2 kg",
      date: "2026-05-07",
      motive: "Salida por venta"
    },
    {
      id: 90,
      productCode: "770602",
      product: "Morcilla casera",
      quantity: "+8 und",
      date: "2026-05-06",
      motive: "Ingreso proveedor"
    },
    {
      id: 89,
      productCode: "770501",
      product: "Combo escolar",
      quantity: "+12 und",
      date: "2026-05-05",
      motive: "Devolucion"
    },
    {
      id: 88,
      productCode: "770201",
      product: "Higado fresco",
      quantity: "-1.5 kg",
      date: "2026-05-04",
      motive: "Salida manual"
    },
    {
      id: 87,
      productCode: "770101",
      product: "Lomo fino",
      quantity: "+6.0 kg",
      date: "2026-05-03",
      motive: "Ingreso proveedor"
    }
  ],
  cart: [],
  ui: {
    selectedClientId: 1,
    selectedInvoiceId: "FAC-2041",
    selectedSaleClientId: 1
  }
};

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
const store = loadStore();

function byId(id) {
  return document.getElementById(id);
}

function cloneDefaults() {
  return JSON.parse(JSON.stringify(DEFAULT_STORE));
}

function loadStore() {
  const fallback = cloneDefaults();

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return fallback;
    }

    const parsed = JSON.parse(raw);
    return {
      ...fallback,
      ...parsed,
      products: Array.isArray(parsed.products) ? parsed.products : fallback.products,
      clients: Array.isArray(parsed.clients) ? parsed.clients : fallback.clients,
      invoices: Array.isArray(parsed.invoices) ? parsed.invoices : fallback.invoices,
      movements: Array.isArray(parsed.movements) ? parsed.movements : fallback.movements,
      cart: Array.isArray(parsed.cart) ? parsed.cart : fallback.cart,
      ui: {
        ...fallback.ui,
        ...(parsed.ui || {})
      }
    };
  } catch (_error) {
    return fallback;
  }
}

function saveStore() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store));
  } catch (_error) {
    // If storage is unavailable, the demo still works for the current page session.
  }
}

function redirectTo(pageName) {
  const target = pageName === "login" ? "index.html" : `${pageName}.html`;
  window.location.href = `./${target}`;
}

function ensureSession() {
  if (page === "login") {
    return;
  }

  if (!store.operator) {
    redirectTo("login");
  }
}

function formatMoney(value) {
  return new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0
  }).format(value);
}

function formatNumber(value, unit) {
  if (unit === "unidad") {
    return `${Math.round(value)} und`;
  }

  return `${Number(value).toFixed(1)} kg`;
}

function formatDate(dateText) {
  const date = new Date(`${dateText}T12:00:00`);
  return new Intl.DateTimeFormat("es-CO", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  }).format(date);
}

function getClient(clientId) {
  return store.clients.find((client) => client.id === clientId);
}

function getProduct(productCode) {
  return store.products.find((product) => product.code === productCode);
}

function getCartTotal() {
  return store.cart.reduce((sum, item) => sum + item.quantity * item.price, 0);
}

function getLowStockProducts() {
  return store.products.filter((product) => product.stock <= product.minStock);
}

function getCurrentInvoiceList() {
  const monthElement = byId("invoiceMonth");
  const yearElement = byId("invoiceYear");
  const searchElement = byId("invoiceSearch");

  const month = monthElement ? Number(monthElement.value) : 0;
  const year = yearElement ? Number(yearElement.value) : 2026;
  const search = searchElement ? searchElement.value.trim().toLowerCase() : "";

  return store.invoices.filter((invoice) => {
    const date = new Date(`${invoice.date}T12:00:00`);
    const matchMonth = month === 0 || date.getMonth() + 1 === month;
    const matchYear = date.getFullYear() === year;
    const client = getClient(invoice.clientId);
    const haystack = `${invoice.id} ${client.name} ${client.document} ${invoice.payment}`.toLowerCase();
    const matchSearch = !search || haystack.includes(search);
    return matchMonth && matchYear && matchSearch;
  });
}

function getCurrentCutInvoices() {
  const month = Number(byId("cutMonth").value);
  const year = Number(byId("cutYear").value);

  return store.invoices.filter((invoice) => {
    const date = new Date(`${invoice.date}T12:00:00`);
    return date.getMonth() + 1 === month && date.getFullYear() === year;
  });
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
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2600);
}

function syncShell() {
  const currentSection = byId("currentSection");
  const todayLabel = byId("todayLabel");
  const operatorLabel = byId("operatorLabel");

  if (currentSection) {
    currentSection.textContent = PAGE_TITLES[page] || "Panel";
  }

  if (todayLabel) {
    const labelDate = new Date(`${TODAY}T12:00:00`);
    todayLabel.textContent = new Intl.DateTimeFormat("es-CO", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric"
    }).format(labelDate);
  }

  if (operatorLabel) {
    operatorLabel.textContent = store.operator || "Operador demo";
  }

  document.querySelectorAll("[data-page-link]").forEach((link) => {
    link.classList.toggle("active", link.dataset.pageLink === page);
  });
}

function renderOverview() {
  const salesToday = store.invoices
    .filter((invoice) => invoice.date === TODAY)
    .reduce((sum, invoice) => sum + invoice.total, 0);
  const totalDebt = store.clients.reduce((sum, client) => sum + client.debt, 0);
  const lowStock = getLowStockProducts();

  byId("statVentasDia").textContent = formatMoney(salesToday);
  byId("statCredito").textContent = formatMoney(totalDebt);
  byId("statBajoStock").textContent = String(lowStock.length);
  byId("statFacturas").textContent = String(store.invoices.length);
  byId("overviewLowStockCount").textContent = `${lowStock.length} activos`;

  byId("overviewLowStock").innerHTML = lowStock.length
    ? lowStock
        .map(
          (product) => `
            <div class="stack-item">
              <div>
                <strong>${product.name}</strong>
                <span class="subtle">${product.code} · ${product.category}</span>
              </div>
              <strong class="tone-danger">${formatNumber(product.stock, product.unit)}</strong>
            </div>
          `
        )
        .join("")
    : '<div class="stack-item"><strong>Todo el stock esta saludable.</strong></div>';

  byId("overviewInvoices").innerHTML = store.invoices
    .slice(0, 4)
    .map((invoice) => {
      const client = getClient(invoice.clientId);
      return `
        <div class="stack-item">
          <div>
            <strong>${invoice.id}</strong>
            <span class="subtle">${client.name} · ${formatDate(invoice.date)}</span>
          </div>
          <strong>${formatMoney(invoice.total)}</strong>
        </div>
      `;
    })
    .join("");
}

function renderSales() {
  const total = getCartTotal();
  const paymentMethod = byId("paymentMethod").value;
  const received = paymentMethod === "Efectivo" ? Number(byId("cashInput").value || 0) : total;
  const change = paymentMethod === "Efectivo" ? Math.max(received - total, 0) : 0;

  if (!store.ui.selectedSaleClientId) {
    store.ui.selectedSaleClientId = store.clients[0]?.id || 0;
  }

  byId("cartItems").textContent = String(store.cart.length);
  byId("cartTotal").textContent = formatMoney(total);
  byId("cartChange").textContent = formatMoney(change);
  byId("paymentTotal").textContent = formatMoney(total);
  byId("paymentReceived").textContent = formatMoney(received);
  byId("paymentChange").textContent = formatMoney(change);
  byId("saleSummaryBadge").textContent = `${store.cart.length} productos`;
  byId("lastInvoiceLabel").textContent = store.ui.selectedInvoiceId || "Sin emitir";
  byId("cashWrap").classList.toggle("hidden", paymentMethod !== "Efectivo");

  byId("customerSelect").innerHTML = store.clients
    .map(
      (client) =>
        `<option value="${client.id}" ${client.id === store.ui.selectedSaleClientId ? "selected" : ""}>${client.name} · ${client.document}</option>`
    )
    .join("");

  byId("productSuggestions").innerHTML = store.products
    .slice(0, 6)
    .map(
      (product) => `
        <button type="button" data-code-pick="${product.code}">
          ${product.name}
        </button>
      `
    )
    .join("");

  byId("salesTable").innerHTML = store.cart.length
    ? store.cart
        .map(
          (item) => `
            <tr>
              <td>${item.code}</td>
              <td>${item.name}</td>
              <td>${formatMoney(item.price)}</td>
              <td>${formatNumber(item.quantity, item.unit)}</td>
              <td>${formatMoney(item.quantity * item.price)}</td>
              <td>
                <button class="remove-button" type="button" data-remove-cart="${item.code}">
                  Quitar
                </button>
              </td>
            </tr>
          `
        )
        .join("")
    : `
        <tr>
          <td colspan="6">No hay productos en la venta actual.</td>
        </tr>
      `;
}

function renderProducts() {
  const filter = byId("productFilter").value;
  const visibleProducts = filter === "Todos"
    ? store.products
    : store.products.filter((product) => product.category === filter);

  byId("productCount").textContent = String(store.products.length);
  byId("productCategoryCount").textContent = String(
    new Set(store.products.map((product) => product.category)).size
  );
  byId("productLowCount").textContent = String(getLowStockProducts().length);

  byId("productsTable").innerHTML = visibleProducts
    .map((product) => {
      const stockClass = product.stock <= product.minStock ? "low" : "good";
      return `
        <tr>
          <td>${product.code}</td>
          <td>
            <strong>${product.name}</strong>
            <div class="subtle">${product.description}</div>
          </td>
          <td>${product.category}</td>
          <td>${formatMoney(product.price)}</td>
          <td><span class="stock-pill ${stockClass}">${formatNumber(product.stock, product.unit)}</span></td>
        </tr>
      `;
    })
    .join("");
}

function renderClients() {
  const search = byId("clientSearch").value.trim().toLowerCase();
  const visibleClients = store.clients.filter((client) => {
    const haystack = `${client.name} ${client.document}`.toLowerCase();
    return !search || haystack.includes(search);
  });

  if (!visibleClients.some((client) => client.id === store.ui.selectedClientId) && visibleClients.length) {
    store.ui.selectedClientId = visibleClients[0].id;
  }

  byId("clientsList").innerHTML = visibleClients
    .map(
      (client) => `
        <button class="client-item ${client.id === store.ui.selectedClientId ? "active" : ""}" type="button" data-client-pick="${client.id}">
          <strong>${client.name}</strong>
          <span class="subtle">${client.document} · ${client.phone}</span>
        </button>
      `
    )
    .join("");

  const selected = getClient(store.ui.selectedClientId);
  if (!selected) {
    return;
  }

  byId("clientName").textContent = selected.name;
  byId("clientDocument").textContent = selected.document;
  byId("clientPhone").textContent = selected.phone;
  byId("clientAddress").textContent = selected.address;
  byId("clientDebt").textContent = formatMoney(selected.debt);
  byId("clientCreditState").textContent = selected.creditEnabled ? "Credito habilitado" : "Credito bloqueado";
  byId("toggleCreditButton").textContent = selected.creditEnabled
    ? "Bloquear credito del cliente"
    : "Habilitar credito del cliente";
}

function renderInventory() {
  const lowStock = getLowStockProducts();
  const selector = byId("inventoryProductSelect");
  const currentSelection = selector.value || store.products[0]?.code || "";

  byId("lowStockTable").innerHTML = lowStock.length
    ? lowStock
        .map(
          (product) => `
            <tr>
              <td>${product.code}</td>
              <td>${product.name}</td>
              <td class="tone-danger">${formatNumber(product.stock, product.unit)}</td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="3">No hay alertas criticas.</td></tr>';

  selector.innerHTML = store.products
    .map(
      (product) =>
        `<option value="${product.code}" ${product.code === currentSelection ? "selected" : ""}>${product.name}</option>`
    )
    .join("");

  const selected = getProduct(selector.value);
  if (selected) {
    byId("inventoryCode").textContent = selected.code;
    byId("inventoryCurrentStock").textContent = formatNumber(selected.stock, selected.unit);
  }

  byId("inventoryMovements").innerHTML = store.movements
    .slice(0, 7)
    .map(
      (movement) => `
        <div class="timeline-item">
          <strong>${movement.product}</strong>
          <span class="subtle">${movement.productCode} · ${movement.motive}</span>
          <strong>${movement.quantity}</strong>
          <small>${formatDate(movement.date)}</small>
        </div>
      `
    )
    .join("");
}

function renderCredits() {
  const debtClients = store.clients.filter((client) => client.debt > 0);
  const blocked = store.clients.filter((client) => !client.creditEnabled);
  const totalDebt = debtClients.reduce((sum, client) => sum + client.debt, 0);
  const selector = byId("creditClientSelect");
  const currentSelection = selector.value;

  byId("creditClientCount").textContent = String(debtClients.length);
  byId("creditTotalDebt").textContent = formatMoney(totalDebt);
  byId("creditBlockedCount").textContent = String(blocked.length);

  byId("creditsTable").innerHTML = debtClients.length
    ? debtClients
        .map(
          (client) => `
            <tr>
              <td>${client.name}</td>
              <td>${client.document}</td>
              <td>${formatMoney(client.debt)}</td>
              <td><span class="pill ${client.creditEnabled ? "ok" : "off"}">${client.creditEnabled ? "Activo" : "Bloqueado"}</span></td>
            </tr>
          `
        )
        .join("")
    : '<tr><td colspan="4">No hay clientes con deuda.</td></tr>';

  const selectionPool = debtClients.length ? debtClients : store.clients;
  const selectedId = selectionPool.some((client) => String(client.id) === currentSelection)
    ? Number(currentSelection)
    : selectionPool[0]?.id;

  selector.innerHTML = selectionPool
    .map(
      (client) =>
        `<option value="${client.id}" ${client.id === selectedId ? "selected" : ""}>${client.name} · ${formatMoney(client.debt)}</option>`
    )
    .join("");
}

function renderInvoices() {
  const filtered = getCurrentInvoiceList();

  if (!filtered.some((invoice) => invoice.id === store.ui.selectedInvoiceId) && filtered.length) {
    store.ui.selectedInvoiceId = filtered[0].id;
  }

  byId("invoiceCountLabel").textContent = String(filtered.length);
  byId("invoicesTable").innerHTML = filtered.length
    ? filtered
        .map((invoice) => {
          const client = getClient(invoice.clientId);
          return `
            <tr class="invoice-row ${invoice.id === store.ui.selectedInvoiceId ? "active" : ""}" data-invoice-pick="${invoice.id}">
              <td>${invoice.id}</td>
              <td>${formatDate(invoice.date)}</td>
              <td>${client.name}</td>
              <td>${invoice.items.length}</td>
              <td>${formatMoney(invoice.total)}</td>
              <td>${invoice.payment}</td>
            </tr>
          `;
        })
        .join("")
    : '<tr><td colspan="6">No hay facturas para este filtro.</td></tr>';

  const selected = filtered.find((invoice) => invoice.id === store.ui.selectedInvoiceId) || filtered[0];
  if (!selected) {
    byId("invoiceDetails").innerHTML = `
      <div class="invoice-card">
        <h3>Sin factura</h3>
        <p class="subtle">Aun no hay informacion para mostrar en el detalle.</p>
      </div>
    `;
    return;
  }

  const client = getClient(selected.clientId);
  byId("invoiceDetails").innerHTML = `
    <div class="invoice-card">
      <h3>${selected.id}</h3>
      <div class="subtle">${formatDate(selected.date)} · ${selected.employee}</div>
      <div class="subtle">${client.name} · ${client.document}</div>
      <div class="invoice-lines">
        ${selected.items
          .map(
            (item) => `
              <div class="invoice-line">
                <div>
                  <strong>${item.name}</strong>
                  <div class="subtle">${formatNumber(item.quantity, item.unit)} · ${formatMoney(item.price)}</div>
                </div>
                <strong>${formatMoney(item.quantity * item.price)}</strong>
              </div>
            `
          )
          .join("")}
      </div>
      <div class="invoice-lines">
        <div class="invoice-line">
          <span>Metodo de pago</span>
          <strong>${selected.payment}</strong>
        </div>
        <div class="invoice-line">
          <span>Total</span>
          <strong>${formatMoney(selected.total)}</strong>
        </div>
        <div class="invoice-line">
          <span>Recibido</span>
          <strong>${formatMoney(selected.received)}</strong>
        </div>
        <div class="invoice-line">
          <span>Cambio</span>
          <strong>${formatMoney(selected.change)}</strong>
        </div>
      </div>
    </div>
  `;
}

function renderCuts() {
  const cutInvoices = getCurrentCutInvoices();
  const revenue = cutInvoices.reduce((sum, invoice) => sum + invoice.total, 0);
  const average = cutInvoices.length ? revenue / cutInvoices.length : 0;
  const creditSales = cutInvoices
    .filter((invoice) => invoice.payment === "Credito")
    .reduce((sum, invoice) => sum + invoice.total, 0);

  byId("monthlyRevenue").textContent = formatMoney(revenue);
  byId("monthlyTickets").textContent = String(cutInvoices.length);
  byId("monthlyAverage").textContent = formatMoney(average);
  byId("monthlyCreditSales").textContent = formatMoney(creditSales);

  const paymentTotals = ["Efectivo", "Tarjeta", "Transferencia", "Credito"]
    .map((payment) => ({
      payment,
      total: cutInvoices
        .filter((invoice) => invoice.payment === payment)
        .reduce((sum, invoice) => sum + invoice.total, 0)
    }))
    .filter((row) => row.total > 0);

  const maxTotal = Math.max(...paymentTotals.map((row) => row.total), 1);
  byId("cutChart").innerHTML = paymentTotals.length
    ? paymentTotals
        .map(
          (row) => `
            <div class="chart-row">
              <header>
                <strong>${row.payment}</strong>
                <span>${formatMoney(row.total)}</span>
              </header>
              <div class="chart-track">
                <div class="chart-fill" style="width: ${(row.total / maxTotal) * 100}%"></div>
              </div>
            </div>
          `
        )
        .join("")
    : '<div class="stack-item"><strong>No hay ventas para el periodo.</strong></div>';

  const lowStock = getLowStockProducts();
  const topInvoice = [...cutInvoices].sort((left, right) => right.total - left.total)[0];
  const insights = [];

  insights.push({
    title: "Ticket promedio",
    detail: cutInvoices.length
      ? `El periodo se mueve con un promedio de ${formatMoney(average)} por factura.`
      : "No hay facturas en el periodo seleccionado."
  });
  insights.push({
    title: "Cartera en curso",
    detail: creditSales
      ? `${formatMoney(creditSales)} del periodo se fue a credito.`
      : "No hubo ventas a credito en el periodo."
  });
  insights.push({
    title: "Reposicion",
    detail: lowStock.length
      ? `${lowStock.length} productos necesitan reposicion pronto.`
      : "El nivel de inventario se ve estable."
  });

  if (topInvoice) {
    const client = getClient(topInvoice.clientId);
    insights.push({
      title: "Factura mas alta",
      detail: `${topInvoice.id} para ${client.name} por ${formatMoney(topInvoice.total)}.`
    });
  }

  byId("cutInsights").innerHTML = insights
    .map(
      (item) => `
        <div class="stack-item">
          <div>
            <strong>${item.title}</strong>
            <span class="subtle">${item.detail}</span>
          </div>
        </div>
      `
    )
    .join("");
}

function renderCurrentPage() {
  syncShell();

  if (page === "resumen") {
    renderOverview();
  }

  if (page === "ventas") {
    renderSales();
  }

  if (page === "productos") {
    renderProducts();
  }

  if (page === "clientes") {
    renderClients();
  }

  if (page === "inventario") {
    renderInventory();
  }

  if (page === "credito") {
    renderCredits();
  }

  if (page === "facturas") {
    renderInvoices();
  }

  if (page === "cortes") {
    renderCuts();
  }
}

function addProductToCart(rawQuery, rawQuantity) {
  const query = rawQuery.trim().toLowerCase();
  if (!query) {
    showToast("Escribe un codigo o nombre para agregar el producto.", "error");
    return;
  }

  const product = store.products.find((item) =>
    item.code.toLowerCase() === query || item.name.toLowerCase().includes(query)
  );

  if (!product) {
    showToast("No se encontro el producto indicado.", "error");
    return;
  }

  let quantity = Number(rawQuantity);
  if (!Number.isFinite(quantity) || quantity <= 0) {
    showToast("Ingresa una cantidad valida.", "error");
    return;
  }

  if (product.unit === "unidad") {
    quantity = Math.max(1, Math.round(quantity));
  } else {
    quantity = Number(quantity.toFixed(1));
  }

  const reserved = store.cart
    .filter((item) => item.code === product.code)
    .reduce((sum, item) => sum + item.quantity, 0);

  if (reserved + quantity > product.stock) {
    showToast("La cantidad supera el stock disponible.", "error");
    return;
  }

  const existing = store.cart.find((item) => item.code === product.code);
  if (existing) {
    existing.quantity += quantity;
  } else {
    store.cart.push({
      code: product.code,
      name: product.name,
      price: product.price,
      quantity,
      unit: product.unit
    });
  }

  byId("saleCode").value = "";
  byId("saleQty").value = "1";
  saveStore();
  renderSales();
  showToast(`${product.name} agregado a la venta.`);
}

function clearSale() {
  store.cart = [];
  byId("cashInput").value = "";
  byId("paymentMethod").value = "";
  saveStore();
  renderSales();
}

function nextInvoiceId() {
  const numbers = store.invoices.map((invoice) => Number(invoice.id.replace("FAC-", "")));
  return `FAC-${Math.max(...numbers, 2040) + 1}`;
}

function pushMovement(product, quantityText, motive) {
  const nextId = Math.max(...store.movements.map((movement) => movement.id), 0) + 1;
  store.movements.unshift({
    id: nextId,
    productCode: product.code,
    product: product.name,
    quantity: quantityText,
    date: TODAY,
    motive
  });
}

function checkout() {
  if (!store.cart.length) {
    showToast("Agrega productos antes de facturar.", "error");
    return;
  }

  const clientId = Number(byId("customerSelect").value);
  const paymentMethod = byId("paymentMethod").value;
  const client = getClient(clientId);
  const total = getCartTotal();

  if (!client || !paymentMethod) {
    showToast("Selecciona cliente y metodo de pago.", "error");
    return;
  }

  if (paymentMethod === "Credito" && !client.creditEnabled) {
    showToast("Este cliente no tiene credito habilitado.", "error");
    return;
  }

  let received = total;
  let change = 0;

  if (paymentMethod === "Efectivo") {
    received = Number(byId("cashInput").value || 0);
    if (received < total) {
      showToast("El valor recibido no cubre el total.", "error");
      return;
    }
    change = received - total;
  }

  store.cart.forEach((item) => {
    const product = getProduct(item.code);
    product.stock -= item.quantity;
    pushMovement(product, `-${formatNumber(item.quantity, item.unit)}`, "Salida por venta");
  });

  if (paymentMethod === "Credito") {
    client.debt += total;
  }

  const newInvoice = {
    id: nextInvoiceId(),
    date: TODAY,
    employee: store.operator,
    clientId,
    payment: paymentMethod,
    received,
    total,
    change,
    items: store.cart.map((item) => ({ ...item }))
  };

  store.invoices.unshift(newInvoice);
  store.ui.selectedInvoiceId = newInvoice.id;
  store.cart = [];
  byId("cashInput").value = "";
  byId("paymentMethod").value = "";

  saveStore();
  showToast(`Factura ${newInvoice.id} creada con exito.`);
  window.setTimeout(() => redirectTo("facturas"), 350);
}

function createProductFromForm() {
  const code = byId("productCode").value.trim();
  const name = byId("productName").value.trim();
  const category = byId("productCategory").value;
  const price = Number(byId("productPrice").value);
  const stock = Number(byId("productStock").value);
  const unit = byId("productUnit").value;
  const description = byId("productDescription").value.trim();
  const hasStock = byId("productStock").value.trim() !== "";

  if (!code || !name || !category || !description || !price || Number.isNaN(stock) || !hasStock) {
    showToast("Completa todos los datos del producto.", "error");
    return;
  }

  if (store.products.some((product) => product.code === code)) {
    showToast("Ese codigo de barras ya existe.", "error");
    return;
  }

  const product = {
    code,
    name,
    category,
    description,
    price,
    stock,
    minStock: unit === "unidad" ? 10 : 6,
    unit
  };

  store.products.unshift(product);
  pushMovement(product, `+${formatNumber(stock, unit)}`, "Nuevo producto");

  byId("productForm").reset();
  byId("productUnit").value = "kg";
  saveStore();
  renderProducts();
  showToast(`${name} fue agregado al catalogo.`);
}

function toggleSelectedClientCredit() {
  const client = getClient(store.ui.selectedClientId);
  if (!client) {
    return;
  }

  client.creditEnabled = !client.creditEnabled;
  saveStore();
  renderClients();
  showToast(
    client.creditEnabled
      ? `Credito habilitado para ${client.name}.`
      : `Credito bloqueado para ${client.name}.`
  );
}

function updateInventory() {
  const product = getProduct(byId("inventoryProductSelect").value);
  const mode = byId("inventoryMode").value;
  const amount = Number(byId("inventoryAmount").value);

  if (!product || !Number.isFinite(amount) || amount <= 0) {
    showToast("Ingresa un movimiento valido.", "error");
    return;
  }

  if (mode === "adjust") {
    const previous = product.stock;
    product.stock = amount;
    const delta = amount - previous;
    const sign = delta >= 0 ? "+" : "-";
    pushMovement(product, `${sign}${formatNumber(Math.abs(delta), product.unit)}`, "Ajuste manual");
  }

  if (mode === "return") {
    product.stock += amount;
    pushMovement(product, `+${formatNumber(amount, product.unit)}`, "Devolucion");
  }

  if (mode === "ingress") {
    product.stock += amount;
    pushMovement(product, `+${formatNumber(amount, product.unit)}`, "Ingreso proveedor");
  }

  if (mode === "manualOut") {
    if (amount > product.stock) {
      showToast("La salida manual supera el stock actual.", "error");
      return;
    }

    product.stock -= amount;
    pushMovement(product, `-${formatNumber(amount, product.unit)}`, "Salida manual");
  }

  byId("inventoryAmount").value = "";
  saveStore();
  renderInventory();
  showToast(`Inventario actualizado para ${product.name}.`);
}

function applyCreditPayment(settleFull = false) {
  const client = getClient(Number(byId("creditClientSelect").value));
  if (!client) {
    showToast("Selecciona un cliente.", "error");
    return;
  }

  const payment = settleFull ? client.debt : Number(byId("creditPayment").value);
  if (!payment || payment <= 0) {
    showToast("Ingresa un abono valido.", "error");
    return;
  }

  client.debt = Math.max(0, client.debt - payment);
  byId("creditPayment").value = "";
  saveStore();
  renderCredits();
  showToast(`Saldo actualizado para ${client.name}.`);
}

function blockOverdueClients() {
  store.clients
    .filter((client) => client.debt >= 100000)
    .forEach((client) => {
      client.creditEnabled = false;
    });

  saveStore();
  renderCredits();
  showToast("Clientes morosos bloqueados para nuevo credito.");
}

function resetDemoState() {
  const fresh = cloneDefaults();
  store.operator = fresh.operator;
  store.products = fresh.products;
  store.clients = fresh.clients;
  store.invoices = fresh.invoices;
  store.movements = fresh.movements;
  store.cart = fresh.cart;
  store.ui = fresh.ui;
  saveStore();
}

function initLoginPage() {
  const loginForm = byId("loginForm");
  const demoButton = byId("demoButton");
  const resetButton = byId("resetDemoButton");

  if (store.operator) {
    byId("usernameInput").value = store.operator;
  }

  loginForm.addEventListener("submit", (event) => {
    event.preventDefault();

    const user = byId("usernameInput").value.trim();
    const pass = byId("passwordInput").value.trim();

    if (!user || !pass) {
      byId("loginError").textContent = "Escribe usuario y contrasena para continuar.";
      return;
    }

    byId("loginError").textContent = "";
    store.operator = user;
    saveStore();
    redirectTo("resumen");
  });

  demoButton.addEventListener("click", () => {
    byId("usernameInput").value = "Andres Cruz";
    byId("passwordInput").value = "1234";
    loginForm.requestSubmit();
  });

  if (resetButton) {
    resetButton.addEventListener("click", () => {
      resetDemoState();
      byId("usernameInput").value = "";
      byId("passwordInput").value = "";
      byId("loginError").textContent = "";
      showToast("La demo fue reiniciada.");
    });
  }
}

function attachSalesEvents() {
  byId("salesForm").addEventListener("submit", (event) => {
    event.preventDefault();
    addProductToCart(byId("saleCode").value, byId("saleQty").value);
  });

  byId("scaleButton").addEventListener("click", () => {
    const randomWeight = (Math.random() * 3.7 + 0.6).toFixed(1);
    byId("saleQty").value = randomWeight;
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

    store.cart = store.cart.filter((item) => item.code !== button.dataset.removeCart);
    saveStore();
    renderSales();
  });

  byId("customerSelect").addEventListener("change", (event) => {
    store.ui.selectedSaleClientId = Number(event.target.value);
    saveStore();
  });

  byId("paymentMethod").addEventListener("change", renderSales);
  byId("cashInput").addEventListener("input", renderSales);
  byId("checkoutButton").addEventListener("click", checkout);
  byId("cancelSaleButton").addEventListener("click", () => {
    clearSale();
    showToast("La venta actual fue cancelada.");
  });
}

function attachProductsEvents() {
  byId("productForm").addEventListener("submit", (event) => {
    event.preventDefault();
    createProductFromForm();
  });

  byId("productFilter").addEventListener("change", renderProducts);
}

function attachClientsEvents() {
  byId("clientSearch").addEventListener("input", renderClients);
  byId("clientsList").addEventListener("click", (event) => {
    const button = event.target.closest("[data-client-pick]");
    if (!button) {
      return;
    }

    store.ui.selectedClientId = Number(button.dataset.clientPick);
    saveStore();
    renderClients();
  });
  byId("toggleCreditButton").addEventListener("click", toggleSelectedClientCredit);
}

function attachInventoryEvents() {
  byId("inventoryProductSelect").addEventListener("change", renderInventory);
  byId("inventoryForm").addEventListener("submit", (event) => {
    event.preventDefault();
    updateInventory();
  });
}

function attachCreditsEvents() {
  byId("applyCreditButton").addEventListener("click", () => applyCreditPayment(false));
  byId("settleCreditButton").addEventListener("click", () => applyCreditPayment(true));
  byId("blockOverdueButton").addEventListener("click", blockOverdueClients);
}

function attachInvoicesEvents() {
  byId("invoiceMonth").addEventListener("change", renderInvoices);
  byId("invoiceYear").addEventListener("change", renderInvoices);
  byId("invoiceSearch").addEventListener("input", renderInvoices);
  byId("invoicesTable").addEventListener("click", (event) => {
    const row = event.target.closest("[data-invoice-pick]");
    if (!row) {
      return;
    }

    store.ui.selectedInvoiceId = row.dataset.invoicePick;
    saveStore();
    renderInvoices();
  });
}

function attachCutsEvents() {
  byId("cutMonth").addEventListener("change", renderCuts);
  byId("cutYear").addEventListener("change", renderCuts);
}

function initWorkspace() {
  ensureSession();
  syncShell();

  if (page === "resumen") {
    renderOverview();
  }

  if (page === "ventas") {
    renderSales();
    attachSalesEvents();
  }

  if (page === "productos") {
    renderProducts();
    attachProductsEvents();
  }

  if (page === "clientes") {
    renderClients();
    attachClientsEvents();
  }

  if (page === "inventario") {
    renderInventory();
    attachInventoryEvents();
  }

  if (page === "credito") {
    renderCredits();
    attachCreditsEvents();
  }

  if (page === "facturas") {
    renderInvoices();
    attachInvoicesEvents();
  }

  if (page === "cortes") {
    renderCuts();
    attachCutsEvents();
  }
}

function init() {
  if (page === "login") {
    initLoginPage();
    return;
  }

  initWorkspace();
}

init();
