const searchForm = document.querySelector(".search-form");
const menuButton = document.querySelector(".menu-button");
const navigation = document.querySelector(".navigation");

if (searchForm) {
    const destinationInput = document.querySelector("#destination");
    const suggestionBox = searchForm.querySelector("[data-home-suggestions]");
    const suggestions = suggestionBox
        ? [...suggestionBox.querySelectorAll(".home-search-suggestion")]
        : [];

    const showSuggestions = () => {
        if (!destinationInput || !suggestionBox) {
            return;
        }

        const query = destinationInput.value.trim().toLowerCase();
        let visibleCount = 0;

        suggestions.forEach((suggestion) => {
            const text = suggestion.dataset.search || "";
            const words = text.split(/\s+/);
            const isVisible = query.length >= 1
                && words.some((word) => word.startsWith(query))
                && visibleCount < 3;

            suggestion.classList.toggle("is-hidden", !isVisible);

            if (isVisible) {
                visibleCount += 1;
            }
        });

        const hasSuggestions = visibleCount > 0;

        suggestionBox.hidden = !hasSuggestions;
        searchForm.classList.toggle(
            "has-visible-suggestions",
            hasSuggestions
        );
    };

    if (destinationInput) {
        destinationInput.addEventListener("input", showSuggestions);
        destinationInput.addEventListener("focus", showSuggestions);
    }

    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const formData = new FormData(searchForm);
        const params = new URLSearchParams();
        const destination = formData.get("destination")
            ? String(formData.get("destination")).trim()
            : "";

        if (!destination) {
            alert("Skriv vart du vill resa.");
            return;
        }

        formData.forEach((value, key) => {
            if (String(value).trim()) {
                params.set(
                    key,
                    String(value).trim()
                );
            }
        });

        window.location.href = "/resor?" + params.toString();
    });

    document.addEventListener("click", function (event) {
        if (!suggestionBox || !destinationInput) {
            return;
        }

        if (
            event.target === destinationInput
            || suggestionBox.contains(event.target)
        ) {
            return;
        }

        suggestionBox.hidden = true;
        searchForm.classList.remove("has-visible-suggestions");
    });
}

if (menuButton && navigation) {
    menuButton.addEventListener("click", function () {
        const menuIsOpen =
            navigation.classList.toggle("mobile-open");

        menuButton.setAttribute(
            "aria-expanded",
            String(menuIsOpen)
        );
    });
}
