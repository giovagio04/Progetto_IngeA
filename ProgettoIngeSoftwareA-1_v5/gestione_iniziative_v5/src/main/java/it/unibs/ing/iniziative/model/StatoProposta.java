package it.unibs.ing.iniziative.model;

/**
 * Enumerazione degli stati che una proposta di iniziativa può assumere (Versione 4).
 * Estende gli stati di V2 e V3 con lo stato RITIRATA per cause di forza maggiore.
 */
public enum StatoProposta {
    INCOMPLETA("Incompleta (non rispetta tutti i vincoli)"),
    VALIDA("Valida (non ancora pubblicata)"),
    APERTA("Aperta (pubblicata in bacheca, iscrizioni attive)"),
    CONFERMATA("Confermata (raggiunto il numero di partecipanti previsto)"),
    ANNULLATA("Annullata (non raggiunto il numero di partecipanti entro il termine)"),
    CONCLUSA("Conclusa (iniziativa terminata)"),
    RITIRATA("Ritirata (cancellata da un configuratore prima dello svolgimento)");

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
