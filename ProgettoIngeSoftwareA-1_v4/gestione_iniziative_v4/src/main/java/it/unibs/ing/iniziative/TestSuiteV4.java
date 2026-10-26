package it.unibs.ing.iniziative;

import it.unibs.ing.iniziative.controller.ConfigurazioneController;
import it.unibs.ing.iniziative.model.*;
import it.unibs.ing.iniziative.storage.DatiSistema;
import it.unibs.ing.iniziative.storage.GestorePersistenza;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Suite di collaudo intensivo per la Versione 4:
 * verifica delle fondamenta V1 + V2 + V3 e di tutti i requisiti specifici della Versione 4:
 * - Disdetta dell'iscrizione da parte di un Fruitore a una proposta APERTA entro le 23:59 del Termine ultimo di iscrizione;
 * - Possibilità per il Fruitore che ha disdetto di re-iscriversi successivamente entro i termini;
 * - Rifiuto della disdetta oltre il termine ultimo di iscrizione o quando la proposta non è più APERTA;
 * - Ritiro da parte di un Configuratore di una proposta APERTA o CONFERMATA fino alle 23:59 del giorno precedente 'Data';
 * - Effetti del ritiro: stato RITIRATA e data nell'ArchivioProposte, rimozione dalla Bacheca (se APERTA),
 *   notifica automatica nello SpazioPersonale di tutti gli aderenti e insieme di iscritti congelato e non più variabile;
 * - Rifiuto del ritiro a partire dal giorno stesso di 'Data';
 * - Simulazione End-to-End interattiva con Kibo (Configuratore + Fruitori + Disdetta + Re-iscrizione + Ritiro + Notifica).
 */
public class TestSuiteV4 {

    private static int testEseguiti = 0;
    private static int testSuperati = 0;
    private static int testFalliti = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   AVVIO SUITE DI COLLAUDO INTENSIVO - VERSIONE 4 (ZANELLA)   ");
        System.out.println("===============================================================");

        // Test V1 + V2 + V3
        eseguiTest("1. [V1-V3] Registrazione Fruitore, univocità globale username, adesione e vincoli capienza/termine", TestSuiteV4::testFondamentaV3Iscrizioni);
        eseguiTest("2. [V3] Scadenze automatiche mezzanotte (CONFERMATA, ANNULLATA, CONCLUSA) e Spazio Personale", TestSuiteV4::testFondamentaV3ScadenzeESpazioPersonale);
        eseguiTest("3. [V1-V2] Fondamenta V1+V2 in V4: 8 RuoloCampoBase, Nota 8, Copia difensiva, MVC e DBC", TestSuiteV4::testFondamentaV1V2InV4);

        // Test specifici V4
        eseguiTest("4. [V4] Disdetta iscrizione valida entro le 23:59 del termine e successiva re-iscrizione", TestSuiteV4::testDisdettaValidaEReiscrizione);
        eseguiTest("5. [V4] Disdetta libera un posto per altri fruitori su una proposta precedentemente al completo", TestSuiteV4::testDisdettaLiberaPostoPropostaAlCompleto);
        eseguiTest("6. [V4] Rifiuto disdetta oltre il termine ultimo di iscrizione o su proposta non APERTA", TestSuiteV4::testRifiutoDisdettaOltreTermineONonAperta);
        eseguiTest("7. [V4] Ritiro proposta APERTA entro il giorno precedente Data: rimozione Bacheca, archivio, notifiche e blocco iscritti", TestSuiteV4::testRitiroPropostaAperta);
        eseguiTest("8. [V4] Ritiro proposta CONFERMATA fino al giorno precedente Data: stato RITIRATA, notifiche e blocco iscritti", TestSuiteV4::testRitiroPropostaConfermata);
        eseguiTest("9. [V4] Rifiuto ritiro nel giorno stesso di Data (o successivo) e su proposte non ritirabili", TestSuiteV4::testRifiutoRitiroGiornoDataOStatoNonValido);
        eseguiTest("10. [V4] Simulazione E2E Main V4 + Kibo (Wizard 8 campi base + Disdetta/Re-iscrizione + Ritiro Configuratore)", TestSuiteV4::testSimulazioneEndToEndMainV4);

        System.out.println("\n===============================================================");
        System.out.printf("Riepilogo V4: %d eseguiti | %d SUPERATI | %d FALLITI\n", testEseguiti, testSuperati, testFalliti);
        System.out.println("===============================================================");

