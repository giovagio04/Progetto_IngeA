package it.unibs.ing.iniziative;

import it.unibs.ing.iniziative.controller.ConfigurazioneController;
import it.unibs.ing.iniziative.model.Configuratore;
import it.unibs.ing.iniziative.model.Fruitore;
import it.unibs.ing.iniziative.model.Utente;
import it.unibs.ing.iniziative.storage.DatiSistema;
import it.unibs.ing.iniziative.storage.GestorePersistenza;
import it.unibs.ing.iniziative.ui.InputDati;
import it.unibs.ing.iniziative.ui.MenuConfiguratore;
import it.unibs.ing.iniziative.ui.MenuFruitore;
import it.unibs.ing.iniziative.ui.MyMenu;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Punto di ingresso dell'applicazione per la gestione delle iniziative (Versione 5).
 * Costruisce su V1, V2, V3 e V4: rispetta l'architettura MVC passando attraverso il {@link ConfigurazioneController},
 * utilizza la libreria Kibo ({@link MyMenu}, {@link InputDati}), aggiorna automaticamente gli stati
 * e le scadenze all'avvio ("allo scadere della mezzanotte") e consente l'accesso sia ai Configuratori
 * (con compilazione degli 8 campi base al primo avvio, modalità interattiva e importazione batch da file)
 * sia ai Fruitori.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    PIATTAFORMA GESTIONE INIZIATIVE (Versione 5)  ");
        System.out.println("=================================================");

        DatiSistema dati;
        try {
            dati = GestorePersistenza.caricaDati();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Nessun dato persistente pregresso o errore di caricamento. Avvio nuovo sistema.");
            dati = new DatiSistema();
        }

        ConfigurazioneController controller = new ConfigurazioneController(dati);

        // Controllo automatico delle scadenze e rimozione proposte da bacheca al primo avvio
        controller.aggiornaStatiEScadenze(LocalDate.now());

        boolean terminaApplicazione = false;
        String[] vociIngresso = {
            "Accedi (Login per Configuratori e Fruitori)",
            "Registrazione nuovo Fruitore (Primo accesso Front-end)"
        };
        MyMenu menuPrincipale = new MyMenu("BENVENUTO NEL SISTEMA INIZIATIVE (V5)", vociIngresso);

        while (!terminaApplicazione) {
            // Aggiorna stati ad ogni iterazione del menu principale
            controller.aggiornaStatiEScadenze(LocalDate.now());

            int scelta = menuPrincipale.scegli();
            switch (scelta) {
                case 1:
                    effettuaLogin(controller);
                    salvaDati(controller);
                    break;
                case 2:
                    registraNuovoFruitore(controller);
                    salvaDati(controller);
                    break;
                case 0:
                    salvaDati(controller);
                    System.out.println("Dati del sistema salvati correttamente in modo persistente. Arrivederci!");
                    terminaApplicazione = true;
                    break;
            }
        }
    }

    private static void effettuaLogin(ConfigurazioneController controller) {
        System.out.println("\n--- ACCESSO AL SISTEMA ---");
        System.out.println("Nota: Per il primo accesso dei nuovi configuratori usare 'config' / 'config'.");
        String usr = InputDati.leggiStringaNonVuota("Username: ");
        String pwd = InputDati.leggiStringaNonVuota("Password: ");

        Utente utenteLoggato = controller.login(usr, pwd);
        if (utenteLoggato == null) {
            System.out.println("Credenziali errate o utente non trovato.");
            return;
        }

        if (utenteLoggato instanceof Configuratore) {
            Configuratore config = (Configuratore) utenteLoggato;

            // Se accede con le credenziali predefinite 'config'/'config', deve obbligatoriamente cambiarle
            if (config.isUsaCredenzialiPredefinite()) {
                System.out.println("\n[ATTENZIONE] Stai accedendo con le credenziali predefinite.");
                System.out.println("Per operare sul back-end devi prima impostare le tue credenziali personali.");

                boolean registrato = false;
                while (!registrato) {
                    String nuovoUsr = InputDati.leggiStringaNonVuota("Nuovo Username personale: ");
                    String nuovaPwd = InputDati.leggiStringaNonVuota("Nuova Password personale: ");

                    Configuratore configAggiornato = controller.registraNuovoConfiguratore(config, nuovoUsr, nuovaPwd);
                    if (configAggiornato == null) {
                        System.out.println("Attenzione: lo username '" + nuovoUsr + "' è già in uso o riservato. Scegline un altro.");
                    } else {
                        config = configAggiornato;
                        registrato = true;
                        System.out.println("Credenziali personali registrate con successo!");
                        salvaDati(controller);
                    }
                }
            }

            MenuConfiguratore menuConfig = new MenuConfiguratore(controller, config);

            // Al primo avvio dell'applicazione il configuratore compila gli 8 campi base (fondamenta V1)
            if (!controller.isCampiBaseFissati()) {
                menuConfig.eseguiImpostazioneManualeCampiBase();
            }

            // Apertura Back-end Configuratore (V5)
            menuConfig.esegui();

        } else if (utenteLoggato instanceof Fruitore) {
            Fruitore fruitore = (Fruitore) utenteLoggato;
            // Apertura Front-end Fruitore (V5)
            MenuFruitore menuFruitore = new MenuFruitore(controller, fruitore);
            menuFruitore.esegui();
        }
    }

    private static void registraNuovoFruitore(ConfigurazioneController controller) {
        System.out.println("\n--- REGISTRAZIONE NUOVO FRUITORE ---");
        System.out.println("Scegli le tue credenziali individuali per operare sul front-end:");

        String username = InputDati.leggiStringaNonVuota("Username desiderato: ");
        if (controller.esisteUsername(username)) {
            System.out.println("Errore: Lo username '" + username + "' è già utilizzato da un altro utente. Scegline un altro.");
            return;
        }

        String password = InputDati.leggiStringaNonVuota("Password desiderata: ");
        Fruitore f = controller.registraFruitore(username, password);
        if (f != null) {
            System.out.println("\n[SUCCESSO] Registrazione completata! Benvenuto, " + f.getUsername() + ".");
            System.out.println("Accesso immediato al Front-end in corso...");
            salvaDati(controller);
            MenuFruitore menuFruitore = new MenuFruitore(controller, f);
            menuFruitore.esegui();
        } else {
            System.out.println("Errore durante la registrazione del fruitore.");
        }
    }

    private static void salvaDati(ConfigurazioneController controller) {
        try {
            controller.salvaDati();
        } catch (Exception e) {
            System.out.println("Avviso: Impossibile salvare i dati: " + e.getMessage());
        }
    }
}
