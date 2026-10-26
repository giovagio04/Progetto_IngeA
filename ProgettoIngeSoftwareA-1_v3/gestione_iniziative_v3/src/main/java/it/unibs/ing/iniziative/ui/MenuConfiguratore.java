package it.unibs.ing.iniziative.ui;

import it.unibs.ing.iniziative.controller.ConfigurazioneController;
import it.unibs.ing.iniziative.model.Bacheca;
import it.unibs.ing.iniziative.model.Campo;
import it.unibs.ing.iniziative.model.Categoria;
import it.unibs.ing.iniziative.model.Configuratore;
import it.unibs.ing.iniziative.model.Proposta;
import it.unibs.ing.iniziative.model.RuoloCampoBase;
import it.unibs.ing.iniziative.model.TipoCampo;
import it.unibs.ing.iniziative.model.ValoreCampo;
import it.unibs.ing.iniziative.storage.DatiSistema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce l'interfaccia a riga di comando (Presentation Layer) per il configuratore nella Versione 3.
 * Costruisce direttamente sulla base di V1 e V2, delegando la logica al {@link ConfigurazioneController}
 * (Architettura MVC), utilizzando {@link MyMenu} e {@link InputDati} (libreria Kibo) e aggiungendo
 * la consultazione dell'Archivio Proposte storico (stati assunti, date e aderenti).
 */
public class MenuConfiguratore {

    private final ConfigurazioneController controller;
    private final Configuratore autore;

    public MenuConfiguratore(ConfigurazioneController controller, Configuratore autore) {
        assert controller != null : "Precondizione violata: il controller non può essere nullo";
        assert autore != null : "Precondizione violata: l'autore non può essere nullo";
        this.controller = controller;
        this.autore = autore;
    }

    public MenuConfiguratore(DatiSistema dati, Configuratore autore) {
        this(new ConfigurazioneController(dati), autore);
    }

