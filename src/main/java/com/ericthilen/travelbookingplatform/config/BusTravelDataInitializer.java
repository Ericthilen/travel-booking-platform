package com.ericthilen.travelbookingplatform.config;

import com.ericthilen.travelbookingplatform.model.BusPickupStop;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.RoomType;
import com.ericthilen.travelbookingplatform.model.Travel;
import com.ericthilen.travelbookingplatform.model.TravelType;
import com.ericthilen.travelbookingplatform.repository.DepartureRepository;
import com.ericthilen.travelbookingplatform.repository.RoomTypeRepository;
import com.ericthilen.travelbookingplatform.repository.TravelRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class BusTravelDataInitializer {

    @Bean
    @Order(3)
    public CommandLineRunner initializeBusTravels(
            TravelRepository travelRepository,
            DepartureRepository departureRepository,
            RoomTypeRepository roomTypeRepository
    ) {
        return args -> {
            for (BusTrip busTrip : busTrips()) {
                if (travelRepository.existsByDestinationAndTravelType(
                        busTrip.destination(),
                        TravelType.BUS
                )) {
                    travelRepository
                            .findByDestinationAndTravelType(
                                    busTrip.destination(),
                                    TravelType.BUS
                            )
                            .ifPresent(existingTravel -> {
                                existingTravel.updatePickupStops(
                                        busTrip.pickupStops()
                                );
                                travelRepository.save(existingTravel);
                                updateExistingDepartures(
                                        existingTravel,
                                        busTrip,
                                        departureRepository
                                );
                            });
                    continue;
                }

                Travel travel = new Travel(
                        busTrip.country(),
                        busTrip.destination(),
                        busTrip.hotelName(),
                        busTrip.nights(),
                        busTrip.price(),
                        busTrip.imageUrl(),
                        busTrip.shortDescription(),
                        busTrip.longDescription(),
                        busTrip.mealType(),
                        pickupSummary(busTrip.pickupStops()),
                        busTrip.hotelStars(),
                        TravelType.BUS,
                        busTrip.facilities(),
                        includedItems(busTrip),
                        dayProgram(busTrip),
                        busTrip.pickupStops()
                );

                Travel savedTravel = travelRepository.save(travel);

                departureRepository.save(new Departure(
                        savedTravel,
                        busTrip.departureDate(),
                        busTrip.departureDate().plusDays(busTrip.nights() + 1),
                        pickupSummary(busTrip.pickupStops()),
                        busTrip.destination(),
                        "BUS-" + busTrip.code() + "-UT",
                        firstPickupTime(busTrip.pickupStops()),
                        busTrip.arrivalTime(),
                        "BUS-" + busTrip.code() + "-HEM",
                        busTrip.returnDepartureTime(),
                        busTrip.returnArrivalTime(),
                        busTrip.price(),
                        busTrip.seats()
                ));

                createRoomTypes(savedTravel, roomTypeRepository);
            }
        };
    }

    private List<BusTrip> busTrips() {
        return List.of(
                new BusTrip("SE01", "Sverige", "Stockholm", "Elite Hotel Adlon", 2, 3295, LocalDate.now().plusDays(24), "Storstadsweekend med Gamla stan, museer och skön hotellvistelse.", "En bekväm bussresa till Stockholm med centralt hotell, tid för shopping, kultur och gemensam rundtur genom stadens mest kända kvarter.", "Frukost", 4, 38, LocalTime.of(15, 30), LocalTime.of(10, 0), LocalTime.of(18, 30), stops("Göteborg:06:00", "Borås:06:50", "Jönköping:08:15", "Linköping:10:10"), image("photo-1509356843151-3e7d96241e11"), facilities("Centralt hotell", "Stadsrundtur", "Frukost", "Fri tid")),
                new BusTrip("SE02", "Sverige", "Malmö", "Clarion Hotel Malmö Live", 2, 2995, LocalDate.now().plusDays(31), "Weekend i Malmö med Västra Hamnen, god mat och närhet till havet.", "Resan går söderut till Malmö där vi bor modernt och centralt. Programmet blandar gemensam stadsrundtur med egen tid för restauranger, shopping och promenader.", "Frukost", 4, 42, LocalTime.of(14, 45), LocalTime.of(10, 30), LocalTime.of(17, 45), stops("Göteborg:06:30", "Varberg:07:20", "Halmstad:08:10", "Helsingborg:09:30"), image("photo-1518005020951-eccb494ad742"), facilities("Centralt hotell", "Stadsrundtur", "Frukost", "Nära havet")),
                new BusTrip("SE03", "Sverige", "Öland", "Hotel Skansen", 3, 4595, LocalDate.now().plusDays(39), "Ölandsweekend med Alvaret, Borgholm och kustnära hotell.", "En lugn bussresa till Öland med natur, historia och öländska smaker. Vi besöker Borgholm och gör utflykter till öns mest omtyckta platser.", "Frukost och en middag", 4, 36, LocalTime.of(16, 20), LocalTime.of(9, 30), LocalTime.of(18, 0), stops("Göteborg:06:15", "Borås:07:00", "Jönköping:08:20", "Växjö:10:15", "Kalmar:11:25"), image("photo-1500530855697-b586d89ba3ee"), facilities("Naturutflykt", "Frukost", "En middag", "Kustnära hotell")),
                new BusTrip("SE04", "Sverige", "Gotland", "Best Western Strand Hotel", 4, 6995, LocalDate.now().plusDays(45), "Gotland med Visby, raukar och färjeresa från fastlandet.", "Vi reser med buss och färja till Gotland för dagar fyllda av ringmur, hav, raukområden och egen tid i Visby.", "Frukost", 4, 34, LocalTime.of(18, 30), LocalTime.of(8, 15), LocalTime.of(21, 0), stops("Göteborg:05:45", "Borås:06:35", "Jönköping:08:00", "Linköping:09:45", "Nynäshamn:12:15"), image("photo-1519681393784-d120267933ba"), facilities("Färja ingår", "Visby", "Utflykt", "Frukost")),
                new BusTrip("SE05", "Sverige", "Dalarna", "Mora Hotell & Spa", 3, 4995, LocalDate.now().plusDays(52), "Dalarna med Siljan, Mora och klassiska svenska miljöer.", "En charmig resa till Dalarna med röda stugor, hantverk, Siljansbygden och bekvämt hotell i Mora.", "Frukost och två middagar", 4, 40, LocalTime.of(16, 0), LocalTime.of(9, 0), LocalTime.of(18, 15), stops("Göteborg:06:00", "Örebro:09:10", "Västerås:10:20", "Fagersta:11:25"), image("photo-1501785888041-af3ef285b470"), facilities("Siljanutflykt", "Spa", "Frukost", "Två middagar")),
                new BusTrip("SE06", "Sverige", "Höga Kusten", "Hotell Höga Kusten", 4, 7495, LocalDate.now().plusDays(60), "Höga Kusten med dramatiska vyer, bro och naturupplevelser.", "Vi reser norrut till Höga Kusten för vandringsvänliga vyer, kustlandskap och utflykter i ett av Sveriges mest speciella områden.", "Frukost och middag", 4, 32, LocalTime.of(17, 45), LocalTime.of(8, 30), LocalTime.of(20, 30), stops("Stockholm:07:00", "Uppsala:07:55", "Gävle:09:10", "Sundsvall:12:10"), image("photo-1441974231531-c6227db76b6e"), facilities("Naturutflykt", "Frukost", "Middag", "Utsiktshotell")),
                new BusTrip("SE07", "Sverige", "Värmland", "Elite Stadshotellet Karlstad", 2, 3495, LocalDate.now().plusDays(66), "Värmland med Karlstad, Vänern och Selma Lagerlöfs Mårbacka.", "En kort och fin bussresa till Värmland med Karlstad som bas, utflykt i det värmländska landskapet och tid vid Vänern.", "Frukost", 4, 42, LocalTime.of(13, 45), LocalTime.of(10, 0), LocalTime.of(16, 30), stops("Göteborg:07:00", "Trollhättan:08:00", "Åmål:09:20"), image("photo-1500534314209-a25ddb2bd429"), facilities("Utflykt", "Frukost", "Centralt hotell", "Fri tid")),
                new BusTrip("EU01", "Tyskland", "Berlin", "Park Inn Alexanderplatz", 4, 6295, LocalDate.now().plusDays(34), "Berlin med historia, shopping och levande stadsliv.", "Bussresa till Berlin med centralt hotell, stadsrundtur och gott om egen tid för museum, mat och sevärdheter.", "Frukost", 4, 44, LocalTime.of(17, 30), LocalTime.of(8, 30), LocalTime.of(20, 0), stops("Göteborg:05:30", "Halmstad:07:00", "Helsingborg:08:15", "Malmö:09:10"), image("photo-1560969184-10fe8719e047"), facilities("Stadsrundtur", "Frukost", "Centralt hotell", "Fri tid")),
                new BusTrip("EU02", "Tyskland", "Hamburg", "Hotel Hafen Hamburg", 3, 5295, LocalDate.now().plusDays(41), "Hamburg med hamn, kanaler och nordtysk stadskänsla.", "Vi reser via Danmark till Hamburg och bor nära hamnen. Resan passar dig som vill uppleva storstad, vatten och bra restauranger.", "Frukost", 4, 44, LocalTime.of(15, 40), LocalTime.of(9, 30), LocalTime.of(18, 30), stops("Göteborg:06:00", "Varberg:06:55", "Halmstad:07:45", "Helsingborg:09:00", "Malmö:09:50"), image("photo-1511527661048-7fe73d85e9a4"), facilities("Hamnutflykt", "Frukost", "Centralt hotell", "Fri tid")),
                new BusTrip("EU03", "Tjeckien", "Prag", "Hotel Majestic Plaza", 5, 7295, LocalDate.now().plusDays(49), "Prag med gamla stan, Karlsbron och klassisk centraleuropeisk charm.", "En innehållsrik bussresa till Prag med hotell nära centrum, guidad tur och kvällar för egna upptäckter.", "Frukost", 4, 40, LocalTime.of(18, 30), LocalTime.of(8, 0), LocalTime.of(21, 30), stops("Stockholm:05:30", "Norrköping:07:15", "Linköping:07:55", "Jönköping:09:30", "Helsingborg:12:10"), image("photo-1519677100203-a0e668c92439"), facilities("Guidad tur", "Frukost", "Centralt hotell", "Utflykt")),
                new BusTrip("EU04", "Polen", "Kraków", "Hotel Wyspianski", 5, 6995, LocalDate.now().plusDays(55), "Kraków med vacker stadskärna, historia och prisvärd semester.", "Resan går genom södra Sverige och vidare till Kraków. Vi bor centralt och erbjuder gemensam rundtur samt egen tid.", "Frukost", 3, 40, LocalTime.of(19, 0), LocalTime.of(8, 0), LocalTime.of(22, 0), stops("Stockholm:05:00", "Linköping:07:20", "Jönköping:09:00", "Helsingborg:11:45", "Malmö:12:35"), image("photo-1543429776-2782fc586c63"), facilities("Stadsrundtur", "Frukost", "Centralt hotell", "Historia")),
                new BusTrip("EU05", "Nederländerna", "Amsterdam", "WestCord City Centre", 5, 7995, LocalDate.now().plusDays(63), "Amsterdam med kanaler, museer och charmiga kvarter.", "Bekväm bussresa till Amsterdam med centralt boende, kanalstadens klassiska miljöer och tid för egna val.", "Frukost", 4, 38, LocalTime.of(18, 0), LocalTime.of(8, 30), LocalTime.of(22, 0), stops("Göteborg:05:45", "Varberg:06:35", "Halmstad:07:25", "Helsingborg:08:40", "Malmö:09:30"), image("photo-1512470876302-972faa2aa9a4"), facilities("Kanalpromenad", "Frukost", "Centralt hotell", "Fri tid")),
                new BusTrip("EU06", "Frankrike", "Paris", "Hotel Lorette Opera", 6, 9495, LocalDate.now().plusDays(72), "Paris med klassiska sevärdheter, caféer och stadspuls.", "En längre bussresa till Paris med hotell i bra läge, stadsrundtur och tid för egna besök vid museer, parker och restauranger.", "Frukost", 4, 36, LocalTime.of(19, 30), LocalTime.of(8, 0), LocalTime.of(23, 0), stops("Göteborg:05:30", "Halmstad:07:00", "Helsingborg:08:15", "Malmö:09:10"), image("photo-1502602898657-3e91760cbb34"), facilities("Stadsrundtur", "Frukost", "Centralt hotell", "Fri tid")),
                new BusTrip("EU07", "Italien", "Gardasjön", "Hotel Garda Bellevue", 7, 10495, LocalDate.now().plusDays(80), "Gardasjön med italienska byar, sjöutsikt och avkoppling.", "Vi reser söderut till Gardasjön för en vecka med vackra vyer, utflykter och italiensk semesterkänsla.", "Frukost och middag", 4, 34, LocalTime.of(16, 30), LocalTime.of(9, 0), LocalTime.of(22, 30), stops("Stockholm:04:45", "Linköping:07:00", "Jönköping:08:30", "Helsingborg:11:10", "Malmö:12:00"), image("photo-1533105079780-92b9be482077"), facilities("Sjöutflykt", "Frukost", "Middag", "Pool")),
                new BusTrip("EU08", "Italien", "Dolomiterna", "Hotel Alpenrose", 6, 9995, LocalDate.now().plusDays(88), "Dolomiterna med berg, vyer och lugna alpbyar.", "En naturnära bussresa till Dolomiterna med hotell i alpmiljö, utflykter och möjlighet till lättare promenader.", "Halvpension", 4, 34, LocalTime.of(17, 15), LocalTime.of(8, 30), LocalTime.of(22, 0), stops("Göteborg:05:15", "Borås:06:00", "Jönköping:07:25", "Helsingborg:10:20", "Malmö:11:10"), image("photo-1506905925346-21bda4d32df4"), facilities("Bergsutflykt", "Halvpension", "Alphotell", "Natur")),
                new BusTrip("EU09", "Italien", "Toscana", "Villa Ricci Hotel", 7, 10995, LocalDate.now().plusDays(96), "Toscana med kullar, vinbyar och italiensk mat.", "En varm och innehållsrik bussresa till Toscana med utflykter till historiska städer, vingårdar och tid för egna promenader.", "Frukost och middag", 4, 32, LocalTime.of(18, 0), LocalTime.of(8, 30), LocalTime.of(23, 0), stops("Stockholm:04:30", "Linköping:06:45", "Jönköping:08:15", "Helsingborg:11:00", "Malmö:11:50"), image("photo-1523906834658-6e24ef2386f9"), facilities("Utflykter", "Frukost", "Middag", "Vinbyar")),
                new BusTrip("EU10", "Österrike", "Tyrolen", "Hotel Innsbruckblick", 6, 8995, LocalDate.now().plusDays(70), "Tyrolen med alper, byar och österrikisk gästfrihet.", "Bussresa till Österrike med halvpension, alpina vyer och utflykter i Tyrolens vackra dalar.", "Halvpension", 4, 40, LocalTime.of(16, 45), LocalTime.of(9, 0), LocalTime.of(21, 0), stops("Göteborg:05:45", "Borås:06:30", "Jönköping:07:55", "Helsingborg:10:35", "Malmö:11:25"), image("photo-1464822759023-fed622ff2c3b"), facilities("Halvpension", "Alputflykt", "Frukost", "Middag")),
                new BusTrip("EU11", "Kroatien", "Istrien", "Hotel Parentium Plava Laguna", 7, 11495, LocalDate.now().plusDays(104), "Istrien med kuststäder, bad och kroatisk semesterkänsla.", "En längre bussresa till Kroatiens kust med hotell nära havet, utflykter till charmiga städer och tid för bad.", "Frukost och middag", 4, 32, LocalTime.of(18, 30), LocalTime.of(8, 30), LocalTime.of(23, 30), stops("Göteborg:05:00", "Borås:05:45", "Jönköping:07:10", "Helsingborg:10:00", "Malmö:10:50"), image("photo-1507525428034-b723cf961d3e"), facilities("Kusthotell", "Frukost", "Middag", "Utflykter")),
                new BusTrip("EU12", "Danmark", "Köpenhamn", "Scandic Spectrum", 2, 3195, LocalDate.now().plusDays(27), "Köpenhamn med Nyhavn, shopping och dansk weekendkänsla.", "Kort bussresa till Köpenhamn med centralt hotell, stadspromenad och gott om egen tid.", "Frukost", 4, 46, LocalTime.of(12, 30), LocalTime.of(11, 0), LocalTime.of(15, 30), stops("Göteborg:06:30", "Varberg:07:20", "Halmstad:08:10", "Helsingborg:09:25", "Malmö:10:10"), image("photo-1513622470522-26c3c8a854bc"), facilities("Centralt hotell", "Frukost", "Stadspromenad", "Fri tid")),
                new BusTrip("EU13", "Norge", "Oslo", "Thon Hotel Opera", 2, 3595, LocalDate.now().plusDays(37), "Oslo med fjord, opera och modern nordisk stadsmiljö.", "En smidig bussresa till Oslo med centralt hotell, gemensam rundtur och egen tid vid hamnen.", "Frukost", 4, 44, LocalTime.of(13, 0), LocalTime.of(10, 0), LocalTime.of(16, 0), stops("Göteborg:07:00", "Kungälv:07:30", "Uddevalla:08:15", "Strömstad:09:35"), image("photo-1513519245088-0e12902e5a38"), facilities("Fjordnära", "Frukost", "Centralt hotell", "Stadsrundtur"))
        );
    }

    private void createRoomTypes(
            Travel travel,
            RoomTypeRepository roomTypeRepository
    ) {
        roomTypeRepository.saveAll(List.of(
                new RoomType(
                        travel,
                        "Standardrum",
                        "Ett bekvämt rum med eget badrum och plats för upp till två personer.",
                        2,
                        0,
                        12
                ),
                new RoomType(
                        travel,
                        "Superiorrum",
                        "Ett rymligare rum med bättre läge och plats för upp till tre personer.",
                        3,
                        1200,
                        6
                ),
                new RoomType(
                        travel,
                        "Familjerum",
                        "Ett större rum för familj eller vänner med plats för upp till fyra personer.",
                        4,
                        2500,
                        3
                )
        ));
    }

    private void updateExistingDepartures(
            Travel travel,
            BusTrip busTrip,
            DepartureRepository departureRepository
    ) {
        for (Departure departure
                : departureRepository.findAllByTravelIdOrderByDepartureDateAsc(
                travel.getId()
        )) {
            departure.updateDetails(
                    travel,
                    departure.getDepartureDate(),
                    departure.getReturnDate(),
                    pickupSummary(busTrip.pickupStops()),
                    departure.getArrivalAirport(),
                    departure.getOutboundFlightNumber(),
                    departure.getOutboundDepartureTime(),
                    departure.getOutboundArrivalTime(),
                    departure.getReturnFlightNumber(),
                    departure.getReturnDepartureTime(),
                    departure.getReturnArrivalTime(),
                    departure.getPricePerPerson(),
                    departure.getAvailableSeats()
            );
            departureRepository.save(departure);
        }
    }

    private List<String> includedItems(BusTrip busTrip) {
        return List.of(
                "Bussresa tur och retur",
                busTrip.nights() + " hotellnätter på " + busTrip.hotelName(),
                busTrip.mealType(),
                "Dag-för-dag-program enligt beskrivning",
                "Bagage i bussens bagageutrymme"
        );
    }

    private List<String> dayProgram(BusTrip busTrip) {
        if (busTrip.nights() <= 2) {
            return List.of(
                    "Dag 1 – Påstigning i Sverige och resa till " + busTrip.destination() + ".",
                    "Dag 2 – Gemensam introduktion och egen tid i " + busTrip.destination() + ".",
                    "Dag 3 – Utcheckning och hemresa till Sverige."
            );
        }

        if (busTrip.nights() <= 4) {
            return List.of(
                    "Dag 1 – Påstigning i Sverige och resa mot " + busTrip.destination() + ".",
                    "Dag 2 – Ankomst, incheckning och kort gemensam orientering.",
                    "Dag 3 – Utflykt eller gemensam aktivitet i området.",
                    "Dag 4 – Egen tid för sevärdheter, shopping eller avkoppling.",
                    "Dag 5 – Utcheckning och bussresa tillbaka mot Sverige."
            );
        }

        return List.of(
                "Dag 1 – Påstigning i Sverige och avresa söderut.",
                "Dag 2 – Resdag med stopp längs vägen och ankomst till " + busTrip.destination() + ".",
                "Dag 3 – Guidad rundtur och gemensam introduktion.",
                "Dag 4 – Utflykt till en sevärdhet i området.",
                "Dag 5 – Egen tid för bad, kultur, mat eller shopping.",
                "Dag 6 – Extra utflykt eller lugn dag på destinationen.",
                "Dag 7 – Utcheckning och hemresa påbörjas.",
                "Dag 8 – Ankomst tillbaka till Sverige."
        );
    }

    private String pickupSummary(List<BusPickupStop> pickupStops) {
        return pickupStops
                .stream()
                .map(BusPickupStop::getCity)
                .reduce((first, second) -> first + ", " + second)
                .orElse("Sverige");
    }

    private LocalTime firstPickupTime(List<BusPickupStop> pickupStops) {
        if (pickupStops.isEmpty()) {
            return LocalTime.of(7, 0);
        }

        return LocalTime.parse(pickupStops.get(0).getDepartureTime());
    }

    private List<BusPickupStop> stops(String... stops) {
        return java.util.Arrays
                .stream(stops)
                .map(stop -> {
                    String[] parts = stop.split(":");
                    return new BusPickupStop(
                            pickupPlace(parts[0]),
                            parts[1] + ":" + parts[2]
                    );
                })
                .toList();
    }

    private String pickupPlace(String city) {
        return switch (city) {
            case "Göteborg" -> "Göteborg, Nils Ericsonterminalen";
            case "Stockholm" -> "Stockholm, Cityterminalen";
            case "Malmö" -> "Malmö, Hyllie station";
            case "Helsingborg" -> "Helsingborg, Knutpunkten";
            case "Jönköping" -> "Jönköping, Resecentrum";
            case "Borås" -> "Borås, Resecentrum";
            case "Halmstad" -> "Halmstad, Regionbussterminalen";
            case "Varberg" -> "Varberg, stationen";
            case "Örebro" -> "Örebro, Resecentrum";
            case "Linköping" -> "Linköping, Fjärrbussterminalen";
            case "Norrköping" -> "Norrköping, Resecentrum";
            case "Uppsala" -> "Uppsala, Stationsgatan";
            case "Gävle" -> "Gävle, Centralstationen";
            case "Sundsvall" -> "Sundsvall, Navet";
            case "Västerås" -> "Västerås, Resecentrum";
            case "Fagersta" -> "Fagersta, stationen";
            case "Trollhättan" -> "Trollhättan, Resecentrum";
            case "Åmål" -> "Åmål, stationen";
            case "Växjö" -> "Växjö, Resecentrum";
            case "Kalmar" -> "Kalmar, Centralstationen";
            case "Nynäshamn" -> "Nynäshamn, färjeterminalen";
            case "Kungälv" -> "Kungälv, resecentrum";
            case "Uddevalla" -> "Uddevalla, Kampenhof";
            case "Strömstad" -> "Strömstad, stationen";
            default -> city;
        };
    }

    private List<String> facilities(String... facilities) {
        return List.of(facilities);
    }

    private String image(String photoId) {
        return "https://images.unsplash.com/" + photoId + "?auto=format&fit=crop&w=1600&q=85";
    }

    private record BusTrip(
            String code,
            String country,
            String destination,
            String hotelName,
            int nights,
            int price,
            LocalDate departureDate,
            String shortDescription,
            String longDescription,
            String mealType,
            int hotelStars,
            int seats,
            LocalTime arrivalTime,
            LocalTime returnDepartureTime,
            LocalTime returnArrivalTime,
            List<BusPickupStop> pickupStops,
            String imageUrl,
            List<String> facilities
    ) {
    }
}
