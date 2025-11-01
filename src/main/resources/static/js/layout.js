(function () {
    // transform status text to badge classes if elements exist
    document.querySelectorAll("td[data-status]").forEach(function (cell) {
        var s = cell.getAttribute("data-status") || "";
        var badge = document.createElement("span");
        badge.className = "status " + s;
        badge.textContent = s || "UNKNOWN";
        cell.innerHTML = "";
        cell.appendChild(badge);
    });
})();