    /**
     * Procedura guidata di primo avvio (ereditata da V1 e V2): il configuratore compila gli 8 campi base
     * previsti dalla specifica con controllo rigoroso del formato e retry in caso di errore.
     */
    public void eseguiImpostazioneManualeCampiBase() {
        System.out.println("\n=======================================================");
        System.out.println("   CONFIGURAZIONE INIZIALE DEI CAMPI BASE (PRIMO AVVIO) ");
        System.out.println("=======================================================");
        System.out.println("Al primo avvio il configuratore compila gli 8 campi base obbligatori");
        System.out.println("e condivisi da tutte le categorie previsti dalla specifica.");
        System.out.println("Attenzione: una volta fissati, i campi base saranno IMMUTABILI per sempre.\n");

        List<Campo> campi = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        // 1. TITOLO (Stringa)
        System.out.println("[1/8] Campo base: " + RuoloCampoBase.TITOLO.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.TITOLO.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.TITOLO.getTipo());
        String titolo = InputDati.leggiStringaNonVuota("Inserisci Titolo: ").trim();
        campi.add(new Campo(RuoloCampoBase.TITOLO.getEtichetta(), RuoloCampoBase.TITOLO.getDescrizione(), true, TipoCampo.STRINGA, titolo));
        System.out.println("-> Campo base 'Titolo' registrato: " + titolo);

        // 2. NUMERO DI PARTECIPANTI (Intero > 0)
        System.out.println("\n[2/8] Campo base: " + RuoloCampoBase.NUMERO_PARTECIPANTI.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.NUMERO_PARTECIPANTI.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.NUMERO_PARTECIPANTI.getTipo());
        int numPartecipanti = InputDati.leggiInteroPositivo("Inserisci Numero di partecipanti (Intero > 0): ");
        campi.add(new Campo(RuoloCampoBase.NUMERO_PARTECIPANTI.getEtichetta(), RuoloCampoBase.NUMERO_PARTECIPANTI.getDescrizione(), true, TipoCampo.INTERO, String.valueOf(numPartecipanti)));
        System.out.println("-> Campo base 'Numero di partecipanti' registrato: " + numPartecipanti);

        // 3. TERMINE ULTIMO DI ISCRIZIONE (Data gg/mm/aaaa)
        System.out.println("\n[3/8] Campo base: " + RuoloCampoBase.TERMINE_ISCRIZIONE.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.TERMINE_ISCRIZIONE.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.TERMINE_ISCRIZIONE.getTipo());
        LocalDate termineIscrizione = InputDati.leggiData("Inserisci Termine ultimo di iscrizione (gg/mm/aaaa): ");
        String termineStr = termineIscrizione.format(df);
        campi.add(new Campo(RuoloCampoBase.TERMINE_ISCRIZIONE.getEtichetta(), RuoloCampoBase.TERMINE_ISCRIZIONE.getDescrizione(), true, TipoCampo.DATA, termineStr));
        System.out.println("-> Campo base 'Termine ultimo di iscrizione' registrato: " + termineStr);

        // 4. LUOGO (Stringa)
        System.out.println("\n[4/8] Campo base: " + RuoloCampoBase.LUOGO.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.LUOGO.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.LUOGO.getTipo());
        String luogo = InputDati.leggiStringaNonVuota("Inserisci Luogo: ").trim();
        campi.add(new Campo(RuoloCampoBase.LUOGO.getEtichetta(), RuoloCampoBase.LUOGO.getDescrizione(), true, TipoCampo.STRINGA, luogo));
        System.out.println("-> Campo base 'Luogo' registrato: " + luogo);

        // 5. DATA (Data gg/mm/aaaa, successiva di almeno 2 giorni rispetto a termine iscrizione)
        System.out.println("\n[5/8] Campo base: " + RuoloCampoBase.DATA.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.DATA.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.DATA.getTipo());
        LocalDate dataInizio;
        while (true) {
            dataInizio = InputDati.leggiData("Inserisci Data (gg/mm/aaaa): ");
            if (dataInizio.isAfter(termineIscrizione.plusDays(1))) {
                break;
            }
            System.out.println("Attenzione: la data deve essere successiva di almeno due giorni rispetto al termine ultimo di iscrizione (" + termineStr + "). Riprova.");
        }
        String dataStr = dataInizio.format(df);
        campi.add(new Campo(RuoloCampoBase.DATA.getEtichetta(), RuoloCampoBase.DATA.getDescrizione(), true, TipoCampo.DATA, dataStr));
        System.out.println("-> Campo base 'Data' registrato: " + dataStr);

        // 6. ORA (Ora hh:mm)
        System.out.println("\n[6/8] Campo base: " + RuoloCampoBase.ORA.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.ORA.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.ORA.getTipo());
        LocalTime ora = InputDati.leggiOra("Inserisci Ora (hh:mm): ");
        String oraStr = ora.format(tf);
        campi.add(new Campo(RuoloCampoBase.ORA.getEtichetta(), RuoloCampoBase.ORA.getDescrizione(), true, TipoCampo.ORA, oraStr));
        System.out.println("-> Campo base 'Ora' registrato: " + oraStr);

        // 7. QUOTA INDIVIDUALE (Decimale >= 0)
        System.out.println("\n[7/8] Campo base: " + RuoloCampoBase.QUOTA_INDIVIDUALE.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.QUOTA_INDIVIDUALE.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.QUOTA_INDIVIDUALE.getTipo());
        double quota = InputDati.leggiDoubleNonNegativo("Inserisci Quota individuale (es. 10.50 o 0): ");
        String quotaStr = String.format(java.util.Locale.US, "%.2f", quota);
        campi.add(new Campo(RuoloCampoBase.QUOTA_INDIVIDUALE.getEtichetta(), RuoloCampoBase.QUOTA_INDIVIDUALE.getDescrizione(), true, TipoCampo.DECIMALE, quotaStr));
        System.out.println("-> Campo base 'Quota individuale' registrato: " + quotaStr);

        // 8. DATA CONCLUSIVA (Data gg/mm/aaaa, >= Data)
        System.out.println("\n[8/8] Campo base: " + RuoloCampoBase.DATA_CONCLUSIVA.getEtichetta());
        System.out.println("Significato: " + RuoloCampoBase.DATA_CONCLUSIVA.getDescrizione());
        System.out.println("Tipo di dato: " + RuoloCampoBase.DATA_CONCLUSIVA.getTipo());
        LocalDate dataConclusiva;
        while (true) {
            dataConclusiva = InputDati.leggiData("Inserisci Data conclusiva (gg/mm/aaaa): ");
            if (!dataConclusiva.isBefore(dataInizio)) {
                break;
            }
            System.out.println("Attenzione: la data conclusiva non può precedere cronologicamente la data di inizio (" + dataStr + "). Riprova.");
        }
        String dataConcStr = dataConclusiva.format(df);
        campi.add(new Campo(RuoloCampoBase.DATA_CONCLUSIVA.getEtichetta(), RuoloCampoBase.DATA_CONCLUSIVA.getDescrizione(), true, TipoCampo.DATA, dataConcStr));
        System.out.println("-> Campo base 'Data conclusiva' registrato: " + dataConcStr);

        System.out.println("\n--- RIEPILOGO DEI CAMPI BASE COMPILATI ---");
        for (int i = 0; i < campi.size(); i++) {
            System.out.println((i + 1) + ". " + campi.get(i));
        }

        if (controller.fissaCampiBase(campi)) {
            try {
                controller.salvaDati();
                System.out.println("\nCampi base fissati e salvati in modo persistente con successo!\n");
            } catch (Exception e) {
                System.out.println("\nAttenzione: errore nel salvataggio dei campi base: " + e.getMessage());
            }
        } else {
            System.out.println("\nErrore durante il fissaggio dei campi base.");
        }
    }