        if (testFalliti > 0) {
            System.exit(1);
        }
    }

    private static void eseguiTest(String nome, Runnable test) {
        testEseguiti++;
        System.out.printf("\n[TEST V4 - %02d] %s ... ", testEseguiti, nome);
        try {
            test.run();
            testSuperati++;
            System.out.println("OK");
        } catch (Throwable t) {
            testFalliti++;
            System.out.println("FALLITO!");
            t.printStackTrace(System.out);
        }
    }

    private static void asserisci(boolean condizione, String messaggio) {
        if (!condizione) {
            throw new AssertionError("Fallimento asserzione: " + messaggio);
        }
    }

    private static DatiSistema creaDatiSistemaConCampiBase() {
        DatiSistema ds = new DatiSistema();
        ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        return ds;
    }

    private static Proposta creaPropostaApertaBase(DatiSistema ds, Categoria cat, Configuratore autore,
                                                   int maxPartecipanti, LocalDate oggi) {
        if (!ds.getConfigurazioneSistema().isCampiBaseImpostati()) {
            ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        }
        Proposta p = new Proposta(cat, autore);
        List<Campo> campi = ds.getTuttiICampiCategoria(cat);

        for (Campo c : campi) {
            switch (c.getNome()) {
                case "Titolo":
                    p.assegnaValore(c, "Gita a Montisola");
                    break;
                case "Numero di partecipanti":
                    p.assegnaValore(c, maxPartecipanti);
                    break;
                case "Termine ultimo di iscrizione":
                    p.assegnaValore(c, oggi.plusDays(5));
                    break;
                case "Luogo":
                    p.assegnaValore(c, "Stazione Brescia");
                    break;
                case "Data":
                    p.assegnaValore(c, oggi.plusDays(7));
                    break;
                case "Ora":
                    p.assegnaValore(c, LocalTime.of(8, 30));
                    break;
                case "Quota individuale":
                    p.assegnaValore(c, 20.00);
                    break;
                case "Data conclusiva":
                    p.assegnaValore(c, oggi.plusDays(8));
                    break;
                default:
                    if (c.isObbligatorio()) {
                        if (c.getTipo() == TipoCampo.BOOLEANO) p.assegnaValore(c, true);
                        else p.assegnaValore(c, "ValoreObbligatorio");
                    }
                    break;
            }
        }
        List<String> errori = p.validaVincoli(oggi, campi);
        asserisci(errori.isEmpty(), "La proposta base deve essere valida: " + errori);
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        asserisci(controller.pubblicaPropostaInBacheca(p, oggi), "Pubblicazione proposta base in bacheca");
        return p;
    }

    // -------------------------------------------------------------------------
    // 1. FONDAMENTA V3: REGISTRAZIONE E VINCOLI ISCRIZIONE
    // -------------------------------------------------------------------------
    private static void testFondamentaV3Iscrizioni() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);

        Configuratore def = (Configuratore) controller.login("config", "config");
        Configuratore admin = controller.registraNuovoConfiguratore(def, "mario_cfg", "pwd1");
        asserisci(admin != null, "Configuratore registrato");

        Fruitore f1 = controller.registraFruitore("elena_fru", "pwd2");
        asserisci(f1 != null && f1.checkInvariant(), "Fruitore registrato");
        asserisci(controller.registraFruitore("mario_cfg", "pwd3") == null, "Rifiutato username già usato da configuratore");
        asserisci(controller.registraFruitore("ELENA_FRU", "pwd4") == null, "Rifiutato username duplicato fruitore");

        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        LocalDate oggi = LocalDate.of(2026, 10, 6);
        Proposta p = creaPropostaApertaBase(ds, gite, admin, 1, oggi);

        asserisci(p.iscriviFruitore("elena_fru", oggi) == null, "Iscrizione valida");
        asserisci(p.iscriviFruitore("elena_fru", oggi) != null, "Rifiutata doppia iscrizione");
        asserisci(p.iscriviFruitore("altro", oggi) != null, "Rifiutata iscrizione oltre capienza");
    }

    // -------------------------------------------------------------------------
    // 2. FONDAMENTA V3: SCADENZE AUTOMATICHE E SPAZIO PERSONALE
    // -------------------------------------------------------------------------
    private static void testFondamentaV3ScadenzeESpazioPersonale() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Fruitore f1 = ds.getGestoreUtenti().registraFruitore("luca", "pwd");
        Proposta p = creaPropostaApertaBase(ds, gite, autore, 1, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));

        // Scadenza termine (oggi + 6) -> CONFERMATA
        ds.aggiornaStatiEScadenze(oggi.plusDays(6));
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Passata a CONFERMATA");
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 1, "Notifica promemoria ricevuta");

        // Scadenza data conclusiva (oggi + 9) -> CONCLUSA
        ds.aggiornaStatiEScadenze(oggi.plusDays(9));
        asserisci(p.getStato() == StatoProposta.CONCLUSA, "Passata a CONCLUSA");
    }

    // -------------------------------------------------------------------------
    // 3. FONDAMENTA V1 + V2 IN V4
    // -------------------------------------------------------------------------
    private static void testFondamentaV1V2InV4() {
        DatiSistema ds = new DatiSistema();
        asserisci(!ds.getConfigurazioneSistema().isCampiBaseImpostati(), "Campi base non pre-impostati prima del wizard");
        ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        asserisci(ds.getConfigurazioneSistema().getCampiBase().size() == RuoloCampoBase.values().length, "8 campi base RuoloCampoBase");

        ConfigurazioneController controller = new ConfigurazioneController(ds);
        controller.creaCategoria("Sport", "Eventi sportivi");
        controller.aggiungiCampoSpecifico("Sport", "Certificato medico", "Obbligo", false, TipoCampo.BOOLEANO);
        asserisci(!controller.aggiungiCampoComune("Certificato medico", "Dup", true, TipoCampo.STRINGA),
                "Impedita collisione tra nuovo campo comune e campo specifico esistente");

        Categoria sport = controller.getCategoria("Sport");
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);
        Proposta p = creaPropostaApertaBase(ds, sport, autore, 2, oggi);
        controller.modificaObbligatorietaCampoSpecifico("Sport", "Certificato medico", true);
        asserisci(p.checkInvariant() && controller.checkInvariant(), "Invarianti DBC rispettati");
    }

    // -------------------------------------------------------------------------
    // 4. [V4] DISDETTA VALIDA ENTRO LE 23:59 DEL TERMINE E RE-ISCRIZIONE
    // -------------------------------------------------------------------------
    private static void testDisdettaValidaEReiscrizione() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 3, oggi);
        asserisci(controller.iscriviFruitoreAProposta(p, "luca", oggi.plusDays(1)) == null, "Iscrizione iniziale riuscita");
        asserisci(p.isIscritto("luca") && p.getNumeroAderenti() == 1, "luca risulta iscritto (1/3)");

        // Disdetta proprio nel giorno di 'Termine ultimo di iscrizione' (oggi + 5, entro le 23:59)
        LocalDate giornoTermine = oggi.plusDays(5);
        String esitoDisdetta = controller.disdiciIscrizioneFruitore(p, "luca", giornoTermine);
        asserisci(esitoDisdetta == null, "La disdetta entro le 23:59 del giorno del termine deve riuscire");
        asserisci(!p.isIscritto("luca") && p.getNumeroAderenti() == 0, "luca non risulta più tra gli aderenti (0/3)");

        // Un fruitore che ha disdetto può iscriversi di nuovo entro il termine ultimo di iscrizione
        String esitoReiscrizione = controller.iscriviFruitoreAProposta(p, "luca", giornoTermine);
        asserisci(esitoReiscrizione == null, "Il fruitore che ha cancellato l'iscrizione può iscriversi di nuovo entro i termini");
        asserisci(p.isIscritto("luca") && p.getNumeroAderenti() == 1, "luca risulta nuovamente iscritto (1/3)");
    }

    // -------------------------------------------------------------------------
    // 5. [V4] DISDETTA LIBERA UN POSTO SU UNA PROPOSTA AL COMPLETO
    // -------------------------------------------------------------------------
    private static void testDisdettaLiberaPostoPropostaAlCompleto() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 2, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));
        p.iscriviFruitore("sara", oggi.plusDays(1));
        asserisci(p.isAlCompleto(), "Proposta al completo (2/2)");
        asserisci(p.iscriviFruitore("marco", oggi.plusDays(2)) != null, "marco rifiutato perché proposta al completo");

        // Sara disdice entro i termini -> si libera un posto
        asserisci(p.disdiciFruitore("sara", oggi.plusDays(2)) == null, "Disdetta di sara riuscita");
        asserisci(!p.isAlCompleto() && p.getPostiRimanenti() == 1, "Si è liberato 1 posto");

        // Ora Marco può iscriversi
        asserisci(p.iscriviFruitore("marco", oggi.plusDays(3)) == null, "marco può ora iscriversi al posto liberato");
        asserisci(p.isAlCompleto(), "Proposta nuovamente al completo con luca e marco");
    }

    // -------------------------------------------------------------------------
    // 6. [V4] RIFIUTO DISDETTA OLTRE IL TERMINE O SU PROPOSTA NON APERTA
    // -------------------------------------------------------------------------
    private static void testRifiutoDisdettaOltreTermineONonAperta() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 1, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));

        // 1. Rifiuto disdetta da parte di chi non è iscritto
        asserisci(p.disdiciFruitore("sara", oggi.plusDays(2)) != null, "Chi non è iscritto non può disdire");

        // 2. Rifiuto disdetta oltre il termine ultimo di iscrizione (oggi + 6 > oggi + 5)
        asserisci(p.disdiciFruitore("luca", oggi.plusDays(6)) != null, "Rifiutata disdetta oltre le 23:59 del termine di iscrizione");
        asserisci(p.isIscritto("luca"), "luca resta iscritto");

        // 3. Rifiuto disdetta quando la proposta è passata a CONFERMATA
        ds.aggiornaStatiEScadenze(oggi.plusDays(6));
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Proposta CONFERMATA");
        asserisci(p.disdiciFruitore("luca", oggi.plusDays(6)) != null, "Rifiutata disdetta su proposta CONFERMATA");
        asserisci(p.isIscritto("luca"), "luca resta iscritto");
    }

    // -------------------------------------------------------------------------
    // 7. [V4] RITIRO PROPOSTA APERTA ENTRO IL GIORNO PRECEDENTE 'DATA'
    // -------------------------------------------------------------------------
    private static void testRitiroPropostaAperta() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Fruitore f1 = controller.registraFruitore("luca", "pwd");
        Fruitore f2 = controller.registraFruitore("sara", "pwd");

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 3, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));
        p.iscriviFruitore("sara", oggi.plusDays(2));

        LocalDate dataRitiro = oggi.plusDays(3); // < Data inizio (oggi + 7)
        asserisci(p.isRitirabile(dataRitiro), "La proposta APERTA è ritirabile prima di Data");
        asserisci(controller.getProposteRitirabili(dataRitiro).contains(p), "Presente tra le proposte ritirabili");

        boolean ritirata = controller.ritiraProposta(p, dataRitiro, "Maltempo previsto");
        asserisci(ritirata, "Ritiro proposta APERTA eseguito con successo");

        // Effetti del ritiro:
        // 1. Stato RITIRATA e registrazione in ArchivioProposte con data del passaggio
        asserisci(p.getStato() == StatoProposta.RITIRATA, "Stato della proposta passato a RITIRATA");
        List<TransizioneStato> storia = p.getCronologiaStati();
        TransizioneStato ultimoStato = storia.get(storia.size() - 1);
        asserisci(ultimoStato.getStato() == StatoProposta.RITIRATA && ultimoStato.getDataAssunzione().equals(dataRitiro),
                "Registrato passaggio a RITIRATA con la relativa data nell'archivio storico");

        // 2. Rimossa dalla Bacheca perché era APERTA
        asserisci(!ds.getBacheca().getProposteAperte().contains(p), "La proposta APERTA ritirata è stata rimossa dalla Bacheca");
        asserisci(ds.getArchivioProposte().getProposte().contains(p), "La proposta resta nell'ArchivioProposte");

        // 3. Notifica automatica a tutti gli iscritti nel loro SpazioPersonale
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 1, "luca ha ricevuto la notifica di ritiro");
        asserisci(f2.getSpazioPersonale().getNumeroNotifiche() == 1, "sara ha ricevuto la notifica di ritiro");
        asserisci(f1.getSpazioPersonale().getNotifiche().get(0).getTesto().contains("RITIRATA"),
                "Il testo della notifica informa del ritiro della proposta");

        // 4. L'insieme degli iscritti resta identico a prima del ritiro e non può più variare
        asserisci(p.getUsernameAderenti().equals(List.of("luca", "sara")), "L'insieme di iscritti è rimasto identico a prima del ritiro");
        asserisci(p.iscriviFruitore("marco", dataRitiro) != null, "Non è più possibile iscriversi a una proposta RITIRATA");
        asserisci(p.disdiciFruitore("luca", dataRitiro) != null, "Non è più possibile disdire da una proposta RITIRATA");
        asserisci(p.getUsernameAderenti().equals(List.of("luca", "sara")), "L'insieme di iscritti non può più variare");
    }

    // -------------------------------------------------------------------------
    // 8. [V4] RITIRO PROPOSTA CONFERMATA FINO AL GIORNO PRECEDENTE 'DATA'
    // -------------------------------------------------------------------------
    private static void testRitiroPropostaConfermata() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Fruitore f1 = controller.registraFruitore("luca", "pwd");
        Proposta p = creaPropostaApertaBase(ds, gite, autore, 1, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));

        // Scadenza termine (oggi + 6): la proposta passa a CONFERMATA (e invia 1^ notifica di conferma)
        LocalDate giornoConferma = oggi.plusDays(6);
        controller.aggiornaStatiEScadenze(giornoConferma);
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Proposta CONFERMATA");
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 1, "1^ notifica (conferma)");

        // Il giorno stesso (oggi + 6) è esattamente il giorno precedente a 'Data' (oggi + 7): fino alle 23:59 è ritirabile!
        LocalDate vigiliaDataInizio = oggi.plusDays(6);
        asserisci(p.isRitirabile(vigiliaDataInizio), "La proposta CONFERMATA è ritirabile fino al giorno precedente Data");

        boolean ritirata = controller.ritiraProposta(p, vigiliaDataInizio, "Indisponibilità guida");
        asserisci(ritirata, "Ritiro proposta CONFERMATA il giorno precedente Data riuscito");
        asserisci(p.getStato() == StatoProposta.RITIRATA, "Stato passato a RITIRATA");
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 2, "luca ha ricevuto la 2^ notifica (ritiro)");
        asserisci(f1.getSpazioPersonale().getNotifiche().get(1).getTesto().contains("RITIRATA"), "Notifica di ritiro ricevuta");
        asserisci(p.getUsernameAderenti().equals(List.of("luca")), "Elenco iscritti rimasto identico (luca)");
    }

    // -------------------------------------------------------------------------
    // 9. [V4] RIFIUTO RITIRO NEL GIORNO STESSO DI 'DATA' (O SUCCESSIVO)
    // -------------------------------------------------------------------------
    private static void testRifiutoRitiroGiornoDataOStatoNonValido() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        controller.registraFruitore("luca", "pwd");
        Proposta p = creaPropostaApertaBase(ds, gite, autore, 1, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));
        controller.aggiornaStatiEScadenze(oggi.plusDays(6));
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Proposta CONFERMATA");

        // Nel giorno stesso di 'Data' (oggi + 7) il ritiro NON è più consentito (scaduto alle 23:59 del giorno precedente)
        LocalDate giornoData = oggi.plusDays(7);
        asserisci(!p.isRitirabile(giornoData), "Non ritirabile nel giorno stesso fissato in Data");
        asserisci(!controller.ritiraProposta(p, giornoData, "Troppo tardi"), "Rifiutato il ritiro nel giorno di Data");
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Lo stato resta CONFERMATA");
    }

    // -------------------------------------------------------------------------
    // 10. [V4] SIMULAZIONE E2E MAIN V4 + KIBO
    // -------------------------------------------------------------------------
    private static void testSimulazioneEndToEndMainV4() {
        Path fileDati = Paths.get("dati_sistema.dat");
        byte[] backup = null;
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        try {
            if (Files.exists(fileDati)) {
                backup = Files.readAllBytes(fileDati);
                Files.delete(fileDati);
            }

            LocalDate oggi = LocalDate.now();
            DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String strTermine = oggi.plusDays(3).format(fmtData);
            String strDataInizio = oggi.plusDays(6).format(fmtData);
            String strDataFine = oggi.plusDays(6).format(fmtData);

            // Flusso simulato nel Main V4:
            // 1. Login config/config -> cambio credenziali in admin_v4 / pass_v4
            // 2. Wizard 8 campi base al primo avvio
            // 3. Menu Configuratore:
            //    - Crea categoria "Sport"
            //    - Crea proposta "Torneo Tennis" (2 partecipanti) e la pubblica subito in bacheca
            //    - Esci da Menu Configuratore (0)
            // 4. Menu Principale:
            //    - 2 (Registra nuovo Fruitore "luca_v4" / "pwd_luca") -> entra in MenuFruitore:
            //      - 2 (Aderisci a proposta #1) -> "s"
            //      - 3 (Disdici iscrizione a proposta #1) -> "s"
            //      - 2 (Aderisci nuovamente a proposta #1) -> "s"
            //      - 0 (Esci da MenuFruitore)
            // 5. Menu Principale:
            //    - 1 (Login Configuratore admin_v4 / pass_v4):
            //      - 7 (Ritira Proposta Aperta o Confermata) -> 1 ("Torneo Tennis") -> "Pioggia" -> "s"
            //      - 0 (Esci da Menu Configuratore)
            // 6. Menu Principale:
            //    - 0 (Salva ed Esci)
            String simulatedInput = String.join("\n",
                    "1",
                    "config", "config",
                    "admin_v4", "pass_v4",
                    // 8 campi base primo avvio:
                    "Titolo Base",
                    "10",
                    strTermine,
                    "Centro Sportivo",
                    strDataInizio,
                    "18:00",
                    "5.00",
                    strDataFine,
                    // Menu Configuratore: crea categoria Sport
                    "1", "3", "2", "Sport", "Tennis", "n", "0", "0",
                    // Menu Configuratore: crea proposta "Torneo Tennis" e pubblica subito
                    "2", "1",
                    "Torneo Tennis",
                    "2",
                    strTermine,
                    "Campo Centrale",
                    strDataInizio,
                    "16:00",
                    "15.00",
                    strDataFine,
                    "s",
                    // Esci da Menu Configuratore
                    "0",
                    // Menu Principale: 2 (Registra nuovo Fruitore luca_v4)
                    "2",
                    "luca_v4", "pwd_luca",
                    // MenuFruitore di luca_v4:
                    "2", "1", "s",  // Aderisce a Torneo Tennis
                    "3", "1", "s",  // Disdice iscrizione da Torneo Tennis
                    "2", "1", "s",  // Si re-iscrive a Torneo Tennis
                    "0",            // Esci da MenuFruitore
                    // Menu Principale: 1 (Login admin_v4)
                    "1",
                    "admin_v4", "pass_v4",
                    // Menu Configuratore: 7 (Ritira proposta #1)
                    "7", "1", "Campo impraticabile", "s",
                    "0",
                    // Menu Principale: 0 (Salva ed Esci)
                    "0"
            ) + "\n";

            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

            Main.main(new String[0]);

            String output = baos.toString(StandardCharsets.UTF_8);
            asserisci(output.contains("CONFIGURAZIONE INIZIALE DEI CAMPI BASE"), "Eseguito wizard 8 campi base");
            asserisci(output.contains("La tua iscrizione all'iniziativa 'Torneo Tennis' è stata cancellata con successo!"),
                    "Disdetta fruitore eseguita con successo");
            asserisci(output.contains("Proposta 'Torneo Tennis' ritirata con successo!"),
                    "Ritiro proposta eseguito con successo dal configuratore");

            // Verifica stato persistente su disco
            DatiSistema salvato = GestorePersistenza.caricaDati();
            asserisci(salvato.getBacheca().isVuota(), "Bacheca vuota dopo il ritiro della proposta aperta");
            asserisci(salvato.getArchivioProposte().getNumeroProposte() == 1, "1 proposta nell'archivio storico");
            Proposta pArchiviata = salvato.getArchivioProposte().getProposte().get(0);
            asserisci(pArchiviata.getStato() == StatoProposta.RITIRATA, "Proposta salvata nello stato RITIRATA");
            asserisci(pArchiviata.getUsernameAderenti().equals(List.of("luca_v4")), "luca_v4 conservato nell'insieme iscritti della proposta ritirata");

            Fruitore luca = salvato.getGestoreUtenti().getFruitore("luca_v4");
            asserisci(luca.getSpazioPersonale().getNumeroNotifiche() == 1, "Notifica di ritiro recapitata nello spazio personale di luca_v4");
            asserisci(luca.getSpazioPersonale().getNotifiche().get(0).getTesto().contains("RITIRATA"),
                    "Messaggio notifica contiene RITIRATA");

        } catch (Exception e) {
            throw new RuntimeException("Errore nella simulazione E2E V4", e);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            try {
                if (backup != null) {
                    Files.write(fileDati, backup);
                } else {
                    Files.deleteIfExists(fileDati);
                }
            } catch (IOException ignored) {}
        }
    }
}
