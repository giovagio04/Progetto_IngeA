package it.unibs.ing.iniziative;

import it.unibs.ing.iniziative.controller.ConfigurazioneController;
import it.unibs.ing.iniziative.model.Configuratore;
import it.unibs.ing.iniziative.model.Utente;
import it.unibs.ing.iniziative.storage.DatiSistema;
import it.unibs.ing.iniziative.storage.GestorePersistenza;
import it.unibs.ing.iniziative.ui.InputDati;
import it.unibs.ing.iniziative.ui.MenuConfiguratore;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    PIATTAFORMA GESTIONE INIZIATIVE (Versione 1)  ");
        System.out.println("=================================================");

        DatiSistema dati;
        try {
            dati = GestorePersistenza.caricaDati();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Nessun dato persistente pregresso o errore di caricamento. Avvio nuovo sistema.");
            dati = new DatiSistema();
        }

        ConfigurazioneController controller = new ConfigurazioneController(dati);

        // Accesso obbligatorio per il configuratore
        Utente utenteLoggato = null;
        while (utenteLoggato == null) {
            System.out.println("\n--- ACCESSO AL SISTEMA ---");
            System.out.println("Nota: Per il primo accesso dei nuovi configuratori usare 'config' / 'config'.");
            String usr = InputDati.leggiStringaNonVuota("Username: ");
            String pwd = InputDati.leggiStringaNonVuota("Password: ");
            
            utenteLoggato = controller.login(usr, pwd);
            if (utenteLoggato == null) {
                System.out.println("Credenziali errate. Riprova.");
            }
        }

        Configuratore config = (Configuratore) utenteLoggato;

        // Se sta usando le credenziali predefinite, è obbligato a scegliere credenziali personali
        if (config.isUsaCredenzialiPredefinite()) {
            System.out.println("\n[ATTENZIONE] Stai accedendo con le credenziali predefinite.");
            System.out.println("Per operare sul back-end devi prima scegliere le tue credenziali personali.");
            
            boolean registrato = false;
            while (!registrato) {
                String nuovoUsr = InputDati.leggiStringaNonVuota("Nuovo Username personale: ");
                String nuovaPwd = InputDati.leggiStringaNonVuota("Nuova Password personale: ");
                
                Configuratore configAggiornato = controller.registraNuovoConfiguratore(config, nuovoUsr, nuovaPwd);
                if (configAggiornato == null) {
                    System.out.println("Attenzione: lo username '" + nuovoUsr + "' è già in uso o non valido. Scegline un altro.");
                } else {
                    config = configAggiornato;
                    utenteLoggato = configAggiornato;
                    registrato = true;
                    System.out.println("Credenziali personali registrate con successo!");
                    
                    // Salvataggio immediato delle nuove credenziali
                    try {
                        controller.salvaDati();
                    } catch (IOException e) {
                        System.out.println("Avviso: Impossibile salvare immediatamente le credenziali: " + e.getMessage());
                    }
                }
            }
        }

        MenuConfiguratore menu = new MenuConfiguratore(controller);

        // Se al primo avvio i campi base non sono ancora stati fissati, richiede le 8 domande
        if (!controller.isCampiBaseFissati()) {
            menu.eseguiImpostazioneManualeCampiBase();
        }
        
        // Apertura del menu operativo per il configuratore
        menu.esegui();
        
        // Salvataggio persistente alla chiusura
        try {
            controller.salvaDati();
            System.out.println("Dati del sistema salvati correttamente in modo persistente. Arrivederci!");
        } catch (Exception e) {
            System.out.println("Errore durante il salvataggio dei dati: " + e.getMessage());
        }
    }
}
