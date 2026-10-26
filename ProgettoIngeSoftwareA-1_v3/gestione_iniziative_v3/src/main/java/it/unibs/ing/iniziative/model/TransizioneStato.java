package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Memorizza una transizione di stato di una proposta, registrando il nuovo stato
 * e la data esatta in cui tale stato è stato assunto.
 * 
 * Invarianti di classe:
 * - stato != null && dataAssunzione != null
 * 
 * @inv stato != null && dataAssunzione != null
 */
public class TransizioneStato implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private StatoProposta stato;
    private LocalDate dataAssunzione;

    /**
     * Costruisce una registrazione di passaggio di stato.
     * 
     * @param stato lo stato assunto dalla proposta
     * @param dataAssunzione la data in cui lo stato è stato assunto
     * @pre stato != null
     * @post checkInvariant()
     */
    public TransizioneStato(StatoProposta stato, LocalDate dataAssunzione) {
        assert stato != null : "Precondizione violata: lo stato non può essere nullo";
        this.stato = stato;
        this.dataAssunzione = dataAssunzione != null ? dataAssunzione : LocalDate.now();
        assert checkInvariant() : "Invariante violato in TransizioneStato";
    }

    public boolean checkInvariant() {
        return stato != null && dataAssunzione != null;
    }

    public StatoProposta getStato() {
        return stato;
    }

    public LocalDate getDataAssunzione() {
        return dataAssunzione;
    }

    @Override
    public String toString() {
        return String.format("%s [Data: %s]", stato.getDescrizione(), dataAssunzione.format(FORMATTER_DATA));
    }
}
