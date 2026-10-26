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
 * Suite di collaudo intensivo per la Versione 2:
 * verifica delle fondamenta V1 (8 campi base compilati al primo avvio, Kibo lib, MVC Controller, DBC),
 * creazione proposte, validazione automatica di tutti i vincoli di dominio,
 * transizioni di stato (INCOMPLETA / VALIDA -> APERTA), Nota 8 su termine iscrizione in pubblicazione,
 * bacheca per categorie e persistenza.
 */
public class TestSuiteV2 {

    private static int testEseguiti = 0;
    private static int testSuperati = 0;
    private static int testFalliti = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   AVVIO SUITE DI COLLAUDO INTENSIVO - VERSIONE 2 (ZANELLA)   ");
        System.out.println("===============================================================");

        eseguiTest("1. Creazione proposta valida con tutti i vincoli rispettati", TestSuiteV2::testCreazionePropostaValida);
        eseguiTest("2. Rifiuto proposta e stato INCOMPLETA se mancano campi obbligatori", TestSuiteV2::testRifiutoMancanzaCampiObbligatori);
        eseguiTest("3. Vincolo Termine Iscrizione successivo alla data odierna", TestSuiteV2::testVincoloTermineIscrizioneDataOdierna);
        eseguiTest("4. Vincolo Data inizio almeno 2 giorni dopo termine iscrizione", TestSuiteV2::testVincoloDataInizioDueGiorniDopo);
        eseguiTest("5. Vincolo Data conclusiva coincidente o successiva alla data inizio", TestSuiteV2::testVincoloDataConclusivaCoincidenteOSuccessiva);
        eseguiTest("6. Vincoli su Quota (>= 0, anche nulla) e Partecipanti (> 0)", TestSuiteV2::testVincoloQuotaEPartecipanti);
        eseguiTest("7. Transizione di stato (VALIDA -> APERTA) e data pubblicazione", TestSuiteV2::testTransizioneDiStatoInBacheca);
        eseguiTest("8. Bacheca: raggruppamento delle proposte per categoria", TestSuiteV2::testRaggruppamentoBachecaPerCategoria);
        eseguiTest("9. Regola di sessione: le proposte valide non pubblicate non sono salvate", TestSuiteV2::testRegolaDiSessioneNonSalvataggio);
        eseguiTest("10. Persistenza fedele della Bacheca e delle proposte aperte", TestSuiteV2::testPersistenzaBacheca);
        eseguiTest("11. Isolamento proposte aperte rispetto a modifiche successive di campi", TestSuiteV2::testIsolamentoModificheSuccessive);
        eseguiTest("12. Nota 8 PDF: rivalutazione termine iscrizione all'atto della pubblicazione", TestSuiteV2::testNota8TermineIscrizioneInPubblicazione);
        eseguiTest("13. Fondamenta V1: 8 RuoloCampoBase e collisione campi comuni/specifici", TestSuiteV2::testFondamentaV1RuoliECollisioni);
        eseguiTest("14. Architettura MVC (ConfigurazioneController) e Design by Contract", TestSuiteV2::testControllerMVCEInvariantiDBC);
        eseguiTest("15. Simulazione E2E Main + Kibo (8 campi base con check formato + Proposta + Bacheca)", TestSuiteV2::testSimulazioneEndToEndMainV2);

        System.out.println("\n===============================================================");
        System.out.printf("Riepilogo V2: %d eseguiti | %d SUPERATI | %d FALLITI\n", testEseguiti, testSuperati, testFalliti);
        System.out.println("===============================================================");

