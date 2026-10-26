package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Rappresenta una notifica recapitata nello spazio personale di un fruitore.
 * 
 * Invarianti di classe:
 * - id > 0
 * - testo != null && !testo.trim().isEmpty()
 * - dataRicezione != null
 * 
 * @inv id > 0 && testo != null && !testo.trim().isEmpty() && dataRicezione != null
 */
public class Notifica implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int id;
    private String testo;
    private LocalDate dataRicezione;

    /**
     * Costruisce una nuova notifica.
     * 
     * @param id identificativo univoco nello spazio personale (> 0)
     * @param testo contenuto testuale della notifica
     * @param dataRicezione data di generazione della notifica
     * @pre id > 0 && testo != null && !testo.trim().isEmpty()
     * @post checkInvariant()
     */
    public Notifica(int id, String testo, LocalDate dataRicezione) {
        assert id > 0 : "Precondizione violata: id notifica deve essere > 0";
        assert testo != null && !testo.trim().isEmpty() : "Precondizione violata: il testo della notifica non può essere vuoto";
        this.id = id;
        this.testo = testo.trim();
        this.dataRicezione = dataRicezione != null ? dataRicezione : LocalDate.now();
        assert checkInvariant() : "Invariante violato in Notifica";
    }

    public boolean checkInvariant() {
        return id > 0 && testo != null && !testo.trim().isEmpty() && dataRicezione != null;
    }

    public int getId() {
        return id;
    }

    public String getTesto() {
        return testo;
    }

    public LocalDate getDataRicezione() {
        return dataRicezione;
    }

    @Override
    public String toString() {
        return String.format("[%s] (Notifica #%d) %s", dataRicezione.format(FORMATTER_DATA), id, testo);
    }
}
