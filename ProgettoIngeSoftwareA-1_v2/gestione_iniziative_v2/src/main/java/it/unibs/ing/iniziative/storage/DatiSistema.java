package it.unibs.ing.iniziative.storage;

import it.unibs.ing.iniziative.model.Bacheca;
import it.unibs.ing.iniziative.model.Campo;
import it.unibs.ing.iniziative.model.Categoria;
import it.unibs.ing.iniziative.model.ConfigurazioneSistema;
import it.unibs.ing.iniziative.model.GestoreUtenti;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contenitore radice per tutti i dati di stato del sistema da salvare e caricare in modo persistente.
 * Nella Versione 2 include anche la Bacheca (archivio delle proposte aperte).
 * 
 * Invarianti di classe:
 * - gestoreUtenti non deve essere nullo e deve rispettare il proprio invariante.
 * - configurazioneSistema non deve essere nullo e deve rispettare il proprio invariante.
 * - La lista delle categorie non deve essere nulla, non deve contenere categorie nulle o duplicate (case-insensitive).
 * - Per ogni categoria, nessun campo specifico deve collidere con i campi globali (base o comuni).
 * - La bacheca non deve essere nulla e deve rispettare il proprio invariante.
 * 
 * @inv gestoreUtenti != null && gestoreUtenti.checkInvariant()
 * @inv configurazioneSistema != null && configurazioneSistema.checkInvariant()
 * @inv categorie != null
 * @inv forall cat in categorie: cat != null && cat.checkInvariant()
 * @inv forall c1, c2 in categorie: c1 != c2 ==> !c1.getNome().equalsIgnoreCase(c2.getNome())
 * @inv bacheca != null && bacheca.checkInvariant()
 */
public class DatiSistema implements Serializable {
    private static final long serialVersionUID = 1L;

    private GestoreUtenti gestoreUtenti;
    private ConfigurazioneSistema configurazioneSistema;
    private List<Categoria> categorie;
    private Bacheca bacheca;

    /**
     * Costruttore: inizializza le componenti del sistema.
     * 
     * @post checkInvariant()
     */
    public DatiSistema() {
        this.gestoreUtenti = new GestoreUtenti(); 
        this.configurazioneSistema = new ConfigurazioneSistema();
        this.categorie = new ArrayList<>();
        this.bacheca = new Bacheca();

        assert checkInvariant() : "Invariante di classe violato in DatiSistema al termine del costruttore";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è verificato, false altrimenti.
     */
    public boolean checkInvariant() {
        if (gestoreUtenti == null || !gestoreUtenti.checkInvariant()) {
            return false;
        }
        if (configurazioneSistema == null || !configurazioneSistema.checkInvariant()) {
            return false;
        }
        if (categorie == null) {
            return false;
        }
        if (bacheca == null || !bacheca.checkInvariant()) {
            return false;
        }

        Set<String> nomiCategorie = new HashSet<>();
        for (Categoria cat : categorie) {
            if (cat == null || !cat.checkInvariant()) {
                return false;
            }
            if (!nomiCategorie.add(cat.getNome().toLowerCase())) {
                return false; // categoria duplicata
            }
            // Controllo che i campi specifici della categoria non collidano con campi globali
            for (Campo cs : cat.getCampiSpecifici()) {
                if (isNomeCampoGlobalePresente(cs.getNome())) {
                    return false; // violazione del vincolo di disgiunzione
                }
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

    /**
     * Verifica se una categoria con il dato nome è presente.
     */
    public boolean esisteCategoria(String nome) {
        if (nome == null) return false;
        return categorie.stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nome.trim()));
    }

    /**
     * Recupera una categoria dato il nome.
     */
    public Categoria getCategoria(String nome) {
        if (nome == null) return null;
        return categorie.stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nome.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Aggiunge una nuova categoria al sistema.
     * 
     * @param c la categoria da aggiungere
     * @return true se aggiunta con successo, false se già presente o non valida
     * @post result == true ==> esisteCategoria(c.getNome())
     */
    public boolean aggiungiCategoria(Categoria c) {
        assert checkInvariant() : "Invariante violato prima di aggiungiCategoria";
        if (c == null || !c.checkInvariant()) return false;

        if (esisteCategoria(c.getNome())) {
            return false;
        }

        // Verifica che nessun campo specifico della categoria collida con campi base o comuni
        for (Campo cs : c.getCampiSpecifici()) {
            if (isNomeCampoGlobalePresente(cs.getNome())) {
                return false;
            }
        }

        int dimensioneIniziale = categorie.size();
        categorie.add(c);

        assert categorie.size() == dimensioneIniziale + 1 : "Postcondizione: dimensione categorie errata";
        assert esisteCategoria(c.getNome()) : "Postcondizione: categoria non trovata dopo aggiunta";
        assert checkInvariant() : "Invariante violato dopo aggiungiCategoria";

        return true;
    }

    /**
     * Rimuove una categoria e a cascata tutti i suoi campi specifici associati.
     * Come previsto dalla specifica, la rimozione non ha alcun effetto sulle proposte già pubblicate in bacheca.
     * 
     * @param nome nome della categoria da rimuovere
     * @return true se rimossa con successo, false se assente
     * @post result == true ==> !esisteCategoria(nome)
     */
    public boolean rimuoviCategoria(String nome) {
        assert checkInvariant() : "Invariante violato prima di rimuoviCategoria";
        if (nome == null) return false;

        boolean rimossa = categorie.removeIf(c -> c.getNome().equalsIgnoreCase(nome.trim()));

        if (rimossa) {
            assert !esisteCategoria(nome) : "Postcondizione: categoria ancora presente dopo rimozione";
        }
        assert checkInvariant() : "Invariante violato dopo rimuoviCategoria";

        return rimossa;
    }

    /**
     * Verifica se un nome di campo è già utilizzato nei campi base o comuni.
     */
    public boolean isNomeCampoGlobalePresente(String nomeCampo) {
        if (nomeCampo == null) return false;
        return configurazioneSistema.haCampoBase(nomeCampo) || configurazioneSistema.haCampoComune(nomeCampo);
    }

    /**
     * Verifica se un nome di campo è già utilizzato come campo specifico in almeno una categoria esistente.
     * Garantisce che l'aggiunta successiva di un campo comune non crei duplicati di nome in categorie già esistenti.
     */
    public boolean haCampoSpecificoInQualcheCategoria(String nomeCampo) {
        if (nomeCampo == null) return false;
        return categorie.stream().anyMatch(cat -> cat.haCampoSpecifico(nomeCampo));
    }

    /**
     * Verifica se un nome di campo è già utilizzato nella categoria specificata
     * (considerando campi base, campi comuni e campi specifici della categoria).
     * Rispetta il vincolo: "Relativamente a ogni singola categoria, ogni campo è dotato di un nome che lo individua univocamente."
     */
    public boolean isNomeCampoPresenteInCategoria(Categoria cat, String nomeCampo) {
        if (nomeCampo == null) return false;
        if (isNomeCampoGlobalePresente(nomeCampo)) return true;
        return cat != null && cat.haCampoSpecifico(nomeCampo);
    }

    /**
     * Restituisce la lista complessiva di tutti i campi appartenenti alla categoria:
     * Campi Base + Campi Comuni + Campi Specifici.
     * Se i campi base non sono ancora stati fissati manualmente (es. nei test rapidi),
     * li inizializza con le etichette standard dei ruoli di specifica.
     */
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
}
