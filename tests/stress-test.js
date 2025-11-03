import http from "k6/http";
import { check, group, sleep } from "k6";

const BASE_URL = "http://127.0.0.1:8081";

export const options = {
    stages: [
        { duration: "10s", target: 10 },
        { duration: "10s", target: 50 },
        { duration: "10s", target: 100 },
        { duration: "10s", target: 0 },
    ],
    thresholds: {
        http_req_duration: ["p(95)<2000"],
        http_req_failed: ["rate<0.2"],
    },
};

let municipalities = [];

export function setup() {
    const res = http.get(`${BASE_URL}/municipalities`);
    if (res.status === 200) {
        municipalities = res.json();
        console.log(`Setup: Found ${municipalities.length} municipalities`);
        return { municipalities };
    }
    return { municipalities: [] };
}

function getFutureBookingDate(daysOffset) {
    const date = new Date("2025-11-03");
    date.setDate(date.getDate() + daysOffset);
    return date.toISOString().split("T")[0];
}

export default function (data) {
    if (!data.municipalities || data.municipalities.length === 0) {
        return;
    }

    const operation = Math.random();

    if (operation < 0.4) {
        group("POST /bookings", () => {
            const muni =
                data.municipalities[
                    Math.floor(Math.random() * data.municipalities.length)
                ];
            const bookingDate = getFutureBookingDate(
                Math.floor(Math.random() * 10) + 7,
            );
            const firstNames = ["John", "Maria", "Pedro", "Ana", "Carlos"];
            const lastNames = [
                "Silva",
                "Santos",
                "Oliveira",
                "Pereira",
                "Costa",
            ];
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
    } else if (operation < 0.8) {
        group("GET /bookings", () => {
            const res = http.get(`${BASE_URL}/bookings`);
            check(res, {
                "GET /bookings status 200": (r) => r.status === 200,
            });
        });
    } else {
        group("GET /municipalities", () => {
            const res = http.get(`${BASE_URL}/municipalities`);
            check(res, {
                "GET /municipalities status 200": (r) => r.status === 200,
            });
        });
    }

    sleep(0.1);
}
