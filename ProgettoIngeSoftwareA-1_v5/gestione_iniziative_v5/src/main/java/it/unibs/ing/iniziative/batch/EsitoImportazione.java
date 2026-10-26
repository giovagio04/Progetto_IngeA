package it.unibs.ing.iniziative.batch;

import it.unibs.ing.iniziative.model.Proposta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raccoglie e organizza i risultati di un'operazione di importazione batch da file (Versione 5).
 * 
 * @inv campiComuniImportati >= 0 && categorieImportate >= 0 && campiSpecificiImportati >= 0
 * @inv proposteValideCreate >= 0 && propostePubblicateInBacheca >= 0
 * @inv nuoveProposteValide != null && errori != null && avvisi != null
 */
public class EsitoImportazione {
    private int campiComuniImportati;
    private int categorieImportate;
    private int campiSpecificiImportati;
    private int proposteValideCreate;
    private int propostePubblicateInBacheca;
    
    private List<Proposta> nuoveProposteValide;
    private List<String> errori;
    private List<String> avvisi;

    public EsitoImportazione() {
        this.campiComuniImportati = 0;
        this.categorieImportate = 0;
        this.campiSpecificiImportati = 0;
        this.proposteValideCreate = 0;
        this.propostePubblicateInBacheca = 0;
        this.nuoveProposteValide = new ArrayList<>();
        this.errori = new ArrayList<>();
        this.avvisi = new ArrayList<>();
        assert checkInvariant() : "Invariante violato in EsitoImportazione";
    }

    public boolean checkInvariant() {
        return campiComuniImportati >= 0
                && categorieImportate >= 0
                && campiSpecificiImportati >= 0
                && proposteValideCreate >= 0
                && propostePubblicateInBacheca >= 0
                && nuoveProposteValide != null
                && errori != null
                && avvisi != null;
    }

    public void incrementaCampiComuni() { campiComuniImportati++; }
    public void incrementaCategorie() { categorieImportate++; }
    public void incrementaCampiSpecifici() { campiSpecificiImportati++; }
    public void incrementaProposteValide() { proposteValideCreate++; }
    public void incrementaPropostePubblicate() { propostePubblicateInBacheca++; }

    public void aggiungiPropostaValida(Proposta p) {
        if (p != null) nuoveProposteValide.add(p);
    }

    public void aggiungiErrore(String err) {
        if (err != null) errori.add(err);
    }

    public void aggiungiAvviso(String avv) {
        if (avv != null) avvisi.add(avv);
    }

    public int getCampiComuniImportati() { return campiComuniImportati; }
    public int getCategorieImportate() { return categorieImportate; }
    public int getCampiSpecificiImportati() { return campiSpecificiImportati; }
    public int getProposteValideCreate() { return proposteValideCreate; }
    public int getPropostePubblicateInBacheca() { return propostePubblicateInBacheca; }
    public List<Proposta> getNuoveProposteValide() { return Collections.unmodifiableList(nuoveProposteValide); }
    public List<String> getErrori() { return Collections.unmodifiableList(errori); }
    public List<String> getAvvisi() { return Collections.unmodifiableList(avvisi); }

    public boolean haErrori() { return !errori.isEmpty(); }

    public void stampaReport() {
        System.out.println("\n=======================================================");
        System.out.println("            REPORT IMPORTAZIONE BATCH DA FILE          ");
        System.out.println("=======================================================");
        System.out.println(" - Campi comuni importati:              " + campiComuniImportati);
        System.out.println(" - Categorie importate:                 " + categorieImportate);
        System.out.println(" - Campi specifici importati:           " + campiSpecificiImportati);
        System.out.println(" - Nuove proposte valide create:        " + proposteValideCreate);
        System.out.println(" - Proposte pubblicate subito in bacheca: " + propostePubblicateInBacheca);
        
        if (!avvisi.isEmpty()) {
            System.out.println("\nAVVISI RILEVATI (" + avvisi.size() + "):");
            for (String avv : avvisi) {
                System.out.println(" ! " + avv);
            }
        }

        if (!errori.isEmpty()) {
            System.out.println("\nERRORI DI PARSING / VINCOLI VIOLATI (" + errori.size() + "):");
            for (String err : errori) {
                System.out.println(" X " + err);
            }
        } else {
            System.out.println("\nNessun errore riscontrato durante l'elaborazione del file.");
        }
        System.out.println("=======================================================");
    }
}
