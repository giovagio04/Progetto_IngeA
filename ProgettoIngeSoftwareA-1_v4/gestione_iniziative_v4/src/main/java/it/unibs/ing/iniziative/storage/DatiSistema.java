package it.unibs.ing.iniziative.storage;

import it.unibs.ing.iniziative.model.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contenitore radice per lo stato persistente dell'applicazione (Versione 4).
 * Estende la struttura di V1, V2 e V3 aggiungendo il ritiro di una proposta (APERTA o CONFERMATA)
 * da parte di un configuratore con rimozione dalla bacheca (se aperta), aggiornamento
 * dell'ArchivioProposte e notifica automatica nello SpazioPersonale di tutti gli aderenti.
 * 
 * Invarianti di classe:
 * - gestoreUtenti != null && gestoreUtenti.checkInvariant()
 * - configurazioneSistema != null && configurazioneSistema.checkInvariant()
 * - categorie != null && senza duplicati né collisioni tra campi specifici e globali
 * - bacheca != null && bacheca.checkInvariant()
 * - archivioProposte != null && archivioProposte.checkInvariant()
 * 
 * @inv gestoreUtenti != null && gestoreUtenti.checkInvariant()
 * @inv configurazioneSistema != null && configurazioneSistema.checkInvariant()
 * @inv categorie != null
 * @inv bacheca != null && bacheca.checkInvariant()
 * @inv archivioProposte != null && archivioProposte.checkInvariant()
 */
public class DatiSistema implements Serializable {
    private static final long serialVersionUID = 1L;

    private GestoreUtenti gestoreUtenti;
    private ConfigurazioneSistema configurazioneSistema;
    private List<Categoria> categorie;
    private Bacheca bacheca;
    private ArchivioProposte archivioProposte;

    public DatiSistema() {
        this.gestoreUtenti = new GestoreUtenti(); 
        this.configurazioneSistema = new ConfigurazioneSistema();
        this.categorie = new ArrayList<>();
        this.bacheca = new Bacheca();
        this.archivioProposte = new ArchivioProposte();

        assert checkInvariant() : "Invariante di classe violato in DatiSistema al termine del costruttore";
    }

    public boolean checkInvariant() {
        if (gestoreUtenti == null || !gestoreUtenti.checkInvariant()) return false;
        if (configurazioneSistema == null || !configurazioneSistema.checkInvariant()) return false;
        if (categorie == null) return false;
        if (bacheca == null || !bacheca.checkInvariant()) return false;
        if (archivioProposte == null || !archivioProposte.checkInvariant()) return false;

        Set<String> nomiCategorie = new HashSet<>();
        for (Categoria cat : categorie) {
            if (cat == null || !cat.checkInvariant()) return false;
            if (!nomiCategorie.add(cat.getNome().toLowerCase())) return false;
            for (Campo cs : cat.getCampiSpecifici()) {
                if (isNomeCampoGlobalePresente(cs.getNome())) return false;
            }
        }
        return true;
    }

    public GestoreUtenti getGestoreUtenti() { 
        return gestoreUtenti; 
    }
    
    public ConfigurazioneSistema getConfigurazioneSistema() { 
        return configurazioneSistema; 
    }
    
    public List<Categoria> getCategorie() { 
        return Collections.unmodifiableList(categorie); 
    }

    public Bacheca getBacheca() {
        if (bacheca == null) {
            bacheca = new Bacheca();
        }
        return bacheca;
    }

    public ArchivioProposte getArchivioProposte() {
        if (archivioProposte == null) {
            archivioProposte = new ArchivioProposte();
        }
        // Sincronizza eventuali proposte aperte inserite direttamente in bacheca
        if (bacheca != null) {
            for (Proposta p : bacheca.getProposteAperte()) {
                archivioProposte.registraProposta(p);
            }
        }
        return archivioProposte;
    }

