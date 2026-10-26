package it.unibs.ing.iniziative.model;

/**
 * I campi base previsti dalla specifica, nell'ordine in cui vengono richiesti al configuratore
 * al primo avvio. Per ciascuno la specifica indica il significato; il configuratore sceglie
 * il nome con cui il campo comparirà nel sistema.
 * Il tipo di dato è una scelta di progetto (la traccia non lo fissa) ed è associato al ruolo,
 * non alla posizione digitata dall'utente.
 */
public enum RuoloCampoBase {
    TITOLO("Titolo", "Nome di fantasia (esplicativo) attribuito all'iniziativa", TipoCampo.STRINGA),
    NUMERO_PARTECIPANTI("Numero di partecipanti", "Numero di persone da coinvolgere nell'iniziativa", TipoCampo.INTERO),
    TERMINE_ISCRIZIONE("Termine ultimo di iscrizione", "Ultimo giorno utile per iscriversi (fino alle ore 23:59)", TipoCampo.DATA),
    LUOGO("Luogo", "Indirizzo del luogo che ospita l'iniziativa o, se itinerante, luogo di ritrovo", TipoCampo.STRINGA),
    DATA("Data", "Data di svolgimento (o di inizio) dell'iniziativa; almeno 2 giorni dopo il termine di iscrizione", TipoCampo.DATA),
    ORA("Ora", "Ora di ritrovo dei partecipanti nel luogo, nella data di inizio", TipoCampo.ORA),
    QUOTA_INDIVIDUALE("Quota individuale", "Spesa (o stima) che ogni partecipante deve sostenere; può essere nulla", TipoCampo.DECIMALE),
    DATA_CONCLUSIVA("Data conclusiva", "Data di conclusione dell'iniziativa; coincide con Data se termina lo stesso giorno", TipoCampo.DATA);

    private final String etichetta;
    private final String descrizione;
    private final TipoCampo tipo;

    RuoloCampoBase(String etichetta, String descrizione, TipoCampo tipo) {
        this.etichetta = etichetta;
        this.descrizione = descrizione;
        this.tipo = tipo;
    }

    /** Nome del ruolo come indicato dalla specifica (usato come suggerimento a video). */
    public String getEtichetta() { return etichetta; }

    /** Significato del campo secondo la specifica. */
    public String getDescrizione() { return descrizione; }

    /** Tipo di dato associato al ruolo. */
    public TipoCampo getTipo() { return tipo; }
}
