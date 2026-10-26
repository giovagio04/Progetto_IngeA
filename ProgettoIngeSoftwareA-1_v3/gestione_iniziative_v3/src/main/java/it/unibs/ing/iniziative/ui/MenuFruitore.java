package it.unibs.ing.iniziative.ui;

import it.unibs.ing.iniziative.controller.ConfigurazioneController;
import it.unibs.ing.iniziative.model.Bacheca;
import it.unibs.ing.iniziative.model.Fruitore;
import it.unibs.ing.iniziative.model.Proposta;
import it.unibs.ing.iniziative.model.SpazioPersonale;
import it.unibs.ing.iniziative.model.ValoreCampo;
import it.unibs.ing.iniziative.storage.DatiSistema;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestisce l'interfaccia a riga di comando per il fruitore (Front-end, Versione 3).
 * Utilizza {@link MyMenu} e {@link InputDati} (libreria Kibo) e delega al {@link ConfigurazioneController}
 * la consultazione della bacheca per categoria, l'iscrizione a proposte aperte
 * e la gestione privata delle notifiche nel proprio Spazio Personale.
 */
public class MenuFruitore {

    private final ConfigurazioneController controller;
    private final Fruitore fruitore;

    public MenuFruitore(ConfigurazioneController controller, Fruitore fruitore) {
        assert controller != null : "Precondizione violata: controller nullo";
        assert fruitore != null : "Precondizione violata: fruitore nullo";
        this.controller = controller;
        this.fruitore = fruitore;
    }

    public MenuFruitore(DatiSistema dati, Fruitore fruitore) {
        this(new ConfigurazioneController(dati), fruitore);
    }

    public void esegui() {
        boolean esci = false;
        while (!esci) {
            controller.aggiornaStatiEScadenze(LocalDate.now());
            SpazioPersonale sp = fruitore.getSpazioPersonale();
            int numNotifiche = sp.getNumeroNotifiche();

            String[] voci = {
                "Visualizza Bacheca Proposte Aperte (per Categoria)",
                "Aderisci a una Proposta Aperta",
                "Visualizza Iniziative a cui sei Iscritto",
                "Accedi allo Spazio Personale (Notifiche: " + numNotifiche + ")"
            };
            String titoloMenu = "MENU FRUITORE (FRONT-END V3) - Utente: " + fruitore.getUsername() +
                    (numNotifiche > 0 ? " [" + numNotifiche + " notifica/e]" : "");
            MyMenu menu = new MyMenu(titoloMenu, voci);

            int scelta = menu.scegli();
            switch (scelta) {
                case 1:
                    visualizzaBacheca();
                    break;
                case 2:
                    aderisciAProposta();
                    break;
                case 3:
                    visualizzaMieIscrizioni();
                    break;
                case 4:
                    gestisciSpazioPersonale();
                    break;
                case 0:
                    esci = true;
                    break;
            }
        }
    }

    // =========================================================================
    // 1. VISUALIZZA BACHECA
    // =========================================================================
    private void visualizzaBacheca() {
        Bacheca bacheca = controller.getBacheca();
        bacheca.visualizzaBachecaPerUtente(controller.getCategorie(), fruitore.getUsername());

        if (bacheca.isVuota()) return;

        boolean visualizzaDettaglio = InputDati.yesOrNo("Desideri visualizzare la scheda dettagliata di una proposta?");
        while (visualizzaDettaglio) {
            List<Proposta> aperte = bacheca.getProposteAperte();
            for (int i = 0; i < aperte.size(); i++) {
                Proposta p = aperte.get(i);
                System.out.println((i + 1) + ". " + p.getTitolo() + " (" + p.getCategoria().getNome() + ")" + 
                        (p.isIscritto(fruitore.getUsername()) ? " [SEI ISCRITTO]" : ""));
            }
            int sc = InputDati.leggiIntero("Seleziona proposta [1-" + aperte.size() + ", 0 per annullare]: ", 0, aperte.size());
            if (sc > 0) {
                System.out.println("\n" + aperte.get(sc - 1).schedaDettagliata());
            }
            visualizzaDettaglio = InputDati.yesOrNo("Desideri visualizzare un'altra scheda?");
        }
    }

