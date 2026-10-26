package it.unibs.ing.iniziative;

import it.unibs.ing.iniziative.controller.ConfigurazioneController;
import it.unibs.ing.iniziative.model.*;
import it.unibs.ing.iniziative.storage.DatiSistema;
import it.unibs.ing.iniziative.storage.GestorePersistenza;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Suite di test esaustiva per verificare tutti i requisiti di dominio,
 * vincoli di integrità, immutabilità, gestione credenziali, persistenza,
 * impostazione dei campi base da parte del configuratore, controller MVC e Design By Contract (DBC).
 */
public class TestSuiteCompleta {

    private static int testEseguiti = 0;
    private static int testSuperati = 0;
    private static int testFalliti = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   AVVIO SUITE DI COLLAUDO INTENSIVO - VERSIONE 1 (ZANELLA)   ");
        System.out.println("===============================================================");

        eseguiTest("1. Campi base fissati dal configuratore: contenuto, tipo e obbligatorietà", TestSuiteCompleta::testCampiBasePresentiEdEsatti);
        eseguiTest("2. Rigorosa immutabilità dei Campi Base (information hiding)", TestSuiteCompleta::testCampiBaseImmutabilita);
        eseguiTest("3. Aggiunta Campi Comuni e blocco collisione con Campi Base/Comuni", TestSuiteCompleta::testCampiComuniCollisioni);
        eseguiTest("4. Modifica obbligatorietà e rimozione Campi Comuni", TestSuiteCompleta::testCampiComuniModificaERimozione);
        eseguiTest("5. Creazione Categorie e rifiuto duplicati case-insensitive", TestSuiteCompleta::testCategorieCreazioneERifiutoDuplicati);
        eseguiTest("6. Campi Specifici: vincoli di unicità (base, comuni, specifici)", TestSuiteCompleta::testCampiSpecificiUnivocita);
        eseguiTest("7. Campi Specifici omonimi ammessi in categorie DIVERSE", TestSuiteCompleta::testCampiSpecificiInCategorieDiverse);
        eseguiTest("8. Modifica obbligatorietà e rimozione Campi Specifici", TestSuiteCompleta::testCampiSpecificiModificaERimozione);
        eseguiTest("9. Rimozione a cascata di una Categoria e dei suoi campi specifici", TestSuiteCompleta::testRimozioneCategoriaACascata);
        eseguiTest("10. Composizione scheda completa di una Categoria (Base + Comuni + Specifici)", TestSuiteCompleta::testSchedaCompletaCategoria);
        eseguiTest("11. Gestione Utenti: primo accesso, cambio credenziali e unicità", TestSuiteCompleta::testGestoreUtentiEPrimoAccesso);
        eseguiTest("12. Serializzazione e persistenza fedele su file", TestSuiteCompleta::testPersistenzaStatoCompleto);
        eseguiTest("13. Simulazione interattiva End-to-End con flussi corretti dei menu (e impostazione Campi Base)", TestSuiteCompleta::testSimulazioneInterattivaMenuE2E);
        eseguiTest("14. Architettura MVC: Operazioni tramite ConfigurazioneController", TestSuiteCompleta::testConfigurazioneController);
        eseguiTest("15. Design By Contract (DBC): Invarianti di classe formali", TestSuiteCompleta::testDesignByContractInvarianti);
        eseguiTest("16. Campi base: 8 ruoli vincolati da specifica, fissaggio unico e validazioni", TestSuiteCompleta::testCampiBaseDefinitiDalConfiguratore);

        System.out.println("\n===============================================================");
        System.out.printf("Riepilogo finale: %d eseguiti | %d SUPERATI | %d FALLITI\n", testEseguiti, testSuperati, testFalliti);
        System.out.println("===============================================================");