    public boolean esisteCategoria(String nome) {
        if (nome == null) return false;
        return categorie.stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nome.trim()));
    }

    public Categoria getCategoria(String nome) {
        if (nome == null) return null;
        return categorie.stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nome.trim()))
                .findFirst()
                .orElse(null);
    }

    public boolean aggiungiCategoria(Categoria c) {
        assert checkInvariant() : "Invariante violato prima di aggiungiCategoria";
        if (c == null || !c.checkInvariant()) return false;
        if (esisteCategoria(c.getNome())) return false;

        for (Campo cs : c.getCampiSpecifici()) {
            if (isNomeCampoGlobalePresente(cs.getNome())) return false;
        }

        categorie.add(c);
        assert checkInvariant() : "Invariante violato dopo aggiungiCategoria";
        return true;
    }

    public boolean rimuoviCategoria(String nome) {
        assert checkInvariant() : "Invariante violato prima di rimuoviCategoria";
        if (nome == null) return false;
        boolean rimossa = categorie.removeIf(c -> c.getNome().equalsIgnoreCase(nome.trim()));
        assert checkInvariant() : "Invariante violato dopo rimuoviCategoria";
        return rimossa;
    }

    public boolean isNomeCampoGlobalePresente(String nomeCampo) {
        if (nomeCampo == null) return false;
        return configurazioneSistema.haCampoBase(nomeCampo) || configurazioneSistema.haCampoComune(nomeCampo);
    }

    public boolean haCampoSpecificoInQualcheCategoria(String nomeCampo) {
        if (nomeCampo == null) return false;
        return categorie.stream().anyMatch(cat -> cat.haCampoSpecifico(nomeCampo));
    }

    public boolean isNomeCampoPresenteInCategoria(Categoria cat, String nomeCampo) {
        if (nomeCampo == null) return false;
        if (isNomeCampoGlobalePresente(nomeCampo)) return true;
        return cat != null && cat.haCampoSpecifico(nomeCampo);
    }

    public List<Campo> getTuttiICampiCategoria(Categoria cat) {
        if (!configurazioneSistema.isCampiBaseImpostati()) {
            configurazioneSistema.inizializzaCampiBasePredefiniti();
        }
        List<Campo> tutti = new ArrayList<>();
        tutti.addAll(configurazioneSistema.getCampiBase());
        tutti.addAll(configurazioneSistema.getCampiComuni());
        if (cat != null) {
            tutti.addAll(cat.getCampiSpecifici());
        }
        return Collections.unmodifiableList(tutti);
    }

    /**
     * Gestisce i passaggi di stato automatici allo scadere delle date ("Allo scadere della mezzanotte"):
     * 1. Se il termine di iscrizione è scaduto (dataOggi > termineIscrizione):
     *    - La proposta viene rimossa dalla bacheca;
     *    - Se aderenti == numeroPartecipanti -> CONFERMATA + promemoria nello spazio personale di ciascun iscritto;
     *    - Se aderenti < numeroPartecipanti -> ANNULLATA + notifica cancellazione nello spazio personale;
     * 2. Se una proposta CONFERMATA ha superato la data conclusiva (dataOggi > dataConclusiva):
     *    - Passa allo stato CONCLUSA.
     * 
     * @param dataOggi data di riferimento corrente per il controllo scadenze
     */
    public void aggiornaStatiEScadenze(LocalDate dataOggi) {
        if (dataOggi == null) {
            dataOggi = LocalDate.now();
        }

        // 1. Controllo proposte aperte in bacheca
        List<Proposta> aperte = new ArrayList<>(getBacheca().getProposteAperte());
        for (Proposta p : aperte) {
            LocalDate termine = p.getTermineIscrizione();
            if (termine != null && dataOggi.isAfter(termine)) {
                // Assicura che la proposta sia registrata nell'archivio storico prima della rimozione dalla bacheca
                getArchivioProposte().registraProposta(p);
                getBacheca().rimuoviProposta(p);

                if (p.getNumeroAderenti() == p.getNumeroPartecipanti()) {
                    // CONFERMA
                    p.conferma(dataOggi);

                    String testoPromemoria = String.format(java.util.Locale.ITALY,
                            "L'iniziativa '%s' è stata CONFERMATA e sarà attuata! Promemoria: si terrà il %s alle ore %s presso '%s'. Quota dovuta: %.2f €.",
                            p.getTitolo(),
                            p.getDataInizio() != null ? p.getDataInizio().format(ValoreCampo.FORMATTER_DATA) : "-",
                            p.getOra() != null ? p.getOra().format(ValoreCampo.FORMATTER_ORA) : "-",
                            p.getLuogo() != null ? p.getLuogo() : "-",
                            p.getQuotaIndividuale() != null ? p.getQuotaIndividuale() : 0.0);

                    for (String usr : p.getUsernameAderenti()) {
                        Fruitore f = gestoreUtenti.getFruitore(usr);
                        if (f != null) {
                            f.getSpazioPersonale().aggiungiNotifica(testoPromemoria, dataOggi);
                        }
                    }

                } else {
                    // ANNULLAMENTO
                    p.annulla(dataOggi);

                    String testoAnnullamento = String.format(
                            "L'iniziativa '%s' è stata ANNULLATA in quanto non è stato raggiunto il numero di partecipanti previsto (%d/%d).",
                            p.getTitolo(),
                            p.getNumeroAderenti(),
                            p.getNumeroPartecipanti() != null ? p.getNumeroPartecipanti() : 0);

                    for (String usr : p.getUsernameAderenti()) {
                        Fruitore f = gestoreUtenti.getFruitore(usr);
                        if (f != null) {
                            f.getSpazioPersonale().aggiungiNotifica(testoAnnullamento, dataOggi);
                        }
                    }
                }
            }
        }

        // 2. Controllo proposte confermate che devono passare a CONCLUSA
        for (Proposta p : getArchivioProposte().getPropostePerStato(StatoProposta.CONFERMATA)) {
            LocalDate conclusiva = p.getDataConclusiva();
            if (conclusiva != null && dataOggi.isAfter(conclusiva)) {
                p.concludi(dataOggi);
            }
        }
    }

    /**
     * Ritira una proposta aperta o confermata per iniziativa di un configuratore (Versione 4):
     * - Verifica che la proposta sia ritirabile alla data indicata (fino alle 23:59 del giorno precedente 'Data');
     * - Se la proposta era APERTA, la rimuove dalla Bacheca;
     * - Passa la proposta allo stato RITIRATA registrando il passaggio e la data nell'ArchivioProposte;
     * - Invia automaticamente una notifica nello SpazioPersonale di tutti i fruitori che avevano aderito;
     * - Mantiene invariato l'insieme degli iscritti associati alla proposta prima del ritiro.
     * 
     * @param proposta la proposta da ritirare
     * @param dataRitiro data di riferimento del ritiro
     * @param motivazione eventuale motivazione del ritiro
     * @return true se il ritiro è avvenuto con successo, false altrimenti
     */
    public boolean ritiraProposta(Proposta proposta, LocalDate dataRitiro, String motivazione) {
        assert checkInvariant() : "Invariante violato prima di ritiraProposta";
        if (proposta == null) return false;
        if (dataRitiro == null) {
            dataRitiro = LocalDate.now();
        }
        if (!proposta.isRitirabile(dataRitiro)) {
            return false;
        }

        // Assicura che la proposta sia registrata nell'archivio storico
        getArchivioProposte().registraProposta(proposta);

        // Se era APERTA, la rimuove dalla bacheca
        if (proposta.getStato() == StatoProposta.APERTA) {
            getBacheca().rimuoviProposta(proposta);
        }

        // Esegue il passaggio di stato a RITIRATA
        if (!proposta.ritira(dataRitiro)) {
            return false;
        }

        // Notifica automatica nello spazio personale di tutti gli iscritti
        String dettaglioMotivo = (motivazione != null && !motivazione.trim().isEmpty())
                ? " Motivazione: " + motivazione.trim() + "."
                : "";
        String testoRitiro = String.format(
                "AVVISO IMPORTANTE: L'iniziativa '%s' prevista per il %s è stata RITIRATA da un configuratore e non avrà luogo.%s",
                proposta.getTitolo(),
                proposta.getDataInizio() != null ? proposta.getDataInizio().format(ValoreCampo.FORMATTER_DATA) : "-",
                dettaglioMotivo);

        for (String usr : proposta.getUsernameAderenti()) {
            Fruitore f = gestoreUtenti.getFruitore(usr);
            if (f != null) {
                f.getSpazioPersonale().aggiungiNotifica(testoRitiro, dataRitiro);
            }
        }

        assert checkInvariant() : "Invariante violato dopo ritiraProposta";
        return true;
    }
}
