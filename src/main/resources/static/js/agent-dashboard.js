const agentChatList = document.querySelector("[data-agent-chat-list]");

if (agentChatList) {
    const selectedStatus = agentChatList.dataset.selectedStatus || "ALL";
    const mineOnly = agentChatList.dataset.mine === "true";
    const currentAgentName = agentChatList.dataset.currentAgentName || "";
    const searchQuery = agentChatList.dataset.searchQuery || "";
    const supportAgents = (agentChatList.dataset.supportAgents || "")
            .split("|")
            .map((agent) => agent.trim())
            .filter(Boolean);
    const tableBody = document.querySelector("[data-agent-chat-table]");
    const emptyElement = document.querySelector("[data-agent-empty]");
    const channelButtons = document.querySelectorAll("[data-channel-filter]");
    const openChats = document.querySelector("[data-open-chats]");
    const openChatsCopy = document.querySelector("[data-open-chats-copy]");
    const waitingChats = document.querySelector("[data-waiting-chats]");
    const waitingChatsCopy = document.querySelector("[data-waiting-chats-copy]");
    const incomingEmails = document.querySelector("[data-incoming-emails]");
    const openChatsMenu = document.querySelector("[data-open-chats-menu]");
    const chatCount = document.querySelector("[data-chat-count]");
    const emailCount = document.querySelector("[data-email-count]");
    const myAssignedCount = document.querySelector("[data-my-assigned-count]");
    const totalConversations = document.querySelector("[data-total-conversations]");
    const channelChat = document.querySelector("[data-channel-chat]");
    const channelEmail = document.querySelector("[data-channel-email]");
    const conversationRange = document.querySelector("[data-conversation-range]");
    const satisfactionAverage =
            document.querySelector("[data-satisfaction-average]");
    const satisfactionCount =
            document.querySelector("[data-satisfaction-count]");
    const agentStatusControl =
            document.querySelector("[data-agent-status-control]");
    const agentStatusButton =
            document.querySelector("[data-agent-status-button]");
    const agentStatusMenu =
            document.querySelector("[data-agent-status-menu]");
    const agentStatusOptions =
            document.querySelectorAll("[data-agent-status-option]");
    const currentAgentStatus =
            document.querySelector("[data-current-agent-status]");
    const csrfToken = agentChatList.dataset.csrfToken || "";
    const csrfParameter = agentChatList.dataset.csrfParameter || "_csrf";

    let selectedChannel = "ALL";
    let latestConversations = [];
    let latestEmailConversations = [];
    let archiveRefreshQueued = false;
    const agentStatusStorageKey = "erigoAgentStatus";
    const emailStatuses = [
        ["OPEN", "Öppen"],
        ["WAITING_ON_US", "Väntar på svar från oss"],
        ["WAITING", "Väntande"],
        ["ESCALATED", "Eskalerad"],
        ["CLOSED", "Avslutad"]
    ];

    const normalizeName = (value) => {
        return String(value || "")
                .trim()
                .toLowerCase();
    };

    const localAgentName = () => {
        if (!currentAgentName.includes("@")) {
            return currentAgentName;
        }

        const name = currentAgentName
                .slice(0, currentAgentName.indexOf("@"))
                .replaceAll(".", " ")
                .replaceAll("_", " ")
                .replaceAll("-", " ");

        if (!name) {
            return "Kundtjänst";
        }

        return name.charAt(0).toUpperCase() + name.slice(1);
    };

    const statusSlug = (status) => {
        return status
                .toLowerCase()
                .replaceAll(" ", "-")
                .replace("å", "a");
    };

    const setAgentStatus = (status) => {
        if (!agentStatusControl || !agentStatusButton) {
            return;
        }

        agentStatusButton.textContent = status;
        agentStatusControl.dataset.agentStatus = statusSlug(status);

        if (currentAgentStatus) {
            currentAgentStatus.textContent = status;
            currentAgentStatus.dataset.agentStatus = statusSlug(status);
        }

        sessionStorage.setItem(agentStatusStorageKey, status);
    };

    if (agentStatusControl && agentStatusButton && agentStatusMenu) {
        const savedAgentStatus = sessionStorage.getItem(agentStatusStorageKey);

        if (savedAgentStatus) {
            setAgentStatus(savedAgentStatus);
        } else {
            setAgentStatus(agentStatusButton.textContent.trim());
        }

        agentStatusButton.addEventListener("click", () => {
            agentStatusMenu.hidden = !agentStatusMenu.hidden;
        });

        agentStatusOptions.forEach((option) => {
            option.addEventListener("click", () => {
                setAgentStatus(option.dataset.agentStatusOption);
                agentStatusMenu.hidden = true;
            });
        });

        document.addEventListener("click", (event) => {
            if (!agentStatusControl.contains(event.target)) {
                agentStatusMenu.hidden = true;
            }
        });
    }

    const statusLabel = (conversation) => {
        if (conversation.statusLabel) {
            return conversation.statusLabel;
        }

        if (conversation.status === "CLOSED") {
            return "Avslutad";
        }

        if (conversation.status === "ESCALATED") {
            return "Eskalerad";
        }

        if (conversation.status === "WAITING_FOR_AGENT") {
            return "Väntar på agent";
        }

        return "Pågår";
    };

    const statusClass = (conversation) => {
        if (conversation.status === "CLOSED") {
            return "closed";
        }

        if (conversation.status === "ESCALATED") {
            return "escalated";
        }

        if (conversation.status === "WAITING_FOR_AGENT") {
            return "waiting";
        }

        if (
            conversation.status === "WAITING"
            || conversation.status === "WAITING_ON_US"
        ) {
            return "waiting";
        }

        return "open";
    };

    const rowStatusClass = (conversation) => {
        const status = statusClass(conversation);
        return status === "open" ? "chat-row-active" : "chat-row-" + status;
    };

    const initial = (name) => {
        const cleanName = name || "Gäst";
        return cleanName.trim().charAt(0).toUpperCase() || "G";
    };

    const conversationUrl = (conversation) => {
        if (conversation.actionUrl) {
            return conversation.actionUrl;
        }

        return "/kundtjanst/chattar/" + conversation.id;
    };

    const csrfInput = () => {
        if (!csrfToken) {
            return null;
        }

        const input = document.createElement("input");
        input.type = "hidden";
        input.name = csrfParameter;
        input.value = csrfToken;

        return input;
    };

    const dashboardInput = () => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "dashboard";
        input.value = "true";

        return input;
    };

    const saveQuickTicketForm = async (event) => {
        event.preventDefault();
        event.stopPropagation();

        const form = event.currentTarget;
        const submitButton = form.querySelector("button[type='submit']");

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "Sparar";
        }

        const response = await fetch(form.action, {
            method: "POST",
            body: new URLSearchParams(new FormData(form)),
            headers: {
                "Accept": "text/html"
            }
        });

        if (response.ok) {
            await refreshDashboard();
            return;
        }

        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = "Spara";
        }
    };

    const quickAgentForm = (conversation) => {
        const form = document.createElement("form");
        const select = document.createElement("select");
        const csrf = csrfInput();

        form.className = "quick-ticket-form quick-agent-form";
        form.method = "post";
        form.action = "/kundtjanst/mejl/" + conversation.id + "/agent";

        select.name = "agentName";
        select.setAttribute("aria-label", "Välj handläggare");

        const emptyOption = document.createElement("option");
        emptyOption.value = "";
        emptyOption.textContent = "Ej tilldelad";
        emptyOption.selected = !conversation.assignedAgentName;
        select.appendChild(emptyOption);

        supportAgents.forEach((agent) => {
            const option = document.createElement("option");
            option.value = agent;
            option.textContent = agent;
            option.selected = agent === conversation.assignedAgentName;
            select.appendChild(option);
        });

        if (
            conversation.assignedAgentName
            && !supportAgents.includes(conversation.assignedAgentName)
        ) {
            const currentOption = document.createElement("option");
            currentOption.value = conversation.assignedAgentName;
            currentOption.textContent = conversation.assignedAgentName;
            currentOption.selected = true;
            select.appendChild(currentOption);
        }

        select.addEventListener("change", () => form.requestSubmit());
        form.addEventListener("submit", saveQuickTicketForm);

        if (csrf) {
            form.appendChild(csrf);
        }

        form.append(dashboardInput(), select);

        return form;
    };

    const quickStatusForm = (conversation) => {
        const form = document.createElement("form");
        const select = document.createElement("select");
        const csrf = csrfInput();

        form.className = "quick-ticket-form quick-status-form";
        form.method = "post";
        form.action = "/kundtjanst/mejl/" + conversation.id + "/status";

        select.name = "status";
        select.setAttribute("aria-label", "Ändra status");

        emailStatuses.forEach(([value, label]) => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = label;
            option.selected = conversation.status === value;
            select.appendChild(option);
        });

        select.addEventListener("change", () => form.requestSubmit());
        form.addEventListener("submit", saveQuickTicketForm);

        if (csrf) {
            form.appendChild(csrf);
        }

        form.append(dashboardInput(), select);

        return form;
    };

    const conversationRow = (conversation) => {
        const row = document.createElement("tr");
        row.dataset.href = conversationUrl(conversation);
        row.dataset.archiveAt = conversation.archiveAt || "";
        row.className = rowStatusClass(conversation);

        const customerCell = document.createElement("td");
        const customer = document.createElement("div");
        const avatar = document.createElement("span");
        const customerText = document.createElement("div");
        const customerName = document.createElement("strong");
        const customerEmail = document.createElement("small");

        customer.className = "crm-customer-cell";
        avatar.textContent = initial(conversation.customerName);
        customerName.textContent = conversation.customerName || "Gäst";
        customerEmail.textContent = conversation.customerEmail || "Ej inloggad";
        customerText.append(customerName, customerEmail);
        customer.append(avatar, customerText);
        customerCell.appendChild(customer);

        const channelCell = document.createElement("td");
        const channel = document.createElement("span");
        const isEmail = conversation.channel === "EMAIL";
        channel.className = "crm-channel-badge " + (isEmail ? "email" : "chat");
        channel.textContent = isEmail ? "E-post" : "Chatt";
        channelCell.appendChild(channel);

        const subjectCell = document.createElement("td");
        subjectCell.textContent = conversation.subject || "Kundchatt";

        const agentCell = document.createElement("td");
        if (isEmail) {
            agentCell.appendChild(quickAgentForm(conversation));
        } else {
            agentCell.textContent = conversation.assignedAgentName
                    || "Ej ansluten";
        }

        const updatedCell = document.createElement("td");
        updatedCell.textContent = conversation.updatedAt;

        const statusCell = document.createElement("td");
        if (isEmail) {
            statusCell.appendChild(quickStatusForm(conversation));
        } else {
            const status = document.createElement("span");
            status.className = "agent-status " + statusClass(conversation);
            status.textContent = statusLabel(conversation);
            statusCell.appendChild(status);
        }

        if (!isEmail && conversation.status === "CLOSED") {
            const timer = document.createElement("small");
            timer.className = "agent-archive-timer";
            timer.dataset.archiveTimer = "true";
            timer.dataset.archiveAt = conversation.archiveAt || "";
            timer.textContent = "Arkiveras om 15:00";
            statusCell.appendChild(timer);
        }

        const actionCell = document.createElement("td");
        const actions = document.createElement("div");
        const link = document.createElement("a");
        actions.className = "agent-row-actions";
        link.className = "agent-open-chat";
        link.href = conversationUrl(conversation);
        link.textContent = "Öppna";
        actions.appendChild(link);

        if (!isEmail && conversation.status === "CLOSED") {
            const archiveForm = document.createElement("form");
            const archiveButton = document.createElement("button");

            archiveForm.method = "post";
            archiveForm.action =
                    "/kundtjanst/chattar/" + conversation.id + "/arkivera";

            if (csrfToken) {
                const csrfInput = document.createElement("input");
                csrfInput.type = "hidden";
                csrfInput.name = csrfParameter;
                csrfInput.value = csrfToken;
                archiveForm.appendChild(csrfInput);
            }

            archiveButton.className = "agent-archive-button";
            archiveButton.type = "submit";
            archiveButton.textContent = "Arkivera";
            archiveForm.appendChild(archiveButton);
            actions.appendChild(archiveForm);
        }

        actionCell.appendChild(actions);

        row.append(
                customerCell,
                channelCell,
                subjectCell,
                agentCell,
                updatedCell,
                statusCell,
                actionCell
        );

        return row;
    };

    const matchesSelectedStatus = (conversation) => {
        if (selectedStatus === "ALL") {
            return true;
        }

        if (selectedStatus === "WAITING_FOR_AGENT") {
            return conversation.status === "WAITING_FOR_AGENT";
        }

        if (selectedStatus === "ARCHIVED") {
            return conversation.status === "ARCHIVED";
        }

        return conversation.status === selectedStatus;
    };

    const visibleConversations = () => {
        const chats = latestConversations.map((conversation) => ({
            ...conversation,
            channel: conversation.channel || "CHAT"
        }));
        const emails = latestEmailConversations.map((conversation) => ({
            ...conversation,
            channel: conversation.channel || "EMAIL"
        }));

        if (selectedChannel === "CHAT") {
            return chats.filter(matchesSelectedStatus);
        }

        if (selectedChannel === "EMAIL") {
            return emails.filter(matchesSelectedStatus);
        }

        return [...chats, ...emails]
                .filter(matchesSelectedStatus)
                .sort((first, second) =>
                        String(second.updatedAt).localeCompare(String(first.updatedAt))
                );
    };

    const updateCounters = (data) => {
        const activeChats =
                data.queueOverview.openChats
                + data.queueOverview.waitingForAgentChats
                + data.queueOverview.escalatedChats;
        const emailValue = data.queueOverview.incomingEmails;

        openChats.textContent = data.queueOverview.openChats;
        openChatsCopy.textContent = data.queueOverview.openChats;
        waitingChats.textContent = data.queueOverview.waitingForAgentChats;
        waitingChatsCopy.textContent = data.queueOverview.waitingForAgentChats;
        incomingEmails.textContent = emailValue;
        openChatsMenu.textContent = activeChats;
        chatCount.textContent = activeChats;
        emailCount.textContent = emailValue;
        totalConversations.textContent = activeChats + emailValue;
        channelChat.textContent = activeChats;
        channelEmail.textContent = emailValue;

        if (myAssignedCount) {
            const agentName = normalizeName(localAgentName());
            const assignedChats =
                    (data.conversations || [])
                            .filter((conversation) =>
                                    normalizeName(conversation.assignedAgentName)
                                    === agentName
                            )
                            .length;
            const assignedEmails =
                    (data.emailConversations || [])
                            .filter((conversation) =>
                                    normalizeName(conversation.assignedAgentName)
                                    === agentName
                            )
                            .length;

            myAssignedCount.textContent = mineOnly
                    ? (data.conversations || []).length
                            + (data.emailConversations || []).length
                    : assignedChats + assignedEmails;
        }

        if (satisfactionAverage) {
            const average = Number(data.customerSatisfactionAverage || 0);
            satisfactionAverage.textContent = average > 0
                    ? average.toFixed(1) + " / 5"
                    : "0.0 / 5";
        }

        if (satisfactionCount) {
            satisfactionCount.textContent =
                    (data.customerSatisfactionCount || 0) + " svar";
        }
    };

    const renderRows = () => {
        const conversations = visibleConversations();
        tableBody.replaceChildren();

        if (!conversations.length) {
            emptyElement.hidden = false;
            conversationRange.textContent =
                    "Inga konversationer i den här vyn";
            return;
        }

        emptyElement.hidden = true;
        conversationRange.textContent =
                "Visar 1-" + conversations.length + " av " + conversations.length;

        conversations.forEach((conversation) => {
            tableBody.appendChild(conversationRow(conversation));
        });

        updateArchiveTimers();
    };

    const renderDashboard = (data) => {
        latestConversations = data.conversations;
        latestEmailConversations = data.emailConversations || [];
        updateCounters(data);
        renderRows();
    };

    const loadEmailConversations = async () => {
        const response = await window.fetch("/kundtjanst/mejl?status=ALL", {
            headers: {
                "Accept": "text/html"
            }
        });

        if (!response.ok) {
            return [];
        }

        const html = await response.text();
        const documentCopy = new DOMParser().parseFromString(html, "text/html");

        return [...documentCopy.querySelectorAll(".email-ticket-preview")]
                .map((ticket) => {
                    const link = ticket.querySelector("a");
                    const statusElement = ticket.querySelector(".agent-status");
                    const href = link ? link.getAttribute("href") : "";
                    const ticketIdMatch = href.match(/ticketId=(\d+)/);
                    const statusClass = [...ticket.classList].find((className) =>
                            className.startsWith("ticket-")
                    );
                    const status = statusClass
                            ? statusClass
                                    .replace("ticket-", "")
                                    .toUpperCase()
                            : "OPEN";

                    return {
                        id: ticketIdMatch ? Number(ticketIdMatch[1]) : 0,
                        channel: "EMAIL",
                        actionUrl: href || "/kundtjanst/mejl",
                        customerName:
                                ticket.querySelector("strong")?.textContent.trim()
                                || "Kund",
                        customerEmail: "Mejlärende",
                        subject:
                                ticket.querySelector("em")?.textContent.trim()
                                || "Mejl",
                        status,
                        statusLabel:
                                statusElement?.textContent.trim()
                                || "Öppen",
                        assignedAgentName: "",
                        updatedAt:
                                ticket.querySelector("time")?.textContent.trim()
                                || "",
                        archiveAt: ""
                    };
                });
    };

    const refreshDashboard = async () => {
        archiveRefreshQueued = false;
        const response = await fetch(
                "/kundtjanst/api/chattar?status="
                + encodeURIComponent(selectedStatus)
                + "&mine="
                + encodeURIComponent(mineOnly)
                + "&q="
                + encodeURIComponent(searchQuery),
                {
                    headers: {
                        "Accept": "application/json"
                    }
                }
        );

        if (response.ok) {
            const data = await response.json();

            if (!Array.isArray(data.emailConversations)) {
                data.emailConversations = await loadEmailConversations();
            }

            renderDashboard(data);
        }
    };

    const formatRemainingTime = (milliseconds) => {
        const seconds = Math.max(0, Math.ceil(milliseconds / 1000));
        const minutes = Math.floor(seconds / 60);
        const restSeconds = seconds % 60;

        return minutes + ":" + String(restSeconds).padStart(2, "0");
    };

    function updateArchiveTimers() {
        document.querySelectorAll("[data-archive-timer]").forEach((timer) => {
            if (!timer.dataset.archiveAt) {
                timer.textContent = "Arkiveras snart";
                return;
            }

            const archiveAt = Date.parse(timer.dataset.archiveAt);

            if (Number.isNaN(archiveAt)) {
                timer.textContent = "Arkiveras snart";
                return;
            }

            const remaining = archiveAt - Date.now();

            if (remaining <= 0) {
                timer.textContent = "Arkiverad";

                if (selectedStatus !== "ARCHIVED" && !archiveRefreshQueued) {
                    archiveRefreshQueued = true;
                    window.setTimeout(refreshDashboard, 800);
                }

                return;
            }

            timer.textContent =
                    "Arkiveras om " + formatRemainingTime(remaining);
        });
    }

    channelButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.preventDefault();
            channelButtons.forEach((item) => item.classList.remove("is-active"));
            button.classList.add("is-active");
            selectedChannel = button.dataset.channelFilter;
            renderRows();
        });
    });

    tableBody.addEventListener("click", (event) => {
        if (event.target.closest("form, button, input, select, textarea")) {
            return;
        }

        const link = event.target.closest("a");

        if (link) {
            return;
        }

        const row = event.target.closest("tr[data-href]");

        if (row) {
            window.location.href = row.dataset.href;
        }
    });

    const dashboardStream = new EventSource("/kundtjanst/api/chattar/stream");
    dashboardStream.addEventListener("refresh", refreshDashboard);
    dashboardStream.onerror = () => {
        dashboardStream.close();
    };

    updateArchiveTimers();
    refreshDashboard();
    window.setInterval(updateArchiveTimers, 1000);
    window.setInterval(refreshDashboard, 5000);
}
