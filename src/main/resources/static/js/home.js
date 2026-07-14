const searchForm = document.querySelector(".search-form");
const menuButton = document.querySelector(".menu-button");
const navigation = document.querySelector(".navigation");

if (searchForm) {
    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const destinationInput =
            document.querySelector("#destination");

        const destination = destinationInput
            ? destinationInput.value.trim()
            : "";

        if (!destination) {
            alert("Skriv vart du vill resa.");
            return;
        }

        window.location.href =
            "/resor?destination="
            + encodeURIComponent(destination);
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