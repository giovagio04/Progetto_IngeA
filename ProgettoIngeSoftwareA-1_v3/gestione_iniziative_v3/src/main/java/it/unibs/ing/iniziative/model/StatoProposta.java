package it.unibs.ing.iniziative.model;

/**
 * Rappresenta gli stati di una proposta di iniziativa nel suo ciclo di vita (Versione 3):
 * - INCOMPLETA: creata ma non rispetta tutti i vincoli di validità;
 * - VALIDA: creata dal configuratore, rispetta i vincoli ma non ancora pubblicata in bacheca;
 * - APERTA: pubblicata in bacheca, aperta alla raccolta delle iscrizioni dei fruitori;
 * - CONFERMATA: chiuse le iscrizioni con il numero di partecipanti previsto raggiunto;
 * - ANNULLATA: chiuse le iscrizioni senza aver raggiunto il numero di partecipanti previsto;
 * - CONCLUSA: giorno successivo alla data conclusiva per un'iniziativa confermata e svolta.
 */
public enum StatoProposta {
    INCOMPLETA("Incompleta / Non valida (vincoli non soddisfatti)"),
    VALIDA("Valida (in sessione, non pubblicata)"),
    APERTA("Aperta (in bacheca per iscrizioni)"),
    CONFERMATA("Confermata (numero partecipanti raggiunto)"),
    ANNULLATA("Annullata (partecipanti insufficienti)"),
    CONCLUSA("Conclusa (evento terminato)");

    private final String descrizione;

    StatoProposta(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    @Override
    public String toString() {
        return descrizione;
    }
}
