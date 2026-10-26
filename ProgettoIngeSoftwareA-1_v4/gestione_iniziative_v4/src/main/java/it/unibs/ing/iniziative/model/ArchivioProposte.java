package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rappresenta l'archivio storico persistente di tutte le proposte pubblicate nel sistema (Versione 4).
 * Conserva tutte le proposte (Aperte, Confermate, Annullate, Concluse, Ritirate) con il dettaglio
 * dei passaggi di stato, delle date in cui sono stati assunti e dell'elenco degli aderenti.
 * 
 * Invarianti di classe:
 * - La lista proposte non deve essere nulla e non deve contenere elementi nulli o invalidi.
 * 
 * @inv proposte != null
 * @inv forall p in proposte: p != null && p.checkInvariant()
 */
public class ArchivioProposte implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Proposta> proposte;

    public ArchivioProposte() {
        this.proposte = new ArrayList<>();
        assert checkInvariant() : "Invariante violato in ArchivioProposte";
    }

    public boolean checkInvariant() {
        if (proposte == null) return false;
        for (Proposta p : proposte) {
            if (p == null || !p.checkInvariant()) return false;
        }
        return true;
    }

    public List<Proposta> getProposte() {
        return Collections.unmodifiableList(proposte);
    }

    public boolean isVuoto() {
        return proposte.isEmpty();
    }

    public int getNumeroProposte() {
        return proposte.size();
    }

    public boolean registraProposta(Proposta p) {
        assert checkInvariant() : "Invariante violato prima di registraProposta";
        if (p == null || !p.checkInvariant()) return false;
        if (!proposte.contains(p)) {
            proposte.add(p);
            assert checkInvariant() : "Invariante violato dopo registraProposta";
            return true;
        }
        return false;
    }

    public List<Proposta> getPropostePerStato(StatoProposta stato) {
        if (stato == null) return Collections.emptyList();
        return proposte.stream()
                .filter(p -> p.getStato() == stato)
                .collect(Collectors.toList());
    }

    public List<Proposta> getPropostePerFruitore(String usernameFruitore) {
        if (usernameFruitore == null) return Collections.emptyList();
        return proposte.stream()
                .filter(p -> p.isIscritto(usernameFruitore))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce tutte le proposte ritirabili alla data di riferimento (Versione 4)
     * (proposte APERTE o CONFERMATE fino al giorno precedente la data di svolgimento).
     */
    public List<Proposta> getProposteRitirabili(LocalDate dataRiferimento) {
        final LocalDate dataRef = (dataRiferimento != null) ? dataRiferimento : LocalDate.now();
        return proposte.stream()
                .filter(p -> p.isRitirabile(dataRef))
                .collect(Collectors.toList());
    }

    /**
     * Visualizzazione completa dell'archivio delle proposte per il configuratore.
     */
    public void visualizzaArchivio() {
        System.out.println("\n=======================================================");
        System.out.println("               ARCHIVIO STORICO DELLE PROPOSTE         ");
        System.out.println("=======================================================");

        if (proposte.isEmpty()) {
            System.out.println("L'archivio delle proposte è attualmente vuoto.");
            System.out.println("=======================================================");
            return;
        }

        for (int i = 0; i < proposte.size(); i++) {
            Proposta p = proposte.get(i);
            System.out.println("\n[" + (i + 1) + "] " + p.getTitolo() + " (Categoria: " + p.getCategoria().getNome() + ")");
            System.out.println("    Stato Attuale: " + p.getStato().getDescrizione());
            System.out.println("    Aderenti: " + p.getNumeroAderenti() + " / " + p.getNumeroPartecipanti());
            System.out.println("    Cronologia passaggi di stato:");
            for (TransizioneStato ts : p.getCronologiaStati()) {
                System.out.println("      - " + ts.toString());
            }
            if (!p.getUsernameAderenti().isEmpty()) {
                System.out.println("    Iscritti: " + String.join(", ", p.getUsernameAderenti()));
            }
        }
        System.out.println("=======================================================");
    }
}
