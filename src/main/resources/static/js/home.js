const searchForm = document.querySelector(".search-form");
const menuButton = document.querySelector(".menu-button");
const navigation = document.querySelector(".navigation");
const homeVideo = document.querySelector("[data-home-video]");

if (homeVideo) {
    homeVideo.muted = true;
    homeVideo.defaultMuted = true;
    homeVideo.loop = true;
    homeVideo.playsInline = true;

    const startHomeVideo = () => {
        if (!homeVideo.paused) {
            return;
        }

        homeVideo.play().catch(() => {
            // Some browsers wait for the first user interaction before autoplay.
        });
    };

    if ("IntersectionObserver" in window) {
        const videoObserver = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    startHomeVideo();
                }
            });
        }, { threshold: 0.25 });

        videoObserver.observe(homeVideo);
    }

    homeVideo.addEventListener("loadeddata", startHomeVideo);
    homeVideo.addEventListener("canplay", startHomeVideo);
    homeVideo.addEventListener("pause", () => {
        if (!homeVideo.ended) {
            window.setTimeout(startHomeVideo, 350);
        }
    });

    ["click", "touchstart", "scroll"].forEach((eventName) => {
        window.addEventListener(eventName, startHomeVideo, {
            once: true,
            passive: true
        });
    });

    startHomeVideo();
}

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
    const typeButtons =
        [...homeFinder.querySelectorAll("[data-type-filter]")];
    const typeFlyouts =
        [...homeFinder.querySelectorAll("[data-type-flyout]")];
    const seasonButtons =
        [...homeFinder.querySelectorAll("[data-season-filter]")];
    const seasonFlyouts =
        [...homeFinder.querySelectorAll("[data-season-flyout]")];
    const seasonGroup = homeFinder.querySelector(".home-season-group");
    const countryButtons =
        [...homeFinder.querySelectorAll("[data-country-filter]")];
    const countryFlyouts =
        [...homeFinder.querySelectorAll("[data-country-flyout]")];
    const countryTypeButtons =
        [...homeFinder.querySelectorAll("[data-country-type-tab]")];
    const placeFilterButtons =
        [...homeFinder.querySelectorAll("[data-place-filter]")];
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
    let openType = "";
    let openSeason = "";
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

    const createSeasonLink = (row) => {
        const link = document.createElement("a");
        const destination =
            row.dataset.destination
            || row.querySelector("span:nth-child(2) strong")?.textContent?.trim()
            || "Resa";
        const hotel =
            row.dataset.hotel
            || row.querySelector("span:nth-child(2) small")?.textContent?.trim()
            || "Hotell";
        const date =
            row.dataset.date
            || row.querySelector("span:first-child strong")?.textContent?.trim()
            || "";
        const price =
            row.dataset.price
            || row.querySelector("span:nth-child(4) strong")?.textContent?.trim()
            || "";

        link.className = "home-season-travel-link";
        link.href = row.href;

        if (row.dataset.image) {
            const image = document.createElement("img");
            image.src = row.dataset.image;
            image.alt = destination;
            link.appendChild(image);
        } else {
            const icon = document.createElement("span");
            icon.className = "home-season-travel-icon";
            icon.textContent = "✈";
            link.appendChild(icon);
        }

        const text = document.createElement("span");
        const titleText = document.createElement("b");
        const hotelText = document.createElement("small");
        const dateText = document.createElement("small");
        const priceText = document.createElement("em");

        titleText.textContent = destination;
        hotelText.textContent = hotel;
        dateText.textContent = date;
        priceText.textContent = price.toString().includes("kr")
            ? price
            : "fr. " + Number(price).toLocaleString("sv-SE") + " kr";

        text.append(titleText, hotelText, dateText);
        link.append(text, priceText);

        return link;
    };

    const renderSeasonFlyout = (flyout, season, seasonName) => {
        const seenTravels = new Set();
        const matchingRows = calendarRows.filter((row) => {
            const matchesSeason =
                (row.dataset.season || "").split(/\s+/).includes(season);
            const travelUrl = row.getAttribute("href") || row.href;

            if (!matchesSeason || seenTravels.has(travelUrl)) {
                return false;
            }

            seenTravels.add(travelUrl);
            return true;
        });

        flyout.replaceChildren();

        const heading = document.createElement("div");
        const headingText = document.createElement("span");
        const headingArrow = document.createElement("strong");

        heading.className = "home-season-flyout-heading";
        headingText.textContent = "Resor i " + seasonName;
        headingArrow.textContent = "→";
        heading.append(headingText, headingArrow);
        flyout.appendChild(heading);

        if (matchingRows.length === 0) {
            const empty = document.createElement("div");
            const name = document.createElement("span");

            empty.className = "home-season-empty";
            empty.append("Tyvärr har vi inga resor just nu under ");
            name.textContent = seasonName;
            empty.append(name, ".");
            flyout.appendChild(empty);
            return;
        }

        const list = document.createElement("div");
        list.className = "home-season-flyout-list";
        matchingRows.forEach((row) => {
            list.appendChild(createSeasonLink(row));
        });
        flyout.appendChild(list);
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

    const closeSeasonFlyouts = () => {
        openSeason = "";

        seasonFlyouts.forEach((flyout) => {
            flyout.hidden = true;
        });
    };

    const closeTypeFlyouts = () => {
        openType = "";

        typeFlyouts.forEach((flyout) => {
            flyout.hidden = true;
            filterTypeFlyout(flyout, "");
        });

        typeButtons.forEach((button) => {
            button.classList.remove("is-active");
        });
    };

    const closeCountryFlyouts = () => {
        openCountry = "";

        countryFlyouts.forEach((flyout) => {
            flyout.hidden = true;
            filterCountryFlyout(flyout, "ALL", "");
        });

        countryButtons.forEach((button) => {
            button.classList.remove("is-active");
        });
    };

    const emptyTextForCountryType = (type) => {
        if (type === "FLIGHT") {
            return "Inga planerade flygresor just nu.";
        }

        if (type === "BUS") {
            return "Inga planerade bussresor just nu.";
        }

        return "Inga planerade resor just nu.";
    };

    const normalizePlace = (value) => {
        return (value || "")
            .toString()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .replace(/\s+/g, " ")
            .trim();
    };

    const linkMatchesPlace = (link, place) => {
        if (!place) {
            return true;
        }

        return normalizePlace(link.dataset.departurePlaces)
            .includes(normalizePlace(place));
    };

    const updatePlaceButtons = (flyout, selectedPlace) => {
        const buttons =
            [...flyout.querySelectorAll("[data-place-filter]")];

        buttons.forEach((button) => {
            button.classList.toggle(
                "is-active",
                (button.dataset.placeFilter || "") === selectedPlace
            );
        });
    };

    const filterTypeFlyout = (flyout, place) => {
        if (!flyout) {
            return;
        }

        const selectedPlace = place || "";
        const links =
            [...flyout.querySelectorAll(".home-country-travel-link")];
        const emptyBox = flyout.querySelector("[data-type-empty]");
        let visibleCount = 0;

        flyout.dataset.selectedPlace = selectedPlace;

        links.forEach((link) => {
            const shouldShow = linkMatchesPlace(link, selectedPlace);

            link.hidden = !shouldShow;

            if (shouldShow) {
                visibleCount += 1;
            }
        });

        updatePlaceButtons(flyout, selectedPlace);

        if (emptyBox) {
            emptyBox.textContent = selectedPlace
                ? "Tyvärr inga påstigningar i " + selectedPlace + "."
                : "Inga resor från den platsen just nu.";
            emptyBox.hidden = visibleCount > 0;
        }
    };

    const filterCountryFlyout = (flyout, type, place) => {
        if (!flyout) {
            return;
        }

        const selectedType = type ?? flyout.dataset.selectedType ?? "ALL";
        const selectedPlace = place ?? flyout.dataset.selectedPlace ?? "";
        const links =
            [...flyout.querySelectorAll(".home-country-travel-link")];
        const buttons =
            [...flyout.querySelectorAll("[data-country-type-tab]")];
        const emptyBox = flyout.querySelector("[data-country-empty]");
        let visibleCount = 0;

        flyout.dataset.selectedType = selectedType;
        flyout.dataset.selectedPlace = selectedPlace;

        links.forEach((link) => {
            const matchesType = selectedType === "ALL"
                || link.dataset.travelType === selectedType;
            const shouldShow = matchesType
                && linkMatchesPlace(link, selectedPlace);

            link.hidden = !shouldShow;

            if (shouldShow) {
                visibleCount += 1;
            }
        });

        buttons.forEach((button) => {
            button.classList.toggle(
                "is-active",
                button.dataset.countryTypeTab === selectedType
            );
        });

        updatePlaceButtons(flyout, selectedPlace);

        if (!emptyBox) {
            return;
        }

        emptyBox.textContent = selectedPlace
            ? "Tyvärr inga påstigningar i " + selectedPlace + "."
            : emptyTextForCountryType(selectedType);
        emptyBox.hidden = visibleCount > 0;
    };

    const positionSidebarFlyout = (button, flyout) => {
        if (!button || !flyout) {
            return;
        }

        const buttonBox = button.getBoundingClientRect();
        const isCountryFlyout = flyout.hasAttribute("data-country-flyout");
        const flyoutWidth = Math.min(
            360,
            window.innerWidth - 28
        );
        const left = Math.min(
            buttonBox.right - 2,
            window.innerWidth - flyoutWidth - 14
        );
        const maxTop = Math.max(
            14,
            window.innerHeight - flyout.offsetHeight - 14
        );
        const wantedTop = isCountryFlyout
            ? Math.min(buttonBox.top - 24, 110)
            : buttonBox.top;
        const top = Math.min(
            wantedTop,
            maxTop
        );
        const finalTop = Math.max(14, top);
        const arrowTop = Math.max(
            18,
            Math.min(
                flyout.offsetHeight - 34,
                buttonBox.top + buttonBox.height / 2 - finalTop - 14
            )
        );

        flyout.style.width = flyoutWidth + "px";
        flyout.style.left = Math.max(14, left) + "px";
        flyout.style.top = finalTop + "px";
        flyout.style.setProperty(
            "--flyout-arrow-top",
            arrowTop + "px"
        );
    };

    const openSeasonFlyout = (button) => {
        const selectedSeason = button.dataset.seasonFilter;
        const seasonName =
            button.dataset.seasonName
            || seasonNames[selectedSeason]
            || selectedSeason;

        if (openSeason === selectedSeason) {
            activeSeason = "";
            closeSeasonFlyouts();
            applyFilters();
            return;
        }

        activeSeason = selectedSeason;
        openSeason = selectedSeason;
        closeTypeFlyouts();
        closeCountryFlyouts();

        seasonFlyouts.forEach((flyout) => {
            const isSelected =
                flyout.dataset.seasonFlyout === selectedSeason;

            flyout.hidden = !isSelected;

            if (isSelected) {
                renderSeasonFlyout(flyout, selectedSeason, seasonName);
                positionSidebarFlyout(button, flyout);
            }
        });

        applyFilters();
    };

    const openCountryFlyout = (button) => {
        const selectedCountry = button.dataset.countryFilter;

        if (openCountry === selectedCountry) {
            closeCountryFlyouts();
            return;
        }

        openCountry = selectedCountry;
        closeTypeFlyouts();
        closeSeasonFlyouts();

        countryFlyouts.forEach((flyout) => {
            const isSelected =
                flyout.dataset.countryFlyout === selectedCountry;

            flyout.hidden = !isSelected;

            if (isSelected) {
                filterCountryFlyout(flyout, "ALL", "");
                positionSidebarFlyout(button, flyout);
            }
        });

        countryButtons.forEach((countryButton) => {
            countryButton.classList.toggle(
                "is-active",
                countryButton === button
            );
        });
    };

    const openTypeFlyout = (button) => {
        const selectedType = button.dataset.typeFilter;

        if (openType === selectedType) {
            closeTypeFlyouts();
            return;
        }

        openType = selectedType;
        closeSeasonFlyouts();
        closeCountryFlyouts();

        typeFlyouts.forEach((flyout) => {
            const isSelected =
                flyout.dataset.typeFlyout === selectedType;

            flyout.hidden = !isSelected;

            if (isSelected) {
                filterTypeFlyout(flyout, "");
                positionSidebarFlyout(button, flyout);
            }
        });

        typeButtons.forEach((typeButton) => {
            typeButton.classList.toggle(
                "is-active",
                typeButton === button
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
            closeTypeFlyouts();
            setView(button.dataset.finderView);
            scrollToFinder();
        });
    });

    typeButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            openTypeFlyout(button);
        });
    });

    seasonButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            openSeasonFlyout(button);
        });
    });

    countryButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            openCountryFlyout(button);
        });
    });

    countryTypeButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();

            filterCountryFlyout(
                button.closest(".home-country-flyout"),
                button.dataset.countryTypeTab,
                undefined
            );
        });
    });

    placeFilterButtons.forEach((button) => {
        button.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();

            const place = button.dataset.placeFilter || "";
            const typeFlyout = button.closest("[data-type-flyout]");
            const countryFlyout = button.closest("[data-country-flyout]");

            if (typeFlyout) {
                filterTypeFlyout(typeFlyout, place);
            }

            if (countryFlyout) {
                filterCountryFlyout(countryFlyout, undefined, place);
            }
        });
    });

    document.addEventListener("click", (event) => {
        if (
            openType
            && !event.target.closest(".home-type-item")
        ) {
            closeTypeFlyouts();
        }

        if (
            seasonGroup
            && openSeason
            && !seasonGroup.contains(event.target)
        ) {
            closeSeasonFlyouts();
        }

        if (
            !countryGroup
            || !openCountry
            || countryGroup.contains(event.target)
        ) {
            return;
        }

        closeCountryFlyouts();
    });

    window.addEventListener("resize", () => {
        closeTypeFlyouts();
        closeSeasonFlyouts();
        closeCountryFlyouts();
    });

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
            closeTypeFlyouts();
            closeSeasonFlyouts();
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
