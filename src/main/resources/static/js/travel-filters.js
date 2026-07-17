document.addEventListener("DOMContentLoaded", () => {
    const filterForm = document.querySelector(".travel-filter-form");

    if (!filterForm) {
        return;
    }

    const priceInput = filterForm.querySelector("#maxPrice");
    const priceText = filterForm.querySelector("[data-price-value]");
    let submitTimer;

    const submitFilters = () => {
        window.clearTimeout(submitTimer);
        submitTimer = window.setTimeout(() => {
            filterForm.requestSubmit();
        }, 350);
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

    if (priceInput) {
        priceInput.addEventListener("input", () => {
            updatePriceText();
            submitFilters();
        });
    }
});