    /**
     * Avvia il menu principale del configuratore per la Versione 3 tramite MyMenu (libreria Kibo).
     */
    public void esegui() {
        boolean esci = false;
        while (!esci) {
            controller.aggiornaStatiEScadenze(LocalDate.now());
            int inAttesa = controller.getProposteValideInSessione().size();
            String[] voci = {
                "Gestione Categorie e Campi (Base, Comuni, Specifici)",
                "Crea nuova Proposta di Iniziativa",
                "Gestisci Proposte di Sessione e Pubblica in Bacheca (" + inAttesa + " in attesa)",
                "Visualizza Bacheca delle Proposte Aperte",
                "Visualizza Configurazione Globale Campi e Categorie",
                "Visualizza Archivio Proposte (Storico Stati e Aderenti)"
            };
            MyMenu menu = new MyMenu("MENU CONFIGURATORE (VERSIONE 3) - Utente: " + autore.getUsername(), voci);
            int scelta = menu.scegli();
            switch (scelta) {
                case 1:
                    menuGestioneStruttura();
                    break;
                case 2:
                    creaNuovaProposta();
                    break;
                case 3:
                    gestisciProposteSessione();
                    break;
                case 4:
                    visualizzaBachecaInterattiva();
                    break;
                case 5:
                    visualizzaConfigurazioneCompleta();
                    break;
                case 6:
                    visualizzaArchivioInterattivo();
                    break;
                case 0:
                    if (!controller.getProposteValideInSessione().isEmpty()) {
                        System.out.println("\n[AVVISO DI SPECIFICA] Sono presenti " + controller.getProposteValideInSessione().size() + 
                                " proposta/e valida/e non pubblicata/e in bacheca.");
                        System.out.println("Al termine della sessione, le proposte non pubblicate NON vengono salvate.");
                    }
                    esci = true;
                    break;
            }
        }
    }

    // =========================================================================
    // 6. VISUALIZZAZIONE ARCHIVIO STORICO DELLE PROPOSTE (V3)
    // =========================================================================
    private void visualizzaArchivioInterattivo() {
        controller.getArchivioProposte().visualizzaArchivio();
        if (controller.getArchivioProposte().isVuoto()) return;

        boolean dettaglio = InputDati.yesOrNo("Desideri visualizzare la scheda completa di una proposta in archivio?");
        while (dettaglio) {
            List<Proposta> tutte = controller.getArchivioProposte().getProposte();
            int sc = InputDati.leggiIntero("Seleziona proposta [1-" + tutte.size() + ", 0 per annullare]: ", 0, tutte.size());
            if (sc > 0) {
                System.out.println("\n" + tutte.get(sc - 1).schedaDettagliata());
            }
            dettaglio = InputDati.yesOrNo("Desideri esaminare un'altra proposta dell'archivio?");
        }
    }