    // =========================================================================
    // 2. ADERISCI A UNA PROPOSTA APERTA
    // =========================================================================
    private void aderisciAProposta() {
        System.out.println("\n--- ADESIONE A UN'INIZIATIVA APERTA ---");
        List<Proposta> aperte = controller.getBacheca().getProposteAperte();
        if (aperte.isEmpty()) {
            System.out.println("Attualmente non ci sono proposte aperte in bacheca.");
            return;
        }

        System.out.println("Proposte attualmente aperte alle iscrizioni:");
        for (int i = 0; i < aperte.size(); i++) {
            Proposta p = aperte.get(i);
            String statoIscritto = p.isIscritto(fruitore.getUsername()) ? " -> [SEI GIÀ ISCRITTO]" : "";
            System.out.println((i + 1) + ". " + p.getTitolo() + " [Cat: " + p.getCategoria().getNome() + 
                    " | Posti: " + p.getNumeroAderenti() + "/" + p.getNumeroPartecipanti() + 
                    " | Termine: " + (p.getTermineIscrizione() != null ? p.getTermineIscrizione().format(ValoreCampo.FORMATTER_DATA) : "-") + 
                    "]" + statoIscritto);
        }

        int sc = InputDati.leggiIntero("Seleziona la proposta a cui aderire [1-" + aperte.size() + ", 0 per annullare]: ", 0, aperte.size());
        if (sc == 0) return;

        Proposta scelta = aperte.get(sc - 1);
        System.out.println("\n" + scelta.schedaDettagliata());

        if (scelta.isIscritto(fruitore.getUsername())) {
            System.out.println("Risulti già iscritto a questa proposta. Non è possibile effettuare un'ulteriore iscrizione.");
            return;
        }

        if (scelta.isAlCompleto()) {
            System.out.println("La proposta ha già raccolto il numero massimo di partecipanti previsto (" + scelta.getNumeroPartecipanti() + ").");
            return;
        }

        boolean conferma = InputDati.yesOrNo("Confermi la tua adesione all'iniziativa '" + scelta.getTitolo() + "'?");
        if (conferma) {
            String errore = controller.iscriviFruitoreAProposta(scelta, fruitore.getUsername(), LocalDate.now());
            if (errore != null) {
                System.out.println("Errore: " + errore);
            } else {
                salvaStatoPersistente();
                System.out.println("\n[SUCCESSO] La tua iscrizione all'iniziativa '" + scelta.getTitolo() + "' è stata registrata con successo!");
                System.out.println("Partecipanti attuali: " + scelta.getNumeroAderenti() + " / " + scelta.getNumeroPartecipanti());
                if (scelta.isAlCompleto()) {
                    System.out.println("Nota: Con la tua adesione la proposta ha raggiunto la capienza massima prevista!");
                }
            }
        } else {
            System.out.println("Adesione annullata.");
        }
    }

    // =========================================================================
    // 3. VISUALIZZA LE MIE ISCRIZIONI
    // =========================================================================
    private void visualizzaMieIscrizioni() {
        System.out.println("\n=======================================================");
        System.out.println("           LE MIE ISCRIZIONI ALLE INIZIATIVE           ");
        System.out.println("=======================================================");

        List<Proposta> mie = controller.getPropostePerFruitore(fruitore.getUsername());
        if (mie.isEmpty()) {
            System.out.println("Non risulti attualmente iscritto ad alcuna iniziativa.");
        } else {
            for (int i = 0; i < mie.size(); i++) {
                Proposta p = mie.get(i);
                System.out.println("[" + (i + 1) + "] " + p.getTitolo() + " (Categoria: " + p.getCategoria().getNome() + ")");
                System.out.println("    Stato: " + p.getStato().getDescrizione());
                System.out.println("    Data Inizio: " + (p.getDataInizio() != null ? p.getDataInizio().format(ValoreCampo.FORMATTER_DATA) : "-") + 
                        " | Ora: " + (p.getOra() != null ? p.getOra().format(ValoreCampo.FORMATTER_ORA) : "-"));
                System.out.println("    Luogo: " + (p.getLuogo() != null ? p.getLuogo() : "-"));
                System.out.println("    Quota individuale: " + (p.getQuotaIndividuale() != null ? String.format(java.util.Locale.ITALY, "%.2f €", p.getQuotaIndividuale()) : "-"));
                System.out.println("    Aderenti: " + p.getNumeroAderenti() + " / " + p.getNumeroPartecipanti());
            }
        }
        System.out.println("=======================================================");
    }

    // =========================================================================
    // 4. ACCEDI ALLO SPAZIO PERSONALE
    // =========================================================================
    private void gestisciSpazioPersonale() {
        SpazioPersonale sp = fruitore.getSpazioPersonale();
        boolean indietro = false;

        while (!indietro) {
            sp.visualizzaNotifiche();
            if (sp.isVuoto()) {
                break;
            }

            String[] voci = { "Cancella selettivamente una notifica" };
            MyMenu menuSpazio = new MyMenu("OPZIONI SPAZIO PERSONALE", voci);
            int sc = menuSpazio.scegli();
            if (sc == 1) {
                int idDaCancellare = InputDati.leggiInteroConMinimo("Inserisci il numero (#ID) della notifica da cancellare (0 per annullare): ", 0);
                if (idDaCancellare > 0) {
                    if (controller.rimuoviNotificaFruitore(fruitore, idDaCancellare)) {
                        salvaStatoPersistente();
                        System.out.println("Notifica #" + idDaCancellare + " rimossa con successo dallo Spazio Personale.");
                    } else {
                        System.out.println("Nessuna notifica trovata con ID #" + idDaCancellare + ".");
                    }
                }
            } else {
                indietro = true;
            }
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
