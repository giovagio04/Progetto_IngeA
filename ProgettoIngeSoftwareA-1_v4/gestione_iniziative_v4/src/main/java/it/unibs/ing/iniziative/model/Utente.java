package it.unibs.ing.iniziative.model;

import java.io.Serializable;

/**
 * Rappresenta un utente generico del sistema.
 * 
 * Invarianti di classe:
 * - Lo username non deve essere nullo né vuoto.
 * - La password non deve essere nulla né vuota.
 * 
 * @inv username != null && !username.trim().isEmpty()
 * @inv password != null && !password.trim().isEmpty()
 */
public abstract class Utente implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected String username;
    protected String password;

    /**
     * Costruttore base per un utente.
     * 
     * @pre username != null && !username.trim().isEmpty()
     * @pre password != null && !password.trim().isEmpty()
     * @post getUsername().equals(username.trim())
     * @post checkPassword(password)
     */
    public Utente(String username, String password) {
        assert username != null && !username.trim().isEmpty() : "Precondizione violata: lo username non può essere vuoto";
        assert password != null && !password.trim().isEmpty() : "Precondizione violata: la password non può essere vuota";
        
        this.username = username.trim();
        this.password = password;

        assert checkInvariant() : "Invariante di classe violato in Utente";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è verificato, false altrimenti.
     */
    public boolean checkInvariant() {
        return username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty();
    }

    public String getUsername() {
        return username;
    }

    /**
     * Aggiorna lo username.
     * 
     * @pre username != null && !username.trim().isEmpty()
     * @post getUsername().equals(username.trim())
     */
    public void setUsername(String username) {
        assert username != null && !username.trim().isEmpty() : "Precondizione violata: lo username non può essere vuoto";
        this.username = username.trim();
        assert checkInvariant() : "Invariante violato dopo setUsername";
    }

    /**
     * Verifica se la password fornita coincide con quella dell'utente.
     */
    public boolean checkPassword(String password) {
        return this.password != null && this.password.equals(password);
    }

    /**
     * Aggiorna la password dell'utente.
     * 
     * @pre password != null && !password.trim().isEmpty()
     * @post checkPassword(password)
     */
    public void setPassword(String password) {
        assert password != null && !password.trim().isEmpty() : "Precondizione violata: la password non può essere vuota";
        this.password = password;
        assert checkInvariant() : "Invariante violato dopo setPassword";
    }
}