    // =========================================================================
    // 2. CREAZIONE NUOVA PROPOSTA DI INIZIATIVA (V2)
    // =========================================================================
    private void creaNuovaProposta() {
        System.out.println("\n--- CREAZIONE DI UNA NUOVA PROPOSTA DI INIZIATIVA ---");
        List<Categoria> categorie = controller.getCategorie();
        if (categorie.isEmpty()) {
            System.out.println("Nessuna categoria disponibile nel sistema.");
            System.out.println("È necessario definire almeno una categoria (opzione 1) prima di poter proporre iniziative.");
            return;
        }

        System.out.println("Seleziona la categoria di appartenenza dell'iniziativa:");
        for (int i = 0; i < categorie.size(); i++) {
            System.out.println((i + 1) + ". " + categorie.get(i).getNome() + 
                    (!categorie.get(i).getDescrizione().isEmpty() ? " (" + categorie.get(i).getDescrizione() + ")" : ""));
        }
        int sceltaCat = InputDati.leggiIntero("Categoria [1-" + categorie.size() + "]: ", 1, categorie.size());
        Categoria catScelta = categorie.get(sceltaCat - 1);

        List<Campo> tuttiCampi = controller.getTuttiICampiCategoria(catScelta);
        System.out.println("\nStai creando una proposta per la categoria: " + catScelta.getNome().toUpperCase());
        System.out.println("Verranno richiesti i valori per i " + tuttiCampi.size() + " campi previsti:");
        System.out.println(" - " + controller.getCampiBase().size() + " Campi Base (comuni e obbligatori)");
        System.out.println(" - " + controller.getCampiComuni().size() + " Campi Comuni");
        System.out.println(" - " + catScelta.getCampiSpecifici().size() + " Campi Specifici");
        System.out.println("-----------------------------------------------------------------");

        Proposta nuovaProposta = new Proposta(catScelta, autore);

        for (Campo c : tuttiCampi) {
            Object valore = InputDati.leggiValorePerCampo(c);
            nuovaProposta.assegnaValore(c, valore);
        }

        System.out.println("\nVerifica dei vincoli di validità in corso...");
        List<String> errori = controller.validaERegistraPropostaInSessione(nuovaProposta, LocalDate.now());

        while (!errori.isEmpty()) {
            System.out.println("\n[ATTENZIONE] La proposta NON rispetta tutti i vincoli stabiliti:");
            for (String err : errori) {
                System.out.println(" X " + err);
            }
            boolean correggi = InputDati.yesOrNo("Desideri correggere uno o più campi per rendere valida la proposta?");
            if (!correggi) {
                System.out.println("La proposta non è valida e non è stata registrata.");
                return;
            }
            System.out.println("\nSeleziona il campo da correggere:");
            for (int i = 0; i < tuttiCampi.size(); i++) {
                Campo c = tuttiCampi.get(i);
                ValoreCampo vc = nuovaProposta.getValoreCampo(c.getNome());
                System.out.println((i + 1) + ". " + c.getNome() + " (Attuale: " + (vc != null ? vc.getValoreFormattato() : "N/D") + ")");
            }
            int scCampo = InputDati.leggiIntero("Campo da correggere [1-" + tuttiCampi.size() + ", 0 per rivalutare]: ", 0, tuttiCampi.size());
            if (scCampo > 0) {
                Campo c = tuttiCampi.get(scCampo - 1);
                Object nuovoVal = InputDati.leggiValorePerCampo(c);
                nuovaProposta.assegnaValore(c, nuovoVal);
            }
            errori = controller.validaERegistraPropostaInSessione(nuovaProposta, LocalDate.now());
        }

        System.out.println("\n[SUCCESSO] La proposta rispetta tutti i vincoli ed è nello stato: VALIDA!");

        boolean pubblicaSubito = InputDati.yesOrNo("Desideri pubblicare subito questa proposta in bacheca?");
        if (pubblicaSubito) {
            if (controller.pubblicaPropostaInBacheca(nuovaProposta, LocalDate.now())) {
                salvaStatoPersistente();
                System.out.println("Proposta pubblicata con successo in bacheca nello stato APERTA!");
                System.out.println("Data di pubblicazione registrata: " + nuovaProposta.getDataPubblicazione().format(ValoreCampo.FORMATTER_DATA));
            } else {
                System.out.println("Errore: impossibile pubblicare la proposta in bacheca.");
            }
        } else {
            System.out.println("Proposta memorizzata nella sessione corrente.");
            System.out.println("Potrai pubblicarla in bacheca in un secondo momento tramite l'opzione 3.");
        }
    }

    // =========================================================================
    // 3. GESTIONE PROPOSTE DI SESSIONE E PUBBLICAZIONE (V2)
    // =========================================================================
    private void gestisciProposteSessione() {
        System.out.println("\n--- PROPOSTE VALIDE IN SESSIONE (NON ANCORA PUBBLICATE) ---");
        List<Proposta> inSessione = controller.getProposteValideInSessione();
        if (inSessione.isEmpty()) {
            System.out.println("Nessuna proposta valida in attesa nella sessione corrente.");
            return;
        }

        for (int i = 0; i < inSessione.size(); i++) {
            Proposta p = inSessione.get(i);
            System.out.println((i + 1) + ". " + p.getTitolo() + " [Cat: " + p.getCategoria().getNome() + "]");
        }

        System.out.println("\nOpzioni:");
        System.out.println("Inserisci il numero della proposta da esaminare/pubblicare (0 per tornare indietro):");
        int sc = InputDati.leggiIntero("Scelta: ", 0, inSessione.size());
        if (sc == 0) return;

        Proposta selezionata = inSessione.get(sc - 1);
        System.out.println("\n" + selezionata.schedaDettagliata());

        boolean pubblica = InputDati.yesOrNo("Vuoi pubblicare questa proposta in bacheca (passaggio allo stato APERTA)?");
        if (pubblica) {
            if (controller.pubblicaPropostaInBacheca(selezionata, LocalDate.now())) {
                salvaStatoPersistente();
                System.out.println("Proposta pubblicata in bacheca con successo!");
                System.out.println("Data di pubblicazione: " + selezionata.getDataPubblicazione().format(ValoreCampo.FORMATTER_DATA));
            } else {
                System.out.println("Attenzione: la proposta non è più valida rispetto alla data odierna di pubblicazione.");
            }
        } else {
            System.out.println("Proposta lasciata in sessione.");
        }
    }