        if (testFalliti > 0) {
            System.exit(1);
        }
    }

    private static void eseguiTest(String nome, Runnable test) {
        testEseguiti++;
        System.out.printf("\n[TEST %02d] %s ... ", testEseguiti, nome);
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

    /**
     * Insieme di campi base usato dai test come esempio (il sistema non lo impone:
     * numero e nomi dei campi base sono scelti dal configuratore).
     */
    private static List<Campo> campiBaseStandard() {
        return List.of(
                new Campo("Titolo", "Nome di fantasia (esplicativo) attribuito all'iniziativa", true, TipoCampo.STRINGA),
                new Campo("Numero di partecipanti", "Numero di persone da coinvolgere nell'iniziativa", true, TipoCampo.INTERO),
                new Campo("Termine ultimo di iscrizione", "Ultimo giorno utile per iscriversi (fino alle ore 23:59)", true, TipoCampo.DATA),
                new Campo("Luogo", "Indirizzo del luogo o ritrovo dei partecipanti", true, TipoCampo.STRINGA),
                new Campo("Data", "Data di inizio dell'iniziativa", true, TipoCampo.DATA),
                new Campo("Ora", "Ora di ritrovo dei partecipanti nel giorno fissato", true, TipoCampo.ORA),
                new Campo("Quota individuale", "Spesa sostenuta da ogni partecipante (può essere 0)", true, TipoCampo.DECIMALE),
                new Campo("Data conclusiva", "Data di conclusione dell'iniziativa", true, TipoCampo.DATA)
        );
    }

    private static void fissaBaseStandard(ConfigurazioneSistema cs) {
        asserisci(cs.fissaCampiBase(campiBaseStandard()), "Fissaggio dei campi base di esempio");
    }

    // -------------------------------------------------------------------------------------
    // 1. CAMPI BASE PRESENTI ED ESATTI
    // -------------------------------------------------------------------------------------
    private static void testCampiBasePresentiEdEsatti() {
        ConfigurazioneSistema cs = new ConfigurazioneSistema();
        fissaBaseStandard(cs);
        List<Campo> base = cs.getCampiBase();

        asserisci(base.size() == 8, "I campi base devono essere esattamente 8, trovati: " + base.size());

        String[] nomiAttesi = {
                "Titolo",
                "Numero di partecipanti",
                "Termine ultimo di iscrizione",
                "Luogo",
                "Data",
                "Ora",
                "Quota individuale",
                "Data conclusiva"
        };

        TipoCampo[] tipiAttesi = {
                TipoCampo.STRINGA,
                TipoCampo.INTERO,
                TipoCampo.DATA,
                TipoCampo.STRINGA,
                TipoCampo.DATA,
                TipoCampo.ORA,
                TipoCampo.DECIMALE,
                TipoCampo.DATA
        };

        for (int i = 0; i < nomiAttesi.length; i++) {
            Campo c = base.get(i);
            asserisci(c.getNome().equalsIgnoreCase(nomiAttesi[i]), 
                    "Campo " + i + " atteso: " + nomiAttesi[i] + ", trovato: " + c.getNome());
            asserisci(c.getTipo() == tipiAttesi[i], 
                    "Tipo campo " + c.getNome() + " atteso: " + tipiAttesi[i] + ", trovato: " + c.getTipo());
            asserisci(c.isObbligatorio(), 
                    "Il campo base '" + c.getNome() + "' DEVE essere obbligatorio");
            asserisci(c.getDescrizione() != null && !c.getDescrizione().isEmpty(), 
                    "Il campo base '" + c.getNome() + "' deve avere una descrizione esplicativa");
        }

        // Verifica del controllo rigoroso di formato per ciascun tipo di dato
        asserisci(TipoCampo.STRINGA.validaFormato("Testo valido"), "Stringa valida accettata");
        asserisci(!TipoCampo.STRINGA.validaFormato(""), "Stringa vuota rifiutata");
        asserisci(TipoCampo.INTERO.validaFormato("25"), "Intero positivo valido accettato");
        asserisci(!TipoCampo.INTERO.validaFormato("non_un_numero"), "Intero testuale rifiutato");
        asserisci(!TipoCampo.INTERO.validaFormato("-5"), "Intero negativo rifiutato");
        asserisci(TipoCampo.DATA.validaFormato("15/10/2026"), "Data gg/mm/aaaa valida accettata");
        asserisci(!TipoCampo.DATA.validaFormato("32/13/2026"), "Data inesistente rifiutata");
        asserisci(!TipoCampo.DATA.validaFormato("testo"), "Data testuale rifiutata");
        asserisci(TipoCampo.ORA.validaFormato("20:30"), "Ora hh:mm valida accettata");
        asserisci(!TipoCampo.ORA.validaFormato("25:99"), "Ora non valida rifiutata");
        asserisci(TipoCampo.DECIMALE.validaFormato("12.50"), "Decimale valido accettato");
        asserisci(TipoCampo.DECIMALE.validaFormato("0"), "Decimale zero valido accettato");
        asserisci(!TipoCampo.DECIMALE.validaFormato("abc"), "Decimale non numerico rifiutato");
    }

    // -------------------------------------------------------------------------------------
    // 2. IMMUTABILITA DEI CAMPI BASE
    // -------------------------------------------------------------------------------------
    private static void testCampiBaseImmutabilita() {
        ConfigurazioneSistema cs = new ConfigurazioneSistema();
        fissaBaseStandard(cs);
        List<Campo> base = cs.getCampiBase();

        boolean eccezioneLanciata = false;
        try {
            base.add(new Campo("CampoFalso", "Desc", true, TipoCampo.STRINGA));
        } catch (UnsupportedOperationException e) {
            eccezioneLanciata = true;
        }
        asserisci(eccezioneLanciata, "getCampiBase() deve restituire una lista non modificabile su add()");

        eccezioneLanciata = false;
        try {
            base.remove(0);
        } catch (UnsupportedOperationException e) {
            eccezioneLanciata = true;
        }
        asserisci(eccezioneLanciata, "getCampiBase() deve restituire una lista non modificabile su remove()");

        // I campi base, una volta fissati, non possono essere reimpostati
        asserisci(!cs.fissaCampiBase(List.of(new Campo("Altro", "x", true, TipoCampo.STRINGA))), "Una seconda impostazione dei campi base deve essere rifiutata");
        asserisci(cs.getCampiBase().size() == 8, "I campi base fissati non devono essere alterati");
    }

    // -------------------------------------------------------------------------------------
    // 3. CAMPI COMUNI COLLISIONI
    // -------------------------------------------------------------------------------------
    private static void testCampiComuniCollisioni() {
        ConfigurazioneSistema cs = new ConfigurazioneSistema();
        fissaBaseStandard(cs);

        // Aggiunta lecita
        boolean ok = cs.aggiungiCampoComune(new Campo("Note", "Note varie", false, TipoCampo.STRINGA));
        asserisci(ok, "Aggiunta del campo comune 'Note' deve riuscire");

        // Tentativo di aggiungere un campo comune omonimo a un campo base
        ok = cs.aggiungiCampoComune(new Campo("Titolo", "Collisione base", true, TipoCampo.STRINGA));
        asserisci(!ok, "Aggiunta di un campo comune chiamato 'Titolo' deve essere respinta (collisione con campo base)");

        ok = cs.aggiungiCampoComune(new Campo("luogo", "Collisione base minuscola", false, TipoCampo.STRINGA));
        asserisci(!ok, "Aggiunta di un campo comune chiamato 'luogo' deve essere respinta (case-insensitive)");

        // Tentativo di duplicare un campo comune
        ok = cs.aggiungiCampoComune(new Campo("note", "Duplicato", true, TipoCampo.STRINGA));
        asserisci(!ok, "Aggiunta di un campo comune già esistente 'note' deve essere respinta (case-insensitive)");

        asserisci(cs.getCampiComuni().size() == 1, "Deve esserci esattamente 1 campo comune inserito");
    }

    // -------------------------------------------------------------------------------------
    // 4. CAMPI COMUNI MODIFICA E RIMOZIONE
    // -------------------------------------------------------------------------------------
    private static void testCampiComuniModificaERimozione() {
        ConfigurazioneSistema cs = new ConfigurazioneSistema();
        fissaBaseStandard(cs);
        cs.aggiungiCampoComune(new Campo("Durata", "Durata evento", false, TipoCampo.INTERO));

        Campo c = cs.getCampoComune("durata");
        asserisci(c != null, "getCampoComune deve trovare 'durata' case-insensitive");
        asserisci(!c.isObbligatorio(), "Inizialmente facoltativo");

        c.setObbligatorio(true);
        asserisci(cs.getCampoComune("Durata").isObbligatorio(), "Dopo la modifica deve risultare obbligatorio");

        boolean rimosso = cs.rimuoviCampoComune("durata");
        asserisci(rimosso, "La rimozione del campo 'durata' deve avere successo");
        asserisci(cs.getCampiComuni().isEmpty(), "La lista dei campi comuni deve essere vuota");

        boolean rimossoInesistente = cs.rimuoviCampoComune("Inesistente");
        asserisci(!rimossoInesistente, "La rimozione di un campo inesistente deve restituire false");
    }

    // -------------------------------------------------------------------------------------
    // 5. CATEGORIE E RIFIUTO DUPLICATI
    // -------------------------------------------------------------------------------------
    private static void testCategorieCreazioneERifiutoDuplicati() {
        DatiSistema ds = new DatiSistema();
        fissaBaseStandard(ds.getConfigurazioneSistema());

        boolean ok = ds.aggiungiCategoria(new Categoria("Sport", "Attività fisiche"));
        asserisci(ok, "Creazione categoria 'Sport' deve riuscire");

        // Duplicati
        ok = ds.aggiungiCategoria(new Categoria("sport", "Altro"));
        asserisci(!ok, "Creazione categoria 'sport' duplicata minuscola deve fallire");

        ok = ds.aggiungiCategoria(new Categoria("SPORT", "Altro maiuscolo"));
        asserisci(!ok, "Creazione categoria 'SPORT' duplicata maiuscola deve fallire");

        // Altra categoria
        ok = ds.aggiungiCategoria(new Categoria("Cultura", "Eventi culturali"));
        asserisci(ok, "Creazione categoria 'Cultura' deve riuscire");

        asserisci(ds.getCategorie().size() == 2, "Devono essere presenti 2 categorie");

        // Immutabilità lista categorie
        boolean exc = false;
        try {
            ds.getCategorie().clear();
        } catch (UnsupportedOperationException e) {
            exc = true;
        }
        asserisci(exc, "getCategorie() deve restituire una vista immutabile");
    }

    // -------------------------------------------------------------------------------------
    // 6. CAMPI SPECIFICI UNIVOCITA
    // -------------------------------------------------------------------------------------
    private static void testCampiSpecificiUnivocita() {
        DatiSistema ds = new DatiSistema();
        fissaBaseStandard(ds.getConfigurazioneSistema());
        ds.getConfigurazioneSistema().aggiungiCampoComune(new Campo("Note", "Info", false, TipoCampo.STRINGA));

        Categoria sport = new Categoria("Sport", "Eventi sportivi");
        ds.aggiungiCategoria(sport);

        // Aggiunta lecita
        boolean ok = sport.aggiungiCampoSpecifico(new Campo("Certificato medico", "Idoneità", true, TipoCampo.BOOLEANO));
        asserisci(ok, "Aggiunta del campo specifico 'Certificato medico' deve riuscire");

        // Collisione con campo base verificata tramite DatiSistema
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "Titolo"), "Il nome 'Titolo' deve risultare occupato (campo base)");
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "data"), "Il nome 'data' deve risultare occupato (campo base)");

        // Collisione con campo comune verificata tramite DatiSistema
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "Note"), "Il nome 'Note' deve risultare occupato (campo comune)");
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "note"), "Il nome 'note' deve risultare occupato (campo comune)");

        // Collisione con campo specifico già esistente
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "Certificato medico"), "Nome già presente nella categoria");
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "certificato MEDICO"), "Nome già presente case-insensitive");

        // Nome libero
        asserisci(!ds.isNomeCampoPresenteInCategoria(sport, "Attrezzatura"), "Il nome 'Attrezzatura' deve risultare libero");
    }

    // -------------------------------------------------------------------------------------
    // 7. CAMPI SPECIFICI IN CATEGORIE DIVERSE (AMMESSI)
    // -------------------------------------------------------------------------------------
    private static void testCampiSpecificiInCategorieDiverse() {
        DatiSistema ds = new DatiSistema();
        fissaBaseStandard(ds.getConfigurazioneSistema());
        Categoria sport = new Categoria("Sport", "Sport");
        Categoria arte = new Categoria("Arte", "Arte");
        ds.aggiungiCategoria(sport);
        ds.aggiungiCategoria(arte);

        // Entrambe hanno un campo specifico chiamato "Guida"
        boolean okSport = sport.aggiungiCampoSpecifico(new Campo("Guida", "Istruttore sportivo", true, TipoCampo.STRINGA));
        boolean okArte = arte.aggiungiCampoSpecifico(new Campo("Guida", "Guida turistica museo", false, TipoCampo.STRINGA));

        asserisci(okSport, "Campo specifico 'Guida' in Sport deve riuscire");
        asserisci(okArte, "Campo specifico 'Guida' in Arte deve riuscire (categorie distinte)");

        // In Sport, "Guida" è occupato
        asserisci(ds.isNomeCampoPresenteInCategoria(sport, "Guida"), "Guida è occupato in Sport");
        // In Arte, "Guida" è occupato
        asserisci(ds.isNomeCampoPresenteInCategoria(arte, "Guida"), "Guida è occupato in Arte");
    }

    // -------------------------------------------------------------------------------------
    // 8. CAMPI SPECIFICI MODIFICA E RIMOZIONE
    // -------------------------------------------------------------------------------------
    private static void testCampiSpecificiModificaERimozione() {
        Categoria sport = new Categoria("Sport", "Sport");
        Campo c = new Campo("Certificato", "Cert", true, TipoCampo.BOOLEANO);
        sport.aggiungiCampoSpecifico(c);

        asserisci(sport.haCampoSpecifico("certificato"), "Ricerca case-insensitive campo specifico");
        Campo trovato = sport.getCampoSpecifico("CERTIFICATO");
        asserisci(trovato != null && trovato.isObbligatorio(), "Trovato e obbligatorio");

        trovato.setObbligatorio(false);
        asserisci(!sport.getCampoSpecifico("Certificato").isObbligatorio(), "Modificato in facoltativo");

        boolean rimosso = sport.rimuoviCampoSpecifico("certificato");
        asserisci(rimosso, "Rimozione campo specifico deve riuscire");
        asserisci(sport.getCampiSpecifici().isEmpty(), "I campi specifici devono essere vuoti");

        boolean rimossoErr = sport.rimuoviCampoSpecifico("NonEsisto");
        asserisci(!rimossoErr, "Rimozione campo inesistente deve restituire false");

        // Immutabilità getCampiSpecifici
        sport.aggiungiCampoSpecifico(c);
        boolean exc = false;
        try {
            sport.getCampiSpecifici().clear();
        } catch (UnsupportedOperationException e) {
            exc = true;
        }
        asserisci(exc, "getCampiSpecifici() deve essere unmodifiable");
    }

    // -------------------------------------------------------------------------------------
    // 9. RIMOZIONE CATEGORIA A CASCATA
    // -------------------------------------------------------------------------------------
    private static void testRimozioneCategoriaACascata() {
        DatiSistema ds = new DatiSistema();
        fissaBaseStandard(ds.getConfigurazioneSistema());
        Categoria musica = new Categoria("Musica", "Concerti");
        musica.aggiungiCampoSpecifico(new Campo("Strumento", "Tipo strumento", true, TipoCampo.STRINGA));
        ds.aggiungiCategoria(musica);

        asserisci(ds.esisteCategoria("Musica"), "Categoria Musica esiste");
        boolean rimossa = ds.rimuoviCategoria("musica");
        asserisci(rimossa, "Rimozione categoria Musica deve avere successo");
        asserisci(!ds.esisteCategoria("Musica"), "Categoria Musica non deve più esistere");

        // Se ricreo la categoria Musica, deve essere nuova e priva di campi
        Categoria nuovaMusica = new Categoria("Musica", "Nuova");
        ds.aggiungiCategoria(nuovaMusica);
        asserisci(nuovaMusica.getCampiSpecifici().isEmpty(), "Nuova categoria deve partire con 0 campi specifici");
    }

    // -------------------------------------------------------------------------------------
    // 10. SCHEDA COMPLETA CATEGORIA
    // -------------------------------------------------------------------------------------
    private static void testSchedaCompletaCategoria() {
        DatiSistema ds = new DatiSistema();
        fissaBaseStandard(ds.getConfigurazioneSistema());
        ds.getConfigurazioneSistema().aggiungiCampoComune(new Campo("Note", "Note", false, TipoCampo.STRINGA));
        ds.getConfigurazioneSistema().aggiungiCampoComune(new Campo("Durata", "Durata", false, TipoCampo.INTERO));

        Categoria sport = new Categoria("Sport", "Sport");
        sport.aggiungiCampoSpecifico(new Campo("Certificato", "Cert", true, TipoCampo.BOOLEANO));
        ds.aggiungiCategoria(sport);

        List<Campo> tutti = ds.getTuttiICampiCategoria(sport);
        // 8 base + 2 comuni + 1 specifico = 11 campi
        asserisci(tutti.size() == 11, "La scheda completa di Sport deve contenere 11 campi, trovati: " + tutti.size());

        // Primi 8 sono i campi base
        for (int i = 0; i < 8; i++) {
            asserisci(ds.getConfigurazioneSistema().haCampoBase(tutti.get(i).getNome()), 
                    "Campo " + i + " deve essere un campo base");
        }

        // 9 e 10 sono campi comuni
        asserisci(tutti.get(8).getNome().equals("Note"), "Campo 9 comune: Note");
        asserisci(tutti.get(9).getNome().equals("Durata"), "Campo 10 comune: Durata");

        // 11 è il campo specifico
        asserisci(tutti.get(10).getNome().equals("Certificato"), "Campo 11 specifico: Certificato");
    }

    // -------------------------------------------------------------------------------------
    // 11. GESTORE UTENTI E PRIMO ACCESSO
    // -------------------------------------------------------------------------------------
    private static void testGestoreUtentiEPrimoAccesso() {
        GestoreUtenti gu = new GestoreUtenti();

        // 1. Login iniziale con credenziali di fabbrica
        Utente uDefault = gu.login("config", "config");
        asserisci(uDefault != null, "Login con config/config deve avere successo");
        asserisci(uDefault instanceof Configuratore, "L'utente deve essere un Configuratore");
        Configuratore cDefault = (Configuratore) uDefault;
        asserisci(cDefault.isUsaCredenzialiPredefinite(), "Deve avere il flag credenziali predefinite = true");

        // 2. Credenziali errate
        asserisci(gu.login("config", "wrong") == null, "Login con password errata deve fallire");
        asserisci(gu.login("sconosciuto", "config") == null, "Login con username inesistente deve fallire");

        // 3. Cambio credenziali: rifiuto riutilizzo 'config'
        Configuratore cFallito = gu.cambiaCredenziali(cDefault, "config", "nuovaPass");
        asserisci(cFallito == null, "Non deve permettere di scegliere di nuovo 'config'");

        // 4. Cambio credenziali valido
        Configuratore cValido = gu.cambiaCredenziali(cDefault, "giovanni", "segreta123");
        asserisci(cValido != null, "Cambio credenziali deve riuscire");
        asserisci(cValido.getUsername().equals("giovanni"), "Username personale aggiornato");
        asserisci(!cValido.isUsaCredenzialiPredefinite(), "Flag credenziali predefinite deve essere false");

        // 5. Login con le nuove credenziali
        Utente uNuovo = gu.login("GIOVANNI", "segreta123");
        asserisci(uNuovo != null, "Login con nuovo username case-insensitive deve riuscire");

        // 6. Tentativo di altro utente di riutilizzare lo username 'giovanni'
        Configuratore cSecondo = (Configuratore) gu.login("config", "config");
        Configuratore cDuplicato = gu.cambiaCredenziali(cSecondo, "giovanni", "pass2");
        asserisci(cDuplicato == null, "Deve rifiutare la scelta di uno username personale già in uso da un altro configuratore");
    }

    // -------------------------------------------------------------------------------------
    // 12. SERIALIZZAZIONE E PERSISTENZA STATO COMPLETO
    // -------------------------------------------------------------------------------------
    private static void testPersistenzaStatoCompleto() {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test_sistema_", ".dat");
            DatiSistema ds = new DatiSistema();
            fissaBaseStandard(ds.getConfigurazioneSistema());

            // Configuriamo dati
            ds.getGestoreUtenti().cambiaCredenziali(
                    (Configuratore) ds.getGestoreUtenti().login("config", "config"),
                    "superadmin", "superpass"
            );
            ds.getConfigurazioneSistema().aggiungiCampoComune(new Campo("Note", "Desc note", false, TipoCampo.STRINGA));
            Categoria sport = new Categoria("Sport", "Attività sportive");
            sport.aggiungiCampoSpecifico(new Campo("Certificato", "Desc cert", true, TipoCampo.BOOLEANO));
            ds.aggiungiCategoria(sport);

            // Salviamo su file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile.toFile()))) {
                oos.writeObject(ds);
            }

            // Ricarichiamo da file
            DatiSistema caricato;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tempFile.toFile()))) {
                caricato = (DatiSistema) ois.readObject();
            }

            // Verifichiamo tutto
            asserisci(caricato.getGestoreUtenti().login("superadmin", "superpass") != null, "Superadmin ricaricato");
            asserisci(caricato.getConfigurazioneSistema().getCampiBase().size() == 8, "8 campi base preservati");
            asserisci(caricato.getConfigurazioneSistema().getCampiComuni().size() == 1, "1 campo comune preservato");
            asserisci(caricato.getCategorie().size() == 1, "1 categoria preservata");
            Categoria sportCaricato = caricato.getCategoria("Sport");
            asserisci(sportCaricato != null, "Categoria Sport presente");
            asserisci(sportCaricato.getCampiSpecifici().size() == 1, "1 campo specifico preservato");
            asserisci(sportCaricato.getCampiSpecifici().get(0).getTipo() == TipoCampo.BOOLEANO, "Tipo BOOLEANO preservato");

        } catch (Exception e) {
            throw new RuntimeException("Errore durante il test di persistenza", e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // 13. SIMULAZIONE INTERATTIVA END-TO-END DEI MENU (E IMPOSTAZIONE CAMPI BASE)
    // -------------------------------------------------------------------------------------
    private static void testSimulazioneInterattivaMenuE2E() {
        // Sequenza di input che esegue:
        // 1. Login errato -> login giusto (config/config)
        // 2. Cambio credenziali: inserisce 'config' (rifiutato) -> poi 'collaudatore' / 'passCollaudo'
        // 3. Impostazione dei Campi Base al primo avvio (numero e nomi decisi dal configuratore):
        //    Titolo (duplicato 'titolo' rifiutato), Data, un campo scartato, Luogo
        //    (3 campi base fissati)
        // 4. Menu 1: Visualizza campi base (immutabili)
        // 5. Menu 2: Campi comuni:
        //    - 1: elenca (vuoto)
        //    - 2: prova 'Titolo' (rifiutato)
        //    - 2: aggiunge 'Note' (Stringa=1, Facoltativo=N)
        //    - 2: aggiunge 'Durata' (Intero=2, Facoltativo=N)
        //    - 4: modifica obbligatorietà 'Note' a S (senza cancellazione)
        //    - 3: rimuove 'Durata'
        //    - 0: torna indietro
        // 6. Menu 3: Categorie:
        //    - 1: elenca (vuoto)
        //    - 2: crea 'Sport' con campo 'Certificato' (Booleano=6, Obbligatorio=S), fine campi=N
        //    - 2: prova a ricreare 'sport' (rifiutata)
        //    - 4: prova ad aggiungere campo 'Note' a 'Sport' (rifiutato perché comune)
        //    - 4: aggiunge campo 'Livello' a 'Sport' (Stringa=1, Facoltativo=N)
        //    - 6: modifica obbligatorietà 'Livello' a S
        //    - 5: rimuove campo 'Livello' da 'Sport'
        //    - 0: torna indietro
        // 7. Menu 4: Visualizzazione configurazione completa
        // 8. Menu 0: Esci

        String inputSimulato = String.join(System.lineSeparator(),
                "config", "passwordErrata",          // Login fallito
                "config", "config",                  // Login riuscito
                "config", "pass",                    // Nuovo username 'config' (rifiutato)
                "collaudatore", "passCollaudo",      // Nuovo username accettato
                // Compilazione degli 8 campi base al primo avvio con controlli di formato e retry:
                "Festa di Inizio Anno",              // Ruolo 1 [Titolo]: stringa valida
                "non_un_numero",                     // Ruolo 2 [Numero di partecipanti]: formato errato! Riprova
                "25",                                // Ruolo 2: intero positivo valido
                "data_non_valida",                   // Ruolo 3 [Termine ultimo di iscrizione]: formato data errato! Riprova
                "10/11/2026",                        // Ruolo 3: data valida
                "Piazza della Loggia",               // Ruolo 4 [Luogo]: stringa valida
                "11/11/2026",                        // Ruolo 5 [Data]: non successiva di 2 giorni rispetto al termine! Riprova
                "15/11/2026",                        // Ruolo 5: data valida (>= 10/11/2026 + 2gg)
                "ora_non_valida",                    // Ruolo 6 [Ora]: formato ora errato! Riprova
                "20:30",                             // Ruolo 6: ora valida
                "non_un_importo",                    // Ruolo 7 [Quota individuale]: formato decimale errato! Riprova
                "15.50",                             // Ruolo 7: importo decimale valido
                "12/11/2026",                        // Ruolo 8 [Data conclusiva]: precede la data di inizio! Riprova
                "16/11/2026",                        // Ruolo 8: data valida (>= 15/11/2026)
                "1",                                 // Menu principale: 1 (Campi base)
                "2",                                 // Menu principale: 2 (Campi comuni)
                "1",                                 // 2.1 Elenca
                "2", "Titolo",                       // 2.2 Prova collisione campo base
                "2", "Note", "Note desc", "1", "N",  // 2.2 Aggiunge Note
                "2", "Durata", "Ore", "2", "N",      // 2.2 Aggiunge Durata
                "4", "Note", "S",                    // 2.4 Modifica obbligatorietà Note
                "3", "Durata",                       // 2.3 Rimuove Durata
                "0",                                 // 2.0 Torna indietro
                "3",                                 // Menu principale: 3 (Categorie)
                "1",                                 // 3.1 Elenca
                "2", "Sport", "Desc sport", "S", "Certificato", "Idoneità", "6", "S", "N", // 3.2 Crea Sport con Certificato
                "2", "sport",                        // 3.2 Duplicato rifiutato
                "4", "Sport", "Note",                // 3.4 Collisione campo comune
                "4", "Sport", "Livello", "Difficolta", "1", "N", // 3.4 Aggiunge Livello
                "6", "Sport", "Livello", "S",        // 3.6 Modifica obbligatorietà Livello
                "5", "Sport", "Livello",             // 3.5 Rimuove Livello
                "0",                                 // 3.0 Torna indietro
                "4",                                 // Menu principale: 4 (Visualizza tutto)
                "0"                                  // Menu principale: 0 (Esci)
        ) + System.lineSeparator();

        InputStream stdinOriginale = System.in;
        PrintStream stdoutOriginale = System.out;

        Path fileDatiPath = Paths.get("dati_sistema.dat");
        Path backupPath = Paths.get("dati_sistema.dat.bak_test");
        boolean backupEsistente = false;

        try {
            if (Files.exists(fileDatiPath)) {
                Files.copy(fileDatiPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                Files.delete(fileDatiPath);
                backupEsistente = true;
            }

            ByteArrayOutputStream bufferOutput = new ByteArrayOutputStream();
            System.setIn(new ByteArrayInputStream(inputSimulato.getBytes()));
            System.setOut(new PrintStream(bufferOutput));

            // Eseguiamo il Main
            Main.main(new String[]{});

            String output = bufferOutput.toString();

            asserisci(output.contains("Credenziali errate. Riprova."), "Rilevamento login errato");
            asserisci(output.contains("è già in uso o non valido"), "Rilevamento rifiuto 'config' come nuovo username");
            asserisci(output.contains("Credenziali personali registrate con successo!"), "Registrazione collaudatore");
            asserisci(output.contains("CONFIGURAZIONE INIZIALE DEI CAMPI BASE (PRIMO AVVIO)"), "Richiesta campi base al primo avvio");
            asserisci(output.contains("Campi base fissati e salvati in modo persistente con successo!"), "Salvataggio campi base");
            asserisci(output.contains("Attenzione: il dato inserito non è nel formato corretto. È richiesto un numero intero"), "Rilevamento formato errato intero e retry");
            asserisci(output.contains("Attenzione: formato data non valido"), "Rilevamento formato errato data e retry");
            asserisci(output.contains("successiva di almeno due giorni"), "Rilevamento vincolo 2 giorni tra iscrizione e data");
            asserisci(output.contains("Attenzione: formato ora non valido"), "Rilevamento formato errato ora e retry");
            asserisci(output.contains("non può precedere cronologicamente la data di inizio"), "Rilevamento vincolo data conclusiva");
            asserisci(output.contains("1. Titolo"), "Riepilogo contiene Titolo");
            asserisci(output.contains("8. Data conclusiva"), "Riepilogo contiene Data conclusiva (8 campi totali)");
            asserisci(output.contains("ELENCO CAMPI BASE (CONDIVISI E IMMUTABILI)"), "Visualizzazione campi base");
            asserisci(output.contains("coincide con un campo base già esistente"), "Collisione campo comune con campo base");
            asserisci(output.contains("ELENCO CAMPI BASE (CONDIVISI E IMMUTABILI)"), "Visualizzazione campi base");
            asserisci(output.contains("coincide con un campo base già esistente"), "Collisione campo comune con campo base");
            asserisci(output.contains("Campo comune 'Note' aggiunto con successo."), "Aggiunta campo comune");
            asserisci(output.contains("Campo comune 'Durata' rimosso con successo."), "Rimozione campo comune");
            asserisci(output.contains("esiste già una categoria con il nome 'sport'"), "Rifiuto duplicato categoria");
            asserisci(output.contains("è già utilizzato in questa categoria (come campo base, comune o specifico)"), "Rifiuto collisione campo specifico con campo comune");
            asserisci(output.contains("Campo specifico 'Livello' rimosso con successo"), "Rimozione campo specifico");
            asserisci(output.contains("CONFIGURAZIONE COMPLESSIVA SISTEMA"), "Report finale globale");
            asserisci(output.contains("Dati del sistema salvati correttamente in modo persistente. Arrivederci!"), "Uscita pulita con salvataggio");

        } catch (IOException e) {
            throw new RuntimeException("Errore I/O durante backup dati per test simulazione", e);
        } finally {
            System.setIn(stdinOriginale);
            System.setOut(stdoutOriginale);
            try {
                if (backupEsistente) {
                    Files.move(backupPath, fileDatiPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.deleteIfExists(fileDatiPath);
                }
            } catch (IOException ignored) {}
        }
    }

    // -------------------------------------------------------------------------------------
    // 14. ARCHITETTURA MVC: OPERAZIONI TRAMITE CONFIGURAZIONECONTROLLER
    // -------------------------------------------------------------------------------------
    private static void testConfigurazioneController() {
        DatiSistema ds = new DatiSistema();
        ConfigurazioneController ctrl = new ConfigurazioneController(ds);

        // Fissaggio campi base via controller (il configuratore decide numero e nomi)
        asserisci(!ctrl.isCampiBaseFissati(), "All'inizio i campi base non sono fissati");
        asserisci(ctrl.fissaCampiBase(campiBaseStandard()), "Fissaggio dei campi base");
        asserisci(ctrl.isCampiBaseFissati(), "Campi base fissati");
        asserisci(ctrl.getCampiBase().size() == 8, "8 campi base via controller");

        // Campi Comuni
        boolean aggComune = ctrl.aggiungiCampoComune("CostoExtra", "Dettaglio", false, TipoCampo.DECIMALE);
        asserisci(aggComune, "Aggiunta campo comune via controller");
        asserisci(ctrl.haCampoComune("CostoExtra"), "haCampoComune via controller");
        asserisci(ctrl.modificaObbligatorietaCampoComune("CostoExtra", true), "Modifica obbligatorietà comune via controller");
        asserisci(ctrl.getCampoComune("CostoExtra").isObbligatorio(), "Obbligatorietà aggiornata");

        // Categorie
        asserisci(ctrl.creaCategoria("Teatro", "Spettacoli teatrali"), "Creazione categoria via controller");
        asserisci(ctrl.esisteCategoria("Teatro"), "Categoria esiste via controller");
        asserisci(!ctrl.creaCategoria("teatro", "Duplicato"), "Rifiuto duplicato categoria");

        // Campi Specifici
        asserisci(ctrl.aggiungiCampoSpecifico("Teatro", "Posto", "Fila e numero", true, TipoCampo.STRINGA), "Aggiunta campo specifico via controller");
        asserisci(ctrl.isNomeCampoPresenteInCategoria(ctrl.getCategoria("Teatro"), "Posto"), "Presenza campo specifico verificata");
        // Verifica che non si possa aggiungere un campo comune con nome già usato come specifico in una categoria
        asserisci(!ctrl.aggiungiCampoComune("posto", "Collisione con specifico", false, TipoCampo.STRINGA), "Rifiuto campo comune omonimo a campo specifico esistente");
        asserisci(ctrl.modificaObbligatorietaCampoSpecifico("Teatro", "Posto", false), "Modifica obbligatorietà campo specifico");
        asserisci(!ctrl.getCategoria("Teatro").getCampoSpecifico("Posto").isObbligatorio(), "Posto diventato facoltativo");

        // Scheda completa
        List<Campo> scheda = ctrl.getTuttiICampiCategoria(ctrl.getCategoria("Teatro"));
        // 8 base + 1 comune (CostoExtra) + 1 specifico (Posto) = 10
        asserisci(scheda.size() == 10, "Scheda Teatro ha 10 campi, trovati: " + scheda.size());

        // Rimozioni
        asserisci(ctrl.rimuoviCampoSpecifico("Teatro", "Posto"), "Rimozione campo specifico via controller");
        asserisci(ctrl.rimuoviCategoria("Teatro"), "Rimozione categoria via controller");
        asserisci(!ctrl.esisteCategoria("Teatro"), "Categoria Teatro non esiste più");
    }

    // -------------------------------------------------------------------------------------
    // 15. DESIGN BY CONTRACT: INVARIANTI DI CLASSE FORMALI
    // -------------------------------------------------------------------------------------
    private static void testDesignByContractInvarianti() {
        // Campo
        Campo campo = new Campo("Nome", "Desc", true, TipoCampo.STRINGA);
        asserisci(campo.checkInvariant(), "Invariante Campo valido");

        // Categoria
        Categoria cat = new Categoria("Sport", "Desc");
        cat.aggiungiCampoSpecifico(campo);
        asserisci(cat.checkInvariant(), "Invariante Categoria valido");

        // ConfigurazioneSistema
        ConfigurazioneSistema cs = new ConfigurazioneSistema();
        fissaBaseStandard(cs);
        asserisci(cs.checkInvariant(), "Invariante ConfigurazioneSistema valido");

        // GestoreUtenti
        GestoreUtenti gu = new GestoreUtenti();
        asserisci(gu.checkInvariant(), "Invariante GestoreUtenti valido");

        // DatiSistema
        DatiSistema ds = new DatiSistema();
        fissaBaseStandard(ds.getConfigurazioneSistema());
        ds.aggiungiCategoria(cat);
        asserisci(ds.checkInvariant(), "Invariante DatiSistema valido");
    }

    // -------------------------------------------------------------------------------------
    // 16. CAMPI BASE: 8 RUOLI VINCOLATI DA SPECIFICA
    // -------------------------------------------------------------------------------------
    private static void testCampiBaseDefinitiDalConfiguratore() {
        ConfigurazioneSistema cs = new ConfigurazioneSistema();
        asserisci(!cs.isCampiBaseImpostati(), "All'inizio i campi base non sono fissati");
        asserisci(cs.getCampiBase().isEmpty(), "All'inizio non esistono campi base");
        asserisci(cs.checkInvariant(), "Invariante valido a campi base non fissati");

        // Elenchi non validi rifiutati
        asserisci(!cs.fissaCampiBase(null), "Elenco nullo rifiutato");
        asserisci(!cs.fissaCampiBase(List.of()), "Elenco vuoto rifiutato");

        // Rifiuto numero di campi diverso da 8 (es. solo 3 campi)
        List<Campo> tre = List.of(
                new Campo("Titolo", "", true, TipoCampo.STRINGA),
                new Campo("Data", "", true, TipoCampo.DATA),
                new Campo("Luogo", "", true, TipoCampo.STRINGA));
        asserisci(!cs.fissaCampiBase(tre), "Elenco con numero diverso da 8 campi base rifiutato");

        // Rifiuto campi base facoltativi (anche se sono 8)
        List<Campo> ottoConUnoFacoltativo = List.of(
                new Campo("Titolo", "", true, TipoCampo.STRINGA),
                new Campo("Numero di partecipanti", "", false, TipoCampo.INTERO), // facoltativo!
                new Campo("Termine ultimo di iscrizione", "", true, TipoCampo.DATA),
                new Campo("Luogo", "", true, TipoCampo.STRINGA),
                new Campo("Data", "", true, TipoCampo.DATA),
                new Campo("Ora", "", true, TipoCampo.ORA),
                new Campo("Quota individuale", "", true, TipoCampo.DECIMALE),
                new Campo("Data conclusiva", "", true, TipoCampo.DATA)
        );
        asserisci(!cs.fissaCampiBase(ottoConUnoFacoltativo), "Campo base facoltativo rifiutato");

        // Rifiuto campi con nomi duplicati
        List<Campo> ottoConDuplicato = List.of(
                new Campo("Titolo", "", true, TipoCampo.STRINGA),
                new Campo("titolo", "", true, TipoCampo.INTERO), // duplicato!
                new Campo("Termine ultimo di iscrizione", "", true, TipoCampo.DATA),
                new Campo("Luogo", "", true, TipoCampo.STRINGA),
                new Campo("Data", "", true, TipoCampo.DATA),
                new Campo("Ora", "", true, TipoCampo.ORA),
                new Campo("Quota individuale", "", true, TipoCampo.DECIMALE),
                new Campo("Data conclusiva", "", true, TipoCampo.DATA)
        );
        asserisci(!cs.fissaCampiBase(ottoConDuplicato), "Nomi duplicati (case-insensitive) rifiutati");

        // Rifiuto tipo non corrispondente al ruolo di specifica (es. Ruolo 0 Titolo passato come INTERO)
        List<Campo> ottoConTipoErrato = List.of(
                new Campo("Titolo", "", true, TipoCampo.INTERO), // deve essere STRINGA
                new Campo("Numero di partecipanti", "", true, TipoCampo.INTERO),
                new Campo("Termine ultimo di iscrizione", "", true, TipoCampo.DATA),
                new Campo("Luogo", "", true, TipoCampo.STRINGA),
                new Campo("Data", "", true, TipoCampo.DATA),
                new Campo("Ora", "", true, TipoCampo.ORA),
                new Campo("Quota individuale", "", true, TipoCampo.DECIMALE),
                new Campo("Data conclusiva", "", true, TipoCampo.DATA)
        );
        asserisci(!cs.fissaCampiBase(ottoConTipoErrato), "Tipo incongruente con il ruolo previsto dalla specifica rifiutato");

        asserisci(!cs.isCampiBaseImpostati(), "Dopo i rifiuti i campi base non risultano fissati");

        // Collisione con un campo comune già esistente
        asserisci(cs.aggiungiCampoComune(new Campo("Note", "", false, TipoCampo.STRINGA)), "Campo comune creato prima dei campi base");
        List<Campo> ottoConCollisioneComune = List.of(
                new Campo("Titolo", "", true, TipoCampo.STRINGA),
                new Campo("Numero di partecipanti", "", true, TipoCampo.INTERO),
                new Campo("Termine ultimo di iscrizione", "", true, TipoCampo.DATA),
                new Campo("Note", "", true, TipoCampo.STRINGA), // collisione con campo comune Note!
                new Campo("Data", "", true, TipoCampo.DATA),
                new Campo("Ora", "", true, TipoCampo.ORA),
                new Campo("Quota individuale", "", true, TipoCampo.DECIMALE),
                new Campo("Data conclusiva", "", true, TipoCampo.DATA)
        );
        asserisci(!cs.fissaCampiBase(ottoConCollisioneComune), "Campo base omonimo di un campo comune rifiutato");

        // Fissaggio corretto degli 8 campi base di specifica
        asserisci(cs.fissaCampiBase(campiBaseStandard()), "Fissaggio dei corretti 8 campi base riuscito");
        asserisci(cs.isCampiBaseImpostati() && cs.getCampiBase().size() == 8, "Fissati esattamente gli 8 campi previsti");
        asserisci(cs.checkInvariant(), "Invariante valido dopo il fissaggio");

        // Una volta fissati sono immutabili (secondo fissaggio rifiutato)
        asserisci(!cs.fissaCampiBase(campiBaseStandard()), "Secondo fissaggio rifiutato");
        asserisci(cs.getCampiBase().size() == 8, "I campi base non sono stati alterati");
        boolean exc = false;
        try {
            cs.getCampiBase().add(new Campo("X", "", true, TipoCampo.STRINGA));
        } catch (UnsupportedOperationException e) {
            exc = true;
        }
        asserisci(exc, "La lista dei campi base non è modificabile dall'esterno");

        // Dopo il fissaggio i campi comuni non possono collidere con i campi base scelti
        asserisci(!cs.aggiungiCampoComune(new Campo("DATA", "", false, TipoCampo.STRINGA)),
                "Campo comune omonimo di un campo base rifiutato");
    }
}
