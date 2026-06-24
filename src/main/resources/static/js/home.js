const searchForm = document.querySelector(".search-card");

searchForm.addEventListener("submit", function (event) {
    event.preventDefault();

    const destination = document.querySelector("#destination").value.trim();

    if (!destination) {
        alert("Skriv vart du vill resa.");
        return;
    }

    alert("Sökning kommer byggas i nästa steg: " + destination);
});