    // =========================================================================
    // 4. VISUALIZZAZIONE BACHECA INTERATTIVA PER CATEGORIA (V2)
    // =========================================================================
    private void visualizzaBachecaInterattiva() {
        Bacheca bacheca = controller.getBacheca();
        bacheca.visualizzaBacheca(controller.getCategorie());

        if (bacheca.isVuota()) return;

        boolean visualizzaDettaglio = InputDati.yesOrNo("Desideri visualizzare la scheda dettagliata di una proposta in bacheca?");
        while (visualizzaDettaglio) {
            List<Proposta> tutte = bacheca.getProposteAperte();
            for (int i = 0; i < tutte.size(); i++) {
                System.out.println((i + 1) + ". " + tutte.get(i).getTitolo() + " (" + tutte.get(i).getCategoria().getNome() + ")");
            }
            int sc = InputDati.leggiIntero("Seleziona proposta [1-" + tutte.size() + ", 0 per annullare]: ", 0, tutte.size());
            if (sc > 0) {
                System.out.println("\n" + tutte.get(sc - 1).schedaDettagliata());
            }
            visualizzaDettaglio = InputDati.yesOrNo("Desideri visualizzare un'altra scheda?");
        }
    }

    // =========================================================================
    // 1. SOTTOMENU GESTIONE STRUTTURA (CATEGORIE E CAMPI - V1)
    // =========================================================================
    private void menuGestioneStruttura() {
        String[] voci = {
            "Visualizza Campi Base (Immutabili)",
            "Gestisci Campi Comuni",
            "Gestisci Categorie e Campi Specifici"
        };
        MyMenu sottomenu = new MyMenu("GESTIONE STRUTTURA: CAMPI E CATEGORIE", voci);
        boolean indietro = false;
        while (!indietro) {
            int sc = sottomenu.scegli();
            switch (sc) {
                case 1:
                    visualizzaCampiBase();
                    break;
                case 2:
                    gestisciCampiComuni();
                    break;
                case 3:
                    gestisciCategorie();
                    break;
                case 0:
                    indietro = true;
                    break;
            }
        }
    }

    private void visualizzaCampiBase() {
        System.out.println("\n--- ELENCO CAMPI BASE (CONDIVISI E IMMUTABILI) ---");
        System.out.println("Nota: I campi base sono fissati al primo avvio e non modificabili.");
        List<Campo> base = controller.getCampiBase();
        for (int i = 0; i < base.size(); i++) {
            System.out.println((i + 1) + ". " + base.get(i).toString());
        }
    }

    private void gestisciCampiComuni() {
        String[] voci = {
            "Visualizza campi comuni",
            "Aggiungi nuovo campo comune",
            "Rimuovi campo comune",
            "Modifica obbligatorietà campo comune"
        };
        MyMenu sottomenu = new MyMenu("GESTIONE CAMPI COMUNI", voci);
        boolean indietro = false;
        while (!indietro) {
            int sc = sottomenu.scegli();
            switch (sc) {
                case 1:
                    elencaCampiComuni();
                    break;
                case 2:
                    aggiungiCampoComune();
                    break;
                case 3:
                    rimuoviCampoComune();
                    break;
                case 4:
                    modificaObbligatorietaCampoComune();
                    break;
                case 0:
                    indietro = true;
                    break;
            }
        }
    }

    private void elencaCampiComuni() {
        System.out.println("\n--- Campi Comuni Attualmente Configurati ---");
        List<Campo> comuni = controller.getCampiComuni();
        if (comuni.isEmpty()) {
            System.out.println("(Nessun campo comune configurato)");
        } else {
            for (int i = 0; i < comuni.size(); i++) {
                System.out.println((i + 1) + ". " + comuni.get(i).toString());
            }
        }
    }

    private void aggiungiCampoComune() {
        System.out.println("\n--- Aggiunta Campo Comune ---");
        String nome = InputDati.leggiStringaNonVuota("Nome del campo: ");

        if (controller.haCampoBase(nome)) {
            System.out.println("Errore: Il nome '" + nome + "' coincide con un campo base già esistente.");
            return;
        }
        if (controller.haCampoComune(nome)) {
            System.out.println("Errore: Un campo comune con il nome '" + nome + "' esiste già.");
            return;
        }
        if (controller.haCampoSpecificoInQualcheCategoria(nome)) {
            System.out.println("Errore: Il nome '" + nome + "' è già utilizzato come campo specifico in una categoria esistente.");
            return;
        }

        String desc = InputDati.leggiStringa("Descrizione o nota (opzionale): ");
        TipoCampo tipo = InputDati.scegliTipoCampo();
        boolean obb = InputDati.yesOrNo("Il campo deve essere obbligatorio?");

        if (controller.aggiungiCampoComune(nome, desc, obb, tipo)) {
            salvaStatoPersistente();
            System.out.println("Campo comune '" + nome + "' aggiunto con successo.");
        } else {
            System.out.println("Errore durante l'aggiunta del campo comune.");
        }
    }

