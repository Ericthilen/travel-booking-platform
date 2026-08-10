const agentConversation = document.querySelector("[data-agent-conversation]");

if (agentConversation) {
    const conversationId = agentConversation.dataset.conversationId;
    const messagesElement = agentConversation.querySelector("[data-agent-messages]");
    const replyForm = agentConversation.querySelector(".agent-reply-form");
    const replyInput = replyForm?.querySelector("textarea");
    const shortcutsToggle =
            agentConversation.querySelector("[data-shortcuts-toggle]");
    const shortcutsMenu =
            agentConversation.querySelector("[data-shortcuts-menu]");
    const replyTemplateButtons =
            agentConversation.querySelectorAll("[data-reply-template]");
    const noteToggle = agentConversation.querySelector("[data-note-toggle]");
    const replyTab = agentConversation.querySelector("[data-reply-tab]");
    const noteForm = agentConversation.querySelector("[data-agent-note-form]");
    const noteInput = noteForm?.querySelector("textarea");
    const noteClose = noteForm?.querySelector("[data-note-close]");
    const csrfToken = agentConversation.dataset.csrfToken;
    const csrfHeader = agentConversation.dataset.csrfHeader;
    let lastMessageCount = messagesElement.children.length;
    let typingTimer;
    let typingStopTimer;
    let remoteTypingName = "";
    let remoteTypingPreview = "";
    let remoteTypingDots = 1;

    messagesElement.scrollTop = messagesElement.scrollHeight;

    const setActiveComposerTab = (activeButton) => {
        agentConversation
                .querySelectorAll(".agent-composer-tabs button")
                .forEach((button) => {
                    button.classList.toggle("is-active", button === activeButton);
                });
    };

    const messageRow = (message) => {
        const row = document.createElement("article");
        const avatar = document.createElement("span");
        const bubble = document.createElement("div");
        const meta = document.createElement("span");
        const author = document.createElement("strong");
        const time = document.createElement("time");
        const text = document.createElement("p");
        const sender = message.sender.toLowerCase();

        row.className = "agent-message-row " + sender;
        avatar.className = "agent-message-avatar";
        avatar.setAttribute("aria-hidden", "true");
        bubble.className = "agent-message " + sender;
        bubble.dataset.messageId = message.id;
        author.textContent = message.author;
        time.textContent = message.createdAt;
        meta.append(author, " · ", time);
        if (message.edited) {
            const edited = document.createElement("em");
            edited.textContent = "redigerat";
            meta.append(" · ", edited);
        }
        text.textContent = message.message;
        text.dataset.messageText = "true";
        bubble.append(meta, text);

        if (message.editable) {
            const editButton = document.createElement("button");
            editButton.className = "agent-message-edit";
            editButton.type = "button";
            editButton.dataset.editMessageId = message.id;
            editButton.textContent = "Redigera";
            bubble.appendChild(editButton);
        }

        row.append(avatar, bubble);

        return row;
    };

    const renderMessages = (messages) => {
        messagesElement.replaceChildren();
        messages.forEach((message) => {
            messagesElement.appendChild(messageRow(message));
        });

        renderTypingIndicator();

        if (messages.length !== lastMessageCount) {
            messagesElement.scrollTop = messagesElement.scrollHeight;
        }

        lastMessageCount = messages.length;
    };

    const renderTypingIndicator = () => {
        const oldIndicator =
                messagesElement.querySelector("[data-typing-indicator]");

        if (oldIndicator) {
            oldIndicator.remove();
        }

        if (!remoteTypingName && !remoteTypingPreview) {
            return;
        }

        const indicator = document.createElement("article");
        const avatar = document.createElement("span");
        const bubble = document.createElement("div");

        indicator.className = "agent-message-row typing";
        indicator.dataset.typingIndicator = "true";
        avatar.className = "agent-message-avatar";
        avatar.setAttribute("aria-hidden", "true");
        bubble.className = "agent-message typing";

        const typingLine = document.createElement("strong");
        typingLine.textContent = remoteTypingName
                ? remoteTypingName
                        + " skriver"
                        + ".".repeat(remoteTypingDots)
                : "Kundens utkast";

        bubble.appendChild(typingLine);

        if (remoteTypingPreview) {
            const preview = document.createElement("p");
            preview.textContent = remoteTypingPreview;
            bubble.appendChild(preview);
        }

        indicator.append(avatar, bubble);
        messagesElement.appendChild(indicator);
        messagesElement.scrollTop = messagesElement.scrollHeight;
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

        if (typing.actor !== "CUSTOMER") {
            return;
        }

        if (!typing.typing) {
            remoteTypingName = "";
            remoteTypingPreview = typing.preview || "";

            if (!remoteTypingPreview) {
                stopRemoteTyping();
                return;
            }

            if (typingTimer) {
                window.clearInterval(typingTimer);
                typingTimer = null;
            }

            renderTypingIndicator();
            return;
        }

        remoteTypingName = typing.name || "Kund";
        remoteTypingPreview = typing.preview || "";
        startTypingLoop();
        renderTypingIndicator();
    };

    const refreshChat = async () => {
        const response = await fetch("/kundtjanst/api/chattar/" + conversationId, {
            headers: {
                "Accept": "application/json"
            }
        });

        if (response.ok) {
            const chat = await response.json();
            renderMessages(chat.messages);
        }
    };

    const startMessageEdit = (button) => {
        const bubble = button.closest(".agent-message");
        const textElement = bubble?.querySelector("[data-message-text]");

        if (!bubble || !textElement || bubble.querySelector(".agent-message-edit-form")) {
            return;
        }

        const originalText = textElement.textContent;
        const form = document.createElement("form");
        const textarea = document.createElement("textarea");
        const actions = document.createElement("div");
        const saveButton = document.createElement("button");
        const cancelButton = document.createElement("button");

        form.className = "agent-message-edit-form";
        form.dataset.messageId = button.dataset.editMessageId;
        textarea.name = "message";
        textarea.rows = 3;
        textarea.value = originalText;
        saveButton.type = "submit";
        saveButton.textContent = "Spara";
        cancelButton.type = "button";
        cancelButton.textContent = "Avbryt";
        actions.className = "agent-message-edit-actions";
        actions.append(saveButton, cancelButton);
        form.append(textarea, actions);

        textElement.hidden = true;
        button.hidden = true;
        bubble.appendChild(form);
        textarea.focus();
        textarea.setSelectionRange(textarea.value.length, textarea.value.length);

        cancelButton.addEventListener("click", () => {
            form.remove();
            textElement.hidden = false;
            button.hidden = false;
        });
    };

    const submitMessageEdit = async (form) => {
        const messageId = form.dataset.messageId;
        const textarea = form.querySelector("textarea");
        const message = textarea?.value.trim();

        if (!message) {
            textarea?.focus();
            return;
        }

        const headers = {
            "Content-Type": "application/x-www-form-urlencoded",
            "Accept": "application/json"
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(
                "/kundtjanst/chattar/"
                + conversationId
                + "/meddelanden/"
                + messageId
                + "/redigera",
                {
                    method: "POST",
                    headers,
                    body: new URLSearchParams({ message })
                }
        );

        if (response.ok) {
            const chat = await response.json();
            renderMessages(chat.messages);
        }
    };

    const sendTyping = async (active, preview = "") => {
        const headers = {};

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        await fetch(
                "/kundtjanst/api/chattar/"
                + conversationId
                + "/typing?active="
                + encodeURIComponent(active)
                + "&preview="
                + encodeURIComponent(preview),
                {
                    method: "POST",
                    headers
                }
        );
    };

    const agentIsTyping = () => {
        sendTyping(true, replyInput.value);

        if (typingStopTimer) {
            window.clearTimeout(typingStopTimer);
        }

        typingStopTimer = window.setTimeout(() => {
            sendTyping(false, replyInput.value);
        }, 1100);
    };

    const chatStream =
            new EventSource("/kundtjanst/api/chattar/" + conversationId + "/stream");

    chatStream.addEventListener("refresh", refreshChat);
    chatStream.addEventListener("typing", handleTypingEvent);
    chatStream.onerror = () => {
        chatStream.close();
    };

    if (replyInput) {
        if (shortcutsToggle && shortcutsMenu) {
            shortcutsToggle.addEventListener("click", () => {
                shortcutsMenu.hidden = !shortcutsMenu.hidden;
                setActiveComposerTab(shortcutsMenu.hidden
                        ? replyTab
                        : shortcutsToggle);
            });
        }

        replyTemplateButtons.forEach((button) => {
            button.addEventListener("click", () => {
                const template = button.dataset.replyTemplate || "";
                const currentValue = replyInput.value.trim();

                replyInput.value = currentValue
                        ? currentValue + "\n\n" + template
                        : template;
                replyInput.focus();
                replyInput.dispatchEvent(new Event("input", { bubbles: true }));

                if (shortcutsMenu) {
                    shortcutsMenu.hidden = true;
                }

                setActiveComposerTab(replyTab);
            });
        });

        replyInput.addEventListener("input", agentIsTyping);
        replyInput.addEventListener("blur", () => {
            sendTyping(false);
        });
        replyInput.addEventListener("keydown", (event) => {
            if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                replyForm?.requestSubmit();
            }
        });
    }

    if (noteToggle && noteForm && noteInput) {
        noteToggle.addEventListener("click", () => {
            noteForm.hidden = !noteForm.hidden;
            setActiveComposerTab(noteForm.hidden
                    ? replyTab
                    : noteToggle);

            if (!noteForm.hidden) {
                noteInput.focus();
            }
        });
    }

    if (noteClose && noteForm && noteInput) {
        noteClose.addEventListener("click", () => {
            noteInput.value = "";
            noteForm.hidden = true;
            setActiveComposerTab(replyTab);
        });
    }

    if (replyForm) {
        replyForm.addEventListener("submit", () => {
            sendTyping(false);
        });
    }

    messagesElement.addEventListener("click", (event) => {
        const editButton = event.target.closest("[data-edit-message-id]");

        if (editButton) {
            startMessageEdit(editButton);
        }
    });

    messagesElement.addEventListener("submit", (event) => {
        const editForm = event.target.closest(".agent-message-edit-form");

        if (editForm) {
            event.preventDefault();
            submitMessageEdit(editForm);
        }
    });
}
