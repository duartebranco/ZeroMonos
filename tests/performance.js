import http from "k6/http";
import { check, group, sleep } from "k6";

const BASE_URL = "http://127.0.0.1:8081";

export const options = {
    stages: [
        { duration: "10s", target: 5 },
        { duration: "10s", target: 5 },
        { duration: "5s", target: 0 },
    ],
    thresholds: {
        http_req_duration: ["p(95)<500"],
        http_req_failed: ["rate<0.05"],
    },
};

let municipalities = [];

export function setup() {
    const res = http.get(`${BASE_URL}/municipalities`);
    if (res.status === 200) {
        municipalities = res.json();
        return { municipalities };
    }
    return { municipalities: [] };
}

function getFutureBookingDate(daysOffset) {
    // Use today's date as the base (dynamically calculated)
    const today = new Date();
    const date = new Date(today);
    date.setDate(date.getDate() + daysOffset);
    return date.toISOString().split("T")[0];
}

export default function (data) {
    if (!data.municipalities || data.municipalities.length === 0) {
        return;
    }

    const operation = Math.random();

    if (operation < 0.5) {
        group("POST /bookings", () => {
            const muni =
                data.municipalities[
                    Math.floor(Math.random() * data.municipalities.length)
                ];
            // Generate dates from today + 1 day onwards (avoid past dates)
            // Use different dates for each iteration to avoid unique constraint violations
            const daysOffset = Math.floor(Math.random() * 30) + 1;
            const bookingDate = getFutureBookingDate(daysOffset);

            const firstNames = ["John", "Maria", "Pedro"];
            const lastNames = ["Silva", "Santos", "Oliveira"];
            const firstName =
                firstNames[Math.floor(Math.random() * firstNames.length)];
            const lastName =
                lastNames[Math.floor(Math.random() * lastNames.length)];

            const booking = {
                citizenName: `${firstName} ${lastName}`,
                municipality: muni,
                description: "Furniture for disposal",
                itemType: "FURNITURE",
                bookingDate: bookingDate,
                timeSlot: Math.floor(Math.random() * 9),
            };

            const res = http.post(
                `${BASE_URL}/bookings`,
                JSON.stringify(booking),
                {
                    headers: { "Content-Type": "application/json" },
                },
            );

            check(res, {
                "POST status 200 or 400": (r) =>
                    r.status === 200 || r.status === 400,
            });
        });
    } else {
        group("GET /bookings", () => {
            const res = http.get(`${BASE_URL}/bookings`);
            check(res, {
                "GET status 200": (r) => r.status === 200,
            });
        });
    }

    sleep(0.1);
}
