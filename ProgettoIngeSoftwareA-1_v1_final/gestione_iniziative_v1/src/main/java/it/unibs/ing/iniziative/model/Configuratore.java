package it.unibs.ing.iniziative.model;

/**
 * Rappresenta un configuratore del sistema.
 * Il configuratore ha il compito di definire campi base, categorie e proporre iniziative.
 * 
 * Invariante di classe ereditato da Utente.
 */
public class Configuratore extends Utente {
    private static final long serialVersionUID = 1L;
    
    // Flag utile per tracciare se il configuratore sta usando le credenziali predefinite o personali
    private boolean usaCredenzialiPredefinite;

    public Configuratore(String username, String password, boolean usaCredenzialiPredefinite) {
        super(username, password);
        this.usaCredenzialiPredefinite = usaCredenzialiPredefinite;
        assert checkInvariant() : "Invariante violato in Configuratore";
    }

    public boolean isUsaCredenzialiPredefinite() {
        return usaCredenzialiPredefinite;
    }

    public void setUsaCredenzialiPredefinite(boolean usaCredenzialiPredefinite) {
        this.usaCredenzialiPredefinite = usaCredenzialiPredefinite;
    }
}