        if (testFalliti > 0) {
            System.exit(1);
        }
    }

    private static void eseguiTest(String nome, Runnable test) {
        testEseguiti++;
        System.out.printf("\n[TEST V2 - %02d] %s ... ", testEseguiti, nome);
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

    private static Proposta creaPropostaValidaBase(DatiSistema ds, Categoria cat, Configuratore autore, LocalDate oggi) {
        if (!ds.getConfigurazioneSistema().isCampiBaseImpostati()) {
            ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        }
        Proposta p = new Proposta(cat, autore);
        List<Campo> campi = ds.getTuttiICampiCategoria(cat);

        for (Campo c : campi) {
            switch (c.getNome()) {
                case "Titolo":
                    p.assegnaValore(c, "Torneo Primavera");
                    break;
                case "Numero di partecipanti":
                    p.assegnaValore(c, 20);
                    break;
                case "Termine ultimo di iscrizione":
                    p.assegnaValore(c, oggi.plusDays(5));
                    break;
                case "Luogo":
                    p.assegnaValore(c, "Palazzetto dello Sport");
                    break;
                case "Data":
                    p.assegnaValore(c, oggi.plusDays(7)); // +2 giorni rispetto a termine
                    break;
                case "Ora":
                    p.assegnaValore(c, LocalTime.of(10, 0));
                    break;
                case "Quota individuale":
                    p.assegnaValore(c, 15.50);
                    break;
                case "Data conclusiva":
                    p.assegnaValore(c, oggi.plusDays(7)); // Stesso giorno
                    break;
                default:
                    if (c.isObbligatorio()) {
                        if (c.getTipo() == TipoCampo.BOOLEANO) p.assegnaValore(c, true);
                        else p.assegnaValore(c, "ValoreObbligatorio");
                    }
                    break;
            }
        }
        p.validaVincoli(oggi, campi);
        return p;
    }

    // -------------------------------------------------------------------------
    // 1. CREAZIONE PROPOSTA VALIDA
    // -------------------------------------------------------------------------
    private static void testCreazionePropostaValida() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria sport = new Categoria("Sport", "Eventi sportivi");
        sport.aggiungiCampoSpecifico(new Campo("Certificato medico", "Idoneità", true, TipoCampo.BOOLEANO));
        ds.aggiungiCategoria(sport);

        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaValidaBase(ds, sport, autore, oggi);
        List<String> errori = p.validaVincoli(oggi, ds.getTuttiICampiCategoria(sport));

        asserisci(errori.isEmpty(), "Una proposta conforme non deve generare errori, trovati: " + errori);
        asserisci(p.getStato() == StatoProposta.VALIDA, "Lo stato dopo validazione superata deve essere VALIDA");
        asserisci(p.getDataPubblicazione() == null, "La data di pubblicazione iniziale deve essere null");
        asserisci(p.getTitolo().equals("Torneo Primavera"), "Titolo corretto");
        asserisci(p.checkInvariant(), "Invariante di classe Proposta rispettato");
    }

    // -------------------------------------------------------------------------
    // 2. MANCANZA CAMPI OBBLIGATORI E STATO INCOMPLETA
    // -------------------------------------------------------------------------
    private static void testRifiutoMancanzaCampiObbligatori() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria sport = new Categoria("Sport", "Sport");
        sport.aggiungiCampoSpecifico(new Campo("Certificato medico", "Idoneità", true, TipoCampo.BOOLEANO));
        ds.aggiungiCategoria(sport);

        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaValidaBase(ds, sport, autore, oggi);

        // Omettiamo un campo obbligatorio specifico
        Campo cert = sport.getCampoSpecifico("Certificato medico");
        p.assegnaValore(cert, null);

        List<String> errori = p.validaVincoli(oggi, ds.getTuttiICampiCategoria(sport));
        asserisci(!errori.isEmpty(), "Deve fallire se manca un campo obbligatorio");
        asserisci(p.getStato() == StatoProposta.INCOMPLETA, "Una proposta con errori deve passare allo stato INCOMPLETA");
        asserisci(!p.pubblicaInBacheca(oggi), "Una proposta INCOMPLETA non può essere pubblicata in bacheca");
        asserisci(errori.stream().anyMatch(e -> e.contains("Certificato medico")),
                "L'errore deve menzionare il campo Certificato medico");
    }

    // -------------------------------------------------------------------------
    // 3. VINCOLO TERMINE ISCRIZIONE > DATA ODIERNA
    // -------------------------------------------------------------------------
    private static void testVincoloTermineIscrizioneDataOdierna() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Gite", "Gite");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Campo cTermine = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Termine ultimo di iscrizione")).findFirst().get();

        // Caso A: Termine iscrizione coincide con oggi -> Rifiutato
        Proposta pOggi = creaPropostaValidaBase(ds, cat, autore, oggi);
        pOggi.assegnaValore(cTermine, oggi);
        List<String> errOggi = pOggi.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(!errOggi.isEmpty() && errOggi.stream().anyMatch(e -> e.contains("Termine ultimo di iscrizione")),
                "Termine iscrizione coincidente con oggi deve essere rifiutato");
        asserisci(pOggi.getStato() == StatoProposta.INCOMPLETA, "Stato deve essere INCOMPLETA");

        // Caso B: Termine iscrizione nel passato -> Rifiutato
        Proposta pPassato = creaPropostaValidaBase(ds, cat, autore, oggi);
        pPassato.assegnaValore(cTermine, oggi.minusDays(1));
        List<String> errPassato = pPassato.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(!errPassato.isEmpty(), "Termine iscrizione passato deve essere rifiutato");

        // Caso C: Termine iscrizione futuro (domani) -> Valido
        Proposta pDomani = creaPropostaValidaBase(ds, cat, autore, oggi);
        pDomani.assegnaValore(cTermine, oggi.plusDays(1));
        Campo cData = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Data")).findFirst().get();
        pDomani.assegnaValore(cData, oggi.plusDays(3)); // 2 giorni dopo
        Campo cFine = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Data conclusiva")).findFirst().get();
        pDomani.assegnaValore(cFine, oggi.plusDays(3));
        List<String> errDomani = pDomani.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(errDomani.isEmpty(), "Termine iscrizione successivo a oggi deve essere accettato");
        asserisci(pDomani.getStato() == StatoProposta.VALIDA, "Stato deve essere VALIDA");
    }

    // -------------------------------------------------------------------------
    // 4. VINCOLO DATA INIZIO >= TERMINE ISCRIZIONE + 2 GIORNI
    // -------------------------------------------------------------------------
    private static void testVincoloDataInizioDueGiorniDopo() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Cinema", "Cinema");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);
        LocalDate termine = oggi.plusDays(5); // 11 ottobre

        Campo cData = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Data")).findFirst().get();
        Campo cFine = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Data conclusiva")).findFirst().get();

        // Caso A: Solo 1 giorno dopo (12 ottobre) -> Rifiutato
        Proposta p1 = creaPropostaValidaBase(ds, cat, autore, oggi);
        p1.assegnaValore(cData, termine.plusDays(1));
        p1.assegnaValore(cFine, termine.plusDays(1));
        List<String> err1 = p1.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(!err1.isEmpty() && err1.stream().anyMatch(e -> e.contains("almeno 2 giorni")),
                "Data inizio a soli +1 giorno dal termine iscrizione deve essere rifiutata");

        // Caso B: Esattamente 2 giorni dopo (13 ottobre) -> Accettato
        Proposta p2 = creaPropostaValidaBase(ds, cat, autore, oggi);
        p2.assegnaValore(cData, termine.plusDays(2));
        p2.assegnaValore(cFine, termine.plusDays(2));
        List<String> err2 = p2.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(err2.isEmpty(), "Data inizio esattamente a +2 giorni deve essere accettata");

        // Caso C: 10 giorni dopo -> Accettato
        Proposta p3 = creaPropostaValidaBase(ds, cat, autore, oggi);
        p3.assegnaValore(cData, termine.plusDays(10));
        p3.assegnaValore(cFine, termine.plusDays(10));
        List<String> err3 = p3.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(err3.isEmpty(), "Data inizio a +10 giorni deve essere accettata");
    }

    // -------------------------------------------------------------------------
    // 5. VINCOLO DATA CONCLUSIVA >= DATA INIZIO
    // -------------------------------------------------------------------------
    private static void testVincoloDataConclusivaCoincidenteOSuccessiva() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Musica", "Concerti");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Campo cData = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Data")).findFirst().get();
        Campo cFine = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Data conclusiva")).findFirst().get();

        LocalDate dataInizio = oggi.plusDays(7);

        // Caso A: Fine precedente a inizio -> Rifiutato
        Proposta pErr = creaPropostaValidaBase(ds, cat, autore, oggi);
        pErr.assegnaValore(cData, dataInizio);
        pErr.assegnaValore(cFine, dataInizio.minusDays(1));
        List<String> err = pErr.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        asserisci(!err.isEmpty() && err.stream().anyMatch(e -> e.contains("Data conclusiva")),
                "Data conclusiva precedente alla data di inizio deve essere rifiutata");

        // Caso B: Fine coincidente (evento di 1 giorno) -> Accettato
        Proposta pCoincidente = creaPropostaValidaBase(ds, cat, autore, oggi);
        pCoincidente.assegnaValore(cData, dataInizio);
        pCoincidente.assegnaValore(cFine, dataInizio);
        asserisci(pCoincidente.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat)).isEmpty(),
                "Data conclusiva coincidente con data inizio deve essere accettata");

        // Caso C: Fine successiva (evento di più giorni consecutivi) -> Accettato
        Proposta pPiuGiorni = creaPropostaValidaBase(ds, cat, autore, oggi);
        pPiuGiorni.assegnaValore(cData, dataInizio);
        pPiuGiorni.assegnaValore(cFine, dataInizio.plusDays(3));
        asserisci(pPiuGiorni.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat)).isEmpty(),
                "Data conclusiva successiva per evento di più giorni consecutivi deve essere accettata");
    }

    // -------------------------------------------------------------------------
    // 6. VINCOLI QUOTA (>= 0) E PARTECIPANTI (> 0)
    // -------------------------------------------------------------------------
    private static void testVincoloQuotaEPartecipanti() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Giochi", "Giochi");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Campo cQuota = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Quota individuale")).findFirst().get();
        Campo cPart = ds.getConfigurazioneSistema().getCampiBase().stream()
                .filter(c -> c.getNome().equals("Numero di partecipanti")).findFirst().get();

        // Quota nulla (0.00) ammessa da specifica ("si noti che la spesa può anche essere nulla")
        Proposta pGratis = creaPropostaValidaBase(ds, cat, autore, oggi);
        pGratis.assegnaValore(cQuota, 0.0);
        asserisci(pGratis.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat)).isEmpty(),
                "Quota pari a 0.0 deve essere ammessa");

        // Quota negativa rifiutata
        Proposta pNeg = creaPropostaValidaBase(ds, cat, autore, oggi);
        pNeg.assegnaValore(cQuota, -5.0);
        asserisci(!pNeg.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat)).isEmpty(),
                "Quota negativa deve essere rifiutata");

        // Partecipanti 0 o negativi rifiutati
        Proposta pZeroPart = creaPropostaValidaBase(ds, cat, autore, oggi);
        pZeroPart.assegnaValore(cPart, 0);
        asserisci(!pZeroPart.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat)).isEmpty(),
                "Partecipanti = 0 deve essere rifiutato");
    }

    // -------------------------------------------------------------------------
    // 7. TRANSIZIONE DI STATO E BACHECA
    // -------------------------------------------------------------------------
    private static void testTransizioneDiStatoInBacheca() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Teatro", "Spettacoli");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaValidaBase(ds, cat, autore, oggi);
        asserisci(p.getStato() == StatoProposta.VALIDA, "Stato iniziale VALIDA");

        Bacheca bacheca = ds.getBacheca();
        // Tentativo di inserire in bacheca proposta non ancora aperta
        boolean aggiunta = bacheca.aggiungiPropostaAperta(p);
        asserisci(!aggiunta, "La bacheca deve rifiutare proposte che non sono nello stato APERTA");

        // Pubblicazione in bacheca
        boolean pubblicata = p.pubblicaInBacheca(oggi);
        asserisci(pubblicata, "Transizione di stato deve riuscire");
        asserisci(p.getStato() == StatoProposta.APERTA, "Nuovo stato deve essere APERTA");
        asserisci(p.getDataPubblicazione().equals(oggi), "Data pubblicazione impostata");

        aggiunta = bacheca.aggiungiPropostaAperta(p);
        asserisci(aggiunta, "Aggiunta della proposta APERTA in bacheca deve riuscire");
        asserisci(bacheca.getNumeroProposteAperte() == 1, "La bacheca contiene 1 proposta");
        asserisci(bacheca.checkInvariant(), "Invariante Bacheca rispettato");
    }

    // -------------------------------------------------------------------------
    // 8. RAGGRUPPAMENTO BACHECA PER CATEGORIA
    // -------------------------------------------------------------------------
    private static void testRaggruppamentoBachecaPerCategoria() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria sport = new Categoria("Sport", "Sport");
        Categoria arte = new Categoria("Arte", "Arte");
        ds.aggiungiCategoria(sport);
        ds.aggiungiCategoria(arte);

        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p1 = creaPropostaValidaBase(ds, sport, autore, oggi);
        p1.pubblicaInBacheca(oggi);
        ds.getBacheca().aggiungiPropostaAperta(p1);

        Proposta p2 = creaPropostaValidaBase(ds, sport, autore, oggi);
        p2.pubblicaInBacheca(oggi);
        ds.getBacheca().aggiungiPropostaAperta(p2);

        Proposta p3 = creaPropostaValidaBase(ds, arte, autore, oggi);
        p3.pubblicaInBacheca(oggi);
        ds.getBacheca().aggiungiPropostaAperta(p3);

        asserisci(ds.getBacheca().getPropostePerCategoria(sport).size() == 2, "2 proposte per Sport");
        asserisci(ds.getBacheca().getPropostePerCategoria(arte).size() == 1, "1 proposta per Arte");
        asserisci(ds.getBacheca().getNumeroProposteAperte() == 3, "3 proposte totali in bacheca");
    }

    // -------------------------------------------------------------------------
    // 9. REGOLA DI SESSIONE: PROPOSTE VALIDE NON SALVATE
    // -------------------------------------------------------------------------
    private static void testRegolaDiSessioneNonSalvataggio() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Sport", "Sport");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta pValidaInSessione = creaPropostaValidaBase(ds, cat, autore, oggi);
        asserisci(pValidaInSessione.getStato() == StatoProposta.VALIDA, "In sessione è VALIDA");

        // Poiché non pubblicata, non entra nella bacheca di DatiSistema
        asserisci(ds.getBacheca().isVuota(), "La bacheca di sistema deve rimanere vuota");
    }

    // -------------------------------------------------------------------------
    // 10. PERSISTENZA BACHECA SU FILE
    // -------------------------------------------------------------------------
    private static void testPersistenzaBacheca() {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test_bacheca_", ".dat");
            DatiSistema ds = creaDatiSistemaConCampiBase();
            Categoria cat = new Categoria("Sport", "Sport");
            ds.aggiungiCategoria(cat);
            Configuratore autore = new Configuratore("mario", "pwd", false);
            LocalDate oggi = LocalDate.of(2026, 10, 6);

            Proposta p = creaPropostaValidaBase(ds, cat, autore, oggi);
            p.pubblicaInBacheca(oggi);
            ds.getBacheca().aggiungiPropostaAperta(p);

            // Salvataggio
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile.toFile()))) {
                oos.writeObject(ds);
            }

            // Ricaricamento
            DatiSistema caricato;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tempFile.toFile()))) {
                caricato = (DatiSistema) ois.readObject();
            }

            Bacheca bCaricata = caricato.getBacheca();
            asserisci(bCaricata.getNumeroProposteAperte() == 1, "1 proposta aperta ricaricata dalla bacheca");
            Proposta pCaricata = bCaricata.getProposteAperte().get(0);
            asserisci(pCaricata.getTitolo().equals("Torneo Primavera"), "Titolo preservato");
            asserisci(pCaricata.getStato() == StatoProposta.APERTA, "Stato APERTA preservato");
            asserisci(pCaricata.getDataPubblicazione().equals(oggi), "Data pubblicazione preservata");
            asserisci(pCaricata.getQuotaIndividuale() == 15.50, "Quota preservata");
            asserisci(pCaricata.getNumeroPartecipanti() == 20, "Partecipanti preservati");

        } catch (Exception e) {
            throw new RuntimeException("Errore nel test di persistenza bacheca", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // 11. ISOLAMENTO PROPOSTE APERTE DA MODIFICHE SUCCESSIVE
    // -------------------------------------------------------------------------
    private static void testIsolamentoModificheSuccessive() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Sport", "Sport");
        Campo campoSpec = new Campo("Attrezzatura", "Note attrezzatura", false, TipoCampo.STRINGA);
        cat.aggiungiCampoSpecifico(campoSpec);
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaValidaBase(ds, cat, autore, oggi);
        p.assegnaValore(campoSpec, "Racchetta");
        p.validaVincoli(oggi, ds.getTuttiICampiCategoria(cat));
        p.pubblicaInBacheca(oggi);
        ds.getBacheca().aggiungiPropostaAperta(p);

        // Successivamente viene modificata l'obbligatorietà del campo specifico e aggiunto un nuovo campo comune
        campoSpec.setObbligatorio(true);
        ds.getConfigurazioneSistema().aggiungiCampoComune(new Campo("NuovoCampoFuturo", "Nota", true, TipoCampo.STRINGA));

        // La proposta già creata in bacheca mantiene la copia difensiva del campo con obbligatorietà originale (false)
        ValoreCampo vc = p.getValori().get("attrezzatura");
        asserisci(vc != null && !vc.getCampo().isObbligatorio(),
                "La copia difensiva di Campo in ValoreCampo deve isolare la proposta da modifiche successive");
        asserisci(ds.getBacheca().getNumeroProposteAperte() == 1, "La proposta in bacheca rimane intatta");
    }

    // -------------------------------------------------------------------------
    // 12. NOTA 8 PDF: RIVALUTAZIONE TERMINE ISCRIZIONE IN PUBBLICAZIONE
    // -------------------------------------------------------------------------
    private static void testNota8TermineIscrizioneInPubblicazione() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria cat = new Categoria("Sport", "Sport");
        ds.aggiungiCategoria(cat);
        Configuratore autore = new Configuratore("mario", "pwd", false);
        LocalDate dataCreazione = LocalDate.of(2026, 10, 6);

        // Termine iscrizione: 11 ottobre (+5 giorni da creazione)
        Proposta p = creaPropostaValidaBase(ds, cat, autore, dataCreazione);
        asserisci(p.getStato() == StatoProposta.VALIDA, "Valida alla data di creazione");

        // Se tentiamo di pubblicarla il giorno stesso della scadenza (11 ottobre) o dopo, deve essere rifiutata (Nota 8)
        LocalDate dataPubblicazioneTardiva = dataCreazione.plusDays(5);
        asserisci(!p.pubblicaInBacheca(dataPubblicazioneTardiva),
                "Nota 8: pubblicazione deve fallire se dataPubblicazione >= termine ultimo di iscrizione");
        asserisci(p.getStato() == StatoProposta.INCOMPLETA, "Nota 8: la proposta cessa di essere valida (INCOMPLETA) e non passa ad APERTA");
    }

    // -------------------------------------------------------------------------
    // 13. FONDAMENTA V1: 8 RUOLOCAMPOBASE E COLLISIONI CAMPI COMUNI/SPECIFICI
    // -------------------------------------------------------------------------
    private static void testFondamentaV1RuoliECollisioni() {
        DatiSistema ds = new DatiSistema();
        asserisci(!ds.getConfigurazioneSistema().isCampiBaseImpostati(),
                "Al primo avvio i campi base non devono essere già pre-fissati prima dell'input del configuratore");

        ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        asserisci(ds.getConfigurazioneSistema().isCampiBaseImpostati(), "Dopo inizializzazione i campi base sono fissati");
        asserisci(ds.getConfigurazioneSistema().getCampiBase().size() == RuoloCampoBase.values().length,
                "Devono esserci esattamente 8 campi base associati a RuoloCampoBase");

        Categoria cat = new Categoria("Sport", "Sport");
        cat.aggiungiCampoSpecifico(new Campo("Certificato medico", "Obbligo certificato", true, TipoCampo.BOOLEANO));
        ds.aggiungiCategoria(cat);

        // Collisione: tentativo di aggiungere un campo comune con lo stesso nome di un campo specifico esistente
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        boolean aggiunto = controller.aggiungiCampoComune("Certificato medico", "Duplicato", false, TipoCampo.STRINGA);
        asserisci(!aggiunto, "Non si può aggiungere un campo comune che collide con un campo specifico di una categoria");
    }

    // -------------------------------------------------------------------------
    // 14. ARCHITETTURA MVC (CONTROLLER) E DESIGN BY CONTRACT
    // -------------------------------------------------------------------------
    private static void testControllerMVCEInvariantiDBC() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        asserisci(ds.checkInvariant(), "Invariante DatiSistema rispettato");

        controller.creaCategoria("Arte", "Mostre artistiche");
        Categoria arte = controller.getCategoria("Arte");
        Configuratore autore = new Configuratore("anna", "pwd", false);
        LocalDate oggi = LocalDate.now();

        Proposta p = creaPropostaValidaBase(ds, arte, autore, oggi);
        List<String> errori = controller.validaERegistraPropostaInSessione(p, oggi);
        asserisci(errori.isEmpty(), "Validazione e registrazione proposta valida in sessione tramite controller");
        asserisci(controller.getProposteValideInSessione().size() == 1, "1 proposta valida in sessione");

        asserisci(controller.pubblicaPropostaInBacheca(p, oggi), "Pubblicazione tramite controller riuscita");
        asserisci(controller.getProposteValideInSessione().isEmpty(), "Rimossa dalle proposte di sessione dopo pubblicazione");
        asserisci(controller.getBacheca().getNumeroProposteAperte() == 1, "1 proposta aperta in bacheca tramite controller");
    }

    // -------------------------------------------------------------------------
    // 15. SIMULAZIONE END-TO-END MAIN + KIBO (8 CAMPI BASE + PROPOSTA + BACHECA)
    // -------------------------------------------------------------------------
    private static void testSimulazioneEndToEndMainV2() {
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
            String strTermine = oggi.plusDays(5).format(fmtData);
            String strDataInizio = oggi.plusDays(8).format(fmtData);
            String strDataFine = oggi.plusDays(8).format(fmtData);

            // Sequenza di input simulata:
            // 1. Login predefinito: config / config
            // 2. Cambio credenziali personali: admin_v2 / secret_v2
            // 3. Compilazione degli 8 campi base (con test di formato errato su NUMERO_PARTECIPANTI: "abc" -> poi "25")
            // 4. Menu principale: 1 (Gestione Categorie e Campi) -> 3 (Gestisci Categorie) -> 2 (Crea nuova categoria: "Sport", "Eventi sportivi", "n") -> 0 (Indietro) -> 0 (Indietro)
            // 5. Menu principale: 2 (Crea nuova proposta) -> 1 (Categoria Sport) -> compilazione 8 campi base -> "s" (Pubblica subito in bacheca)
            // 6. Menu principale: 4 (Visualizza Bacheca) -> "n" (non visualizzare scheda dettaglio)
            // 7. Menu principale: 0 (Esci)
            String simulatedInput = String.join("\n",
                    "config", "config",
                    "admin_v2", "secret_v2",
                    // 8 campi base al primo avvio:
                    "Titolo Evento Iniziale",
                    "abc", "25", // Errore formato su intero + retry valido
                    strTermine,
                    "Stadio Comunale",
                    strDataInizio,
                    "09:30",
                    "10.00",
                    strDataFine,
                    // Menu principale -> 1 (Struttura) -> 3 (Categorie) -> 2 (Crea nuova categoria)
                    "1",
                    "3",
                    "2",
                    "Sport",
                    "Eventi sportivi",
                    "n",
                    "0",
                    "0",
                    // Menu principale -> 2 (Crea proposta) -> 1 (Sport)
                    "2",
                    "1",
                    "Maratona Cittadina",
                    "50",
                    strTermine,
                    "Piazza Loggia",
                    strDataInizio,
                    "08:30",
                    "12.50",
                    strDataFine,
                    "s", // Pubblica subito in bacheca
                    // Menu principale -> 4 (Visualizza Bacheca) -> n (no dettaglio extra)
                    "4",
                    "n",
                    // Menu principale -> 0 (Esci)
                    "0"
            ) + "\n";

            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

            Main.main(new String[0]);

            String output = baos.toString(StandardCharsets.UTF_8);
            asserisci(output.contains("CONFIGURAZIONE INIZIALE DEI CAMPI BASE"), "Avviato wizard 8 campi base");
            asserisci(output.contains("Attenzione: il dato inserito non è nel formato corretto"), "Rilevato formato errato in compilazione campi base");
            asserisci(output.contains("VALIDA!"), "Proposta validata con successo");
            asserisci(output.contains("Proposta pubblicata con successo in bacheca nello stato APERTA!"), "Proposta pubblicata in bacheca");
            asserisci(output.contains("Maratona Cittadina"), "Bacheca mostra la proposta pubblicata");

            DatiSistema salvato = GestorePersistenza.caricaDati();
            asserisci(salvato.getConfigurazioneSistema().isCampiBaseImpostati(), "Campi base salvati su disco");
            asserisci(salvato.getBacheca().getNumeroProposteAperte() == 1, "1 proposta aperta salvata su disco");
            asserisci(salvato.getBacheca().getProposteAperte().get(0).getTitolo().equals("Maratona Cittadina"),
                    "Titolo proposta aperta salvato correttamente");

        } catch (Exception e) {
            throw new RuntimeException("Errore nella simulazione E2E V2", e);
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
