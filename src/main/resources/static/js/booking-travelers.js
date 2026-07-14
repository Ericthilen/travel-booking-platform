const travelerSelect = document.querySelector("#numberOfTravelers");
const roomSelect = document.querySelector("#numberOfRooms");
const roomDistribution = document.querySelector("#roomDistribution");
const distributionMessage = document.querySelector("#distributionMessage");
const travelerSelectionForm = document.querySelector(
    "#travelerSelectionForm"
);

function createRoomDistribution() {
    const numberOfTravelers = Number(travelerSelect.value);
    const numberOfRooms = Number(roomSelect.value);

    roomDistribution.innerHTML = "";

    for (let index = 0; index < numberOfRooms; index++) {
        const roomCard = document.createElement("div");
        roomCard.className = "room-distribution-card";

        const label = document.createElement("label");
        label.setAttribute("for", `roomOccupancy${index}`);
        label.textContent = `Rum ${index + 1}`;

        const select = document.createElement("select");
        select.id = `roomOccupancy${index}`;
        select.name = "roomOccupancies";
        select.className = "room-occupancy-select";

        for (
            let occupancy = 1;
            occupancy <= numberOfTravelers;
            occupancy++
        ) {
            const option = document.createElement("option");

            option.value = occupancy;
            option.textContent = `${occupancy} ${
                occupancy === 1 ? "person" : "personer"
            }`;

            select.appendChild(option);
        }

        const suggestedOccupancy = Math.floor(
            numberOfTravelers / numberOfRooms
        );

        select.value = Math.max(1, suggestedOccupancy);

        select.addEventListener(
            "change",
            updateDistributionMessage
        );

        roomCard.appendChild(label);
        roomCard.appendChild(select);
        roomDistribution.appendChild(roomCard);
    }

    distributeTravelersEvenly();
    updateDistributionMessage();
}

function distributeTravelersEvenly() {
    const numberOfTravelers = Number(travelerSelect.value);
    const selects = document.querySelectorAll(
        ".room-occupancy-select"
    );

    let remainingTravelers = numberOfTravelers;

    selects.forEach((select, index) => {
        const remainingRooms = selects.length - index;

        const occupancy = Math.ceil(
            remainingTravelers / remainingRooms
        );

        select.value = Math.max(1, occupancy);
        remainingTravelers -= occupancy;
    });
}

function updateDistributionMessage() {
    const numberOfTravelers = Number(travelerSelect.value);

    const selectedOccupancies = Array.from(
        document.querySelectorAll(".room-occupancy-select")
    ).map(select => Number(select.value));

    const distributedTravelers = selectedOccupancies.reduce(
        (total, occupancy) => total + occupancy,
        0
    );

    if (distributedTravelers === numberOfTravelers) {
        distributionMessage.textContent =
            "Alla resenärer är fördelade.";

        distributionMessage.classList.remove("error");
        distributionMessage.classList.add("valid");
        return true;
    }

    distributionMessage.textContent =
        `Du har fördelat ${distributedTravelers} av ` +
        `${numberOfTravelers} resenärer.`;

    distributionMessage.classList.remove("valid");
    distributionMessage.classList.add("error");

    return false;
}

travelerSelect.addEventListener(
    "change",
    createRoomDistribution
);

roomSelect.addEventListener(
    "change",
    createRoomDistribution
);

travelerSelectionForm.addEventListener(
    "submit",
    function (event) {
        if (!updateDistributionMessage()) {
            event.preventDefault();
        }
    }
);

createRoomDistribution();