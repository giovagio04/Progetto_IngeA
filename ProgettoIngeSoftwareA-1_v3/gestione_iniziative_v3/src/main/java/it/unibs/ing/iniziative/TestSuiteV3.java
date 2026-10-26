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
 * Suite di collaudo intensivo per la Versione 3:
 * verifica delle fondamenta V1 + V2 e di tutti i requisiti specifici della Versione 3:
 * - Registrazione e login Fruitori con univocità globale dello username;
 * - Adesione a proposte aperte con controllo termine, unicità iscrizione e capienza massima;
 * - Passaggi di stato automatici allo scadere della mezzanotte (APERTA -> CONFERMATA -> CONCLUSA e APERTA -> ANNULLATA);
 * - Notifiche automatiche negli spazi personali e cancellazione selettiva;
 * - Archivio storico delle proposte con cronologia completa degli stati e lista immutabile degli aderenti;
 * - Simulazione End-to-End interattiva con Kibo (Configuratore + Fruitori).
 */
public class TestSuiteV3 {

    private static int testEseguiti = 0;
    private static int testSuperati = 0;
    private static int testFalliti = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   AVVIO SUITE DI COLLAUDO INTENSIVO - VERSIONE 3 (ZANELLA)   ");
        System.out.println("===============================================================");

        eseguiTest("1. Registrazione Fruitore e univocità username globale (Configuratori + Fruitori)", TestSuiteV3::testRegistrazioneEUnivocitaFruitore);
        eseguiTest("2. Adesione valida di un Fruitore a una proposta APERTA entro il termine", TestSuiteV3::testAdesioneValidaFruitore);
        eseguiTest("3. Rifiuto doppia iscrizione dello stesso Fruitore alla medesima proposta", TestSuiteV3::testRifiutoDoppiaIscrizione);
        eseguiTest("4. Rifiuto iscrizione a proposta che ha già raggiunto il numero di partecipanti", TestSuiteV3::testRifiutoPropostaAlCompleto);
        eseguiTest("5. Rifiuto iscrizione oltre il termine ultimo di iscrizione", TestSuiteV3::testRifiutoIscrizioneOltreTermine);
        eseguiTest("6. Scadenza mezzanotte con capienza raggiunta: APERTA -> CONFERMATA + Notifica Promemoria", TestSuiteV3::testConfermaAutomaticaENotificaPromemoria);
        eseguiTest("7. Scadenza mezzanotte con iscritti insufficienti: APERTA -> ANNULLATA + Notifica Annullamento", TestSuiteV3::testAnnullamentoAutomaticoENotifica);
        eseguiTest("8. Passaggio automatico CONFERMATA -> CONCLUSA il giorno successivo alla Data conclusiva", TestSuiteV3::testPassaggioConfermataAConclusa);
        eseguiTest("9. Archivio Proposte: conservazione cronologia stati (con date) e aderenti immutabili", TestSuiteV3::testArchivioStoricoECronologiaStati);
        eseguiTest("10. Spazio Personale: isolamento, persistenza su file e cancellazione selettiva notifiche", TestSuiteV3::testSpazioPersonaleCancellazioneEPersistenza);
        eseguiTest("11. Fondamenta V1+V2 in V3: 8 RuoloCampoBase, Nota 8, Copia difensiva, MVC e DBC", TestSuiteV3::testFondamentaV1V2InV3);
        eseguiTest("12. Simulazione E2E Main V3 + Kibo (Configuratore + Fruitori + Scadenze + Notifiche)", TestSuiteV3::testSimulazioneEndToEndMainV3);

        System.out.println("\n===============================================================");
        System.out.printf("Riepilogo V3: %d eseguiti | %d SUPERATI | %d FALLITI\n", testEseguiti, testSuperati, testFalliti);
        System.out.println("===============================================================");

