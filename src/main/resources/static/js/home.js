const searchForm = document.querySelector(".search-form");
const menuButton = document.querySelector(".menu-button");
const navigation = document.querySelector(".navigation");

if (searchForm) {
    const destinationInput = document.querySelector("#destination");
    const suggestionBox = searchForm.querySelector("[data-home-suggestions]");
    const suggestions = suggestionBox
        ? [...suggestionBox.querySelectorAll(".home-search-suggestion")]
        : [];

    const showSuggestions = () => {
        if (!destinationInput || !suggestionBox) {
            return;
        }

        const query = destinationInput.value.trim().toLowerCase();
        let visibleCount = 0;

        suggestions.forEach((suggestion) => {
            const text = suggestion.dataset.search || "";
            const words = text.split(/\s+/);
            const isVisible = query.length >= 1
                && words.some((word) => word.startsWith(query))
                && visibleCount < 3;

            suggestion.classList.toggle("is-hidden", !isVisible);

            if (isVisible) {
                visibleCount += 1;
            }
        });

        const hasSuggestions = visibleCount > 0;

        suggestionBox.hidden = !hasSuggestions;
        searchForm.classList.toggle(
            "has-visible-suggestions",
            hasSuggestions
        );
    };

    if (destinationInput) {
        destinationInput.addEventListener("input", showSuggestions);
        destinationInput.addEventListener("focus", showSuggestions);
    }

    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();

        const formData = new FormData(searchForm);
        const params = new URLSearchParams();
        const destination = formData.get("destination")
            ? String(formData.get("destination")).trim()
            : "";

        if (!destination) {
            alert("Skriv vart du vill resa.");
            return;
        }

        formData.forEach((value, key) => {
            if (String(value).trim()) {
                params.set(
                    key,
                    String(value).trim()
                );
            }
        });

        window.location.href = "/resor?" + params.toString();
    });

    document.addEventListener("click", function (event) {
        if (!suggestionBox || !destinationInput) {
            return;
        }

        if (
            event.target === destinationInput
            || suggestionBox.contains(event.target)
        ) {
            return;
        }

        suggestionBox.hidden = true;
        searchForm.classList.remove("has-visible-suggestions");
    });
}

if (menuButton && navigation) {
    menuButton.addEventListener("click", function () {
        const menuIsOpen =
            navigation.classList.toggle("mobile-open");

        menuButton.setAttribute(
            "aria-expanded",
            String(menuIsOpen)
        );
    });
}

const homeFinder = document.querySelector("[data-home-finder]");

