(function () {
    const apiBase = ""; // same origin; controllers expose /bookings and /municipalities

    // Elements
    const nameInput = document.getElementById("name");
    const municipalityInput = document.getElementById("municipality");
    const descriptionInput = document.getElementById("description");
    const itemTypeSelect = document.getElementById("itemType");
    const dateInput = document.getElementById("date");
    const timeSlotSelect = document.getElementById("timeSlot");
    const bookingForm = document.getElementById("bookingForm");
    const feedback = document.getElementById("feedback");
    const clearBtn = document.getElementById("clearBtn");

    const checkBtn = document.getElementById("checkBtn");
    const tokenInput = document.getElementById("token");
    const resultBlock = document.getElementById("checkResult");
    const cancelButtonContainer = document.getElementById(
        "cancelButtonContainer",
    );
    const cancelBookingBtn = document.getElementById("cancelBookingBtn");

    // State for the current booking being viewed
    let currentBooking = null;

    // Initialize result block
    if (resultBlock) {
        resultBlock.textContent = "No results";
        resultBlock.style.marginTop = "1.2rem";
    }

    // Prepare valid municipality names for client-side validation
    window.validMunicipalities = [];
    if (
        typeof MUNICIPALITIES !== "undefined" &&
        MUNICIPALITIES &&
        municipalityInput
    ) {
        window.validMunicipalities = MUNICIPALITIES.map((m) =>
            (m || "").toLowerCase(),
        );
    }

    // Set minimum date to today
    if (dateInput) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        dateInput.min = tomorrow.toISOString().split("T")[0];
    }

    // Update available time slots based on selected date and municipality
    async function updateAvailableTimeSlots() {
        if (!dateInput || !municipalityInput || !timeSlotSelect) return;

        const date = dateInput.value;
        const municipality = municipalityInput.value.trim();

        // Reset if no date or municipality selected
        if (!date || !municipality) {
            resetTimeSlotSelect("Select date and municipality first");
            return;
        }

        // Check if date is in the past
        if (isDateInPast(date)) {
            resetTimeSlotSelect("Cannot book appointments in the past");
            return;
        }

        // Validate municipality
        if (!window.validMunicipalities.includes(municipality.toLowerCase())) {
            resetTimeSlotSelect("Please select a valid municipality");
            return;
        }

        try {
            const response = await fetch(
                `/bookings/available-slots?date=${encodeURIComponent(date)}&municipality=${encodeURIComponent(municipality)}`,
            );

            if (!response.ok)
                throw new Error("Failed to fetch available slots");

            const { availableSlots = [], formattedSlots = [] } =
                await response.json();

            if (availableSlots.length === 0) {
                resetTimeSlotSelect(
                    "No slots available for this date, please pick another date",
                );
            } else {
                populateTimeSlots(availableSlots, formattedSlots);
            }
        } catch (error) {
            console.error("Error fetching available slots:", error);
            resetTimeSlotSelect("Error loading slots");
        }
    }

    // Helper functions for time slot management
    function resetTimeSlotSelect(message) {
        timeSlotSelect.innerHTML = `<option value="">${message}</option>`;
        timeSlotSelect.disabled = true;
    }

    function populateTimeSlots(slots, formattedSlots) {
        timeSlotSelect.innerHTML =
            '<option value="">Select a time slot</option>';
        slots.forEach((slot, index) => {
            const option = document.createElement("option");
            option.value = slot;
            option.textContent = formattedSlots[index] || `Slot ${slot}`;
            timeSlotSelect.appendChild(option);
        });
        timeSlotSelect.disabled = false;
    }

    function isDateInPast(dateString) {
        const selectedDate = new Date(dateString);
        const today = new Date();
        selectedDate.setHours(0, 0, 0, 0);
        today.setHours(0, 0, 0, 0);
        return selectedDate < today;
    }

    // Event listeners for dynamic slot updates (only if elements exist)
    if (dateInput && municipalityInput && timeSlotSelect) {
        dateInput.addEventListener("change", updateAvailableTimeSlots);
        municipalityInput.addEventListener(
            "input",
            debounce(updateAvailableTimeSlots, 500),
        );
        municipalityInput.addEventListener("change", updateAvailableTimeSlots);
    }

    // Debounce function to avoid excessive API calls
    function debounce(func, wait) {
        let timeout;
        return (...args) => {
            clearTimeout(timeout);
            timeout = setTimeout(() => func(...args), wait);
        };
    }

    function showMessage(msg, isError) {
        if (feedback) {
            feedback.textContent = msg;
            feedback.style.color = isError ? "#b45309" : "#0f5132";
        } else {
            // fallback to console if there is no feedback element on the page
            if (isError) {
                console.error(msg);
            } else {
                console.log(msg);
            }
        }
    }

    // Booking form behavior only when present
    if (bookingForm) {
        if (clearBtn) {
            clearBtn.addEventListener("click", function () {
                bookingForm.reset();
                if (feedback) feedback.textContent = "";
                // Reset time slot selection
                if (timeSlotSelect) {
                    timeSlotSelect.disabled = true;
                    timeSlotSelect.innerHTML =
                        '<option value="">Select date and municipality first</option>';
                }
                // Reset input styling
                [
                    nameInput,
                    descriptionInput,
                    municipalityInput,
                    itemTypeSelect,
                ].forEach((input) => {
                    if (input) {
                        input.style.borderColor = "";
                    }
                });
                // clear the result block and show the English default
                if (resultBlock) {
                    resultBlock.textContent = "No results";
                    resultBlock.style.marginTop = "1.2rem";
                }
            });
        }

        bookingForm.addEventListener("submit", async function (e) {
            e.preventDefault();

            // Defensive checks for inputs
            const name = nameInput ? (nameInput.value || "").trim() : "";
            const municipality = municipalityInput
                ? (municipalityInput.value || "").trim()
                : "";
            const description = descriptionInput
                ? (descriptionInput.value || "").trim()
                : "";
            const itemType = itemTypeSelect ? itemTypeSelect.value : "";
            const date = dateInput ? dateInput.value : "";
            const timeSlot = timeSlotSelect ? timeSlotSelect.value : "";

            if (
                !name ||
                !municipality ||
                !description ||
                !itemType ||
                !date ||
                !timeSlot
            ) {
                return showMessage(
                    "Please fill in all fields before submitting.",
                    true,
                );
            }

            // Validate name format
            const nameError = validateName(name);
            if (nameError) {
                nameInput.style.borderColor = "#dc3545";
                return showMessage(nameError, true);
            }

            // Validate description length
            const descError = validateDescription(description);
            if (descError) {
                descriptionInput.style.borderColor = "#dc3545";
                return showMessage(descError, true);
            }

            // Validate date is not in the past
            if (isDateInPast(date)) {
                return showMessage(
                    "Cannot book appointments in the past. Please select today's date or a future date.",
                    true,
                );
            }

            if (
                !window.validMunicipalities.includes(municipality.toLowerCase())
            ) {
                return showMessage(
                    "Please select a valid municipality from the list.",
                    true,
                );
            }

            // Validate time slot is a valid number
            const timeSlotNumber = parseInt(timeSlot);
            if (
                isNaN(timeSlotNumber) ||
                timeSlotNumber < 0 ||
                timeSlotNumber > 8
            ) {
                return showMessage("Please select a valid time slot.", true);
            }

            showMessage("Sending request...", false);

            try {
                const payload = {
                    citizenName: name,
                    municipality: municipality,
                    description: description,
                    itemType: itemType,
                    bookingDate: date,
                    timeSlot: timeSlotNumber,
                };

                const res = await fetch(apiBase + "/bookings", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload),
                });

                if (!res.ok) {
                    let errorMessage = "Server error";
                    try {
                        const errorData = await res.json();
                        if (errorData.error) {
                            errorMessage = errorData.error;
                        }
                    } catch {
                        const txt = await res.text();
                        errorMessage = txt || "Server error";
                    }
                    throw new Error(errorMessage);
                }

                const data = await res.json();
                showMessage(
                    `✅ Booking confirmed! Token: ${data.token}`,
                    false,
                );
                // show token/result in the result block (ensure spacing)
                if (resultBlock) {
                    resultBlock.textContent = JSON.stringify(data, null, 2);
                    resultBlock.style.marginTop = "1.2rem";
                }
                bookingForm.reset();
                // Reset time slot selection after form reset
                if (timeSlotSelect) {
                    timeSlotSelect.disabled = true;
                    timeSlotSelect.innerHTML =
                        '<option value="">Select date and municipality first</option>';
                }
            } catch (err) {
                console.error(err);
                showMessage(
                    "Error creating booking: " + (err.message || err),
                    true,
                );
            }
        });
    }

    // Small utility: create a transient toast message
    function createToast(message, isError) {
        const toast = document.createElement("div");
        toast.className = "citizen-toast";
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

    // Cancel booking flow: confirmation + PATCH request + update display + toast
    async function cancelBooking() {
        if (!currentBooking || !currentBooking.id) return;

        const confirmed = await showConfirmation(
            "Cancel booking " +
                currentBooking.token +
                "? This action cannot be undone.",
        );
        if (!confirmed) return;

        try {
            const res = await fetch(
                apiBase +
                    "/bookings/" +
                    encodeURIComponent(currentBooking.id) +
                    "?status=" +
                    encodeURIComponent("CANCELLED"),
                { method: "PATCH" },
            );
            if (!res.ok) {
                const txt = await res.text();
                throw new Error(txt || "Server error");
            }

            const updated = await res.json();
            currentBooking = updated;

            // Update the display
            resultBlock.textContent = JSON.stringify(updated, null, 2);
            resultBlock.style.marginTop = "1.2rem";

            // Hide cancel button if booking is now cancelled
            if (updated.status === "CANCELLED") {
                cancelButtonContainer.style.display = "none";
            }

            createToast("Booking cancelled successfully", false);
        } catch (err) {
            console.error(err);
            createToast(
                "Error cancelling booking: " + (err.message || err),
                true,
            );
        }
    }

    // Attach cancel button handler if element exists
    if (cancelBookingBtn) {
        cancelBookingBtn.addEventListener("click", function (e) {
            e.preventDefault();
            cancelBooking();
        });
    }

    // Token check behavior only when relevant elements exist
    if (checkBtn && tokenInput && resultBlock) {
        checkBtn.addEventListener("click", async function () {
            const token = (tokenInput.value || "").trim();
            if (!token) {
                resultBlock.textContent = "Please enter a token.";
                cancelButtonContainer.style.display = "none";
                currentBooking = null;
                return;
            }

            resultBlock.textContent = "Loading...";
            cancelButtonContainer.style.display = "none";
            try {
                const res = await fetch(
                    apiBase + "/bookings/" + encodeURIComponent(token),
                );
                if (res.status === 404) {
                    resultBlock.textContent = "Booking not found.";
                    cancelButtonContainer.style.display = "none";
                    currentBooking = null;
                    return;
                }
                if (!res.ok) {
                    const txt = await res.text();
                    throw new Error(txt || "Server error");
                }
                const data = await res.json();
                resultBlock.textContent = JSON.stringify(data, null, 2);
                resultBlock.style.marginTop = "1.2rem";
                currentBooking = data;

                // Show cancel button only if booking is not already cancelled
                if (data.status !== "CANCELLED") {
                    cancelButtonContainer.style.display = "flex";
                } else {
                    cancelButtonContainer.style.display = "none";
                }
            } catch (err) {
                console.error(err);
                resultBlock.textContent =
                    "Error fetching: " + (err.message || err);
                cancelButtonContainer.style.display = "none";
                currentBooking = null;
            }
        });

        // allow Enter key inside input to trigger the search
        tokenInput.addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                e.preventDefault();
                checkBtn.click();
            }
        });
    }

    // Enhanced UX: municipality autocomplete suggestion (only if municipalities and input exist)
    if (
        municipalityInput &&
        typeof MUNICIPALITIES !== "undefined" &&
        MUNICIPALITIES
    ) {
        municipalityInput.addEventListener("input", (e) => {
            const value = e.target.value.toLowerCase();
            if (!value) return;

            const match = MUNICIPALITIES.find((m) =>
                m.toLowerCase().startsWith(value),
            );
            if (match) {
                e.target.setAttribute("placeholder", `E.g.: ${match}`);
            }
        });
    }

    // Validation functions
    function validateName(name) {
        if (!name || name.trim().length === 0) {
            return "Name cannot be empty";
        }

        // Check for at least two words
        const words = name.trim().split(/\s+/);
        if (words.length < 2) {
            return "Name must have at least two words";
        }

        // Check each word starts with uppercase and contains only letters
        const namePattern = /^[A-Z][a-z]+$/;
        for (let word of words) {
            if (!namePattern.test(word)) {
                return "Each word must start with uppercase letter and contain only letters";
            }
        }

        return null; // Valid
    }

    function validateDescription(description) {
        if (!description || description.trim().length === 0) {
            return "Description cannot be empty";
        }

        if (description.trim().length < 4) {
            return "Description must have at least 4 characters";
        }

        return null; // Valid
    }
})();
