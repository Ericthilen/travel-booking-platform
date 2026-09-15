const chatRoot = document.querySelector(".customer-chat");

if (chatRoot) {
    const openButton = chatRoot.querySelector("[data-chat-open]");
    const closeButton = chatRoot.querySelector("[data-chat-close]");
    const panel = chatRoot.querySelector("[data-chat-panel]");
    const messagesElement = chatRoot.querySelector("[data-chat-messages]");
    const form = chatRoot.querySelector("[data-chat-form]");
    const input = chatRoot.querySelector("[data-chat-input]");
    const statusText = chatRoot.querySelector("[data-chat-status]");
    const closeDialog = chatRoot.querySelector("[data-chat-close-dialog]");
    const closeDialogTitle = closeDialog?.querySelector("h3");
    const closeDialogText = closeDialog?.querySelector("p");
    const endChatButton = chatRoot.querySelector("[data-chat-end]");
    const cancelCloseButton = chatRoot.querySelector("[data-chat-cancel-close]");
    const csrfToken = chatRoot.dataset.csrfToken;
    const csrfHeader = chatRoot.dataset.csrfHeader;
    const chatUser = chatRoot.dataset.chatUser || "guest";
    const storageKey = "erigoChatPublicId:" + chatUser;

    let publicId = sessionStorage.getItem(storageKey);
    let currentChatStatus = "";
    let isSending = false;
    let eventSource;
    let typingTimer;
    let typingStopTimer;
    let remoteTypingName = "";
    let remoteTypingPreview = "";
    let remoteTypingDots = 1;

    const headers = () => {
        const requestHeaders = {
            "Content-Type": "application/json"
        };

        if (csrfToken && csrfHeader) {
            requestHeaders[csrfHeader] = csrfToken;
        }

        return requestHeaders;
    };

    const scrollToBottom = () => {
        messagesElement.scrollTop = messagesElement.scrollHeight;
    };

    const firstName = (name) => {
        if (!name) {
            return "";
        }

        return name.trim().split(/\s+/)[0] || "";
    };

    const senderLabel = (message) => {
        if (message.sender === "CUSTOMER") {
            return "Du";
        }

        if (message.sender === "AGENT") {
            const author = message.author || "Kundtjänst";

            if (author.includes("(Kundtjänst)")) {
                return author;
            }

            return (firstName(author) || "Kundtjänst") + " (Kundtjänst)";
        }

        if (message.sender === "SYSTEM") {
            return message.author || "Kundtjänst";
        }

        return "EriGo Assist";
    };

    const renderMessages = (chat) => {
        messagesElement.replaceChildren();

        chat.messages.forEach((message) => {
            const bubble = document.createElement("article");
            const meta = document.createElement("span");
            const text = document.createElement("p");

            bubble.className = "customer-chat-message "
                    + "is-"
                    + message.sender.toLowerCase();
            meta.textContent =
                    senderLabel(message)
                    + " · "
                    + message.time;
            if (message.edited) {
                meta.textContent += " · redigerat";
            }
            text.textContent = message.message;

            bubble.append(meta, text);

            if (message.identificationRequest) {
                bubble.appendChild(identificationForm(message));
            }

            messagesElement.appendChild(bubble);
        });

        statusText.textContent = chat.statusLabel;
        currentChatStatus = chat.status;
        chatRoot.classList.toggle(
            "is-escalated",
            chat.status === "ESCALATED"
        );

        form.hidden = false;

        if (chat.status === "CLOSED") {
            input.placeholder =
                    "Skriv ett nytt meddelande för att återaktivera chatten...";
        } else {
            input.placeholder = "Skriv din fråga här...";
        }

        renderTypingIndicator();
        scrollToBottom();
    };

    const identificationForm = (message) => {
        if (message.identificationSubmitted) {
            const sent = document.createElement("strong");
            sent.className = "customer-identification-sent";
            sent.textContent = "Skickat";

            return sent;
        }

        const formElement = document.createElement("form");
        const customerNumber = document.createElement("input");
        const bookingNumber = document.createElement("input");
        const firstName = document.createElement("input");
        const lastName = document.createElement("input");
        const submitButton = document.createElement("button");

        formElement.className = "customer-identification-form";
        formElement.dataset.identificationForm = "true";
        formElement.dataset.requestMessageId = message.id;
        customerNumber.name = "customerNumber";
        customerNumber.placeholder = "Kundnummer";
        customerNumber.autocomplete = "off";
        bookingNumber.name = "bookingNumber";
        bookingNumber.placeholder = "Bokningsnummer";
        bookingNumber.required = true;
        bookingNumber.autocomplete = "off";
        firstName.name = "firstName";
        firstName.placeholder = "Förnamn";
        firstName.autocomplete = "given-name";
        lastName.name = "lastName";
        lastName.placeholder = "Efternamn";
        lastName.autocomplete = "family-name";
        submitButton.type = "submit";
        submitButton.textContent = "Skicka uppgifter";

        formElement.append(
                customerNumber,
                bookingNumber,
                firstName,
                lastName,
                submitButton
        );

        return formElement;
    };

    const renderTypingIndicator = () => {
        const oldIndicator =
                messagesElement.querySelector("[data-typing-indicator]");

        if (oldIndicator) {
            oldIndicator.remove();
        }

        if (!remoteTypingName) {
            return;
        }

        const indicator = document.createElement("article");
        indicator.className = "customer-chat-message is-typing";
        indicator.dataset.typingIndicator = "true";

        const typingLine = document.createElement("strong");
        typingLine.textContent =
                remoteTypingName
                + " skriver"
                + ".".repeat(remoteTypingDots);

        indicator.appendChild(typingLine);

        if (remoteTypingPreview) {
            const preview = document.createElement("p");
            preview.textContent = remoteTypingPreview;
            indicator.appendChild(preview);
        }

        messagesElement.appendChild(indicator);
        scrollToBottom();
    };

    const startTypingLoop = () => {
        if (typingTimer) {
            return;
        }

        typingTimer = window.setInterval(() => {
            remoteTypingDots = remoteTypingDots === 3 ? 1 : remoteTypingDots + 1;
            renderTypingIndicator();
        }, 450);
    };

    const stopRemoteTyping = () => {
        remoteTypingName = "";
        remoteTypingPreview = "";
        remoteTypingDots = 1;

        if (typingTimer) {
            window.clearInterval(typingTimer);
            typingTimer = null;
        }

        renderTypingIndicator();
    };

    const handleTypingEvent = (event) => {
        const typing = JSON.parse(event.data);

        if (typing.actor !== "AGENT" && typing.actor !== "AI") {
            return;
        }

        if (!typing.typing) {
            stopRemoteTyping();
            return;
        }

        remoteTypingName = typing.name
                || (typing.actor === "AI" ? "EriGo Assistent" : "Kundtjänst");
        remoteTypingPreview = "";
        startTypingLoop();
        renderTypingIndicator();
    };

    const connectStream = () => {
        if (!publicId || eventSource) {
            return;
        }

        eventSource = new EventSource("/chat/" + publicId + "/stream");
        eventSource.addEventListener("refresh", fetchChat);
        eventSource.addEventListener("typing", handleTypingEvent);
        eventSource.onerror = () => {
            eventSource.close();
            eventSource = null;
        };
    };

    const disconnectStream = () => {
        if (eventSource) {
            eventSource.close();
            eventSource = null;
        }
    };

    const fetchChat = async () => {
        if (!publicId) {
            return;
        }

        const response = await fetch("/chat/" + publicId);

        if (!response.ok) {
            sessionStorage.removeItem(storageKey);
            publicId = "";
            return;
        }

        renderMessages(await response.json());
    };

    const startChat = async () => {
        if (publicId) {
            await fetchChat();

            if (publicId) {
                return;
            }
        }

        const response = await fetch("/chat/start", {
            method: "POST",
            headers: headers()
        });

        if (!response.ok) {
            return;
        }

        const chat = await response.json();
        publicId = chat.publicId;
        sessionStorage.setItem(storageKey, publicId);
        renderMessages(chat);
        connectStream();
    };

    const sendMessage = async (message) => {
        if (!message || isSending) {
            return;
        }

        isSending = true;
        form.classList.add("is-sending");

        await startChat();

        let response = await fetch("/chat/" + publicId + "/messages", {
            method: "POST",
            headers: headers(),
            body: JSON.stringify({ message })
        });

        if (response.status === 403) {
            sessionStorage.removeItem(storageKey);
            publicId = "";
            await startChat();
            response = await fetch("/chat/" + publicId + "/messages", {
                method: "POST",
                headers: headers(),
                body: JSON.stringify({ message })
            });
        }

        if (response.ok) {
            input.value = "";
            sendTyping(false);
            renderMessages(await response.json());
        }

        form.classList.remove("is-sending");
        isSending = false;
    };

    const submitIdentification = async (formElement) => {
        if (!publicId || isSending) {
            return;
        }

        isSending = true;
        formElement.classList.add("is-sending");

        const formData = new FormData(formElement);
        const response = await fetch("/chat/" + publicId + "/identify", {
            method: "POST",
            headers: headers(),
            body: JSON.stringify({
                customerNumber: formData.get("customerNumber") || "",
                bookingNumber: formData.get("bookingNumber") || "",
                firstName: formData.get("firstName") || "",
                lastName: formData.get("lastName") || "",
                requestMessageId: formElement.dataset.requestMessageId || null
            })
        });

        if (response.ok) {
            renderMessages(await response.json());
        }

        formElement.classList.remove("is-sending");
        isSending = false;
    };

    const openChat = async () => {
        panel.hidden = false;
        chatRoot.classList.add("is-open");

        if (publicId) {
            await fetchChat();
            connectStream();
        }

        input.focus();
    };

    const closeChat = () => {
        panel.hidden = true;
        closeDialog.hidden = true;
        chatRoot.classList.remove("is-open");

        disconnectStream();
        stopRemoteTyping();
    };

    const sendTyping = async (active, preview = "") => {
        if (!publicId) {
            return;
        }

        await fetch(
                "/chat/"
                + publicId
                + "/typing?active="
                + encodeURIComponent(active)
                + "&preview="
                + encodeURIComponent(preview),
                {
                    method: "POST",
                    headers: headers()
                }
        );
    };

    const customerIsTyping = async () => {
        if (!publicId) {
            return;
        }

        sendTyping(true, input.value);

        if (typingStopTimer) {
            window.clearTimeout(typingStopTimer);
        }

        typingStopTimer = window.setTimeout(() => {
            sendTyping(false, input.value);
        }, 1100);
    };

    const showCloseDialog = () => {
        if (!publicId) {
            closeChat();
            return;
        }

        const isClosed = currentChatStatus === "CLOSED";

        if (closeDialogTitle) {
            closeDialogTitle.textContent = isClosed
                    ? "Chatten är avslutad"
                    : "Avsluta chatten";
        }

        if (closeDialogText) {
            closeDialogText.textContent = isClosed
                    ? "Chatten är redan avslutad."
                    : "Kryssa rutan";
        }

        endChatButton.hidden = isClosed;
        cancelCloseButton.textContent = isClosed
                ? "Stäng fönstret"
                : "Stäng fönstret";
        closeDialog.hidden = false;
    };

    const endChat = async () => {
        if (!publicId) {
            return;
        }

        const response = await fetch("/chat/" + publicId + "/close", {
            method: "POST",
            headers: headers()
        });

        if (response.ok) {
            sessionStorage.removeItem(storageKey);
            publicId = "";
        }

        closeChat();
    };

    openButton.addEventListener("click", openChat);
    closeButton.addEventListener("click", showCloseDialog);
    endChatButton.addEventListener("click", endChat);
    cancelCloseButton.addEventListener("click", closeChat);

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        sendMessage(input.value.trim());
    });

    messagesElement.addEventListener("submit", (event) => {
        const identification = event.target.closest(
                "[data-identification-form]"
        );

        if (identification) {
            event.preventDefault();
            submitIdentification(identification);
        }
    });

    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            sendMessage(input.value.trim());
        }
    });

    input.addEventListener("input", customerIsTyping);

    input.addEventListener("blur", () => {
        sendTyping(false);
    });
}