    private void rimuoviCampoComune() {
        System.out.println("\n--- Rimozione Campo Comune ---");
        List<Campo> comuni = controller.getCampiComuni();
        if (comuni.isEmpty()) {
            System.out.println("Nessun campo comune disponibile da rimuovere.");
            return;
        }
        elencaCampiComuni();
        String nome = InputDati.leggiStringaNonVuota("Nome del campo comune da rimuovere: ");
        if (controller.rimuoviCampoComune(nome)) {
            salvaStatoPersistente();
            System.out.println("Campo comune '" + nome + "' rimosso con successo.");
        } else {
            System.out.println("Campo comune non trovato.");
        }
    }

    private void modificaObbligatorietaCampoComune() {
        System.out.println("\n--- Modifica Obbligatorietà Campo Comune ---");
        List<Campo> comuni = controller.getCampiComuni();
        if (comuni.isEmpty()) {
            System.out.println("Nessun campo comune configurato.");
            return;
        }
        elencaCampiComuni();
        String nome = InputDati.leggiStringaNonVuota("Nome del campo da modificare: ");
        Campo campo = controller.getCampoComune(nome);
        if (campo != null) {
            System.out.println("Stato attuale: " + (campo.isObbligatorio() ? "Obbligatorio" : "Facoltativo"));
            boolean nuovoStato = InputDati.yesOrNo("Nuovo stato: Deve essere obbligatorio?");
            controller.modificaObbligatorietaCampoComune(nome, nuovoStato);
            salvaStatoPersistente();
            System.out.println("Obbligatorietà del campo '" + campo.getNome() + "' aggiornata a: " + (nuovoStato ? "Obbligatorio" : "Facoltativo"));
        } else {
            System.out.println("Campo comune non trovato.");
        }
    }

    private void gestisciCategorie() {
        String[] voci = {
            "Visualizza elenco categorie",
            "Crea nuova categoria",
            "Rimuovi categoria",
            "Aggiungi campo specifico a una categoria",
            "Rimuovi campo specifico da una categoria",
            "Modifica obbligatorietà campo specifico"
        };
        MyMenu sottomenu = new MyMenu("GESTIONE CATEGORIE E CAMPI SPECIFICI", voci);
        boolean indietro = false;
        while (!indietro) {
            int sc = sottomenu.scegli();
            switch (sc) {
                case 1:
                    elencaCategorie();
                    break;
                case 2:
                    creaNuovaCategoria();
                    break;
                case 3:
                    rimuoviCategoria();
                    break;
                case 4:
                    aggiungiCampoSpecifico();
                    break;
                case 5:
                    rimuoviCampoSpecifico();
                    break;
                case 6:
                    modificaObbligatorietaCampoSpecifico();
                    break;
                case 0:
                    indietro = true;
                    break;
            }
        }
    }

    private void elencaCategorie() {
        System.out.println("\n--- Categorie Registrate ---");
        List<Categoria> cats = controller.getCategorie();
        if (cats.isEmpty()) {
            System.out.println("(Nessuna categoria presente)");
        } else {
            for (int i = 0; i < cats.size(); i++) {
                Categoria cat = cats.get(i);
                System.out.println((i + 1) + ". " + cat.getNome() + 
                        (!cat.getDescrizione().isEmpty() ? " (" + cat.getDescrizione() + ")" : "") +
                        " [Campi specifici: " + cat.getCampiSpecifici().size() + "]");
            }
        }
    }

    private void creaNuovaCategoria() {
        System.out.println("\n--- Creazione Nuova Categoria ---");
        String nome = InputDati.leggiStringaNonVuota("Nome della nuova categoria: ");
        if (controller.esisteCategoria(nome)) {
            System.out.println("Attenzione: esiste già una categoria con il nome '" + nome + "'.");
            return;
        }

        String desc = InputDati.leggiStringa("Descrizione della categoria (opzionale): ");
        Categoria nuovaCat = new Categoria(nome, desc);

        boolean aggiungiSubito = InputDati.yesOrNo("Desideri inserire subito dei campi specifici per questa categoria?");
        while (aggiungiSubito) {
            String nomeCampo = InputDati.leggiStringaNonVuota("Nome campo specifico: ");
            if (controller.isNomeCampoPresenteInCategoria(nuovaCat, nomeCampo)) {
                System.out.println("Attenzione: il nome '" + nomeCampo + "' è già usato come campo base, comune o specifico.");
            } else {
                String descCampo = InputDati.leggiStringa("Descrizione del campo specifico: ");
                TipoCampo tipo = InputDati.scegliTipoCampo();
                boolean obb = InputDati.yesOrNo("Il campo specifico deve essere obbligatorio?");
                nuovaCat.aggiungiCampoSpecifico(new Campo(nomeCampo, descCampo, obb, tipo));
                System.out.println("Campo specifico '" + nomeCampo + "' aggiunto alla nuova categoria.");
            }
            aggiungiSubito = InputDati.yesOrNo("Desideri aggiungere un altro campo specifico per questa categoria?");
        }

        if (controller.aggiungiCategoria(nuovaCat)) {
            salvaStatoPersistente();
            System.out.println("Categoria '" + nome + "' creata e salvata con successo!");
        } else {
            System.out.println("Errore: impossibile creare la categoria.");
        }
    }

