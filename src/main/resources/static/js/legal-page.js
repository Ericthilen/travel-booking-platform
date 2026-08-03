document.addEventListener("DOMContentLoaded", () => {
    const legalPage = document.querySelector(".legal-page");
    const legalHero = document.querySelector(".legal-hero");
    const legalLayout = document.querySelector(".legal-layout");
    const legalContent = document.querySelector(".legal-content-card");
    const legalNavLinks = [...document.querySelectorAll("[data-legal-nav-link]")];

    if (
        !legalPage
        || !legalHero
        || !legalLayout
        || !legalContent
        || legalNavLinks.length === 0
    ) {
        return;
    }

    const setActiveLink = (pathname) => {
        legalNavLinks.forEach((link) => {
            const isActive = new URL(link.href).pathname === pathname;

            link.classList.toggle("is-active", isActive);

            if (isActive) {
                link.setAttribute("aria-current", "page");
            } else {
                link.removeAttribute("aria-current");
            }
        });
    };

    const replacePageContent = (
        documentHtml,
        targetUrl,
        pushState,
        layoutTop
    ) => {
        const parser = new DOMParser();
        const nextDocument = parser.parseFromString(documentHtml, "text/html");
        const nextHero = nextDocument.querySelector(".legal-hero");
        const nextContent = nextDocument.querySelector(".legal-content-card");
        const nextTitle = nextDocument.querySelector("title");

        if (!nextHero || !nextContent) {
            window.location.href = targetUrl;
            return;
        }

        legalHero.innerHTML = nextHero.innerHTML;
        legalContent.innerHTML = nextContent.innerHTML;

        if (nextTitle) {
            document.title = nextTitle.textContent;
        }

        if (pushState) {
            window.history.pushState(
                {},
                "",
                targetUrl
            );
        }

        setActiveLink(new URL(targetUrl).pathname);

        const nextLayoutTop = legalLayout.getBoundingClientRect().top;

        window.scrollBy({
            top: nextLayoutTop - layoutTop,
            behavior: "auto"
        });
    };

    const loadLegalPage = async (targetUrl, pushState = true) => {
        const layoutTop = legalLayout.getBoundingClientRect().top;

        legalPage.classList.add("is-loading");

        try {
            const response = await fetch(targetUrl, {
                headers: {
                    "X-Requested-With": "fetch"
                }
            });

            if (!response.ok) {
                window.location.href = targetUrl;
                return;
            }

            replacePageContent(
                await response.text(),
                targetUrl,
                pushState,
                layoutTop
            );
        } catch {
            window.location.href = targetUrl;
        } finally {
            legalPage.classList.remove("is-loading");
        }
    };

    legalNavLinks.forEach((link) => {
        link.addEventListener("click", (event) => {
            const targetUrl = link.href;

            if (new URL(targetUrl).pathname === window.location.pathname) {
                event.preventDefault();
                setActiveLink(window.location.pathname);
                return;
            }

            event.preventDefault();
            loadLegalPage(targetUrl);
        });
    });

    window.addEventListener("popstate", () => {
        loadLegalPage(window.location.href, false);
    });

    setActiveLink(window.location.pathname);
});
