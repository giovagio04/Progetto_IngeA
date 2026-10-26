package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Rappresenta una proposta di iniziativa afferente a una specifica categoria (Versione 3).
 * Estende la struttura della Versione 2 aggiungendo l'elenco degli aderenti fruitori
 * e la cronologia storica persistente di tutti i passaggi di stato assunti (VALIDA, APERTA,
 * CONFERMATA, ANNULLATA, CONCLUSA) con le rispettive date.
 * 
 * Invarianti di classe:
 * - La categoria e l'autore non devono essere nulli e devono rispettare i propri invarianti.
 * - La mappa dei valori, lo stato, la lista aderenti e la cronologia stati non devono essere nulli.
 * - Se la proposta è stata pubblicata (APERTA, CONFERMATA, ANNULLATA, CONCLUSA), dataPubblicazione != null;
 *   altrimenti dataPubblicazione == null.
 * - Ogni elemento di valori e cronologiaStati rispetta il proprio invariante.
 * 
 * @inv categoria != null && categoria.checkInvariant()
 * @inv autore != null && autore.checkInvariant()
 * @inv valori != null && stato != null && usernameAderenti != null && cronologiaStati != null
 * @inv (stato == StatoProposta.APERTA || stato == StatoProposta.CONFERMATA || stato == StatoProposta.ANNULLATA || stato == StatoProposta.CONCLUSA) <==> (dataPubblicazione != null)
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

    // Versione 3: Elenco aderenti e storico degli stati assunti
    private List<String> usernameAderenti;
    private List<TransizioneStato> cronologiaStati;

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
        this.usernameAderenti = new ArrayList<>();
        this.cronologiaStati = new ArrayList<>();
        this.cronologiaStati.add(new TransizioneStato(StatoProposta.VALIDA, LocalDate.now()));

        assert checkInvariant() : "Invariante violato al termine del costruttore Proposta";
    }

    /**
     * Verifica l'invariante di classe.
     */
    public boolean checkInvariant() {
        if (categoria == null || !categoria.checkInvariant()) return false;
        if (autore == null || !autore.checkInvariant()) return false;
        if (valori == null || stato == null || usernameAderenti == null || cronologiaStati == null) return false;
        boolean pubblicata = (stato == StatoProposta.APERTA || stato == StatoProposta.CONFERMATA
                || stato == StatoProposta.ANNULLATA || stato == StatoProposta.CONCLUSA);
        if (pubblicata != (dataPubblicazione != null)) return false;
        for (ValoreCampo vc : valori.values()) {
            if (vc == null || !vc.checkInvariant()) return false;
        }
        for (TransizioneStato ts : cronologiaStati) {
            if (ts == null || !ts.checkInvariant()) return false;
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

    public List<String> getUsernameAderenti() {
        return Collections.unmodifiableList(usernameAderenti);
    }

    public int getNumeroAderenti() {
        return usernameAderenti.size();
    }

    public List<TransizioneStato> getCronologiaStati() {
        return Collections.unmodifiableList(cronologiaStati);
    }

    /**
     * Assegna un valore a un campo della proposta (consentito solo prima della pubblicazione).
     */
    public void assegnaValore(Campo campo, Object valore) {
        assert checkInvariant() : "Invariante violato prima di assegnaValore";
        if (campo != null && dataPubblicazione == null) {
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

    public boolean isAlCompleto() {
        Integer partecipanti = getNumeroPartecipanti();
        return partecipanti != null && usernameAderenti.size() >= partecipanti;
    }

    public int getPostiRimanenti() {
        Integer partecipanti = getNumeroPartecipanti();
        if (partecipanti == null) return 0;
        return Math.max(0, partecipanti - usernameAderenti.size());
    }

    public boolean isIscritto(String usernameFruitore) {
        if (usernameFruitore == null) return false;
        return usernameAderenti.stream().anyMatch(u -> u.equalsIgnoreCase(usernameFruitore.trim()));
    }

    /**
     * Iscrive un fruitore all'iniziativa aperta (Versione 3).
     * Controlla:
     * 1. Stato APERTA;
     * 2. Termine ultimo di iscrizione non scaduto (fino alle ore 23:59 del giorno indicato);
     * 3. Unicità dell'iscrizione del fruitore alla proposta;
     * 4. Capienza massima ("Numero di partecipanti") non ancora raggiunta.
     * 
     * @param usernameFruitore username del fruitore che richiede l'adesione
     * @param dataCorrente data odierna per la verifica della scadenza
     * @return null se l'iscrizione è avvenuta con successo, altrimenti il messaggio d'errore
     */
    public String iscriviFruitore(String usernameFruitore, LocalDate dataCorrente) {
        assert checkInvariant() : "Invariante violato prima di iscriviFruitore";
        if (usernameFruitore == null || usernameFruitore.trim().isEmpty()) {
            return "Username non valido.";
        }
        if (this.stato != StatoProposta.APERTA) {
            return "La proposta non è aperta alle iscrizioni (stato attuale: " + stato.getDescrizione() + ").";
        }
        LocalDate d = (dataCorrente != null) ? dataCorrente : LocalDate.now();
        LocalDate termine = getTermineIscrizione();
        if (termine != null && d.isAfter(termine)) {
            return "Il periodo di iscrizione è terminato il " + termine.format(ValoreCampo.FORMATTER_DATA) + " alle ore 23:59.";
        }
        if (isIscritto(usernameFruitore)) {
            return "Risulti già iscritto a questa iniziativa. Non è possibile iscriversi più volte.";
        }
        if (isAlCompleto()) {
            return "La proposta ha già raccolto il numero di partecipanti previsto (" + getNumeroPartecipanti() + "). Iscrizioni chiuse.";
        }

        usernameAderenti.add(usernameFruitore.trim());
        assert isIscritto(usernameFruitore) : "Postcondizione violata in iscriviFruitore";
        assert checkInvariant() : "Invariante violato dopo iscriviFruitore";
        return null;
    }

    // --- Transizioni di Stato con registrazione storica ---

    /**
     * Pubblica la proposta in bacheca (VALIDA -> APERTA), rivalutando il termine ultimo di iscrizione
     * rispetto alla data di pubblicazione (Nota 8, pag. 23) e registrando il passaggio di stato.
     */
    public boolean pubblicaInBacheca(LocalDate dataPubb) {
        assert checkInvariant() : "Invariante violato prima di pubblicaInBacheca";
        if (this.stato != StatoProposta.VALIDA) {
            return false;
        }
        LocalDate dataEffettiva = (dataPubb != null) ? dataPubb : LocalDate.now();
        LocalDate termine = getTermineIscrizione();
        if (termine == null || !termine.isAfter(dataEffettiva)) {
            this.stato = StatoProposta.INCOMPLETA;
            return false; // Nota 8: il termine di iscrizione non è più successivo alla data di pubblicazione
        }
        this.stato = StatoProposta.APERTA;
        this.dataPubblicazione = dataEffettiva;
        this.cronologiaStati.add(new TransizioneStato(StatoProposta.APERTA, dataEffettiva));

        assert this.stato == StatoProposta.APERTA && this.dataPubblicazione != null : "Postcondizione violata in pubblicaInBacheca";
        assert checkInvariant() : "Invariante violato dopo pubblicaInBacheca";
        return true;
    }

    /**
     * Passaggio di stato APERTA -> CONFERMATA (allo scadere della mezzanotte del termine di iscrizione
     * quando il numero di aderenti eguaglia il numero di partecipanti previsto).
     */
    public boolean conferma(LocalDate data) {
        assert checkInvariant() : "Invariante violato prima di conferma";
        if (this.stato != StatoProposta.APERTA) {
            return false;
        }
        LocalDate d = (data != null) ? data : LocalDate.now();
        this.stato = StatoProposta.CONFERMATA;
        this.cronologiaStati.add(new TransizioneStato(StatoProposta.CONFERMATA, d));
        assert checkInvariant() : "Invariante violato dopo conferma";
        return true;
    }

    /**
     * Passaggio di stato APERTA -> ANNULLATA (allo scadere della mezzanotte del termine di iscrizione
     * quando non è stato raggiunto il numero di partecipanti previsto).
     */
    public boolean annulla(LocalDate data) {
        assert checkInvariant() : "Invariante violato prima di annulla";
        if (this.stato != StatoProposta.APERTA) {
            return false;
        }
        LocalDate d = (data != null) ? data : LocalDate.now();
        this.stato = StatoProposta.ANNULLATA;
        this.cronologiaStati.add(new TransizioneStato(StatoProposta.ANNULLATA, d));
        assert checkInvariant() : "Invariante violato dopo annulla";
        return true;
    }

    /**
     * Passaggio di stato CONFERMATA -> CONCLUSA (il giorno successivo alla data conclusiva).
     */
    public boolean concludi(LocalDate data) {
        assert checkInvariant() : "Invariante violato prima di concludi";
        if (this.stato != StatoProposta.CONFERMATA) {
            return false;
        }
        LocalDate d = (data != null) ? data : LocalDate.now();
        this.stato = StatoProposta.CONCLUSA;
        this.cronologiaStati.add(new TransizioneStato(StatoProposta.CONCLUSA, d));
        assert checkInvariant() : "Invariante violato dopo concludi";
        return true;
    }

    // --- Validazione vincoli (V2 + V3) ---

    /**
     * Valida la proposta rispetto a tutti i vincoli di specifica e aggiorna automaticamente
     * lo stato in VALIDA (se non ci sono errori) o INCOMPLETA (se ci sono errori).
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

        // Aggiornamento automatico dello stato (se non ancora pubblicata)
        if (this.dataPubblicazione == null) {
            if (errori.isEmpty()) {
                this.stato = StatoProposta.VALIDA;
                this.cronologiaStati.clear();
                this.cronologiaStati.add(new TransizioneStato(StatoProposta.VALIDA, dataRiferimento));
            } else {
                this.stato = StatoProposta.INCOMPLETA;
                this.cronologiaStati.clear();
            }
        }

        assert checkInvariant() : "Invariante violato dopo validaVincoli";
        return errori;
    }

    public String schedaDettagliata() {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("INIZIATIVA: ").append(getTitolo() != null ? getTitolo().toUpperCase() : "(Senza Titolo)").append("\n");
        sb.append("Categoria: ").append(categoria.getNome()).append("\n");
        sb.append("Stato attuale: ").append(stato.getDescrizione()).append("\n");
        sb.append("Proposta da: ").append(autore.getUsername()).append("\n");
        if (dataPubblicazione != null) {
            sb.append("Data pubblicazione in bacheca: ").append(dataPubblicazione.format(ValoreCampo.FORMATTER_DATA)).append("\n");
        }
        sb.append("Partecipanti: ").append(usernameAderenti.size()).append(" / ").append(getNumeroPartecipanti() != null ? getNumeroPartecipanti() : "-");
        if (stato == StatoProposta.APERTA) {
            sb.append(" (Posti disponibili: ").append(getPostiRimanenti()).append(")");
        }
        sb.append("\n-----------------------------------------------------------------\n");
        sb.append("DETTAGLIO DEI CAMPI COMPILATI:\n");
        for (ValoreCampo vc : valori.values()) {
            sb.append(String.format(" * %-30s : %s\n", vc.getCampo().getNome(), vc.getValoreFormattato()));
        }
        sb.append("-----------------------------------------------------------------\n");
        sb.append("STORICO DELLE TRANSIZIONI DI STATO:\n");
        for (TransizioneStato ts : cronologiaStati) {
            sb.append(" -> ").append(ts.toString()).append("\n");
        }
        sb.append("-----------------------------------------------------------------\n");
        sb.append("ELENCO ADERENTI (").append(usernameAderenti.size()).append("):\n");
        if (usernameAderenti.isEmpty()) {
            sb.append(" (Nessun aderente registrato)\n");
        } else {
            for (int i = 0; i < usernameAderenti.size(); i++) {
                sb.append(" ").append(i + 1).append(". ").append(usernameAderenti.get(i)).append("\n");
            }
        }
        sb.append("=================================================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] '%s' (%s) - Aderenti: %d/%d",
                stato.name(),
                getTitolo() != null ? getTitolo() : "(Senza Titolo)",
                categoria.getNome(),
                usernameAderenti.size(),
                getNumeroPartecipanti() != null ? getNumeroPartecipanti() : 0);
    }
}