        if (testFalliti > 0) {
            System.exit(1);
        }
    }

    private static void eseguiTest(String nome, Runnable test) {
        testEseguiti++;
        System.out.printf("\n[TEST V3 - %02d] %s ... ", testEseguiti, nome);
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
    // 1. REGISTRAZIONE FRUITORE E UNIVOCITÀ USERNAME GLOBALE
    // -------------------------------------------------------------------------
    private static void testRegistrazioneEUnivocitaFruitore() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);

        Configuratore def = (Configuratore) controller.login("config", "config");
        Configuratore admin = controller.registraNuovoConfiguratore(def, "mario_cfg", "pwd1");
        asserisci(admin != null, "Configuratore mario_cfg registrato");

        // Registrazione nuovo fruitore
        Fruitore f1 = controller.registraFruitore("elena_fru", "pwd2");
        asserisci(f1 != null && f1.checkInvariant(), "Fruitore elena_fru registrato correttamente");
        asserisci(controller.login("elena_fru", "pwd2") instanceof Fruitore, "Login fruitore funzionante");

        // Tentativo di registrare un fruitore con lo username di un configuratore o di "config"
        asserisci(controller.registraFruitore("mario_cfg", "pwd3") == null,
                "Lo username di un fruitore non può coincidere con quello di un configuratore");
        asserisci(controller.registraFruitore("config", "pwd3") == null,
                "Lo username di un fruitore non può coincidere con lo username predefinito config");

        // Tentativo di registrare un fruitore con lo username di un altro fruitore (case-insensitive)
        asserisci(controller.registraFruitore("ELENA_FRU", "pwd4") == null,
                "Lo username di un fruitore non può coincidere con quello di un altro fruitore");
    }

    // -------------------------------------------------------------------------
    // 2. ADESIONE VALIDA DI UN FRUITORE A UNA PROPOSTA APERTA
    // -------------------------------------------------------------------------
    private static void testAdesioneValidaFruitore() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 3, oggi);
        ds.getGestoreUtenti().registraFruitore("luca", "pwd");

        // Iscrizione il giorno stesso della scadenza (fino alle ore 23:59 del Termine ultimo di iscrizione)
        LocalDate giornoScadenza = oggi.plusDays(5);
        String esito = p.iscriviFruitore("luca", giornoScadenza);
        asserisci(esito == null, "L'iscrizione entro le 23:59 del giorno di scadenza deve riuscire");
        asserisci(p.isIscritto("luca"), "luca risulta iscritto");
        asserisci(p.getNumeroAderenti() == 1, "1 aderente registrato");
        asserisci(p.getPostiRimanenti() == 2, "2 posti rimanenti su 3");
    }

    // -------------------------------------------------------------------------
    // 3. RIFIUTO DOPPIA ISCRIZIONE DELLO STESSO FRUITORE
    // -------------------------------------------------------------------------
    private static void testRifiutoDoppiaIscrizione() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 3, oggi);
        asserisci(p.iscriviFruitore("luca", oggi) == null, "Prima iscrizione riuscita");

        String secondoTentativo = p.iscriviFruitore("LUCA", oggi);
        asserisci(secondoTentativo != null, "La seconda iscrizione dello stesso fruitore deve essere rifiutata");
        asserisci(p.getNumeroAderenti() == 1, "Il numero di aderenti resta 1");
    }

    // -------------------------------------------------------------------------
    // 4. RIFIUTO ISCRIZIONE QUANDO LA PROPOSTA È AL COMPLETO
    // -------------------------------------------------------------------------
    private static void testRifiutoPropostaAlCompleto() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 2, oggi);
        asserisci(p.iscriviFruitore("luca", oggi) == null, "Iscrizione 1/2 riuscita");
        asserisci(p.iscriviFruitore("sara", oggi) == null, "Iscrizione 2/2 riuscita");
        asserisci(p.isAlCompleto(), "La proposta è al completo");

        String terzoTentativo = p.iscriviFruitore("marco", oggi);
        asserisci(terzoTentativo != null, "L'iscrizione oltre la capienza massima deve essere rifiutata");
        asserisci(p.getNumeroAderenti() == 2, "Gli aderenti restano 2");
    }

    // -------------------------------------------------------------------------
    // 5. RIFIUTO ISCRIZIONE OLTRE IL TERMINE ULTIMO DI ISCRIZIONE
    // -------------------------------------------------------------------------
    private static void testRifiutoIscrizioneOltreTermine() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 5, oggi);
        LocalDate dopoScadenza = oggi.plusDays(6); // Termine era oggi + 5
        String esito = p.iscriviFruitore("luca", dopoScadenza);
        asserisci(esito != null, "L'iscrizione dopo il termine ultimo deve essere rifiutata");
        asserisci(p.getNumeroAderenti() == 0, "Nessun aderente registrato");
    }

    // -------------------------------------------------------------------------
    // 6. SCADENZA MEZZANOTTE CON CAPIENZA RAGGIUNTA: APERTA -> CONFERMATA + PROMEMORIA
    // -------------------------------------------------------------------------
    private static void testConfermaAutomaticaENotificaPromemoria() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Fruitore f1 = ds.getGestoreUtenti().registraFruitore("luca", "pwd");
        Fruitore f2 = ds.getGestoreUtenti().registraFruitore("sara", "pwd");

        Proposta p = creaPropostaApertaBase(ds, gite, autore, 2, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));
        p.iscriviFruitore("sara", oggi.plusDays(2));

        // Allo scadere della mezzanotte del termine (oggi + 6 giorni)
        LocalDate giornoDopoTermine = oggi.plusDays(6);
        ds.aggiornaStatiEScadenze(giornoDopoTermine);

        asserisci(ds.getBacheca().isVuota(), "La proposta scaduta deve essere rimossa dalla bacheca");
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "La proposta al completo deve passare a CONFERMATA");
        asserisci(ds.getArchivioProposte().getProposte().contains(p), "La proposta resta nell'ArchivioProposte");

        // Verifica notifiche promemoria (data, ora, luogo, quota)
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 1, "luca ha ricevuto 1 notifica");
        asserisci(f2.getSpazioPersonale().getNumeroNotifiche() == 1, "sara ha ricevuto 1 notifica");
        String msg = f1.getSpazioPersonale().getNotifiche().get(0).getTesto();
        asserisci(msg.contains("CONFERMATA") && msg.contains("Stazione Brescia") && msg.contains("08:30") && msg.contains("20,00"),
                "Il promemoria contiene stato confermato, data/ora, luogo e importo: " + msg);
    }

    // -------------------------------------------------------------------------
    // 7. SCADENZA MEZZANOTTE CON ISCRITTI INSUFFICIENTI: APERTA -> ANNULLATA + NOTIFICA
    // -------------------------------------------------------------------------
    private static void testAnnullamentoAutomaticoENotifica() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Fruitore f1 = ds.getGestoreUtenti().registraFruitore("luca", "pwd");

        // Richiesti 3 partecipanti, ma si iscrive solo 1
        Proposta p = creaPropostaApertaBase(ds, gite, autore, 3, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));

        LocalDate giornoDopoTermine = oggi.plusDays(6);
        ds.aggiornaStatiEScadenze(giornoDopoTermine);

        asserisci(ds.getBacheca().isVuota(), "La proposta scaduta viene tolta dalla bacheca");
        asserisci(p.getStato() == StatoProposta.ANNULLATA, "La proposta con iscritti insufficienti passa ad ANNULLATA");
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 1, "L'iscritto riceve la notifica di annullamento");
        asserisci(f1.getSpazioPersonale().getNotifiche().get(0).getTesto().contains("ANNULLATA"),
                "Il testo della notifica informa dell'annullamento");
    }

    // -------------------------------------------------------------------------
    // 8. PASSAGGIO AUTOMATICO CONFERMATA -> CONCLUSA
    // -------------------------------------------------------------------------
    private static void testPassaggioConfermataAConclusa() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        ds.getGestoreUtenti().registraFruitore("luca", "pwd");
        Proposta p = creaPropostaApertaBase(ds, gite, autore, 1, oggi);
        p.iscriviFruitore("luca", oggi.plusDays(1));

        // 1. Dopo il termine iscrizione (oggi + 6): diventa CONFERMATA
        ds.aggiornaStatiEScadenze(oggi.plusDays(6));
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Stato CONFERMATA");

        // 2. Nel giorno stesso della Data conclusiva (oggi + 8): è ancora CONFERMATA
        ds.aggiornaStatiEScadenze(oggi.plusDays(8));
        asserisci(p.getStato() == StatoProposta.CONFERMATA, "Nel giorno della data conclusiva è ancora CONFERMATA");

        // 3. Il giorno successivo alla Data conclusiva (oggi + 9): passa a CONCLUSA
        ds.aggiornaStatiEScadenze(oggi.plusDays(9));
        asserisci(p.getStato() == StatoProposta.CONCLUSA, "Il giorno successivo alla data conclusiva passa a CONCLUSA");
    }

    // -------------------------------------------------------------------------
    // 9. ARCHIVIO PROPOSTE: CRONOLOGIA STATI E ADERENTI IMMUTABILI
    // -------------------------------------------------------------------------
    private static void testArchivioStoricoECronologiaStati() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        Categoria gite = new Categoria("Gite", "Escursioni");
        ds.aggiungiCategoria(gite);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate dCreazione = LocalDate.of(2026, 10, 6);

        ds.getGestoreUtenti().registraFruitore("luca", "pwd");
        Proposta p = creaPropostaApertaBase(ds, gite, autore, 1, dCreazione);
        p.iscriviFruitore("luca", dCreazione.plusDays(1));

        LocalDate dConferma = dCreazione.plusDays(6);
        ds.aggiornaStatiEScadenze(dConferma);

        LocalDate dConclusione = dCreazione.plusDays(9);
        ds.aggiornaStatiEScadenze(dConclusione);

        List<TransizioneStato> storia = p.getCronologiaStati();
        asserisci(storia.size() == 4, "Devono essere registrati tutti i 4 stati (VALIDA, APERTA, CONFERMATA, CONCLUSA), trovati: " + storia.size());
        asserisci(storia.get(0).getStato() == StatoProposta.VALIDA && storia.get(0).getDataAssunzione().equals(dCreazione), "1° stato VALIDA");
        asserisci(storia.get(1).getStato() == StatoProposta.APERTA && storia.get(1).getDataAssunzione().equals(dCreazione), "2° stato APERTA");
        asserisci(storia.get(2).getStato() == StatoProposta.CONFERMATA && storia.get(2).getDataAssunzione().equals(dConferma), "3° stato CONFERMATA");
        asserisci(storia.get(3).getStato() == StatoProposta.CONCLUSA && storia.get(3).getDataAssunzione().equals(dConclusione), "4° stato CONCLUSA");

        // Tentativo di iscrivere qualcuno dopo la conferma/conclusione deve fallire e non alterare gli aderenti
        asserisci(p.iscriviFruitore("altro", dConclusione) != null, "Non si può aderire a proposta non APERTA");
        asserisci(p.getUsernameAderenti().size() == 1 && p.getUsernameAderenti().contains("luca"),
                "Elenco aderenti rimasto immutato");
    }

    // -------------------------------------------------------------------------
    // 10. SPAZIO PERSONALE: CANCELLAZIONE SELETTIVA E PERSISTENZA
    // -------------------------------------------------------------------------
    private static void testSpazioPersonaleCancellazioneEPersistenza() {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test_v3_spazio_", ".dat");
            DatiSistema ds = creaDatiSistemaConCampiBase();
            Fruitore f = ds.getGestoreUtenti().registraFruitore("luca", "pwd");
            LocalDate oggi = LocalDate.of(2026, 10, 6);

            f.getSpazioPersonale().aggiungiNotifica("Notifica 1 da conservare", oggi);
            f.getSpazioPersonale().aggiungiNotifica("Notifica 2 da cancellare", oggi);
            f.getSpazioPersonale().aggiungiNotifica("Notifica 3 da conservare", oggi);
            asserisci(f.getSpazioPersonale().getNumeroNotifiche() == 3, "3 notifiche iniziali");

            // Cancellazione selettiva della notifica #2
            boolean rimossa = f.getSpazioPersonale().rimuoviNotifica(2);
            asserisci(rimossa, "Notifica #2 rimossa con successo");
            asserisci(f.getSpazioPersonale().getNumeroNotifiche() == 2, "Restano 2 notifiche");

            // Salvataggio su file e ricaricamento in nuova sessione
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile.toFile()))) {
                oos.writeObject(ds);
            }
            DatiSistema caricato;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tempFile.toFile()))) {
                caricato = (DatiSistema) ois.readObject();
            }

            Fruitore fCaricato = caricato.getGestoreUtenti().getFruitore("luca");
            asserisci(fCaricato != null, "Fruitore ricaricato");
            List<Notifica> notifiche = fCaricato.getSpazioPersonale().getNotifiche();
            asserisci(notifiche.size() == 2, "Lo spazio personale alla riapertura è consistente con la chiusura precedente (2 notifiche)");
            asserisci(notifiche.get(0).getId() == 1 && notifiche.get(1).getId() == 3, "Rimaste esattamente le notifiche #1 e #3");

        } catch (Exception e) {
            throw new RuntimeException("Errore nel test di persistenza Spazio Personale", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // 11. FONDAMENTA V1 + V2 IN V3: RUOLI BASE, NOTA 8, COPIA DIFENSIVA, MVC E DBC
    // -------------------------------------------------------------------------
    private static void testFondamentaV1V2InV3() {
        DatiSistema ds = new DatiSistema();
        asserisci(!ds.getConfigurazioneSistema().isCampiBaseImpostati(), "Campi base non pre-impostati prima del wizard");
        ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        asserisci(ds.getConfigurazioneSistema().getCampiBase().size() == RuoloCampoBase.values().length, "8 campi base RuoloCampoBase");

        ConfigurazioneController controller = new ConfigurazioneController(ds);
        controller.creaCategoria("Sport", "Eventi sportivi");
        controller.aggiungiCampoSpecifico("Sport", "Certificato medico", "Obbligo", false, TipoCampo.BOOLEANO);

        // Collisione campo comune con campo specifico esistente
        asserisci(!controller.aggiungiCampoComune("Certificato medico", "Dup", true, TipoCampo.STRINGA),
                "Impedita collisione tra nuovo campo comune e campo specifico esistente");

        Categoria sport = controller.getCategoria("Sport");
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 6);

        Proposta p = creaPropostaApertaBase(ds, sport, autore, 2, oggi);
        // Modifica successiva dell'obbligatorietà del campo specifico non altera la proposta già pubblicata
        controller.modificaObbligatorietaCampoSpecifico("Sport", "Certificato medico", true);
        asserisci(p.checkInvariant() && controller.checkInvariant(), "Invarianti DBC rispettati");

        // Verifica Nota 8 in V3: pubblicazione tardiva rifiutata
        Proposta pTardiva = new Proposta(sport, autore);
        for (Campo c : controller.getTuttiICampiCategoria(sport)) {
            if (c.getNome().equals("Titolo")) pTardiva.assegnaValore(c, "Evento Tardivo");
            else if (c.getNome().equals("Numero di partecipanti")) pTardiva.assegnaValore(c, 5);
            else if (c.getNome().equals("Termine ultimo di iscrizione")) pTardiva.assegnaValore(c, oggi.plusDays(2));
            else if (c.getNome().equals("Luogo")) pTardiva.assegnaValore(c, "Campo");
            else if (c.getNome().equals("Data")) pTardiva.assegnaValore(c, oggi.plusDays(5));
            else if (c.getNome().equals("Ora")) pTardiva.assegnaValore(c, LocalTime.of(15, 0));
            else if (c.getNome().equals("Quota individuale")) pTardiva.assegnaValore(c, 0.0);
            else if (c.getNome().equals("Data conclusiva")) pTardiva.assegnaValore(c, oggi.plusDays(5));
            else pTardiva.assegnaValore(c, true);
        }
        pTardiva.validaVincoli(oggi, controller.getTuttiICampiCategoria(sport));
        asserisci(!pTardiva.pubblicaInBacheca(oggi.plusDays(2)), "Nota 8: rifiutata pubblicazione quando dataPubblicazione >= termine");
        asserisci(pTardiva.getStato() == StatoProposta.INCOMPLETA, "Nota 8: la proposta cessa di essere valida");
    }

    // -------------------------------------------------------------------------
    // 12. SIMULAZIONE E2E MAIN V3 + KIBO (CONFIGURATORE + FRUITORI + SCADENZE)
    // -------------------------------------------------------------------------
    private static void testSimulazioneEndToEndMainV3() {
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

            // Flusso simulato nel Main V3:
            // 1. Scelta 1 (Login) -> config / config -> cambio in admin_v3 / pass_v3
            // 2. Wizard 8 campi base al primo avvio (con test errore formato "xyz" su partecipanti -> "10")
            // 3. Menu Configuratore:
            //    - 1 (Struttura) -> 3 (Categorie) -> 2 (Crea categoria "Sport", "Calcetto", "n") -> 0 -> 0
            //    - 2 (Crea proposta) -> 1 (Sport) -> 8 campi base (1 partecipante richiesto) -> "s" (Pubblica subito)
            //    - 0 (Esci da Menu Configuratore)
            // 4. Menu Principale V3:
            //    - 2 (Registrazione nuovo Fruitore: "luca_v3" / "pwd_luca") -> entra subito in MenuFruitore:
            //      - 1 (Visualizza Bacheca) -> "n" (no dettaglio)
            //      - 2 (Aderisci a proposta) -> 1 (Partita Calcetto) -> "s" (Conferma adesione)
            //      - 3 (Visualizza le mie iscrizioni)
            //      - 0 (Esci da MenuFruitore)
            // 5. Menu Principale V3:
            //    - 0 (Salva ed Esci)
            String simulatedInput = String.join("\n",
                    "1",
                    "config", "config",
                    "admin_v3", "pass_v3",
                    // 8 campi base primo avvio:
                    "Titolo Base",
                    "xyz", "10",
                    strTermine,
                    "Centro Sportivo",
                    strDataInizio,
                    "18:00",
                    "5.00",
                    strDataFine,
                    // Menu Configuratore: crea categoria Sport
                    "1", "3", "2", "Sport", "Calcetto", "n", "0", "0",
                    // Menu Configuratore: crea proposta con 1 partecipante e pubblica subito
                    "2", "1",
                    "Partita Calcetto",
                    "1",
                    strTermine,
                    "Campo A",
                    strDataInizio,
                    "20:30",
                    "8.00",
                    strDataFine,
                    "s",
                    // Esci da Menu Configuratore
                    "0",
                    // Menu Principale: 2 (Registra nuovo Fruitore luca_v3)
                    "2",
                    "luca_v3", "pwd_luca",
                    // Ora dentro MenuFruitore di luca_v3:
                    "1", "n",       // Visualizza bacheca
                    "2", "1", "s",  // Aderisci alla proposta #1
                    "3",            // Visualizza mie iscrizioni
                    "0",            // Esci da MenuFruitore
                    // Menu Principale: 0 (Salva ed Esci)
                    "0"
            ) + "\n";

            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

            Main.main(new String[0]);

            String output = baos.toString(StandardCharsets.UTF_8);
            asserisci(output.contains("CONFIGURAZIONE INIZIALE DEI CAMPI BASE"), "Eseguito wizard 8 campi base");
            asserisci(output.contains("Proposta pubblicata con successo in bacheca nello stato APERTA!"), "Proposta pubblicata");
            asserisci(output.contains("La tua iscrizione all'iniziativa 'Partita Calcetto' è stata registrata con successo!"),
                    "Fruitore luca_v3 iscritto con successo");

            // Verifica stato persistente e avanzamento scadenza
            DatiSistema salvato = GestorePersistenza.caricaDati();
            asserisci(salvato.getBacheca().getNumeroProposteAperte() == 1, "1 proposta aperta salvata");
            asserisci(salvato.getArchivioProposte().getNumeroProposte() == 1, "1 proposta nell'archivio storico");

            // Avanziamo la data oltre il termine di iscrizione (oggi + 4 giorni)
            salvato.aggiornaStatiEScadenze(oggi.plusDays(4));
            Proposta pArchiviata = salvato.getArchivioProposte().getProposte().get(0);
            asserisci(pArchiviata.getStato() == StatoProposta.CONFERMATA, "Proposta passata a CONFERMATA");
            Fruitore luca = salvato.getGestoreUtenti().getFruitore("luca_v3");
            asserisci(luca.getSpazioPersonale().getNumeroNotifiche() == 1, "Notifica recapitata nello spazio personale di luca_v3");

        } catch (Exception e) {
            throw new RuntimeException("Errore nella simulazione E2E V3", e);
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
