const responsiblePersonalNumber = document.querySelector(
    "#responsiblePersonalNumber"
);

const responsibleFirstName = document.querySelector(
    "#responsibleFirstName"
);

const responsibleLastName = document.querySelector(
    "#responsibleLastName"
);

const copyCheckboxes = document.querySelectorAll(
    ".copy-responsible-checkbox"
);

const previousTravelerSelects = document.querySelectorAll(
    ".previous-traveler-select"
);

copyCheckboxes.forEach(function (checkbox) {
    checkbox.addEventListener("change", function () {
        const travelerCard = checkbox.closest(".traveler-card");

        if (!travelerCard) {
            return;
        }

        const personalNumberInput = travelerCard.querySelector(
            ".traveler-personal-number"
        );

        const firstNameInput = travelerCard.querySelector(
            ".traveler-first-name"
        );

        const lastNameInput = travelerCard.querySelector(
            ".traveler-last-name"
        );

        if (!checkbox.checked) {
            personalNumberInput.readOnly = false;
            firstNameInput.readOnly = false;
            lastNameInput.readOnly = false;

            return;
        }

        personalNumberInput.value =
            responsiblePersonalNumber.value;

        firstNameInput.value =
            responsibleFirstName.value;

        lastNameInput.value =
            responsibleLastName.value;

        personalNumberInput.readOnly = true;
        firstNameInput.readOnly = true;
        lastNameInput.readOnly = true;
    });
});

function updateCopiedTravelers() {
    copyCheckboxes.forEach(function (checkbox) {
        if (!checkbox.checked) {
            return;
        }

        const travelerCard = checkbox.closest(".traveler-card");

        travelerCard.querySelector(
            ".traveler-personal-number"
        ).value = responsiblePersonalNumber.value;

        travelerCard.querySelector(
            ".traveler-first-name"
        ).value = responsibleFirstName.value;

        travelerCard.querySelector(
            ".traveler-last-name"
        ).value = responsibleLastName.value;
    });
}

responsiblePersonalNumber.addEventListener(
    "input",
    updateCopiedTravelers
);

responsibleFirstName.addEventListener(
    "input",
    updateCopiedTravelers
);

responsibleLastName.addEventListener(
    "input",
    updateCopiedTravelers
);

previousTravelerSelects.forEach(function (select) {
    select.addEventListener("change", function () {
        const selectedOption = select.options[select.selectedIndex];
        const travelerCard = select.closest(".traveler-card");

        if (!selectedOption || !travelerCard || select.value === "") {
            return;
        }

        const copyCheckbox = travelerCard.querySelector(
            ".copy-responsible-checkbox"
        );

        if (copyCheckbox) {
            copyCheckbox.checked = false;
        }

        const personalNumberInput = travelerCard.querySelector(
            ".traveler-personal-number"
        );

        const firstNameInput = travelerCard.querySelector(
            ".traveler-first-name"
        );

        const lastNameInput = travelerCard.querySelector(
            ".traveler-last-name"
        );

        personalNumberInput.readOnly = false;
        firstNameInput.readOnly = false;
        lastNameInput.readOnly = false;

        personalNumberInput.value = selectedOption.dataset.personalNumber;
        firstNameInput.value = selectedOption.dataset.firstName;
        lastNameInput.value = selectedOption.dataset.lastName;
    });
});
