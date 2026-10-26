package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rappresenta la bacheca delle sole proposte nello stato APERTA del sistema (Versione 3).
 * Una proposta rimane in bacheca finché è aperta; allo scadere della mezzanotte del termine
 * di iscrizione viene rimossa dalla bacheca.
 * 
 * Invarianti di classe:
 * - La lista proposteAperte non deve essere nulla.
 * - Ogni proposta in bacheca non è nulla, rispetta il proprio invariante ed è nello stato APERTA.
 * 
 * @inv proposteAperte != null
 * @inv forall p in proposteAperte: p != null && p.checkInvariant() && p.getStato() == StatoProposta.APERTA
 */
public class Bacheca implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Proposta> proposteAperte;

    public Bacheca() {
        this.proposteAperte = new ArrayList<>();
        assert checkInvariant() : "Invariante violato in Bacheca";
    }

    public boolean checkInvariant() {
        if (proposteAperte == null) return false;
        for (Proposta p : proposteAperte) {
            if (p == null || !p.checkInvariant() || p.getStato() != StatoProposta.APERTA || p.getDataPubblicazione() == null) {
                return false;
            }
        }
        return true;
    }

    public List<Proposta> getProposteAperte() {
        return Collections.unmodifiableList(proposteAperte);
    }

    public boolean isVuota() {
        return proposteAperte.isEmpty();
    }

    public int getNumeroProposteAperte() {
        return proposteAperte.size();
    }

    public boolean aggiungiPropostaAperta(Proposta proposta) {
        assert checkInvariant() : "Invariante violato prima di aggiungiPropostaAperta";
        if (proposta == null || !proposta.checkInvariant()) return false;
        if (proposta.getStato() != StatoProposta.APERTA) {
            return false;
        }
        if (!proposteAperte.contains(proposta)) {
            proposteAperte.add(proposta);
            assert checkInvariant() : "Invariante violato dopo aggiungiPropostaAperta";
            return true;
        }
        return false;
    }

    public boolean rimuoviProposta(Proposta proposta) {
        if (proposta == null) return false;
        return proposteAperte.remove(proposta);
    }

    public List<Proposta> getPropostePerCategoria(Categoria categoria) {
        if (categoria == null) return Collections.emptyList();
        return proposteAperte.stream()
                .filter(p -> p.getCategoria().equals(categoria))
                .collect(Collectors.toList());
    }

    public List<Proposta> getPropostePerCategoria(String nomeCategoria) {
        if (nomeCategoria == null) return Collections.emptyList();
        return proposteAperte.stream()
                .filter(p -> p.getCategoria().getNome().equalsIgnoreCase(nomeCategoria.trim()))
                .collect(Collectors.toList());
    }

    /**
     * Stampa della bacheca suddivisa per categoria.
     */
    public void visualizzaBacheca(List<Categoria> categorieDisponibili) {
        visualizzaBachecaPerUtente(categorieDisponibili, null);
    }

    /**
     * Stampa della bacheca personalizzata per l'utente, evidenziando se è già iscritto.
     */
    public void visualizzaBachecaPerUtente(List<Categoria> categorieDisponibili, String usernameFruitore) {
        System.out.println("\n=======================================================");
        System.out.println("                 BACHECA DELLE PROPOSTE APERTE          ");
        System.out.println("=======================================================");

        if (proposteAperte.isEmpty()) {
            System.out.println("Attualmente la bacheca è vuota: nessuna proposta aperta.");
            System.out.println("=======================================================");
            return;
        }

        if (categorieDisponibili != null) {
            for (Categoria cat : categorieDisponibili) {
                List<Proposta> proposteCat = getPropostePerCategoria(cat);
                if (!proposteCat.isEmpty()) {
                    System.out.println("\n>>> CATEGORIA: " + cat.getNome().toUpperCase() + " (" + proposteCat.size() + " proposta/e aperta/e)");
                    for (int i = 0; i < proposteCat.size(); i++) {
                        Proposta p = proposteCat.get(i);
                        String statoIscritto = "";
                        if (usernameFruitore != null && p.isIscritto(usernameFruitore)) {
                            statoIscritto = " [SEI ISCRITTO]";
                        }
                        System.out.println("   [" + (i + 1) + "] " + p.getTitolo() + 
                                " | Inizio: " + (p.getDataInizio() != null ? p.getDataInizio().format(ValoreCampo.FORMATTER_DATA) : "-") +
                                " | Iscrizioni entro: " + (p.getTermineIscrizione() != null ? p.getTermineIscrizione().format(ValoreCampo.FORMATTER_DATA) : "-") +
                                " | Posti: " + p.getNumeroAderenti() + "/" + p.getNumeroPartecipanti() +
                                statoIscritto);
                    }
                }
            }
        }

        // Eventuali categorie rimosse successivamente
        List<Proposta> orfane = proposteAperte.stream()
                .filter(p -> categorieDisponibili == null || !categorieDisponibili.contains(p.getCategoria()))
                .collect(Collectors.toList());
        if (!orfane.isEmpty()) {
            System.out.println("\n>>> ALTRE CATEGORIE ARCHIVIATE:");
            for (Proposta p : orfane) {
                System.out.println("   * [Cat: " + p.getCategoria().getNome() + "] " + p.getTitolo());
            }
        }
        System.out.println("=======================================================");
    }
}
