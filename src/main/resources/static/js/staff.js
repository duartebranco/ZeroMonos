// Use the same-origin controllers exposed by the app
(function () {
    const apiBase = "";
    const searchInput = document.getElementById("searchMunicipality");
    const searchBtn = document.getElementById("searchBtn");
    const refreshBtn = document.getElementById("refreshBtn");
    const table = document.getElementById("bookingsTable");
    const tbody = table.querySelector("tbody");

    // Client-side filtering by municipality (simple, works on currently rendered rows)
    searchBtn.addEventListener("click", function () {
        const q = searchInput.value.trim().toLowerCase();
        if (!q) {
            // show all rows
            tbody.querySelectorAll("tr[th\\:each]").forEach(() => {}); // no-op: template marker not present at runtime
        }
        Array.from(tbody.querySelectorAll("tr")).forEach((row) => {
            // skip placeholder row (no th:each at runtime) by checking number of cells
            const cells = row.querySelectorAll("td");
            if (cells.length < 2) return;
            const municipalityCell = cells[2];
            const municipality = (
                (municipalityCell && municipalityCell.textContent) ||
                ""
            ).toLowerCase();
            if (!q || municipality.includes(q)) {
                row.style.display = "";
            } else {
                row.style.display = "none";
            }
        });
    });

    // Reload page data from server (full refresh)
    refreshBtn.addEventListener("click", function () {
        location.reload();
    });

    // Small utility: create a transient toast message
    function createToast(message, isError) {
        const id = "staff-toast";
        // allow multiple toasts, each unique
        const toast = document.createElement("div");
        toast.className = "staff-toast";
        toast.style.position = "fixed";
        toast.style.right = "1rem";
        toast.style.top = "1rem";
        toast.style.background = isError ? "#f8d7da" : "#d1e7dd";
        toast.style.color = isError ? "#842029" : "#0f5132";
        toast.style.padding = "0.6rem 1rem";
        toast.style.borderRadius = "8px";
        toast.style.boxShadow = "0 6px 18px rgba(16,24,40,0.08)";
        toast.style.zIndex = 9999;
        toast.style.fontSize = "0.95rem";
        toast.textContent = message;
        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.transition = "opacity 220ms ease";
            toast.style.opacity = "0";
            setTimeout(() => {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 250);
        }, 2800);
    }

    // Small utility: a pretty little confirmation popup that returns a Promise<boolean>
    function showConfirmation(message) {
        return new Promise((resolve) => {
            const overlay = document.createElement("div");
            overlay.style.position = "fixed";
            overlay.style.left = "0";
            overlay.style.top = "0";
            overlay.style.right = "0";
            overlay.style.bottom = "0";
            overlay.style.background = "rgba(0,0,0,0.35)";
            overlay.style.display = "flex";
            overlay.style.alignItems = "center";
            overlay.style.justifyContent = "center";
            overlay.style.zIndex = 10000;

            const box = document.createElement("div");
            box.style.background = "#ffffff";
            box.style.padding = "1rem 1.25rem";
            box.style.borderRadius = "10px";
            box.style.boxShadow = "0 12px 30px rgba(2,6,23,0.12)";
            box.style.maxWidth = "92%";
            box.style.width = "420px";
            box.style.textAlign = "left";

            const msg = document.createElement("div");
            msg.style.marginBottom = "0.75rem";
            msg.style.fontSize = "0.95rem";
            msg.textContent = message;

            const actions = document.createElement("div");
            actions.style.display = "flex";
            actions.style.justifyContent = "flex-end";
            actions.style.gap = "0.5rem";

            const noBtn = document.createElement("button");
            noBtn.className = "btn secondary";
            noBtn.textContent = "Cancel";

            const yesBtn = document.createElement("button");
            yesBtn.className = "btn";
            yesBtn.textContent = "Confirm";

            actions.appendChild(noBtn);
            actions.appendChild(yesBtn);
            box.appendChild(msg);
            box.appendChild(actions);
            overlay.appendChild(box);
            document.body.appendChild(overlay);

            function cleanup(result) {
                if (overlay.parentNode) overlay.parentNode.removeChild(overlay);
                resolve(result);
            }

            noBtn.addEventListener("click", () => cleanup(false));
            yesBtn.addEventListener("click", () => cleanup(true));

            // keyboard support
            overlay.addEventListener("keydown", (e) => {
                if (e.key === "Escape") cleanup(false);
            });
            // focus yes for quick action
            yesBtn.focus();
        });
    }

    // Enhance status visuals on initial load (in case some cells were not transformed by layout scripts)
    document.querySelectorAll("td[data-status]").forEach(function (cell) {
        const s = cell.getAttribute("data-status") || "";
        cell.innerHTML = "";
        const span = document.createElement("span");
        span.className = "status " + s;
        span.textContent = s || "UNKNOWN";
        cell.appendChild(span);
    });

    // Expose function for inline onchange handlers to update status
    window.updateStatus = async function (id, status) {
        if (!status) return;
        const confirmed = await showConfirmation(
            "Change status of booking " + id + " to " + status + "?",
        );
        if (!confirmed) return;

        try {
            const res = await fetch(
                apiBase +
                    "/bookings/" +
                    encodeURIComponent(id) +
                    "?status=" +
                    encodeURIComponent(status),
                { method: "PATCH" },
            );
            if (!res.ok) {
                const txt = await res.text();
                throw new Error(txt || "Server error");
            }
            const updated = await res.json();

            // find the row for this booking and update its status cell
            Array.from(tbody.querySelectorAll("tr")).forEach((row) => {
                const cells = row.querySelectorAll("td");
                if (cells.length < 2) return;
                const idCell = cells[0];
                if (
                    idCell &&
                    String((idCell.textContent || "").trim()) === String(id)
                ) {
                    const statusCell = row.querySelector("td[data-status]");
                    if (statusCell) {
                        statusCell.setAttribute(
                            "data-status",
                            updated.status || "",
                        );
                        statusCell.innerHTML = "";
                        const span = document.createElement("span");
                        span.className = "status " + (updated.status || "");
                        span.textContent = updated.status || "UNKNOWN";
                        statusCell.appendChild(span);
                    }
                }
            });

            createToast(
                "Status updated to " + (updated.status || status),
                false,
            );
        } catch (err) {
            console.error(err);
            createToast("Error updating status: " + (err.message || err), true);
        }
    };

    // Delete booking flow: confirmation + DELETE request + remove row + toast
    async function deleteBooking(id) {
        if (!id) return;
        const confirmed = await showConfirmation(
            "Delete booking " + id + "? This action cannot be undone.",
        );
        if (!confirmed) return;
        try {
            const res = await fetch(
                apiBase + "/bookings/" + encodeURIComponent(id),
                { method: "DELETE" },
            );
            if (res.status === 404) {
                createToast("Booking not found.", true);
                return;
            }
            if (!res.ok) {
                const txt = await res.text();
                throw new Error(txt || "Server error");
            }

            // Remove row from table
            const btn = document.querySelector(`[data-delete-id="${id}"]`);
            if (btn) {
                const row = btn.closest("tr");
                if (row && row.parentNode) row.parentNode.removeChild(row);
            }

            createToast("Booking deleted", false);
        } catch (err) {
            console.error(err);
            createToast(
                "Error deleting booking: " + (err.message || err),
                true,
            );
        }
    }

    // Wire delete buttons present at load time and also handle future rows
    function attachDeleteHandlers(root = document) {
        Array.from(
            (root || document).querySelectorAll(".delete-booking"),
        ).forEach(function (btn) {
            // avoid attaching multiple listeners
            if (btn.dataset.deleteAttached) return;
            btn.dataset.deleteAttached = "1";
            btn.addEventListener("click", function (e) {
                e.preventDefault();
                const id = btn.getAttribute("data-delete-id");
                deleteBooking(id);
            });
        });
    }

    // Attach handlers initially
    attachDeleteHandlers();

    // Observe tbody for newly added rows and attach handlers automatically
    try {
        const observer = new MutationObserver(function (mutations) {
            mutations.forEach(function (m) {
                if (m.addedNodes && m.addedNodes.length) {
                    attachDeleteHandlers(m.target || document);
                }
            });
        });
        if (tbody) {
            observer.observe(tbody, { childList: true, subtree: true });
        }
    } catch (e) {
        // MutationObserver may not be available in some test environments; it's non-fatal
        console.warn("MutationObserver not attached:", e && e.message);
    }
})();