if (homeFinder) {
    const viewButtons =
        [...homeFinder.querySelectorAll("[data-finder-view]")];
    const panels =
        [...homeFinder.querySelectorAll("[data-finder-panel]")];
    const title = homeFinder.querySelector("[data-finder-title]");
    const description =
        homeFinder.querySelector("[data-finder-description]");
    const resetButton = homeFinder.querySelector("[data-finder-reset]");
    const emptyMessage = homeFinder.querySelector("[data-finder-empty]");
    const seasonButtons =
        [...homeFinder.querySelectorAll("[data-season-filter]")];
    const countryButtons =
        [...homeFinder.querySelectorAll("[data-country-filter]")];
    const countryFlyouts =
        [...homeFinder.querySelectorAll("[data-country-flyout]")];
    const countryGroup = homeFinder.querySelector(".home-country-group");
    const calendarRows =
        [...homeFinder.querySelectorAll(".home-calendar-row")];
    const lastMinuteCards =
        [...homeFinder.querySelectorAll(".home-last-minute-card")];
    const mapMarkers =
        [...homeFinder.querySelectorAll(".home-map-marker")];
    const dateFilter = homeFinder.querySelector("[data-date-filter]");
    const airportFilter = homeFinder.querySelector("[data-airport-filter]");
    const nightsFilter = homeFinder.querySelector("[data-nights-filter]");

    let activeView = "calendar";
    let activeSeason = "";
    let openCountry = "";

    const viewCopy = {
        calendar: {
            title: "Kalender",
            description:
                "Se kommande flygavgångar och klicka dig vidare till resan."
        },
        map: {
            title: "Världskarta",
            description:
                "Dra, zooma och välj resa direkt från kartan."
        },
        "last-minute": {
            title: "Sista minuten",
            description:
                "Hitta de närmaste avgångarna med lediga platser."
        }
    };

    const seasonNames = {
        var: "vår",
        sommar: "sommar",
        host: "höst",
        vinter: "vinter"
    };

    const dedupeSelect = (select) => {
        if (!select) {
            return;
        }

        const seen = new Set();

        [...select.options].forEach((option) => {
            if (!option.value) {
                return;
            }

            if (seen.has(option.value)) {
                option.remove();
                return;
            }

            seen.add(option.value);
        });
    };

    dedupeSelect(airportFilter);
    dedupeSelect(nightsFilter);

    const itemMatches = (item) => {
        const matchesSeason = !activeSeason
            || (item.dataset.season || "").split(/\s+/).includes(activeSeason);
        const matchesDate = !dateFilter
            || !dateFilter.value
            || item.dataset.date === dateFilter.value;
        const matchesAirport = !airportFilter
            || !airportFilter.value
            || item.dataset.airport === airportFilter.value;
        const matchesNights = !nightsFilter
            || !nightsFilter.value
            || item.dataset.nights === nightsFilter.value;

        return matchesSeason
            && matchesDate
            && matchesAirport
            && matchesNights;
    };

    const mapMarkerMatches = (marker) => {
        const matchesSeason = !activeSeason
            || (marker.dataset.season || "").split(/\s+/).includes(activeSeason);
        return matchesSeason;
    };

    const visibleItemsForActiveView = () => {
        if (activeView === "map") {
            return mapMarkers.filter(mapMarkerMatches);
        }

        if (activeView === "last-minute") {
            return lastMinuteCards.filter(itemMatches);
        }

        return calendarRows.filter(itemMatches);
    };

    const updateEmptyMessage = () => {
        const hasVisibleItems = visibleItemsForActiveView().length > 0;

        if (!emptyMessage) {
            return;
        }

        if (hasVisibleItems) {
            emptyMessage.hidden = true;
            return;
        }

        if (activeSeason) {
            emptyMessage.textContent =
                "Tyvärr har vi inga avgångar i "
                + seasonNames[activeSeason]
                + ".";
        } else {
            emptyMessage.textContent =
                "Tyvärr har vi inga avgångar för det filtret just nu.";
        }

        emptyMessage.hidden = false;
    };

    const applyFilters = () => {
        calendarRows.forEach((row) => {
            row.classList.toggle(
                "is-hidden",
                !itemMatches(row)
            );
        });

        lastMinuteCards.forEach((card) => {
            card.classList.toggle(
                "is-hidden",
                !itemMatches(card)
            );
        });

        mapMarkers.forEach((marker) => {
            marker.classList.toggle(
                "is-hidden",
                !mapMarkerMatches(marker)
            );
        });

        seasonButtons.forEach((button) => {
            button.classList.toggle(
                "is-active",
                button.dataset.seasonFilter === activeSeason
            );
        });

        countryButtons.forEach((button) => {
            button.classList.toggle(
                "is-active",
                button.dataset.countryFilter === openCountry
            );
        });

        updateEmptyMessage();
    };

    const closeCountryFlyouts = () => {
        openCountry = "";

        countryFlyouts.forEach((flyout) => {
            flyout.hidden = true;
        });

        countryButtons.forEach((button) => {
            button.classList.remove("is-active");
        });
    };

    const positionCountryFlyout = (button, flyout) => {
        if (!countryGroup || !button || !flyout) {
            return;
        }

        const buttonBox = button.getBoundingClientRect();
        const flyoutWidth = Math.min(
            360,
            window.innerWidth - 28
        );
        const left = Math.min(
            buttonBox.right - 2,
            window.innerWidth - flyoutWidth - 14
        );
        const top = Math.min(
            buttonBox.top,
            window.innerHeight - flyout.offsetHeight - 14
        );

        flyout.style.width = flyoutWidth + "px";
        flyout.style.left = Math.max(14, left) + "px";
        flyout.style.top = Math.max(14, top) + "px";
    };

    const openCountryFlyout = (button) => {
        const selectedCountry = button.dataset.countryFilter;

        if (openCountry === selectedCountry) {
            closeCountryFlyouts();
            return;
        }

        openCountry = selectedCountry;

        countryFlyouts.forEach((flyout) => {
            const isSelected =
                flyout.dataset.countryFlyout === selectedCountry;

            flyout.hidden = !isSelected;

            if (isSelected) {
                positionCountryFlyout(button, flyout);
            }
        });

        countryButtons.forEach((countryButton) => {
            countryButton.classList.toggle(
                "is-active",
                countryButton === button
            );
        });
    };

    const setView = (view) => {
        activeView = view;

        panels.forEach((panel) => {
            panel.classList.toggle(
                "is-active",
                panel.dataset.finderPanel === view
            );
        });

        viewButtons.forEach((button) => {
            button.classList.toggle(
                "is-active",
                button.dataset.finderView === view
            );
        });

        if (title && viewCopy[view]) {
            title.textContent = viewCopy[view].title;
        }

        if (description && viewCopy[view]) {
            description.textContent = viewCopy[view].description;
        }

        updateEmptyMessage();
    };

    const scrollToFinder = () => {
        homeFinder.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    };

    viewButtons.forEach((button) => {
        button.addEventListener("click", () => {
            setView(button.dataset.finderView);
            scrollToFinder();
        });
    });

    seasonButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const selectedSeason = button.dataset.seasonFilter;

            activeSeason = activeSeason === selectedSeason
                ? ""
                : selectedSeason;

            applyFilters();
        });
    });

    countryButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            openCountryFlyout(button);
        });
    });

    document.addEventListener("click", (event) => {
        if (
            !countryGroup
            || !openCountry
            || countryGroup.contains(event.target)
        ) {
            return;
        }

        closeCountryFlyouts();
    });

    window.addEventListener("resize", closeCountryFlyouts);

    [dateFilter, airportFilter, nightsFilter].forEach((filter) => {
        if (!filter) {
            return;
        }

        filter.addEventListener("input", applyFilters);
        filter.addEventListener("change", applyFilters);
    });

    if (resetButton) {
        resetButton.addEventListener("click", () => {
            activeSeason = "";
            closeCountryFlyouts();

            if (dateFilter) {
                dateFilter.value = "";
            }

            if (airportFilter) {
                airportFilter.value = "";
            }

            if (nightsFilter) {
                nightsFilter.value = "";
            }

            applyFilters();
        });
    }

    const mapShell = homeFinder.querySelector("[data-map-shell]");
    const mapLayer = homeFinder.querySelector("[data-map-layer]");
    const zoomIn = homeFinder.querySelector("[data-map-zoom-in]");
    const zoomOut = homeFinder.querySelector("[data-map-zoom-out]");
    const mapPopup = homeFinder.querySelector("[data-map-popup]");
    const mapPopupImage = homeFinder.querySelector("[data-map-popup-image]");
    const mapPopupCountry = homeFinder.querySelector("[data-map-popup-country]");
    const mapPopupTitle = homeFinder.querySelector("[data-map-popup-title]");
    const mapPopupHotel = homeFinder.querySelector("[data-map-popup-hotel]");
    const mapPopupPrice = homeFinder.querySelector("[data-map-popup-price]");
    const mapPopupLink = homeFinder.querySelector("[data-map-popup-link]");
    const mapPopupClose = homeFinder.querySelector("[data-map-popup-close]");

    if (mapShell && mapLayer) {
        let scale = 1;
        let offsetX = -210;
        let offsetY = -18;
        let startX = 0;
        let startY = 0;
        let dragging = false;
        let activeMapMarker = null;

        const formatPrice = (value) => {
            const price = Number(value);

            if (!Number.isFinite(price)) {
                return "Pris visas på resan";
            }

            return "fr. "
                + new Intl.NumberFormat("sv-SE").format(price)
                + " kr";
        };

        const closeMapPopup = () => {
            activeMapMarker = null;

            if (mapPopup) {
                mapPopup.hidden = true;
            }

            mapMarkers.forEach((marker) => {
                marker.classList.remove("is-active");
            });
        };

        const positionMapPopup = () => {
            if (!mapPopup || !activeMapMarker || mapPopup.hidden) {
                return;
            }

            const shellBox = mapShell.getBoundingClientRect();
            const markerBox = activeMapMarker.getBoundingClientRect();
            const popupBox = mapPopup.getBoundingClientRect();
            const preferredLeft =
                markerBox.left - shellBox.left - popupBox.width / 2;
            const preferredTop =
                markerBox.top - shellBox.top - popupBox.height - 18;
            const maxLeft = shellBox.width - popupBox.width - 16;
            const maxTop = shellBox.height - popupBox.height - 16;

            mapPopup.style.left =
                Math.max(16, Math.min(maxLeft, preferredLeft)) + "px";
            mapPopup.style.top =
                Math.max(16, Math.min(maxTop, preferredTop)) + "px";
        };

        const openMapPopup = (marker) => {
            activeMapMarker = marker;

            if (
                mapPopupImage
                && marker.dataset.mapImage
            ) {
                mapPopupImage.src = marker.dataset.mapImage;
                mapPopupImage.alt = marker.dataset.mapTitle || "Resmål";
            }

            if (mapPopupCountry) {
                mapPopupCountry.textContent = marker.dataset.mapCountry || "";
            }

            if (mapPopupTitle) {
                mapPopupTitle.textContent = marker.dataset.mapTitle || "";
            }

            if (mapPopupHotel) {
                mapPopupHotel.textContent = marker.dataset.mapHotel || "";
            }

            if (mapPopupPrice) {
                mapPopupPrice.textContent = formatPrice(marker.dataset.mapPrice);
            }

            if (mapPopupLink) {
                mapPopupLink.href = marker.dataset.mapUrl || "#";
            }

            mapMarkers.forEach((mapMarker) => {
                mapMarker.classList.toggle(
                    "is-active",
                    mapMarker === marker
                );
            });

            if (mapPopup) {
                mapPopup.hidden = false;
                positionMapPopup();
            }
        };

        const renderMap = () => {
            mapLayer.style.transform =
                "translate("
                + offsetX
                + "px, "
                + offsetY
                + "px) scale("
                + scale
                + ")";

            positionMapPopup();
        };

        const zoom = (direction) => {
            const nextScale = Math.min(
                2.6,
                Math.max(
                    0.78,
                    scale + direction
                )
            );

            scale = nextScale;
            renderMap();
        };

        mapShell.addEventListener("pointerdown", (event) => {
            if (
                event.target.closest(".home-map-marker")
                || event.target.closest(".home-map-popup")
            ) {
                return;
            }

            dragging = true;
            startX = event.clientX - offsetX;
            startY = event.clientY - offsetY;
            mapShell.classList.add("is-dragging");
            mapShell.setPointerCapture(event.pointerId);
        });

        mapShell.addEventListener("pointermove", (event) => {
            if (!dragging) {
                return;
            }

            offsetX = event.clientX - startX;
            offsetY = event.clientY - startY;
            renderMap();
        });

        mapShell.addEventListener("pointerup", () => {
            dragging = false;
            mapShell.classList.remove("is-dragging");
        });

        mapShell.addEventListener("click", (event) => {
            if (
                event.target.closest(".home-map-marker")
                || event.target.closest(".home-map-popup")
            ) {
                return;
            }

            closeMapPopup();
        });

        mapMarkers.forEach((marker) => {
            marker.addEventListener("click", (event) => {
                event.preventDefault();
                event.stopPropagation();
                openMapPopup(marker);
            });
        });

        if (mapPopupClose) {
            mapPopupClose.addEventListener("click", closeMapPopup);
        }

        mapShell.addEventListener("wheel", (event) => {
            event.preventDefault();
            zoom(event.deltaY < 0 ? 0.12 : -0.12);
        }, { passive: false });

        if (zoomIn) {
            zoomIn.addEventListener("click", () => zoom(0.16));
        }

        if (zoomOut) {
            zoomOut.addEventListener("click", () => zoom(-0.16));
        }

        renderMap();
    }

    applyFilters();
}
