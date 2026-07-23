package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.legal.LegalDocumentVersions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class LegalController {

    @GetMapping("/resevillkor")
    public String travelTerms(Model model) {
        return legalPage(
                model,
                "Resevillkor",
                "Villkor för bokning, resor, ansvar och kundens skyldigheter.",
                List.of(
                        new LegalSection(
                                "Bokning",
                                "En bokning är bindande när kunden har slutfört bokningen och godkänt villkoren. Bokningsbekräftelsen visar resmål, hotell, resenärer, pris och betalningsplan."
                        ),
                        new LegalSection(
                                "Kundens ansvar",
                                "Kunden ansvarar för att namn, personnummer, kontaktuppgifter och resehandlingar är korrekta. Eventuella fel ska meddelas EriGo Travel så snart som möjligt."
                        ),
                        new LegalSection(
                                "Ändringar",
                                "Ändringar kan vara möjliga före avresa beroende på resa, tillgänglighet och gällande regler. Prisjusteringar kan tillkomma."
                        )
                )
        );
    }

    @GetMapping("/avbokningsvillkor")
    public String cancellationTerms(Model model) {
        return legalPage(
                model,
                "Avbokningsvillkor",
                "Exempelvillkor för hur avbokningar, avgifter och återbetalningar hanteras.",
                List.of(
                        new LegalSection(
                                "Avbokning minst 15 dagar före avresa",
                                "Vid avbokning minst 15 dagar före avresa kan en avbokningskostnad på 50 % av totalpriset tas ut. Kunden förlorar aldrig mer än vad som redan har betalats."
                        ),
                        new LegalSection(
                                "Avbokning nära avresa",
                                "Vid avbokning 14 dagar eller mindre före avresa kan redan inbetalda belopp vara helt förbrukade enligt bokningens villkor."
                        ),
                        new LegalSection(
                                "Återbetalning",
                                "Eventuell återbetalning beräknas utifrån inbetalt belopp, avbokningskostnad och bokningens betalningsplan."
                        )
                )
        );
    }

    @GetMapping("/integritetspolicy")
    public String privacyPolicy(Model model) {
        return legalPage(
                model,
                "Integritetspolicy",
                "Information om hur personuppgifter kan behandlas i samband med bokning och kundkonto.",
                List.of(
                        new LegalSection(
                                "Personuppgifter",
                                "EriGo Travel behandlar uppgifter som namn, e-post, telefonnummer, personnummer, bokningsinformation och betalningshistorik för att kunna hantera resor och kundservice."
                        ),
                        new LegalSection(
                                "Syfte",
                                "Uppgifterna används för bokning, betalning, fakturering, kundkommunikation, reseadministration och support."
                        ),
                        new LegalSection(
                                "Lagring",
                                "Uppgifter sparas så länge de behövs för bokningen, lagkrav, ekonomi och kundservice. Kunden kan kontakta EriGo Travel för frågor om sina uppgifter."
                        )
                )
        );
    }

    @GetMapping({"/cookies", "/cookieinformation"})
    public String cookieInformation(Model model) {
        return legalPage(
                model,
                "Cookieinformation",
                "Information om hur cookies kan användas på webbplatsen.",
                List.of(
                        new LegalSection(
                                "Nödvändiga cookies",
                                "Webbplatsen kan använda nödvändiga cookies för inloggning, bokningsflöde, säkerhet och sessionshantering."
                        ),
                        new LegalSection(
                                "Analys och förbättring",
                                "Exempelvis kan anonym statistik användas för att förstå hur sidan används och förbättra upplevelsen."
                        ),
                        new LegalSection(
                                "Webbläsarinställningar",
                                "Kunden kan vanligtvis blockera eller radera cookies i sin webbläsare. Vissa funktioner kan då sluta fungera."
                        )
                )
        );
    }

    @GetMapping("/betalningsvillkor")
    public String paymentTerms(Model model) {
        return legalPage(
                model,
                "Betalningsvillkor",
                "Information om handpenning, slutbetalning och betalningsstatus.",
                List.of(
                        new LegalSection(
                                "Anmälningsavgift",
                                "Om bokningen har anmälningsavgift ska beloppet betalas senast det datum som visas i bokningen och på fakturan."
                        ),
                        new LegalSection(
                                "Slutbetalning",
                                "Resterande belopp ska betalas senast bokningens slutbetalningsdatum. Betalningsstatus uppdateras när betalning registreras."
                        ),
                        new LegalSection(
                                "Simulerade betalningar",
                                "I denna projektversion kan betalningar vara simulerade. Det ska inte tolkas som att en riktig betalning har dragits."
                        )
                )
        );
    }

    @GetMapping({"/kontakt", "/kontaktuppgifter"})
    public String contactInformation(Model model) {
        return legalPage(
                model,
                "Kontaktuppgifter",
                "Så här kan kunden kontakta EriGo Travel vid frågor om bokning, betalning eller resa.",
                List.of(
                        new LegalSection(
                                "Kundservice",
                                "E-post: kundservice@erigo-travel.example. Telefon: 08-123 456 78."
                        ),
                        new LegalSection(
                                "Postadress",
                                "EriGo Travel AB, Exempelgatan 1, 111 22 Stockholm."
                        ),
                        new LegalSection(
                                "Organisationsuppgifter",
                                "Organisationsnummer och företagsuppgifter bör fyllas i med verkliga uppgifter innan lansering."
                        )
                )
        );
    }

    @GetMapping({"/paketresor", "/information-om-paketresor"})
    public String packageTravelInformation(Model model) {
        return legalPage(
                model,
                "Information om paketresor",
                "Kundinformation om rättigheter vid paketresor.",
                List.of(
                        new LegalSection(
                                "Paketresa",
                                "När flyg och hotell säljs tillsammans kan resan vara en paketresa. Kunden omfattas då av särskilda rättigheter enligt paketreseregler."
                        ),
                        new LegalSection(
                                "Arrangörens ansvar",
                                "Arrangören ansvarar för att paketresan som helhet fullgörs korrekt enligt avtalet."
                        ),
                        new LegalSection(
                                "Hjälp under resan",
                                "Kunden ska kunna kontakta arrangören om problem uppstår under resan."
                        )
                )
        );
    }

    @GetMapping({"/resegaranti", "/information-om-resegaranti"})
    public String travelGuaranteeInformation(Model model) {
        return legalPage(
                model,
                "Information om resegaranti",
                "Exempelinformation om ekonomiskt skydd vid paketresor.",
                List.of(
                        new LegalSection(
                                "Skydd vid obestånd",
                                "Resegarantin är till för att skydda kundens betalningar om arrangören hamnar på obestånd."
                        ),
                        new LegalSection(
                                "Hemtransport",
                                "När transport ingår i paketresan ska skyddet även kunna omfatta hemtransport om det krävs."
                        ),
                        new LegalSection(
                                "Kontroll före lansering",
                                "Verkliga uppgifter om resegaranti och garantigivare ska kontrolleras och fyllas i innan webbplatsen används skarpt."
                        )
                )
        );
    }

    private String legalPage(
            Model model,
            String title,
            String introduction,
            List<LegalSection> sections
    ) {
        model.addAttribute("title", title);
        model.addAttribute("introduction", introduction);
        model.addAttribute("sections", sections);
        model.addAttribute(
                "termsVersion",
                LegalDocumentVersions.CURRENT_TERMS_VERSION
        );
        model.addAttribute(
                "termsDate",
                LegalDocumentVersions.CURRENT_TERMS_DATE
        );

        return "legal-page";
    }

    public record LegalSection(
            String heading,
            String body
    ) {
    }
}
