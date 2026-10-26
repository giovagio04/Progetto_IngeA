package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestisce i campi globali (Base e Comuni) definiti per il sistema.
 * I campi base sono esattamente quelli previsti da {@link RuoloCampoBase}: al primo avvio il
 * configuratore sceglie il nome di ciascuno, poi restano immutabili per l'intera vita del sistema.
 * 
 * Invarianti di classe:
 * - Le liste campiBase e campiComuni non devono essere nulle.
 * - I campi base sono fissati se e solo se la lista non è vuota; in tal caso sono esattamente
 *   tanti quanti i ruoli, tutti obbligatori e ciascuno del tipo previsto dal proprio ruolo.
 * - Non ci devono essere elementi nulli nelle collezioni.
 * - Non ci devono essere collisioni di nome (case-insensitive) tra campi base e campi comuni,
 *   né duplicati interni a ciascuna lista.
 * 
 * @inv campiBase != null && campiComuni != null
 * @inv campiBaseImpostati <==> !campiBase.isEmpty()
 * @inv campiBaseImpostati ==> campiBase.size() == RuoloCampoBase.values().length
 * @inv forall i: campiBase.get(i).isObbligatorio() && campiBase.get(i).getTipo() == RuoloCampoBase.values()[i].getTipo()
 * @inv forall c in campiComuni: c.checkInvariant()
 * @inv forall cb in campiBase, cc in campiComuni: !cb.getNome().equalsIgnoreCase(cc.getNome())
 */
public class ConfigurazioneSistema implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Campo> campiBase;
    private List<Campo> campiComuni;
    private boolean campiBaseImpostati;

    /**
     * Costruttore: parte senza campi base (da fissare al primo avvio) e senza campi comuni.
     * 
     * @post !isCampiBaseImpostati()
     * @post checkInvariant()
     */
    public ConfigurazioneSistema() {
        this.campiBase = new ArrayList<>();
        this.campiComuni = new ArrayList<>();
        this.campiBaseImpostati = false;

        assert checkInvariant() : "Invariante violato al termine del costruttore ConfigurazioneSistema";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è verificato, false altrimenti.
     */
    public boolean checkInvariant() {
        if (campiBase == null || campiComuni == null) {
            return false;
        }
        if (campiBaseImpostati != !campiBase.isEmpty()) {
            return false;
        }
        if (campiBaseImpostati && campiBase.size() != RuoloCampoBase.values().length) {
            return false;
        }

        Set<String> nomiTutti = new HashSet<>();
        for (int i = 0; i < campiBase.size(); i++) {
            Campo c = campiBase.get(i);
            if (c == null || !c.checkInvariant() || !c.isObbligatorio()
                    || c.getTipo() != RuoloCampoBase.values()[i].getTipo()) {
                return false;
            }
            if (!nomiTutti.add(c.getNome().toLowerCase())) {
                return false; // duplicato nei campi base
            }
        }

        for (Campo c : campiComuni) {
            if (c == null || !c.checkInvariant()) {
                return false;
            }
            if (!nomiTutti.add(c.getNome().toLowerCase())) {
                return false; // collisione tra campo comune e base, o duplicato comune
            }
        }

        return true;
    }

    /**
     * Fissa in modo definitivo i campi base scelti dal configuratore al primo avvio.
     * Operazione consentita una sola volta: dopo il successo i campi base non sono più modificabili.
     * Devono essere esattamente 8 (come definiti in RuoloCampoBase), tutti obbligatori,
     * con tipo corrispondente al ruolo previsto dalla specifica e nomi distinti.
     * 
     * @param nuoviCampiBase gli 8 campi base
     * @return true se fissati con successo; false se già fissati o se l'elenco non è valido
     * @pre nuoviCampiBase != null
     * @post result ==> isCampiBaseImpostati() && getCampiBase().size() == RuoloCampoBase.values().length
     * @post !result ==> lo stato non è cambiato
     */
    public synchronized boolean fissaCampiBase(List<Campo> nuoviCampiBase) {
        if (campiBaseImpostati || nuoviCampiBase == null || nuoviCampiBase.size() != RuoloCampoBase.values().length) {
            return false;
        }
        Set<String> nomiUnici = new HashSet<>();
        for (int i = 0; i < nuoviCampiBase.size(); i++) {
            Campo c = nuoviCampiBase.get(i);
            if (c == null || !c.checkInvariant() || !c.isObbligatorio()
                    || c.getTipo() != RuoloCampoBase.values()[i].getTipo()
                    || haCampoComune(c.getNome())) {
                return false;
            }
            if (!nomiUnici.add(c.getNome().toLowerCase())) {
                return false; // nomi duplicati
            }
        }
        campiBase.addAll(nuoviCampiBase);
        campiBaseImpostati = true;

        assert campiBaseImpostati && campiBase.size() == RuoloCampoBase.values().length : "Postcondizione violata in fissaCampiBase";
        assert checkInvariant() : "Invariante violato dopo fissaCampiBase";
        return true;
    }

    /**
     * Restituisce la vista immutabile dei campi base.
     * Garantisce l'information hiding impedendo modifiche esterne alla collezione.
     */
    public List<Campo> getCampiBase() {
        return Collections.unmodifiableList(campiBase);
    }

    /**
     * Restituisce la vista immutabile dei campi comuni.
     */
    public List<Campo> getCampiComuni() {
        return Collections.unmodifiableList(campiComuni);
    }
    
    public boolean isCampiBaseImpostati() {
        return campiBaseImpostati;
    }

    public boolean haCampoBase(String nomeCampo) {
        if (nomeCampo == null) return false;
        return campiBase.stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()));
    }

    public boolean haCampoComune(String nomeCampo) {
        if (nomeCampo == null) return false;
        return campiComuni.stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()));
    }

    public Campo getCampoComune(String nomeCampo) {
        if (nomeCampo == null) return null;
        return campiComuni.stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Aggiunge un campo comune, assicurando che non entri in collisione con i campi base
     * o con altri campi comuni già esistenti.
     * 
     * @param campo il campo comune da aggiungere
     * @return true se aggiunto con successo, false in caso di collisione o null
     * @post result == true ==> haCampoComune(campo.getNome())
     */
    public boolean aggiungiCampoComune(Campo campo) {
        assert checkInvariant() : "Invariante violato prima di aggiungiCampoComune";
        if (campo == null) return false;

        String nome = campo.getNome();
        if (haCampoBase(nome) || haCampoComune(nome)) {
            return false; // Collisione di nome non consentita
        }

        int dimensioneIniziale = campiComuni.size();
        campiComuni.add(campo);

        assert campiComuni.size() == dimensioneIniziale + 1 : "Postcondizione: dimensione campi comuni non incrementata";
        assert haCampoComune(nome) : "Postcondizione: campo comune non trovato dopo aggiunta";
        assert checkInvariant() : "Invariante violato dopo aggiungiCampoComune";

        return true;
    }

    /**
     * Rimuove un campo comune dato il suo nome.
     * 
     * @param nomeCampo il nome del campo comune da eliminare
     * @return true se rimosso, false se assente
     * @post result == true ==> !haCampoComune(nomeCampo)
     */
    public boolean rimuoviCampoComune(String nomeCampo) {
        assert checkInvariant() : "Invariante violato prima di rimuoviCampoComune";
        if (nomeCampo == null) return false;

        boolean rimosso = campiComuni.removeIf(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()));

        if (rimosso) {
            assert !haCampoComune(nomeCampo) : "Postcondizione: campo comune ancora presente dopo rimozione";
        }
        assert checkInvariant() : "Invariante violato dopo rimuoviCampoComune";

        return rimosso;
    }
}
