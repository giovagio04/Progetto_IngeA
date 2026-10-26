package it.unibs.ing.iniziative.model;

/**
 * Rappresenta l'utente Fruitore che interagisce con il front-end dell'applicazione (a partire dalla Versione 3).
 * Ogni fruitore dispone di uno Spazio Personale privato per la consultazione e gestione delle notifiche.
 * 
 * Invarianti di classe:
 * - Rispetta l'invariante della superclasse Utente.
 * - Lo spazioPersonale non è mai nullo e rispetta il proprio invariante.
 * 
 * @inv super.checkInvariant() && spazioPersonale != null && spazioPersonale.checkInvariant()
 */
public class Fruitore extends Utente {
    private static final long serialVersionUID = 1L;

    private SpazioPersonale spazioPersonale;

    /**
     * Costruisce un nuovo Fruitore con le credenziali individuali scelte al primo accesso.
     * 
     * @param username username univoco scelto dal fruitore
     * @param password password scelta dal fruitore
     * @pre username != null && !username.trim().isEmpty()
     * @pre password != null && !password.trim().isEmpty()
     * @post checkInvariant()
     */
    public Fruitore(String username, String password) {
        super(username, password);
        this.spazioPersonale = new SpazioPersonale();
        assert checkInvariant() : "Invariante violato in Fruitore";
    }

    @Override
    public boolean checkInvariant() {
        return super.checkInvariant() && getSpazioPersonale().checkInvariant();
    }

    public SpazioPersonale getSpazioPersonale() {
        if (spazioPersonale == null) {
            spazioPersonale = new SpazioPersonale();
        }
        return spazioPersonale;
    }

    @Override
    public String toString() {
        return "Fruitore: " + getUsername() + " (Notifiche: " + getSpazioPersonale().getNumeroNotifiche() + ")";
    }
}
