package com.ericthilen.travelbookingplatform.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerSupportKnowledgeBase {

    public List<String> topics() {
        return List.of(
                "Resor och hotell",
                "Bokningar",
                "Betalningar och fakturor",
                "Avbokningar",
                "Konto och inloggning",
                "Villkor och resegaranti"
        );
    }

    public List<KnowledgeArticle> articles() {
        return List.of(
                new KnowledgeArticle(
                        "Mallorca - Sun Bay Resort",
                        "Spanien",
                        "Familjevänlig solresa med frukost, nära stranden och Palma.",
                        "Passar kunder som vill ha trygg charter, badvikar, restauranger och enkel semesterlogistik. Bra att nämna: kort transferkänsla, många utflykter, strandnära läge och smidig resa för barnfamiljer.",
                        "Påminn kunden om att högsäsong kan innebära mer folk vid stränderna. Rekommendera att boka utflykter tidigt om kunden vill se Palma, bergsbyar eller båtturer."
                ),
                new KnowledgeArticle(
                        "Kreta - Blue Coast Hotel",
                        "Grekland",
                        "Modernt hotell med halvpension, pool, havsutsikt och grekisk mat.",
                        "Passar par och familjer som vill kombinera bad med lokal matkultur. Bra att lyfta: halvpension, havsnära känsla, poolbar, restaurang och lugn semesterbas.",
                        "Tipsa om bekväma skor för utflykter och att kvällar kan vara något svalare under vår och höst. Kunden bör kontrollera flygtider och bagageregler på bokningssidan."
                ),
                new KnowledgeArticle(
                        "Ayia Napa - Ocean View Resort",
                        "Cypern",
                        "Modern semester nära strand, pool, restauranger och nöjen.",
                        "Passar kunder som vill ha sol, bad och mer liv på kvällarna. Bra att nämna: nära centrum, stor pool, gym och enkel tillgång till restauranger.",
                        "För familjer kan det vara bra att fråga om kunden vill bo lugnare eller nära centrum. Sommaren kan vara mycket varm, så rekommendera solskydd och vatten."
                ),
                new KnowledgeArticle(
                        "Gran Canaria - Palm Garden Hotel",
                        "Spanien",
                        "All Inclusive med flera pooler och sol nästan året runt.",
                        "Passar kunder som prioriterar bekvämlighet och stabilt väder. Bra att lyfta: All Inclusive, barnklubb, underhållning och flera poolområden.",
                        "Bra vinteralternativ om avgångar finns. Om kunden söker lugn semester bör agenten dubbelkolla hotellområde och rumstyp innan rekommendation."
                ),
                new KnowledgeArticle(
                        "Sicilien - Villa Mare",
                        "Italien",
                        "Personlig resa med frukost, havsutsikt, terrass och italiensk matkultur.",
                        "Passar kunder som vill ha mer kultur, mat och lugn semester än klassisk resort. Bra att lyfta: restauranger, badvikar, lokala utflykter och charmigare hotellkänsla.",
                        "Hotellet har 3 stjärnor, så förväntningarna ska vara personligt och mysigt snarare än stort lyxresort. Rekommendera hyrbil eller organiserade utflykter för den som vill se mer."
                ),
                new KnowledgeArticle(
                        "Antalya - Golden Beach Resort",
                        "Turkiet",
                        "Prisvärd All Inclusive med privat strand, barnklubb, spa och vattenrutschkanor.",
                        "Passar barnfamiljer och kunder som vill ha mycket inkluderat. Bra att lyfta: femstjärnigt hotell, privat strand, All Inclusive och aktiviteter på området.",
                        "Förklara tydligt vad som ingår i All Inclusive och att vissa spa- eller strandtjänster kan kosta extra beroende på upplägg. Kontrollera passregler och resevillkor inför avresa."
                ),
                new KnowledgeArticle(
                        "Betalning och faktura",
                        "Bokning",
                        "Kunden ser handpenning, slutbetalning, inbetalt belopp och återstående belopp på bokningssidan.",
                        "Om inget ska betalas direkt ska kunden ändå se betalningsinformationen i bokningsflödet. Faktura kan laddas ner från bokningen eller Mitt konto.",
                        "Rabattkoder kan läggas till före betalning. Admin kan lägga in rabatt även från bokningsvyn och rabatten ska synas i prisdetaljer och betalningsinformation."
                ),
                new KnowledgeArticle(
                        "Avbokning och intyg",
                        "Bokning",
                        "Kunden eller admin kan avboka enligt reglerna som visas i avbokningsprocessen.",
                        "Agenten ska hjälpa kunden förstå avbokningsvillkor, eventuell avgift, återbetalning och hur avbokningsintyg skickas.",
                        "Efter avbokning ska ändringar spärras. Om ärendet är oklart bör agenten eskalera och sammanfatta vad kunden behöver hjälp med."
                )
        );
    }

    public String welcomeText() {
        return "Hej! Jag är EriGo Assist. Jag kan hjälpa dig med resor, "
                + "bokningar, betalningar, fakturor, avbokningar och konto. "
                + "Om jag inte kan lösa det kopplar jag vidare till en agent.";
    }

    public String travelHelp() {
        return "Du kan söka resor via Resor-sidan eller startsidans reseplanerare. "
                + "Där kan du filtrera på destination, land, flygplats, datum, "
                + "antal nätter, måltidstyp, hotellstandard, maxpris och lediga platser. "
                + "EriGo har just nu resor till Mallorca, Kreta, Ayia Napa, "
                + "Gran Canaria, Sicilien och Antalya. Mallorca passar särskilt bra "
                + "för familjer som vill bo nära strand och Palma. Kreta passar kunder "
                + "som vill kombinera sol, bad och grekisk mat. Ayia Napa passar kunder "
                + "som vill ha strand, pool och nära nöjen. Gran Canaria är ett tryggt "
                + "solval med All Inclusive och bra semesterkänsla nästan året runt. "
                + "Sicilien är mer personlig och passar kunder som gillar mat, kultur "
                + "och lugnare hotell. Antalya är prisvärt, familjevänligt och har "
                + "All Inclusive, privat strand och aktiviteter för barn. På varje "
                + "resesida ser du hotell, destination, flygtider, rum och avgångar.";
    }

    public String bookingHelp() {
        return "Du hittar din resa under Inför resan > Din resa. Ange kundnummer, "
                + "bokningsnummer och e-postadress från bokningsbekräftelsen. "
                + "Är du inloggad finns dina bokningar även under Mitt konto.";
    }

    public String paymentHelp() {
        return "Betalningsinformationen finns på bokningssidan. Där visas handpenning, "
                + "sista datum för handpenning, inbetalt belopp, återstående belopp "
                + "och slutbetalningsdatum. Fakturor kan laddas ner från bokningen "
                + "eller från Mitt konto.";
    }

    public String cancellationHelp() {
        return "Avbokning hanteras från bokningssidan. Systemet visar avbokningsvillkor, "
                + "eventuell avbokningsavgift och om något belopp återbetalas innan "
                + "avbokningen bekräftas. Efter avbokning kan kundtjänst hjälpa till "
                + "med avbokningsintyg.";
    }

    public String accountHelp() {
        return "På Mitt konto kan du se profil, bokningar, fakturor, betalningar, "
                + "avbokade resor, inställningar och lösenord. Om du inte kan logga in, "
                + "kontrollera e-post och lösenord eller skapa ett konto med samma e-post "
                + "som din bokning.";
    }

    public String legalHelp() {
        return "Under Inför resan > Resevillkor finns resevillkor, avbokningsvillkor, "
                + "betalningsvillkor, integritetspolicy, cookies, paketreseinformation "
                + "och resegaranti. Texterna är exempeltexter och bör granskas juridiskt "
                + "före verklig lansering.";
    }

    public String baggageHelp() {
        return "Flyginformationen visar vilket handbagage och incheckat bagage som ingår. "
                + "På bokningssidan ser du även flygnummer, tider, flygplats och ungefärlig "
                + "flygtid för både utresa och hemresa.";
    }

    public String travelerHelp() {
        return "Resenärernas uppgifter fylls i i bokningsflödet. Om en uppgift behöver "
                + "rättas före avresa kan kundtjänst eller admin hjälpa till, så länge "
                + "bokningen inte är avbokad och ändringen inte ligger för nära avresa.";
    }

    public String discountHelp() {
        return "Rabattkod kan anges i bokningsflödet och på bokningssidan så länge inget "
                + "är betalt. Admin kan även lägga till rabattkod på kundens bokning. "
                + "Rabatten visas i prisdetaljerna och påverkar betalningsinformationen.";
    }

    public String contactHelp() {
        return "Du kan kontakta kundtjänst via chatten. Om ärendet behöver hanteras av "
                + "en människa skickas hela konversationen vidare till en agent, så du "
                + "slipper börja om.";
    }

    public String escalationText() {
        return "Jag skickar vidare detta till kundtjänst så att en agent kan hjälpa dig. "
                + "Du kan fortsätta skriva här, och agenten ser hela historiken.";
    }

    public record KnowledgeArticle(
            String title,
            String category,
            String summary,
            String agentGuidance,
            String importantNotes
    ) {
    }
}
