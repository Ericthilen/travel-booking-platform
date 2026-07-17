document.addEventListener("DOMContentLoaded", () => {
    const accountPanel = document.querySelector(".account-preview-card");

    if (!accountPanel) {
        return;
    }

    const menuLinks = Array.from(accountPanel.querySelectorAll(".account-menu [data-account-target]"));
    const scrollLinks = Array.from(document.querySelectorAll("[data-account-target]"));

    const setActiveLink = (targetName) => {
        menuLinks.forEach((link) => {
            link.classList.toggle(
                "active-account-link",
                link.dataset.accountTarget === targetName
            );
        });
    };

    const scrollToSection = (targetName) => {
        const section = document.querySelector(`[data-account-section="${targetName}"]`);

        if (!section) {
            return;
        }

        setActiveLink(targetName);
        section.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    };

    scrollLinks.forEach((link) => {
        link.addEventListener("click", (event) => {
            event.preventDefault();
            scrollToSection(link.dataset.accountTarget);
        });
    });

    const query = new URLSearchParams(window.location.search);
    const page = query.get("page");

    if (page) {
        window.requestAnimationFrame(() => scrollToSection(page));
    } else {
        setActiveLink("profile");
    }
});
