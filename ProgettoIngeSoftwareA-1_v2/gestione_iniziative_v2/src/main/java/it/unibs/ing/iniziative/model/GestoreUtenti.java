package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestisce la memorizzazione e le operazioni sui configuratori del sistema.
 * Nella Versione 1, l'unico attore del sistema è il Configuratore.
 * 
 * Invarianti di classe:
 * - La mappa utenti non deve essere nulla né vuota (contiene sempre almeno il configuratore predefinito o altri configuratori).
 * - Per ogni voce della mappa, la chiave deve coincidere con lo username normalizzato in minuscolo dell'utente.
 * - Nessun utente memorizzato deve essere nullo o violare il proprio invariante.
 * 
 * @inv utenti != null && !utenti.isEmpty()
 * @inv forall k in utenti.keySet(): utenti.get(k) != null && utenti.get(k).getUsername().equalsIgnoreCase(k)
 * @inv forall u in utenti.values(): u != null && u.checkInvariant()
 */
public class GestoreUtenti implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_CONFIG_USER = "config";
    public static final String DEFAULT_CONFIG_PASS = "config";

    private Map<String, Utente> utenti;

    /**
     * Costruttore: inserisce l'utenza predefinita speciale per i nuovi configuratori autorizzati.
     * 
     * @post checkInvariant()
     * @post esisteUsername(DEFAULT_CONFIG_USER)
     */
    public GestoreUtenti() {
        this.utenti = new HashMap<>();
        // Inserisce l'utenza predefinita speciale per i nuovi configuratori autorizzati
        Configuratore defaultConfig = new Configuratore(DEFAULT_CONFIG_USER, DEFAULT_CONFIG_PASS, true);
        this.utenti.put(defaultConfig.getUsername().toLowerCase(), defaultConfig);

        assert checkInvariant() : "Invariante di classe violato in GestoreUtenti al termine del costruttore";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è verificato, false altrimenti.
     */
    public boolean checkInvariant() {
        if (utenti == null || utenti.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Utente> entry : utenti.entrySet()) {
            String key = entry.getKey();
            Utente u = entry.getValue();
            if (key == null || u == null || !u.checkInvariant()) {
                return false;
            }
            if (!key.equalsIgnoreCase(u.getUsername())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica se uno username è già presente a sistema.
     * 
     * @param username lo username da verificare
     * @return true se presente, false altrimenti
     */
    public boolean esisteUsername(String username) {
        if (username == null) return false;
        return utenti.containsKey(username.trim().toLowerCase());
    }

    /**
     * Esegue il login dell'utente.
     * 
     * @param username username inserito
     * @param password password inserita
     * @return l'istanza dell'Utente autenticato se credenziali valide, null altrimenti
     */
    public Utente login(String username, String password) {
        assert checkInvariant() : "Invariante violato prima di login";
        if (username == null || password == null) return null;
        Utente u = utenti.get(username.trim().toLowerCase());
        if (u != null && u.checkPassword(password)) {
            return u;
        }
        return null;
    }

    /**
     * Completa la registrazione del configuratore cambiando le sue credenziali predefinite
     * in credenziali personali univoche, oppure aggiorna le credenziali di un configuratore esistente.
     * Restituisce il Configuratore aggiornato, oppure null se il nuovo username è già occupato.
     * 
     * @param utente il configuratore corrente
     * @param nuovoUsername il nuovo username scelto
     * @param nuovaPassword la nuova password scelta
     * @return l'istanza aggiornata di Configuratore, oppure null se operazione fallita
     * @post result != null ==> esisteUsername(nuovoUsername) && !result.isUsaCredenzialiPredefinite()
     */
    public Configuratore cambiaCredenziali(Configuratore utente, String nuovoUsername, String nuovaPassword) {
        assert checkInvariant() : "Invariante violato prima di cambiaCredenziali";
        if (utente == null || nuovoUsername == null || nuovaPassword == null) return null;
        String nuovoTrim = nuovoUsername.trim();
        if (nuovoTrim.isEmpty() || nuovaPassword.trim().isEmpty()) return null;
        String nuovoKey = nuovoTrim.toLowerCase();

        // Se è diverso dal suo attuale username, verifichiamo che non sia già occupato
        if (!utente.getUsername().equalsIgnoreCase(nuovoTrim) && utenti.containsKey(nuovoKey)) {
            return null; // Username già occupato
        }

        if (utente.isUsaCredenzialiPredefinite()) {
            // Non si può riutilizzare lo username predefinito come username personale
            if (nuovoKey.equalsIgnoreCase(DEFAULT_CONFIG_USER)) {
                return null;
            }
            // Creiamo un nuovo profilo personale per il configuratore
            Configuratore nuovoConfig = new Configuratore(nuovoTrim, nuovaPassword, false);
            this.utenti.put(nuovoKey, nuovoConfig);

            assert esisteUsername(nuovoTrim) : "Postcondizione: nuovo username non presente";
            assert !nuovoConfig.isUsaCredenzialiPredefinite() : "Postcondizione: il nuovo configuratore non deve usare credenziali predefinite";
            assert checkInvariant() : "Invariante violato dopo creazione profilo configuratore";

            return nuovoConfig;
        } else {
            // Aggiornamento credenziali normale
            utenti.remove(utente.getUsername().toLowerCase());
            utente.setUsername(nuovoTrim);
            utente.setPassword(nuovaPassword);
            utenti.put(nuovoKey, utente);

            assert esisteUsername(nuovoTrim) : "Postcondizione: username aggiornato non presente";
            assert checkInvariant() : "Invariante violato dopo aggiornamento credenziali";

            return utente;
        }
    }
}
