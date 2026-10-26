package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Definisce la struttura di un campo (Base, Comune o Specifico).
 * 
 * Invarianti di classe:
 * - Il nome non deve essere nullo né vuoto.
 * - Il tipo del campo non deve essere nullo.
 * - La descrizione non deve essere nulla (può essere stringa vuota).
 * 
 * @inv nome != null && !nome.trim().isEmpty()
 * @inv tipo != null
 * @inv descrizione != null
 */
public class Campo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String descrizione;
    private boolean obbligatorio;
    private TipoCampo tipo;
    private String valore;

    /**
     * Costruttore per creare un nuovo campo (senza valore preimpostato).
     * 
     * @pre nome != null && !nome.trim().isEmpty()
     * @pre tipo != null
     * @post getNome().equals(nome.trim())
     * @post getTipo() == tipo
     * @post isObbligatorio() == obbligatorio
     */
    public Campo(String nome, String descrizione, boolean obbligatorio, TipoCampo tipo) {
        this(nome, descrizione, obbligatorio, tipo, null);
    }

    /**
     * Costruttore per creare un nuovo campo con valore associato (compilazione).
     * 
     * @pre nome != null && !nome.trim().isEmpty()
     * @pre tipo != null
     * @post getNome().equals(nome.trim())
     * @post getTipo() == tipo
     * @post isObbligatorio() == obbligatorio
     */
    public Campo(String nome, String descrizione, boolean obbligatorio, TipoCampo tipo, String valore) {
        assert nome != null && !nome.trim().isEmpty() : "Precondizione violata: il nome del campo non può essere vuoto";
        assert tipo != null : "Precondizione violata: il tipo del campo non può essere nullo";

        this.nome = nome.trim();
        this.descrizione = (descrizione != null) ? descrizione.trim() : "";
        this.obbligatorio = obbligatorio;
        this.tipo = tipo;
        this.valore = (valore != null) ? valore.trim() : null;

        assert checkInvariant() : "Invariante di classe violato in Campo";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è rispettato, false altrimenti.
     */
    public boolean checkInvariant() {
        return nome != null && !nome.trim().isEmpty()
                && tipo != null
                && descrizione != null;
    }

    public String getNome() { 
        return nome; 
    }

    public String getDescrizione() { 
        return descrizione; 
    }

    public boolean isObbligatorio() { 
        return obbligatorio; 
    }

    public TipoCampo getTipo() { 
        return tipo; 
    }

    public String getValore() {
        return valore;
    }

    public void setValore(String valore) {
        this.valore = (valore != null) ? valore.trim() : null;
    }

    public boolean isCompilato() {
        return valore != null && !valore.trim().isEmpty();
    }

    /**
     * Modifica la descrizione del campo.
     * 
     * @pre checkInvariant()
     * @post checkInvariant()
     */
    public void setDescrizione(String descrizione) {
        assert checkInvariant() : "Invariante violato prima di setDescrizione";
        this.descrizione = (descrizione != null) ? descrizione.trim() : "";
        assert checkInvariant() : "Invariante violato dopo setDescrizione";
    }

    /**
     * Modifica l'obbligatorietà del campo.
     * 
     * @pre checkInvariant()
     * @post isObbligatorio() == obbligatorio
     * @post checkInvariant()
     */
    public void setObbligatorio(boolean obbligatorio) {
        assert checkInvariant() : "Invariante violato prima di setObbligatorio";
        this.obbligatorio = obbligatorio;
        assert isObbligatorio() == obbligatorio : "Postcondizione violata: obbligatorietà non aggiornata";
        assert checkInvariant() : "Invariante violato dopo setObbligatorio";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Campo campo = (Campo) o;
        return nome.equalsIgnoreCase(campo.nome); // I nomi identificano univocamente il campo
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("%s [%s, %s]%s%s",
                nome,
                tipo != null ? tipo.getDescrizione() : "N/D",
                obbligatorio ? "Obbligatorio" : "Facoltativo",
                (!descrizione.isEmpty() ? " - " + descrizione : ""),
                (valore != null && !valore.isEmpty() ? " (Valore: " + valore + ")" : ""));
    }
}
