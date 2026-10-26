package it.unibs.ing.iniziative;

import it.unibs.ing.iniziative.batch.EsitoImportazione;
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
 * Suite di collaudo intensivo per la Versione 5:
 * verifica delle fondamenta V1 + V2 + V3 + V4 e di tutti i requisiti specifici della Versione 5:
 * - Importazione in modalità batch da uno o più file di input (campi comuni, categorie, campi specifici, proposte);
 * - Validazione dei vincoli e dei tipi durante l'importazione batch (rifiuto proposte non valide o collisioni nomi);
 * - Coesistenza tra modalità batch e modalità interattiva (che resta pienamente operativa);
 * - Verifica del dataset dimostrativo per la prova orale (Punto 6, pag. 30 del PDF: >=2 configuratori,
 *   >=3 fruitori, >=2 categorie, >=3 proposte aperte, >=2 confermate, >=1 conclusa, >=1 annullata, >=1 ritirata);
 * - Simulazione End-to-End interattiva con Kibo (Wizard 8 campi base + Import Batch + Interattivo + Fruitori + V4).
 */
public class TestSuiteV5 {

    private static int testEseguiti = 0;
    private static int testSuperati = 0;
    private static int testFalliti = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   AVVIO SUITE DI COLLAUDO INTENSIVO - VERSIONE 5 (ZANELLA)   ");
        System.out.println("===============================================================");

        // Test V1 - V4
        eseguiTest("1. [V1-V4] Fondamenta complete: 8 RuoloCampoBase, Nota 8, Copia difensiva, Iscrizioni, Disdette, Scadenze e Ritiri", TestSuiteV5::testFondamentaCompleteV1V4);

        // Test specifici V5
        eseguiTest("2. [V5] Importazione batch da file completo (campi comuni, categorie, campi specifici e proposte)", TestSuiteV5::testImportazioneBatchFileCompleto);
        eseguiTest("3. [V5] Importazione batch da più file di input in sequenza (struttura + proposte)", TestSuiteV5::testImportazioneBatchMultiFile);
        eseguiTest("4. [V5] Robustezza importazione batch: rifiuto vincoli violati, formati errati e collisioni nomi campi", TestSuiteV5::testImportazioneBatchErroriEVincoli);
        eseguiTest("5. [V5] Coesistenza modalità Batch e modalità Interattiva (pubblicazione interattiva di proposta importata)", TestSuiteV5::testCoesistenzaBatchEInterattiva);
        eseguiTest("6. [V5 + Orale] Verifica dataset dimostrativo prova orale (Punto 6 pag. 30 PDF)", TestSuiteV5::testDatasetDimostrazioneProvaOrale);
        eseguiTest("7. [V5] Simulazione E2E Main V5 + Kibo (Wizard 8 campi base + Import Batch + Pubblicazione Interattiva + V4)", TestSuiteV5::testSimulazioneEndToEndMainV5);

        System.out.println("\n===============================================================");
        System.out.printf("Riepilogo V5: %d eseguiti | %d SUPERATI | %d FALLITI\n", testEseguiti, testSuperati, testFalliti);
        System.out.println("===============================================================");

