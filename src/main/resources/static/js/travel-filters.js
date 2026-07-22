document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.querySelector(".travel-filter-form");

    if (!filterForm) {
        return;
    }

    const priceInput = filterForm.querySelector("#maxPrice");
    const priceText = filterForm.querySelector("[data-price-value]");
    let submitTimer;

    const submitFilters = (delay = 350) => {
        window.clearTimeout(submitTimer);
        submitTimer = window.setTimeout(() => {
            filterForm.requestSubmit();
        }, delay);
    };

    const updatePriceText = () => {
        if (!priceInput || !priceText) {
            return;
        }

        const price = Number(priceInput.value);
        priceText.textContent = price >= 20000
            ? "20 000+ kr"
            : `${price.toLocaleString("sv-SE")} kr`;
    };

    filterForm
        .querySelectorAll("select, input[type='date'], input[type='checkbox']")
        .forEach((field) => {
            field.addEventListener("change", submitFilters);
        });

    filterForm
        .querySelectorAll("input[type='number']")
        .forEach((field) => {
            field.addEventListener("input", submitFilters);
        });

    filterForm
        .querySelectorAll("input[type='search']")
        .forEach((field) => {
            field.addEventListener("input", () => {
                submitFilters(1200);
            });

            field.addEventListener("keydown", (event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    filterForm.requestSubmit();
                }
            });
        });

    if (priceInput) {
        priceInput.addEventListener("input", () => {
            updatePriceText();
            submitFilters();
        });
    }

    filterForm
        .querySelectorAll(".departure-calendar-grid button[data-date]")
        .forEach((button) => {
            button.addEventListener("click", () => {
                const dateInput = filterForm.querySelector(
                    "input[name='earliestDepartureDate']"
                );
                const monthInput = filterForm.querySelector(
                    "[data-calendar-month-input]"
                );

                if (!dateInput || button.disabled) {
                    return;
                }

                dateInput.value = button.dataset.date;

                if (monthInput) {
                    monthInput.value = button.dataset.date;
                }

                submitFilters(0);
            });
        });

    filterForm
        .querySelectorAll(".departure-calendar-heading button[data-calendar-month]")
        .forEach((button) => {
            button.addEventListener("click", () => {
                const monthInput = filterForm.querySelector(
                    "[data-calendar-month-input]"
                );

                if (!monthInput) {
                    return;
                }

                monthInput.value = button.dataset.calendarMonth;
                submitFilters(0);
            });
        });

});
