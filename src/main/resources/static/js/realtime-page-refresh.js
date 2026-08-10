const realtimeRefreshPage = document.querySelector("[data-realtime-refresh]");

if (realtimeRefreshPage) {
    const protectCompose =
            realtimeRefreshPage.dataset.realtimeProtectCompose === "true";
    let refreshQueued = false;
    let firstRefreshEvent = true;

    const hasUnsavedText = () => {
        if (!protectCompose) {
            return false;
        }

        const activeElement = document.activeElement;
        const editor = document.getElementById("reply-editor");

        if (editor && editor.textContent.trim()) {
            return true;
        }

        if (!activeElement) {
            return false;
        }

        if (activeElement.closest(".email-desk-composer")) {
            return true;
        }

        return activeElement.matches(
                "input, textarea, select, [contenteditable='true']"
        );
    };

    const showRefreshNotice = () => {
        if (document.querySelector("[data-realtime-refresh-notice]")) {
            return;
        }

        const notice = document.createElement("button");
        notice.type = "button";
        notice.className = "realtime-refresh-notice";
        notice.dataset.realtimeRefreshNotice = "true";
        notice.textContent = "Nya uppdateringar finns - visa nu";
        notice.addEventListener("click", () => window.location.reload());
        document.body.appendChild(notice);
    };

    const refreshPage = () => {
        if (firstRefreshEvent) {
            firstRefreshEvent = false;
            return;
        }

        if (refreshQueued) {
            return;
        }

        refreshQueued = true;
        window.setTimeout(() => {
            if (hasUnsavedText()) {
                refreshQueued = false;
                showRefreshNotice();
                return;
            }

            window.location.reload();
        }, 350);
    };

    const eventSource = new EventSource("/kundtjanst/api/chattar/stream");
    eventSource.addEventListener("refresh", refreshPage);
    eventSource.onerror = () => eventSource.close();
}
