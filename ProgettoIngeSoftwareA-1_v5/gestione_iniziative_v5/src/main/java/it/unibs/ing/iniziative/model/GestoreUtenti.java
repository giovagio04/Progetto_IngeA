package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestisce l'autenticazione, la registrazione e l'univocità di tutti gli utenti del sistema
 * (sia configuratori sia fruitori, a partire dalla Versione 3).
 * Lo username di ciascun utente è univoco nell'intero sistema (non può coincidere né con quello
 * di un altro fruitore né con quello di un configuratore).
 * 
 * Invarianti di classe:
 * - La mappa utenti non deve essere nulla né vuota.
 * - Per ogni voce della mappa, la chiave coincide con lo username in minuscolo dell'utente.
 * - Nessun utente memorizzato è nullo o viola il proprio invariante.
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
        Configuratore defaultConfig = new Configuratore(DEFAULT_CONFIG_USER, DEFAULT_CONFIG_PASS, true);
        this.utenti.put(defaultConfig.getUsername().toLowerCase(), defaultConfig);

        assert checkInvariant() : "Invariante di classe violato in GestoreUtenti al termine del costruttore";
    }

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
     * Verifica se uno username è già presente a sistema (tra configuratori o fruitori).
     */
    public boolean esisteUsername(String username) {
        if (username == null) return false;
        return utenti.containsKey(username.trim().toLowerCase());
    }

    /**
     * Esegue il login dell'utente (Configuratore o Fruitore).
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
     * Registra un nuovo utente Fruitore verificando che lo username non sia già occupato
     * da nessun altro utente (né fruitore né configuratore).
     * 
     * @param username username scelto dal fruitore
     * @param password password scelta dal fruitore
     * @return il nuovo Fruitore registrato, oppure null se username già in uso o dati non validi
     * @post result != null ==> esisteUsername(username) && getFruitore(username) == result
     */
    public Fruitore registraFruitore(String username, String password) {
        assert checkInvariant() : "Invariante violato prima di registraFruitore";
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }
        String key = username.trim().toLowerCase();
        if (esisteUsername(key)) {
            return null; // Username già occupato da configuratore o altro fruitore
        }

        Fruitore nuovoFruitore = new Fruitore(username.trim(), password);
        utenti.put(key, nuovoFruitore);

        assert esisteUsername(username) : "Postcondizione violata in registraFruitore";
        assert checkInvariant() : "Invariante violato dopo registraFruitore";
        return nuovoFruitore;
    }

    /**
     * Recupera un Fruitore registrato dato il suo username.
     */
    public Fruitore getFruitore(String username) {
        if (username == null) return null;
        Utente u = utenti.get(username.trim().toLowerCase());
        if (u instanceof Fruitore) {
            return (Fruitore) u;
        }
        return null;
    }

    /**
     * Completa la registrazione del configuratore cambiando le sue credenziali predefinite
     * in credenziali personali univoche, oppure aggiorna le credenziali di un configuratore esistente.
     */
    public Configuratore cambiaCredenziali(Configuratore utente, String nuovoUsername, String nuovaPassword) {
        assert checkInvariant() : "Invariante violato prima di cambiaCredenziali";
        if (utente == null || nuovoUsername == null || nuovaPassword == null) return null;
        String nuovoTrim = nuovoUsername.trim();
        if (nuovoTrim.isEmpty() || nuovaPassword.trim().isEmpty()) return null;
        String nuovoKey = nuovoTrim.toLowerCase();

        if (!utente.getUsername().equalsIgnoreCase(nuovoTrim) && utenti.containsKey(nuovoKey)) {
            return null; // Username già occupato
        }

        if (utente.isUsaCredenzialiPredefinite()) {
            if (nuovoKey.equalsIgnoreCase(DEFAULT_CONFIG_USER)) {
                return null;
            }
            Configuratore nuovoConfig = new Configuratore(nuovoTrim, nuovaPassword, false);
            this.utenti.put(nuovoKey, nuovoConfig);

            assert esisteUsername(nuovoTrim) : "Postcondizione: nuovo username non presente";
            assert !nuovoConfig.isUsaCredenzialiPredefinite() : "Postcondizione: il nuovo configuratore non deve usare credenziali predefinite";
            assert checkInvariant() : "Invariante violato dopo creazione profilo configuratore";

            return nuovoConfig;
        } else {
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
