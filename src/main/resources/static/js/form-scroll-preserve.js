(() => {
    const storageKey = "erigo:form-scroll-position";
    const submitKey = "erigo:last-form-submit";
    const pendingClassName = "erigo-scroll-restore-pending";

    if ("scrollRestoration" in history) {
        history.scrollRestoration = "manual";
    }

    const injectPendingStyle = () => {
        if (document.getElementById("erigo-scroll-restore-style")) {
            return;
        }

        const style = document.createElement("style");
        style.id = "erigo-scroll-restore-style";
        style.textContent = `
            html.${pendingClassName},
            html.${pendingClassName} body {
                visibility: hidden !important;
            }
        `;

        document.head.appendChild(style);
    };

    const readStoredPosition = () => {
        try {
            const stored = sessionStorage.getItem(storageKey);
            return stored ? JSON.parse(stored) : null;
        } catch (error) {
            return null;
        }
    };

    const readLastSubmit = () => {
        try {
            const stored = sessionStorage.getItem(submitKey);
            return stored ? JSON.parse(stored) : null;
        } catch (error) {
            return null;
        }
    };

    const buildFormState = (form) => {
        const action = new URL(form.action || window.location.href, window.location.href);

        return {
            path: window.location.pathname,
            y: window.scrollY,
            actionPath: action.pathname,
            submittedAt: Date.now()
        };
    };

    const scrollWithoutAnimation = (y) => {
        window.scrollTo({
            top: y,
            left: 0,
            behavior: "auto"
        });
    };

    const originalScrollBehavior = document.documentElement.style.scrollBehavior;
    const storedPosition = window.__erigoPendingScrollPosition || readStoredPosition();

    if (storedPosition
            && (storedPosition.path === window.location.pathname
                    || storedPosition.actionPath === window.location.pathname)) {
        window.__erigoRestoredScroll = true;
        injectPendingStyle();
        document.documentElement.classList.add(pendingClassName);
        document.documentElement.style.scrollBehavior = "auto";

        const targetY = storedPosition.y || 0;

        const revealPage = () => {
            sessionStorage.removeItem(storageKey);
            scrollWithoutAnimation(targetY);

            requestAnimationFrame(() => {
                scrollWithoutAnimation(targetY);
                document.documentElement.classList.remove(pendingClassName);
                window.__erigoPendingScrollPosition = null;

                window.setTimeout(() => {
                    document.documentElement.style.scrollBehavior = originalScrollBehavior;
                }, 120);
            });
        };

        const restoreWhenReady = (attempt = 0) => {
            scrollWithoutAnimation(targetY);

            const pageIsTallEnough =
                    document.documentElement.scrollHeight >= targetY + window.innerHeight
                    || attempt > 20;

            if (pageIsTallEnough) {
                requestAnimationFrame(revealPage);
                return;
            }

            requestAnimationFrame(() => restoreWhenReady(attempt + 1));
        };

        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", () => restoreWhenReady());
        } else {
            restoreWhenReady();
        }
    }

    const hideMessageAfterDelay = (message) => {
        window.setTimeout(() => {
            message.classList.add("erigo-save-message-hiding");

            window.setTimeout(() => {
                message.remove();
            }, 240);
        }, 5000);
    };

    const placeSaveMessages = () => {
        const lastSubmit = readLastSubmit();
        const successMessages = Array.from(document.querySelectorAll(
                ".account-message.success,"
                + ".details-message.success-message:not(.inline-section-message),"
                + ".admin-success-message,"
                + ".admin-alert.success,"
                + ".agent-alert.success"
        )).filter((message) => !message.closest("form"));

        if (successMessages.length === 0) {
            return;
        }

        let targetForm = null;

        if (lastSubmit && Date.now() - (lastSubmit.submittedAt || 0) < 20000) {
            targetForm = Array.from(document.querySelectorAll("form")).find((form) => {
                const action = new URL(form.action || window.location.href, window.location.href);
                return action.pathname === lastSubmit.actionPath;
            });
        }

        successMessages.forEach((message) => {
            message.classList.add("erigo-local-save-message");

            if (targetForm) {
                targetForm.insertAdjacentElement("afterend", message);
            }

            hideMessageAfterDelay(message);
        });

        sessionStorage.removeItem(submitKey);
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", placeSaveMessages);
    } else {
        placeSaveMessages();
    }

    document.addEventListener("submit", (event) => {
        const form = event.target;

        if (!(form instanceof HTMLFormElement)) {
            return;
        }

        if (form.method.toLowerCase() !== "post" || form.dataset.noScrollRestore === "true") {
            return;
        }

        sessionStorage.setItem(
                storageKey,
                JSON.stringify(buildFormState(form))
        );
        sessionStorage.setItem(submitKey, JSON.stringify(buildFormState(form)));
    }, true);
})();