        if (testFalliti > 0) {
            System.exit(1);
        }
    }

    private static void eseguiTest(String nome, Runnable test) {
        testEseguiti++;
        System.out.printf("\n[TEST V5 - %02d] %s ... ", testEseguiti, nome);
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

    // -------------------------------------------------------------------------
    // 1. FONDAMENTA COMPLETE V1 - V4
    // -------------------------------------------------------------------------
    private static void testFondamentaCompleteV1V4() {
        DatiSistema ds = new DatiSistema();
        asserisci(!ds.getConfigurazioneSistema().isCampiBaseImpostati(), "Campi base non pre-impostati prima del wizard");
        ds.getConfigurazioneSistema().inizializzaCampiBasePredefiniti();
        asserisci(ds.getConfigurazioneSistema().getCampiBase().size() == RuoloCampoBase.values().length, "8 campi base RuoloCampoBase");

        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Configuratore def = (Configuratore) controller.login("config", "config");
        Configuratore admin = controller.registraNuovoConfiguratore(def, "admin_cfg", "pwd1");
        Fruitore f1 = controller.registraFruitore("luca", "pwd2");
        asserisci(admin != null && f1 != null, "Configuratore e Fruitore registrati");

        controller.creaCategoria("Sport", "Eventi sportivi");
        Categoria sport = controller.getCategoria("Sport");
        LocalDate oggi = LocalDate.of(2026, 10, 23);

        Proposta p = new Proposta(sport, admin);
        for (Campo c : controller.getTuttiICampiCategoria(sport)) {
            switch (c.getNome()) {
                case "Titolo": p.assegnaValore(c, "Torneo Calcetto"); break;
                case "Numero di partecipanti": p.assegnaValore(c, 2); break;
                case "Termine ultimo di iscrizione": p.assegnaValore(c, oggi.plusDays(4)); break;
                case "Luogo": p.assegnaValore(c, "Centro Sportivo"); break;
                case "Data": p.assegnaValore(c, oggi.plusDays(7)); break;
                case "Ora": p.assegnaValore(c, LocalTime.of(18, 0)); break;
                case "Quota individuale": p.assegnaValore(c, 10.0); break;
                case "Data conclusiva": p.assegnaValore(c, oggi.plusDays(7)); break;
            }
        }
        asserisci(controller.validaERegistraPropostaInSessione(p, oggi).isEmpty(), "Proposta valida");
        asserisci(controller.pubblicaPropostaInBacheca(p, oggi), "Proposta pubblicata");

        // Iscrizione, disdetta e re-iscrizione (V3 + V4)
        asserisci(controller.iscriviFruitoreAProposta(p, "luca", oggi.plusDays(1)) == null, "Iscrizione luca");
        asserisci(controller.disdiciIscrizioneFruitore(p, "luca", oggi.plusDays(2)) == null, "Disdetta luca");
        asserisci(controller.iscriviFruitoreAProposta(p, "luca", oggi.plusDays(3)) == null, "Re-iscrizione luca");

        // Ritiro proposta (V4)
        asserisci(controller.ritiraProposta(p, oggi.plusDays(3), "Maltempo"), "Ritiro proposta riuscito");
        asserisci(p.getStato() == StatoProposta.RITIRATA, "Stato RITIRATA");
        asserisci(f1.getSpazioPersonale().getNumeroNotifiche() == 1, "Notifica ritiro ricevuta");
        asserisci(controller.checkInvariant(), "Invarianti DBC verificati");
    }

    // -------------------------------------------------------------------------
    // 2. [V5] IMPORTAZIONE BATCH DA FILE COMPLETO
    // -------------------------------------------------------------------------
    private static void testImportazioneBatchFileCompleto() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate dataRef = LocalDate.of(2026, 10, 23);

        File fileBatch = new File("batch_esempi/completo.txt");
        if (!fileBatch.exists()) {
            fileBatch = new File("C:/Users/nahiz/OneDrive/Documenti/GitHub/IgnSf_A/ProgettoIngeSoftwareA-1_v5/batch_esempi/completo.txt");
        }
        asserisci(fileBatch.exists(), "Il file batch di esempio deve esistere: " + fileBatch.getAbsolutePath());

        EsitoImportazione esito = controller.importaDaFileBatch(fileBatch, autore, dataRef);
        asserisci(!esito.haErrori(), "Nessun errore durante l'importazione di completo.txt: " + esito.getErrori());
        asserisci(esito.getCampiComuniImportati() == 2, "Importati 2 campi comuni");
        asserisci(esito.getCategorieImportate() == 2, "Importate 2 categorie (Ciclismo, Cultura e Musei)");
        asserisci(esito.getCampiSpecificiImportati() == 3, "Importati 3 campi specifici");
        asserisci(esito.getProposteValideCreate() == 2, "Create 2 proposte valide");
        asserisci(esito.getPropostePubblicateInBacheca() == 1, "1 proposta pubblicata subito in bacheca");

        // La seconda proposta (pubblica_subito: false) si trova nelle proposte valide di sessione del controller
        asserisci(controller.getProposteValideInSessione().size() == 1, "1 proposta valida memorizzata nella sessione corrente");
        asserisci(controller.getBacheca().getNumeroProposteAperte() == 1, "1 proposta aperta in bacheca");
    }

    // -------------------------------------------------------------------------
    // 3. [V5] IMPORTAZIONE BATCH DA PIÙ FILE DI INPUT IN SEQUENZA
    // -------------------------------------------------------------------------
    private static void testImportazioneBatchMultiFile() {
        Path file1 = null;
        Path file2 = null;
        try {
            file1 = Files.createTempFile("batch_struttura_", ".txt");
            file2 = Files.createTempFile("batch_proposte_", ".txt");

            String contenuto1 = String.join("\n",
                    "[CAMPO_COMUNE]",
                    "nome: Assicurazione Inclusa",
                    "descrizione: Copertura assicurativa",
                    "obbligatorio: true",
                    "tipo: BOOLEANO",
                    "",
                    "[CATEGORIA]",
                    "nome: Escursionismo",
                    "descrizione: Trekking in montagna",
                    "[CAMPO_SPECIFICO]",
                    "nome: Difficoltà",
                    "descrizione: Livello CAI (T, E, EE)",
                    "obbligatorio: true",
                    "tipo: STRINGA"
            );
            Files.writeString(file1, contenuto1, StandardCharsets.UTF_8);

            String contenuto2 = String.join("\n",
                    "[PROPOSTA]",
                    "categoria: Escursionismo",
                    "pubblica_subito: true",
                    "Titolo: Salita al Monte Guglielmo",
                    "Numero di partecipanti: 15",
                    "Termine ultimo di iscrizione: 10/11/2026",
                    "Luogo: Zone (BS)",
                    "Data: 15/11/2026",
                    "Ora: 07:30",
                    "Quota individuale: 5.00",
                    "Data conclusiva: 15/11/2026",
                    "Assicurazione Inclusa: true",
                    "Difficoltà: E"
            );
            Files.writeString(file2, contenuto2, StandardCharsets.UTF_8);

            DatiSistema ds = creaDatiSistemaConCampiBase();
            ConfigurazioneController controller = new ConfigurazioneController(ds);
            Configuratore autore = new Configuratore("admin", "pwd", false);
            LocalDate oggi = LocalDate.of(2026, 10, 23);

            EsitoImportazione esito1 = controller.importaDaFileBatch(file1.toFile(), autore, oggi);
            asserisci(!esito1.haErrori() && esito1.getCategorieImportate() == 1 && esito1.getCampiComuniImportati() == 1,
                    "1° file batch (struttura) importato con successo");

            EsitoImportazione esito2 = controller.importaDaFileBatch(file2.toFile(), autore, oggi);
            asserisci(!esito2.haErrori() && esito2.getPropostePubblicateInBacheca() == 1,
                    "2° file batch (proposte) importato con successo");
            asserisci(controller.getBacheca().getProposteAperte().get(0).getTitolo().equals("Salita al Monte Guglielmo"),
                    "Proposta pubblicata in bacheca dal secondo file batch");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (file1 != null) Files.deleteIfExists(file1);
                if (file2 != null) Files.deleteIfExists(file2);
            } catch (IOException ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // 4. [V5] ROBUSTEZZA IMPORTAZIONE BATCH: VINCOLI VIOLATI E COLLISIONI
    // -------------------------------------------------------------------------
    private static void testImportazioneBatchErroriEVincoli() {
        Path fileErr = null;
        try {
            fileErr = Files.createTempFile("batch_errori_", ".txt");
            String contenuto = String.join("\n",
                    "[CATEGORIA]",
                    "nome: Musica",
                    "descrizione: Concerti",
                    "[CAMPO_SPECIFICO]",
                    "nome: Strumento",
                    "descrizione: Strumento richiesto",
                    "obbligatorio: true",
                    "tipo: STRINGA",
                    "",
                    "# Collisione: campo comune con lo stesso nome di un campo specifico già esistente",
                    "[CAMPO_COMUNE]",
                    "nome: Strumento",
                    "descrizione: Duplicato",
                    "obbligatorio: false",
                    "tipo: STRINGA",
                    "",
                    "# Proposta invalida: Data inizio a solo 1 giorno dal termine e campo obbligatorio 'Strumento' mancante",
                    "[PROPOSTA]",
                    "categoria: Musica",
                    "pubblica_subito: true",
                    "Titolo: Jam Session",
                    "Numero di partecipanti: 5",
                    "Termine ultimo di iscrizione: 10/11/2026",
                    "Luogo: Auditorium",
                    "Data: 11/11/2026",
                    "Ora: 21:00",
                    "Quota individuale: 0.00",
                    "Data conclusiva: 11/11/2026"
            );
            Files.writeString(fileErr, contenuto, StandardCharsets.UTF_8);

            DatiSistema ds = creaDatiSistemaConCampiBase();
            ConfigurazioneController controller = new ConfigurazioneController(ds);
            Configuratore autore = new Configuratore("admin", "pwd", false);
            LocalDate oggi = LocalDate.of(2026, 10, 23);

            EsitoImportazione esito = controller.importaDaFileBatch(fileErr.toFile(), autore, oggi);
            asserisci(esito.getCategorieImportate() == 1, "Categoria Musica importata");
            asserisci(esito.getCampiComuniImportati() == 0, "Campo comune in collisione con campo specifico rifiutato");
            asserisci(esito.getProposteValideCreate() == 0 && esito.haErrori(), "Proposta invalida rifiutata con segnalazione errori");
            asserisci(controller.getBacheca().isVuota(), "Bacheca rimasta vuota");
            asserisci(controller.checkInvariant(), "Invarianti DBC preservati anche a fronte di errori nel file batch");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (fileErr != null) Files.deleteIfExists(fileErr);
            } catch (IOException ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // 5. [V5] COESISTENZA MODALITÀ BATCH E MODALITÀ INTERATTIVA
    // -------------------------------------------------------------------------
    private static void testCoesistenzaBatchEInterattiva() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);
        Configuratore autore = new Configuratore("admin", "pwd", false);
        LocalDate oggi = LocalDate.of(2026, 10, 23);

        File fileBatch = new File("batch_esempi/completo.txt");
        if (!fileBatch.exists()) {
            fileBatch = new File("C:/Users/nahiz/OneDrive/Documenti/GitHub/IgnSf_A/ProgettoIngeSoftwareA-1_v5/batch_esempi/completo.txt");
        }
        controller.importaDaFileBatch(fileBatch, autore, oggi);

        // La seconda proposta importata da batch ('Visita Guidata al Museo Santa Giulia') è in sessione nello stato VALIDA
        List<Proposta> inSessione = controller.getProposteValideInSessione();
        asserisci(inSessione.size() == 1, "1 proposta valida in sessione");
        Proposta propostaDaPubblicare = inSessione.get(0);
        asserisci(propostaDaPubblicare.getStato() == StatoProposta.VALIDA, "Stato VALIDA");

        // Il configuratore la pubblica in modalità interattiva
        asserisci(controller.pubblicaPropostaInBacheca(propostaDaPubblicare, oggi), "Pubblicazione interattiva riuscita");
        asserisci(controller.getProposteValideInSessione().isEmpty(), "Rimossa dalle proposte in attesa");
        asserisci(controller.getBacheca().getNumeroProposteAperte() == 2, "Ora entrambe le proposte sono aperte in bacheca");
    }

    // -------------------------------------------------------------------------
    // 6. [V5 + ORALE] VERIFICA DATASET DIMOSTRATIVO PROVA ORALE (PUNTO 6 PAG. 30 PDF)
    // -------------------------------------------------------------------------
    private static void testDatasetDimostrazioneProvaOrale() {
        DatiSistema ds = creaDatiSistemaConCampiBase();
        ConfigurazioneController controller = new ConfigurazioneController(ds);

        // - almeno 2 configuratori
        Configuratore def1 = (Configuratore) controller.login("config", "config");
        Configuratore cfg1 = controller.registraNuovoConfiguratore(def1, "marco_cfg", "pwd1");
        Configuratore def2 = (Configuratore) controller.login("config", "config");
        Configuratore cfg2 = controller.registraNuovoConfiguratore(def2, "elena_cfg", "pwd2");
        asserisci(cfg1 != null && cfg2 != null, "Almeno 2 configuratori registrati");

        // - almeno 3 fruitori
        Fruitore fru1 = controller.registraFruitore("luca_fru", "pwd1");
        Fruitore fru2 = controller.registraFruitore("sara_fru", "pwd2");
        Fruitore fru3 = controller.registraFruitore("paolo_fru", "pwd3");
        asserisci(fru1 != null && fru2 != null && fru3 != null, "Almeno 3 fruitori registrati");

        // - almeno 2 categorie
        controller.creaCategoria("Gite", "Escursioni e viaggi");
        controller.creaCategoria("Sport", "Attività sportive");
        Categoria gite = controller.getCategoria("Gite");
        Categoria sport = controller.getCategoria("Sport");

        LocalDate baseDate = LocalDate.of(2026, 10, 1);

        // Creazione di 7 proposte:
        // 1) Proposta CONCLUSA (creata 01/10, termine 03/10, data 06/10, conclusiva 06/10)
        Proposta pConclusa = creaPropostaHelper(controller, gite, cfg1, "Gita sul Garda", 1,
                baseDate, baseDate.plusDays(2), baseDate.plusDays(5), baseDate.plusDays(5));
        controller.iscriviFruitoreAProposta(pConclusa, "luca_fru", baseDate.plusDays(1));

        // 2) Proposta ANNULLATA/CANCELLATA (creata 01/10, termine 03/10, richiesti 3 partecipanti ma solo 1 iscritto)
        Proposta pAnnullata = creaPropostaHelper(controller, sport, cfg2, "Torneo Volley", 3,
                baseDate, baseDate.plusDays(2), baseDate.plusDays(6), baseDate.plusDays(6));
        controller.iscriviFruitoreAProposta(pAnnullata, "sara_fru", baseDate.plusDays(1));

        // Avanzamento al 04/10: pConclusa diventa CONFERMATA, pAnnullata diventa ANNULLATA
        controller.aggiornaStatiEScadenze(baseDate.plusDays(3));
        // Avanzamento al 08/10: pConclusa diventa CONCLUSA
        controller.aggiornaStatiEScadenze(baseDate.plusDays(7));

        LocalDate oggi = LocalDate.of(2026, 10, 23);

        // 3) 2 Proposte CONFERMATE (create il 15/10, termine 20/10, data inizio 28/10)
        LocalDate d15 = LocalDate.of(2026, 10, 15);
        Proposta pConf1 = creaPropostaHelper(controller, gite, cfg1, "Visita Museo Santa Giulia", 2,
                d15, d15.plusDays(5), oggi.plusDays(5), oggi.plusDays(5));
        controller.iscriviFruitoreAProposta(pConf1, "luca_fru", d15.plusDays(1));
        controller.iscriviFruitoreAProposta(pConf1, "sara_fru", d15.plusDays(2));

        Proposta pConf2 = creaPropostaHelper(controller, sport, cfg2, "Gara Podistica", 1,
                d15, d15.plusDays(5), oggi.plusDays(6), oggi.plusDays(6));
        controller.iscriviFruitoreAProposta(pConf2, "paolo_fru", d15.plusDays(1));

        controller.aggiornaStatiEScadenze(oggi);

        // 4) 1 Proposta RITIRATA (creata oggi, iscritti luca_fru e paolo_fru, poi ritirata)
        Proposta pRitirata = creaPropostaHelper(controller, gite, cfg1, "Serata Osservatorio", 2,
                oggi, oggi.plusDays(4), oggi.plusDays(8), oggi.plusDays(8));
        controller.iscriviFruitoreAProposta(pRitirata, "luca_fru", oggi);
        controller.ritiraProposta(pRitirata, oggi, "Guasto telescopio");

        // 5) 3 Proposte APERTE
        Proposta pAperta1 = creaPropostaHelper(controller, gite, cfg1, "Gita a Montisola", 10,
                oggi, oggi.plusDays(10), oggi.plusDays(15), oggi.plusDays(15));
        Proposta pAperta2 = creaPropostaHelper(controller, sport, cfg2, "Partita Calcetto", 10,
                oggi, oggi.plusDays(12), oggi.plusDays(16), oggi.plusDays(16));
        Proposta pAperta3 = creaPropostaHelper(controller, gite, cfg1, "Mostra Guido Crepax", 15,
                oggi, oggi.plusDays(14), oggi.plusDays(18), oggi.plusDays(18));
        controller.iscriviFruitoreAProposta(pAperta1, "luca_fru", oggi);
        controller.iscriviFruitoreAProposta(pAperta2, "sara_fru", oggi);
        controller.iscriviFruitoreAProposta(pAperta3, "paolo_fru", oggi);

        // Verifica di tutti i conteggi richiesti dal Punto 6 (pag. 30 PDF)
        ArchivioProposte archivio = controller.getArchivioProposte();
        asserisci(archivio.getPropostePerStato(StatoProposta.APERTA).size() == 3, "3 proposte APERTE");
        asserisci(archivio.getPropostePerStato(StatoProposta.CONFERMATA).size() == 2, "2 proposte CONFERMATE");
        asserisci(archivio.getPropostePerStato(StatoProposta.CONCLUSA).size() == 1, "1 proposta CONCLUSA");
        asserisci(archivio.getPropostePerStato(StatoProposta.ANNULLATA).size() == 1, "1 proposta ANNULLATA");
        asserisci(archivio.getPropostePerStato(StatoProposta.RITIRATA).size() == 1, "1 proposta RITIRATA");
        asserisci(controller.checkInvariant(), "Invarianti DBC dell'intero dataset orale rispettati");
    }

    private static Proposta creaPropostaHelper(ConfigurazioneController controller, Categoria cat, Configuratore autore,
                                               String titolo, int maxPartecipanti, LocalDate dataCreazione,
                                               LocalDate termine, LocalDate dataInizio, LocalDate dataFine) {
        Proposta p = new Proposta(cat, autore);
        for (Campo c : controller.getTuttiICampiCategoria(cat)) {
            switch (c.getNome()) {
                case "Titolo": p.assegnaValore(c, titolo); break;
                case "Numero di partecipanti": p.assegnaValore(c, maxPartecipanti); break;
                case "Termine ultimo di iscrizione": p.assegnaValore(c, termine); break;
                case "Luogo": p.assegnaValore(c, "Brescia Centro"); break;
                case "Data": p.assegnaValore(c, dataInizio); break;
                case "Ora": p.assegnaValore(c, LocalTime.of(9, 30)); break;
                case "Quota individuale": p.assegnaValore(c, 12.00); break;
                case "Data conclusiva": p.assegnaValore(c, dataFine); break;
            }
        }
        List<String> err = controller.validaERegistraPropostaInSessione(p, dataCreazione);
        asserisci(err.isEmpty(), "Proposta helper valida: " + err);
        asserisci(controller.pubblicaPropostaInBacheca(p, dataCreazione), "Proposta helper pubblicata");
        return p;
    }

    // -------------------------------------------------------------------------
    // 7. [V5] SIMULAZIONE E2E MAIN V5 + KIBO
    // -------------------------------------------------------------------------
    private static void testSimulazioneEndToEndMainV5() {
        Path fileDati = Paths.get("dati_sistema.dat");
        Path tempBatch = null;
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
            String strTermine = oggi.plusDays(4).format(fmtData);
            String strDataInizio = oggi.plusDays(8).format(fmtData);
            String strDataFine = oggi.plusDays(8).format(fmtData);

            tempBatch = Files.createTempFile("e2e_v5_batch_", ".txt");
            String batchContent = String.join("\n",
                    "[CAMPO_COMUNE]",
                    "nome: Sponsor",
                    "descrizione: Sponsor ufficiale",
                    "obbligatorio: false",
                    "tipo: STRINGA",
                    "",
                    "[CATEGORIA]",
                    "nome: Cultura",
                    "descrizione: Eventi culturali",
                    "[CAMPO_SPECIFICO]",
                    "nome: Guida",
                    "descrizione: Guida presente",
                    "obbligatorio: true",
                    "tipo: BOOLEANO",
                    "",
                    "[PROPOSTA]",
                    "categoria: Cultura",
                    "pubblica_subito: true",
                    "Titolo: Visita Castello",
                    "Numero di partecipanti: 5",
                    "Termine ultimo di iscrizione: " + strTermine,
                    "Luogo: Castello di Brescia",
                    "Data: " + strDataInizio,
                    "Ora: 10:00",
                    "Quota individuale: 10.00",
                    "Data conclusiva: " + strDataFine,
                    "Guida: true",
                    "Sponsor: Comune BS",
                    "",
                    "[PROPOSTA]",
                    "categoria: Cultura",
                    "pubblica_subito: false",
                    "Titolo: Mostra Fotografica",
                    "Numero di partecipanti: 10",
                    "Termine ultimo di iscrizione: " + strTermine,
                    "Luogo: Museo Santa Giulia",
                    "Data: " + strDataInizio,
                    "Ora: 15:00",
                    "Quota individuale: 5.00",
                    "Data conclusiva: " + strDataFine,
                    "Guida: false"
            );
            Files.writeString(tempBatch, batchContent, StandardCharsets.UTF_8);

            // Flusso simulato nel Main V5:
            // 1. Login config/config -> cambio credenziali in admin_v5 / pass_v5
            // 2. Wizard 8 campi base al primo avvio
            // 3. Menu Configuratore:
            //    - 8 (Importazione Dati in Modalità Batch) -> percorso tempBatch -> "n" (non importare altro file)
            //    - 3 (Gestisci Proposte di Sessione e Pubblica in Bacheca) -> 1 (Mostra Fotografica) -> "s" (Pubblica in bacheca)
            //    - 0 (Esci da Menu Configuratore)
            // 4. Menu Principale:
            //    - 0 (Salva ed Esci)
            String simulatedInput = String.join("\n",
                    "1",
                    "config", "config",
                    "admin_v5", "pass_v5",
                    // 8 campi base primo avvio:
                    "Titolo Base",
                    "10",
                    strTermine,
                    "Brescia",
                    strDataInizio,
                    "09:00",
                    "0.00",
                    strDataFine,
                    // Menu Configuratore: 8 (Importa file batch)
                    "8",
                    tempBatch.toAbsolutePath().toString(),
                    "n",
                    // Menu Configuratore: 3 (Pubblica interattivamente la seconda proposta importata in sessione)
                    "3", "1", "s",
                    // Esci da Menu Configuratore e dal programma
                    "0",
                    "0"
            ) + "\n";

            System.setIn(new ByteArrayInputStream(simulatedInput.getBytes(StandardCharsets.UTF_8)));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

            Main.main(new String[0]);

            String output = baos.toString(StandardCharsets.UTF_8);
            asserisci(output.contains("CONFIGURAZIONE INIZIALE DEI CAMPI BASE"), "Eseguito wizard 8 campi base");
            asserisci(output.contains("REPORT IMPORTAZIONE BATCH DA FILE"), "Report importazione batch stampato");
            asserisci(output.contains("Proposta pubblicata in bacheca con successo!"),
                    "Seconda proposta importata in sessione pubblicata poi interattivamente");

            DatiSistema salvato = GestorePersistenza.caricaDati();
            asserisci(salvato.getCategorie().size() == 1, "Categoria Cultura salvata");
            asserisci(salvato.getConfigurazioneSistema().getCampiComuni().size() == 1, "Campo comune Sponsor salvato");
            asserisci(salvato.getBacheca().getNumeroProposteAperte() == 2, "Entrambe le proposte (Visita Castello e Mostra Fotografica) sono ora APERTE in bacheca");

        } catch (Exception e) {
            throw new RuntimeException("Errore nella simulazione E2E V5", e);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            try {
                if (tempBatch != null) Files.deleteIfExists(tempBatch);
                if (backup != null) {
                    Files.write(fileDati, backup);
                } else {
                    Files.deleteIfExists(fileDati);
                }
            } catch (IOException ignored) {}
        }
    }
}