    private void rimuoviCategoria() {
        System.out.println("\n--- Rimozione Categoria ---");
        List<Categoria> cats = controller.getCategorie();
        if (cats.isEmpty()) {
            System.out.println("Nessuna categoria presente da rimuovere.");
            return;
        }
        elencaCategorie();
        String nome = InputDati.leggiStringaNonVuota("Nome della categoria da rimuovere: ");
        if (!controller.esisteCategoria(nome)) {
            System.out.println("Categoria '" + nome + "' non trovata.");
            return;
        }

        boolean conferma = InputDati.yesOrNo("Sei sicuro di voler rimuovere la categoria '" + nome + 
                "' e tutti i suoi campi specifici?");
        if (conferma) {
            if (controller.rimuoviCategoria(nome)) {
                salvaStatoPersistente();
                System.out.println("Categoria '" + nome + "' e relativi campi specifici rimossi con successo.");
            } else {
                System.out.println("Errore durante la rimozione della categoria.");
            }
        } else {
            System.out.println("Operazione annullata.");
        }
    }

    private void aggiungiCampoSpecifico() {
        System.out.println("\n--- Aggiungi Campo Specifico a Categoria ---");
        List<Categoria> cats = controller.getCategorie();
        if (cats.isEmpty()) {
            System.out.println("Nessuna categoria presente. Creane prima una.");
            return;
        }
        elencaCategorie();
        String catNome = InputDati.leggiStringaNonVuota("Nome della categoria a cui aggiungere il campo: ");
        Categoria cat = controller.getCategoria(catNome);
        if (cat == null) {
            System.out.println("Categoria '" + catNome + "' non trovata.");
            return;
        }

        String nomeCampo = InputDati.leggiStringaNonVuota("Nome del campo specifico: ");
        if (controller.isNomeCampoPresenteInCategoria(cat, nomeCampo)) {
            System.out.println("Errore: Il nome '" + nomeCampo + "' è già utilizzato in questa categoria (come campo base, comune o specifico).");
            return;
        }

        String desc = InputDati.leggiStringa("Descrizione del campo specifico: ");
        TipoCampo tipo = InputDati.scegliTipoCampo();
        boolean obb = InputDati.yesOrNo("Il campo specifico deve essere obbligatorio?");

        if (controller.aggiungiCampoSpecifico(catNome, nomeCampo, desc, obb, tipo)) {
            salvaStatoPersistente();
            System.out.println("Campo specifico '" + nomeCampo + "' aggiunto alla categoria '" + cat.getNome() + "'.");
        } else {
            System.out.println("Errore durante l'aggiunta del campo specifico.");
        }
    }

    private void rimuoviCampoSpecifico() {
        System.out.println("\n--- Rimuovi Campo Specifico da Categoria ---");
        List<Categoria> cats = controller.getCategorie();
        if (cats.isEmpty()) {
            System.out.println("Nessuna categoria presente.");
            return;
        }
        elencaCategorie();
        String catNome = InputDati.leggiStringaNonVuota("Nome della categoria: ");
        Categoria cat = controller.getCategoria(catNome);
        if (cat == null) {
            System.out.println("Categoria non trovata.");
            return;
        }

        List<Campo> specifici = cat.getCampiSpecifici();
        if (specifici.isEmpty()) {
            System.out.println("La categoria '" + cat.getNome() + "' non possiede campi specifici.");
            return;
        }

        System.out.println("Campi specifici presenti in '" + cat.getNome() + "':");
        for (Campo c : specifici) {
            System.out.println("- " + c.toString());
        }

        String nomeCampo = InputDati.leggiStringaNonVuota("Nome del campo specifico da rimuovere: ");
        if (controller.rimuoviCampoSpecifico(catNome, nomeCampo)) {
            salvaStatoPersistente();
            System.out.println("Campo specifico '" + nomeCampo + "' rimosso con successo dalla categoria '" + cat.getNome() + "'.");
        } else {
            System.out.println("Campo specifico non trovato.");
        }
    }

