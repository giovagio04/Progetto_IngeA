package it.unibs.ing.iniziative.model;

/**
 * Rappresenta gli stati possibili di una proposta di iniziativa nella Versione 2.
 * - INCOMPLETA: la proposta non rispetta tutti i vincoli di dominio o manca di campi obbligatori.
 * - VALIDA: la proposta rispetta tutti i vincoli di dominio ed è completa di tutti i campi obbligatori;
 *           risiede nella sessione corrente del configuratore ma non è ancora stata pubblicata in bacheca.
 * - APERTA: la proposta è stata pubblicata nella bacheca ed è attiva la raccolta delle iscrizioni;
 *           viene salvata in modo persistente con la rispettiva data di pubblicazione.
 */
public enum StatoProposta {
    INCOMPLETA("Non valida (vincoli non soddisfatti)"),
    VALIDA("Valida (in sessione, non pubblicata)"),
    APERTA("Aperta (pubblicata in bacheca)");

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
