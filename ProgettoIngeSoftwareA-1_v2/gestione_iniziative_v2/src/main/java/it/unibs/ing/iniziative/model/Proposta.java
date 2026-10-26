package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Rappresenta una proposta di iniziativa afferente a una specifica categoria.
 * Contiene i valori assegnati a tutti i campi (base, comuni e specifici) della categoria.
 * Gestisce la validazione automatica dei vincoli e le transizioni di stato (VALIDA -> APERTA).
 * 
 * Invarianti di classe:
 * - La categoria e l'autore non devono essere nulli e devono rispettare i propri invarianti.
 * - La mappa dei valori e lo stato non devono essere nulli.
 * - Se lo stato è APERTA, la data di pubblicazione non deve essere nulla; altrimenti deve essere nulla.
 * 
 * @inv categoria != null && categoria.checkInvariant()
 * @inv autore != null && autore.checkInvariant()
 * @inv valori != null && stato != null
 * @inv (stato == StatoProposta.APERTA) <==> (dataPubblicazione != null)
 */
public class Proposta implements Serializable {
    private static final long serialVersionUID = 1L;

    // Nomi standard dei campi base (da specifica / RuoloCampoBase)
    public static final String CAMPO_TITOLO = RuoloCampoBase.TITOLO.getEtichetta();
    public static final String CAMPO_PARTECIPANTI = RuoloCampoBase.NUMERO_PARTECIPANTI.getEtichetta();
    public static final String CAMPO_TERMINE_ISCRIZIONE = RuoloCampoBase.TERMINE_ISCRIZIONE.getEtichetta();
    public static final String CAMPO_LUOGO = RuoloCampoBase.LUOGO.getEtichetta();
    public static final String CAMPO_DATA = RuoloCampoBase.DATA.getEtichetta();
    public static final String CAMPO_ORA = RuoloCampoBase.ORA.getEtichetta();
    public static final String CAMPO_QUOTA = RuoloCampoBase.QUOTA_INDIVIDUALE.getEtichetta();
    public static final String CAMPO_DATA_CONCLUSIVA = RuoloCampoBase.DATA_CONCLUSIVA.getEtichetta();

    private Categoria categoria;
    private Map<String, ValoreCampo> valori;
    private Configuratore autore;
    private StatoProposta stato;
    private LocalDate dataPubblicazione;