    private void modificaObbligatorietaCampoSpecifico() {
        System.out.println("\n--- Modifica Obbligatorietà Campo Specifico ---");
        List<Categoria> cats = controller.getCategorie();
        if (cats.isEmpty()) {
            System.out.println("Nessuna categoria presente.");
            return;
        }
        elencaCategorie();
        String catNome = InputDati.leggiStringaNonVuota("Nome della categoria: ");
        Categoria cat = controller.getCategoria(catNome);
        if (cat == null) {
            System.out.println("Categoria non trovata.");
            return;
        }

        List<Campo> specifici = cat.getCampiSpecifici();
        if (specifici.isEmpty()) {
            System.out.println("La categoria '" + cat.getNome() + "' non possiede campi specifici.");
            return;
        }

        System.out.println("Campi specifici di '" + cat.getNome() + "':");
        for (Campo c : specifici) {
            System.out.println("- " + c.toString());
        }

        String nomeCampo = InputDati.leggiStringaNonVuota("Nome del campo specifico da modificare: ");
        Campo campo = cat.getCampoSpecifico(nomeCampo);
        if (campo != null) {
            System.out.println("Stato attuale: " + (campo.isObbligatorio() ? "Obbligatorio" : "Facoltativo"));
            boolean nuovoStato = InputDati.yesOrNo("Nuovo stato: Deve essere obbligatorio?");
            controller.modificaObbligatorietaCampoSpecifico(catNome, nomeCampo, nuovoStato);
            salvaStatoPersistente();
            System.out.println("Obbligatorietà del campo '" + campo.getNome() + "' aggiornata a: " + (nuovoStato ? "Obbligatorio" : "Facoltativo"));
        } else {
            System.out.println("Campo specifico non trovato.");
        }
    }

    // =========================================================================
    // 5. VISUALIZZAZIONE CONFIGURAZIONE COMPLETA (V1)
    // =========================================================================
    private void visualizzaConfigurazioneCompleta() {
        System.out.println("\n=======================================================");
        System.out.println("           CONFIGURAZIONE COMPLESSIVA SISTEMA          ");
        System.out.println("=======================================================");

        System.out.println("\n[1] CAMPI BASE (Comuni a tutte le categorie, Obbligatori e Immutabili):");
        List<Campo> base = controller.getCampiBase();
        for (Campo c : base) {
            System.out.println("   * " + c.toString());
        }

        System.out.println("\n[2] CAMPI COMUNI (Condivisi da tutte le categorie):");
        List<Campo> comuni = controller.getCampiComuni();
        if (comuni.isEmpty()) {
            System.out.println("   (Nessun campo comune configurato)");
        } else {
            for (Campo c : comuni) {
                System.out.println("   * " + c.getNome() + " [" + c.getTipo().getDescrizione() + ", " + 
                        (c.isObbligatorio() ? "Obbligatorio" : "Facoltativo") + "]");
                if (!c.getDescrizione().isEmpty()) {
                    System.out.println("     Descrizione: " + c.getDescrizione());
                }
            }
        }

        System.out.println("\n[3] CATEGORIE E RELATIVE SCHEDE CAMPI:");
        List<Categoria> categorie = controller.getCategorie();
        if (categorie.isEmpty()) {
            System.out.println("   (Nessuna categoria attualmente configurata)");
        } else {
            for (Categoria cat : categorie) {
                System.out.println("\n-------------------------------------------------------");
                System.out.println("CATEGORIA: " + cat.getNome().toUpperCase());
                if (!cat.getDescrizione().isEmpty()) {
                    System.out.println("Descrizione: " + cat.getDescrizione());
                }
                System.out.println("Composizione campi per le iniziative di questa categoria:");
                
                System.out.println("  a) Campi Base (" + base.size() + "):");
                for (Campo cb : base) {
                    System.out.println("     - " + cb.getNome() + " [Obbligatorio, " + cb.getTipo().getDescrizione() + "]");
                }

                System.out.println("  b) Campi Comuni (" + comuni.size() + "):");
                if (comuni.isEmpty()) {
                    System.out.println("     (Nessun campo comune presente)");
                } else {
                    for (Campo cc : comuni) {
                        System.out.println("     - " + cc.getNome() + " [" + (cc.isObbligatorio() ? "Obbligatorio" : "Facoltativo") + 
                                ", " + cc.getTipo().getDescrizione() + "]");
                    }
                }

                System.out.println("  c) Campi Specifici (" + cat.getCampiSpecifici().size() + "):");
                if (cat.getCampiSpecifici().isEmpty()) {
                    System.out.println("     (Nessun campo specifico per questa categoria)");
                } else {
                    for (Campo cs : cat.getCampiSpecifici()) {
                        System.out.println("     - " + cs.getNome() + " [" + (cs.isObbligatorio() ? "Obbligatorio" : "Facoltativo") + 
                                ", " + cs.getTipo().getDescrizione() + "]" + 
                                (!cs.getDescrizione().isEmpty() ? " -> " + cs.getDescrizione() : ""));
                    }
                }
            }
            System.out.println("-------------------------------------------------------");
        }
    }

    private void salvaStatoPersistente() {
        try {
            controller.salvaDati();
        } catch (Exception e) {
            System.out.println("Avviso: impossibile salvare lo stato su disco: " + e.getMessage());
        }
    }
}
