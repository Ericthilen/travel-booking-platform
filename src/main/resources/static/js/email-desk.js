const replyEditor = document.getElementById('reply-editor');
const hiddenInput = document.getElementById('reply-message-hidden');
const replyForm = document.querySelector('.email-desk-composer');

if (replyForm && replyEditor && hiddenInput) {
    replyForm.addEventListener('submit', () => {
        hiddenInput.value = replyEditor.innerHTML;
    });
}

function getActiveEditor(button) {
    const form = button.closest('form');
    if (form) {
        const editor = form.querySelector('.email-reply-editor');
        if (editor) return editor;
        const textarea = form.querySelector('textarea');
        if (textarea) return textarea;
    }
    return replyEditor || document.querySelector('textarea');
}

// Toolbar actions
document.querySelectorAll("[data-wrap]").forEach((button) => {
    button.addEventListener("click", () => {
        const editor = getActiveEditor(button);
        if (!editor) return;

        const action = button.dataset.wrap;
        if (editor.contentEditable === "true") {
            if (action === "**") document.execCommand("bold", false);
            else if (action === "_") document.execCommand("italic", false);
        } else {
            // Fallback for normal textareas
            const start = editor.selectionStart || 0;
            const end = editor.selectionEnd || 0;
            const selected = editor.value.slice(start, end) || "text";
            const replacement = action + selected + action;
            editor.value = editor.value.slice(0, start) + replacement + editor.value.slice(end);
        }
    });
});

document.querySelectorAll("[data-insert]").forEach((button) => {
    button.addEventListener("click", () => {
        const editor = getActiveEditor(button);
        if (!editor) return;

        const text = button.dataset.insert.replaceAll("\\n", "\n");
        if (editor.contentEditable === "true") {
            document.execCommand("insertText", false, text);
        } else {
            const start = editor.selectionStart || editor.value.length;
            const end = editor.selectionEnd || editor.value.length;
            editor.value = editor.value.slice(0, start) + text + editor.value.slice(end);
        }
    });
});

// Color picker
document.querySelectorAll(".email-format-toolbar input[type='color']").forEach((input) => {
    input.addEventListener("change", () => {
        const editor = getActiveEditor(input);
        if (!editor) return;

        if (editor.contentEditable === "true") {
            document.execCommand("foreColor", false, input.value);
        } else {
            const text = "[färg " + input.value + "] ";
            const start = editor.selectionStart || editor.value.length;
            editor.value = editor.value.slice(0, start) + text + editor.value.slice(editor.selectionEnd || editor.value.length);
        }
    });
});

// Template select
document.querySelectorAll(".email-template-select, .email-compose-form select").forEach((select) => {
    select.addEventListener("change", () => {
        if (!select.value) return;
        const editor = getActiveEditor(select);
        if (!editor) return;

        if (editor.contentEditable === "true") {
            // Templates might contain HTML or plain text
            const content = select.value;
            if (content.includes('<')) {
                const div = document.createElement('div');
                div.innerHTML = content;
                editor.appendChild(div);
            } else {
                document.execCommand("insertHTML", false, content.replace(/\n/g, '<br>'));
            }
        } else {
            const start = editor.selectionStart || editor.value.length;
            editor.value = editor.value.slice(0, start) + select.value + editor.value.slice(editor.selectionEnd || editor.value.length);
        }
        select.value = "";
    });
});

// Signature logic
const insertSignatureBtn = document.getElementById('insert-signature-btn');
const signatureTemplate = document.getElementById('email-signature-template');

if (insertSignatureBtn && signatureTemplate) {
    insertSignatureBtn.addEventListener('click', function() {
        const editor = getActiveEditor(this);
        if (!editor) return;

        if (editor.contentEditable === "true") {
            // Check if signature already exists
            if (editor.querySelector('.email-signature-wrapper')) {
                return;
            }
            const signatureHtml = signatureTemplate.innerHTML.trim();
            editor.insertAdjacentHTML('beforeend', '<br><br>' + signatureHtml);
        } else {
            // Fallback for textarea
            const signatureHtml = signatureTemplate.innerHTML.trim();
            if (!editor.value.includes('email-signature-wrapper')) {
                const prefix = editor.value.trim() ? '\n\n' : '';
                editor.value += prefix + signatureHtml;
            }
        }
    });
}