    /**
     * Costruttore: istanzia una nuova proposta per la categoria e il configuratore indicati.
     * 
     * @param categoria la categoria di appartenenza
     * @param autore il configuratore creatore
     * @pre categoria != null && autore != null
     * @post checkInvariant()
     */
    public Proposta(Categoria categoria, Configuratore autore) {
        assert categoria != null : "Precondizione violata: la categoria non può essere nulla";
        assert autore != null : "Precondizione violata: l'autore non può essere nullo";

        this.categoria = categoria;
        this.autore = autore;
        this.valori = new LinkedHashMap<>();
        this.stato = StatoProposta.VALIDA;
        this.dataPubblicazione = null;

        assert checkInvariant() : "Invariante violato al termine del costruttore Proposta";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è rispettato, false altrimenti
     */
    public boolean checkInvariant() {
        if (categoria == null || !categoria.checkInvariant()) return false;
        if (autore == null || !autore.checkInvariant()) return false;
        if (valori == null || stato == null) return false;
        if ((stato == StatoProposta.APERTA) != (dataPubblicazione != null)) return false;
        for (ValoreCampo vc : valori.values()) {
            if (vc == null || !vc.checkInvariant()) return false;
        }
        return true;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Configuratore getAutore() {
        return autore;
    }

    public StatoProposta getStato() {
        return stato;
    }

    public LocalDate getDataPubblicazione() {
        return dataPubblicazione;
    }

    public Map<String, ValoreCampo> getValori() {
        return Collections.unmodifiableMap(valori);
    }

    /**
     * Assegna un valore a un campo della proposta (consentito solo finché la proposta non è APERTA).
     * 
     * @param campo il campo da compilare
     * @param valore il valore da associare
     */
    public void assegnaValore(Campo campo, Object valore) {
        assert checkInvariant() : "Invariante violato prima di assegnaValore";
        if (campo != null && stato != StatoProposta.APERTA) {
            valori.put(campo.getNome().toLowerCase(), new ValoreCampo(campo, valore));
        }
        assert checkInvariant() : "Invariante violato dopo assegnaValore";
    }

    public ValoreCampo getValoreCampo(String nomeCampo) {
        if (nomeCampo == null) return null;
        return valori.get(nomeCampo.trim().toLowerCase());
    }

    // --- Metodi helper di accesso ai campi base ---

    public String getTitolo() {
        ValoreCampo vc = getValoreCampo(CAMPO_TITOLO);
        return vc != null ? vc.asString() : null;
    }

    public Integer getNumeroPartecipanti() {
        ValoreCampo vc = getValoreCampo(CAMPO_PARTECIPANTI);
        return vc != null ? vc.asInteger() : null;
    }

    public LocalDate getTermineIscrizione() {
        ValoreCampo vc = getValoreCampo(CAMPO_TERMINE_ISCRIZIONE);
        return vc != null ? vc.asLocalDate() : null;
    }

    public String getLuogo() {
        ValoreCampo vc = getValoreCampo(CAMPO_LUOGO);
        return vc != null ? vc.asString() : null;
    }

    public LocalDate getDataInizio() {
        ValoreCampo vc = getValoreCampo(CAMPO_DATA);
        return vc != null ? vc.asLocalDate() : null;
    }

    public LocalTime getOra() {
        ValoreCampo vc = getValoreCampo(CAMPO_ORA);
        return vc != null ? vc.asLocalTime() : null;
    }

    public Double getQuotaIndividuale() {
        ValoreCampo vc = getValoreCampo(CAMPO_QUOTA);
        return vc != null ? vc.asDouble() : null;
    }

    public LocalDate getDataConclusiva() {
        ValoreCampo vc = getValoreCampo(CAMPO_DATA_CONCLUSIVA);
        return vc != null ? vc.asLocalDate() : null;
    }

    /**
     * Valida la proposta rispetto a tutti i vincoli di specifica e aggiorna automaticamente
     * lo stato in VALIDA (se non ci sono errori) o INCOMPLETA (se ci sono errori):
     * 1. Tutti i campi obbligatori della categoria (base, comuni e specifici) devono essere compilati.
     * 2. "Termine ultimo di iscrizione" deve essere successivo alla data odierna.
     * 3. "Data" (data inizio) deve essere successiva di almeno due giorni rispetto a "Termine ultimo di iscrizione".
     * 4. "Data conclusiva" deve essere >= "Data".
     * 5. "Quota individuale" >= 0.
     * 6. "Numero di partecipanti" > 0.
     * 
     * @param dataRiferimento Data odierna di verifica
     * @param campiAttesi Lista di tutti i campi associati alla categoria al momento della creazione
     * @return Lista di eventuali errori riscontrati (vuota se la proposta è valida)
     * @post result.isEmpty() && stato != StatoProposta.APERTA ==> getStato() == StatoProposta.VALIDA
     * @post !result.isEmpty() && stato != StatoProposta.APERTA ==> getStato() == StatoProposta.INCOMPLETA
     */
    public List<String> validaVincoli(LocalDate dataRiferimento, List<Campo> campiAttesi) {
        assert checkInvariant() : "Invariante violato prima di validaVincoli";
        List<String> errori = new ArrayList<>();

        if (dataRiferimento == null) {
            dataRiferimento = LocalDate.now();
        }

        // 1. Controllo compilazione di tutti i campi obbligatori
        if (campiAttesi != null) {
            for (Campo c : campiAttesi) {
                if (c.isObbligatorio()) {
                    ValoreCampo vc = getValoreCampo(c.getNome());
                    if (vc == null || !vc.isCompilato()) {
                        errori.add("Il campo obbligatorio '" + c.getNome() + "' non è stato compilato.");
                    }
                }
            }
        }

        // 2. Controllo Titolo e Luogo non vuoti
        String titolo = getTitolo();
        if (titolo == null || titolo.trim().isEmpty()) {
            errori.add("Il campo 'Titolo' è obbligatorio e non può essere vuoto.");
        }
        String luogo = getLuogo();
        if (luogo == null || luogo.trim().isEmpty()) {
            errori.add("Il campo 'Luogo' è obbligatorio e non può essere vuoto.");
        }

        // 3. Controllo Ora valida
        if (getOra() == null) {
            errori.add("Il campo 'Ora' deve contenere un orario valido (hh:mm).");
        }

        // 4. Controllo Numero di partecipanti (> 0)
        Integer partecipanti = getNumeroPartecipanti();
        if (partecipanti == null || partecipanti <= 0) {
            errori.add("Il numero di partecipanti deve essere un intero positivo (> 0).");
        }

        // 5. Controllo Quota individuale (>= 0)
        Double quota = getQuotaIndividuale();
        if (quota == null || quota < 0.0) {
            errori.add("La quota individuale non può essere negativa (può essere nulla).");
        }

        // 6. Controllo Termine ultimo di iscrizione (> dataRiferimento)
        LocalDate termineIscrizione = getTermineIscrizione();
        if (termineIscrizione == null) {
            errori.add("La data 'Termine ultimo di iscrizione' è obbligatoria.");
        } else if (!termineIscrizione.isAfter(dataRiferimento)) {
            errori.add("Il 'Termine ultimo di iscrizione' (" + termineIscrizione.format(ValoreCampo.FORMATTER_DATA) + 
                    ") deve essere successivo alla data corrente (" + dataRiferimento.format(ValoreCampo.FORMATTER_DATA) + ").");
        }

        // 7. Controllo Data inizio (>= termineIscrizione + 2 giorni)
        LocalDate dataInizio = getDataInizio();
        if (dataInizio == null) {
            errori.add("La 'Data' di svolgimento è obbligatoria.");
        } else if (termineIscrizione != null) {
            long giorniDiDifferenza = ChronoUnit.DAYS.between(termineIscrizione, dataInizio);
            if (giorniDiDifferenza < 2) {
                errori.add("La 'Data' di svolgimento (" + dataInizio.format(ValoreCampo.FORMATTER_DATA) + 
                        ") deve essere successiva di almeno 2 giorni rispetto al termine di iscrizione (" + 
                        termineIscrizione.format(ValoreCampo.FORMATTER_DATA) + "). Trovati giorni di scarto: " + giorniDiDifferenza + ".");
            }
        }

        // 8. Controllo Data conclusiva (>= Data inizio)
        LocalDate dataFine = getDataConclusiva();
        if (dataFine == null) {
            errori.add("La 'Data conclusiva' è obbligatoria.");
        } else if (dataInizio != null && dataFine.isBefore(dataInizio)) {
            errori.add("La 'Data conclusiva' (" + dataFine.format(ValoreCampo.FORMATTER_DATA) + 
                    ") non può essere precedente alla data di inizio (" + dataInizio.format(ValoreCampo.FORMATTER_DATA) + ").");
        }

        // Assegnazione automatica dello stato (se non già pubblicata in bacheca)
        if (this.stato != StatoProposta.APERTA) {
            this.stato = errori.isEmpty() ? StatoProposta.VALIDA : StatoProposta.INCOMPLETA;
        }

        assert checkInvariant() : "Invariante violato dopo validaVincoli";
        return errori;
    }

    /**
     * Pubblica la proposta nella bacheca del sistema, facendola passare dallo stato VALIDA allo stato APERTA.
     * Rivaluta che il termine ultimo di iscrizione sia successivo alla data di pubblicazione (Nota 8, pag. 23)
     * e registra la data di pubblicazione.
     * 
     * @param dataPubb data in cui avviene la pubblicazione
     * @return true se la proposta era VALIDA ed è passata ad APERTA; false altrimenti
     * @post result ==> getStato() == StatoProposta.APERTA && getDataPubblicazione() != null
     */
    public boolean pubblicaInBacheca(LocalDate dataPubb) {
        assert checkInvariant() : "Invariante violato prima di pubblicaInBacheca";
        if (this.stato != StatoProposta.VALIDA) {
            return false; // Solo una proposta VALIDA può essere pubblicata
        }
        LocalDate dataEffettiva = (dataPubb != null) ? dataPubb : LocalDate.now();
        LocalDate termine = getTermineIscrizione();
        if (termine == null || !termine.isAfter(dataEffettiva)) {
            this.stato = StatoProposta.INCOMPLETA;
            return false; // Il termine di iscrizione non è più successivo alla data di pubblicazione
        }
        this.stato = StatoProposta.APERTA;
        this.dataPubblicazione = dataEffettiva;

        assert this.stato == StatoProposta.APERTA && this.dataPubblicazione != null : "Postcondizione violata in pubblicaInBacheca";
        assert checkInvariant() : "Invariante violato dopo pubblicaInBacheca";
        return true;
    }

    /**
     * Genera una visualizzazione testuale completa e ordinata dell'iniziativa.
     */
    public String schedaDettagliata() {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("INIZIATIVA: ").append(getTitolo() != null ? getTitolo().toUpperCase() : "(Senza Titolo)").append("\n");
        sb.append("Categoria: ").append(categoria.getNome()).append("\n");
        sb.append("Stato proposta: ").append(stato.getDescrizione()).append("\n");
        sb.append("Proposta da: ").append(autore.getUsername()).append("\n");
        if (dataPubblicazione != null) {
            sb.append("Data pubblicazione in bacheca: ").append(dataPubblicazione.format(ValoreCampo.FORMATTER_DATA)).append("\n");
        }
        sb.append("-----------------------------------------------------------------\n");
        sb.append("DETTAGLIO DEI CAMPI COMPILATI:\n");
        for (ValoreCampo vc : valori.values()) {
            sb.append(String.format(" * %-30s : %s\n", vc.getCampo().getNome(), vc.getValoreFormattato()));
        }
        sb.append("=================================================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] '%s' (%s) - Pubblicata il: %s",
                stato.name(),
                getTitolo() != null ? getTitolo() : "(Senza Titolo)",
                categoria.getNome(),
                dataPubblicazione != null ? dataPubblicazione.format(ValoreCampo.FORMATTER_DATA) : "Non pubblicata");
    }
}
