package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rappresenta la bacheca / archivio delle proposte aperte del sistema.
 * Contiene esclusivamente proposte nello stato APERTA che sono state pubblicate e salvate in modo persistente.
 * 
 * Invarianti di classe:
 * - La lista proposteAperte non deve essere nulla.
 * - Ogni proposta in bacheca non deve essere nulla, deve rispettare il proprio invariante
 *   e deve trovarsi nello stato APERTA con data di pubblicazione non nulla.
 * 
 * @inv proposteAperte != null
 * @inv forall p in proposteAperte: p != null && p.checkInvariant() && p.getStato() == StatoProposta.APERTA
 */
public class Bacheca implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Proposta> proposteAperte;

    /**
     * Costruttore: inizializza una bacheca vuota.
     * 
     * @post isVuota()
     * @post checkInvariant()
     */
    public Bacheca() {
        this.proposteAperte = new ArrayList<>();
        assert checkInvariant() : "Invariante violato in Bacheca al termine del costruttore";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è verificato, false altrimenti
     */
    public boolean checkInvariant() {
        if (proposteAperte == null) {
            return false;
        }
        for (Proposta p : proposteAperte) {
            if (p == null || !p.checkInvariant() || p.getStato() != StatoProposta.APERTA || p.getDataPubblicazione() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Restituisce la vista immutabile delle proposte aperte.
     */
    public List<Proposta> getProposteAperte() {
        return Collections.unmodifiableList(proposteAperte);
    }

    public boolean isVuota() {
        return proposteAperte.isEmpty();
    }

    public int getNumeroProposteAperte() {
        return proposteAperte.size();
    }

    /**
     * Aggiunge una proposta nello stato APERTA alla bacheca.
     * 
     * @param proposta la proposta aperta da inserire
     * @return true se inserita con successo; false se nulla, non APERTA o già presente
     * @post result ==> getProposteAperte().contains(proposta)
     */
    public boolean aggiungiPropostaAperta(Proposta proposta) {
        assert checkInvariant() : "Invariante violato prima di aggiungiPropostaAperta";
        if (proposta == null || !proposta.checkInvariant()) return false;
        if (proposta.getStato() != StatoProposta.APERTA) {
            return false; // In bacheca possono entrare solo proposte nello stato APERTA
        }
        if (!proposteAperte.contains(proposta)) {
            proposteAperte.add(proposta);
            assert checkInvariant() : "Invariante violato dopo aggiungiPropostaAperta";
            return true;
        }
        return false;
    }

    /**
     * Restituisce le proposte aperte appartenenti alla categoria specificata.
     */
    public List<Proposta> getPropostePerCategoria(Categoria categoria) {
        if (categoria == null) return Collections.emptyList();
        return proposteAperte.stream()
                .filter(p -> p.getCategoria().equals(categoria))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce le proposte aperte appartenenti alla categoria identificata dal nome.
     */
    public List<Proposta> getPropostePerCategoria(String nomeCategoria) {
        if (nomeCategoria == null) return Collections.emptyList();
        return proposteAperte.stream()
                .filter(p -> p.getCategoria().getNome().equalsIgnoreCase(nomeCategoria.trim()))
                .collect(Collectors.toList());
    }

    /**
     * Stampa formattata dell'intera bacheca raggruppata categoria per categoria.
     */
    public void visualizzaBacheca(List<Categoria> categorieDisponibili) {
        System.out.println("\n=======================================================");
        System.out.println("                 BACHECA DELLE PROPOSTE APERTE          ");
        System.out.println("=======================================================");

        if (proposteAperte.isEmpty()) {
            System.out.println("Attualmente la bacheca è vuota: nessuna proposta aperta.");
            System.out.println("=======================================================");
            return;
        }

        // Mostra le proposte per ciascuna categoria nota
        if (categorieDisponibili != null) {
            for (Categoria cat : categorieDisponibili) {
                List<Proposta> proposteCat = getPropostePerCategoria(cat);
                if (!proposteCat.isEmpty()) {
                    System.out.println("\n>>> CATEGORIA: " + cat.getNome().toUpperCase() + " (" + proposteCat.size() + " proposta/e aperta/e)");
                    for (int i = 0; i < proposteCat.size(); i++) {
                        Proposta p = proposteCat.get(i);
                        System.out.println("   [" + (i + 1) + "] " + p.getTitolo() + 
                                " | Data Inizio: " + (p.getDataInizio() != null ? p.getDataInizio().format(ValoreCampo.FORMATTER_DATA) : "-") +
                                " | Iscrizioni entro: " + (p.getTermineIscrizione() != null ? p.getTermineIscrizione().format(ValoreCampo.FORMATTER_DATA) : "-") +
                                " | Pubblicata il: " + (p.getDataPubblicazione() != null ? p.getDataPubblicazione().format(ValoreCampo.FORMATTER_DATA) : "-") +
                                " (Autore: " + p.getAutore().getUsername() + ")");
                    }
                }
            }
        }

        // Mostra eventuali proposte la cui categoria è stata rimossa successivamente (archivio storico)
        List<Proposta> orfane = proposteAperte.stream()
                .filter(p -> categorieDisponibili == null || !categorieDisponibili.contains(p.getCategoria()))
                .collect(Collectors.toList());
        if (!orfane.isEmpty()) {
            System.out.println("\n>>> ALTRE CATEGORIE ARCHIVIATE (" + orfane.size() + " proposta/e):");
            for (Proposta p : orfane) {
                System.out.println("   * [Cat: " + p.getCategoria().getNome() + "] " + p.getTitolo() + 
                        " (Pubblicata il: " + p.getDataPubblicazione().format(ValoreCampo.FORMATTER_DATA) + ")");
            }
        }
        System.out.println("=======================================================");
    }
}
