document.addEventListener("DOMContentLoaded", () => {
    const pickupInfos = [...document.querySelectorAll(".pickup-info")];

    if (pickupInfos.length === 0) {
        return;
    }

    const closePickupInfos = (exceptInfo) => {
        pickupInfos.forEach((info) => {
            if (info !== exceptInfo) {
                info.open = false;
            }
        });
    };

    pickupInfos.forEach((info) => {
        info.addEventListener("toggle", () => {
            if (info.open) {
                closePickupInfos(info);
            }
        });
    });

    document.addEventListener("click", (event) => {
        if (event.target.closest(".pickup-info")) {
            return;
        }

        closePickupInfos();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closePickupInfos();
        }
    });
});
