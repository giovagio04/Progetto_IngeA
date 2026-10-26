package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Rappresenta una categoria di iniziative.
 * Ogni categoria ha un nome univoco, una descrizione opzionale e un insieme di campi specifici.
 * 
 * Invarianti di classe:
 * - Il nome della categoria non deve essere nullo né vuoto.
 * - La descrizione non deve essere nulla.
 * - La lista dei campi specifici non deve essere nulla.
 * - Non devono esserci campi specifici nulli o con nomi duplicati (case-insensitive).
 * 
 * @inv nome != null && !nome.trim().isEmpty()
 * @inv descrizione != null
 * @inv campiSpecifici != null
 * @inv forall c in campiSpecifici: c != null && c.checkInvariant()
 * @inv forall c1, c2 in campiSpecifici: c1 != c2 ==> !c1.getNome().equalsIgnoreCase(c2.getNome())
 */
public class Categoria implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String descrizione;
    private List<Campo> campiSpecifici;

    /**
     * Costruttore per creare una nuova categoria.
     * 
     * @pre nome != null && !nome.trim().isEmpty()
     * @post getNome().equals(nome.trim())
     * @post getCampiSpecifici().isEmpty()
     */
    public Categoria(String nome, String descrizione) {
        assert nome != null && !nome.trim().isEmpty() : "Precondizione violata: il nome della categoria non può essere vuoto";

        this.nome = nome.trim();
        this.descrizione = (descrizione != null) ? descrizione.trim() : "";
        this.campiSpecifici = new ArrayList<>();

        assert checkInvariant() : "Invariante di classe violato in Categoria dopo la costruzione";
    }

    /**
     * Verifica l'invariante di classe.
     * @return true se l'invariante è verificato, false altrimenti.
     */
    public boolean checkInvariant() {
        if (nome == null || nome.trim().isEmpty() || descrizione == null || campiSpecifici == null) {
            return false;
        }
        Set<String> nomiVisti = new HashSet<>();
        for (Campo c : campiSpecifici) {
            if (c == null || !c.checkInvariant()) {
                return false;
            }
            if (!nomiVisti.add(c.getNome().toLowerCase())) {
                return false; // duplicato trovato
            }
        }
        return true;
    }

    public String getNome() { 
        return nome; 
    }

    public String getDescrizione() { 
        return descrizione; 
    }
    
    /**
     * Restituisce una vista non modificabile dei campi specifici.
     */
    public List<Campo> getCampiSpecifici() {
        return Collections.unmodifiableList(campiSpecifici);
    }

    /**
     * Verifica se la categoria contiene un campo specifico con il nome indicato.
     * 
     * @param nomeCampo nome del campo da ricercare
     * @return true se presente, false altrimenti
     */
    public boolean haCampoSpecifico(String nomeCampo) {
        if (nomeCampo == null) return false;
        return campiSpecifici.stream().anyMatch(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()));
    }

    /**
     * Recupera il campo specifico corrispondente al nome indicato.
     * 
     * @param nomeCampo nome del campo cercato
     * @return il campo corrispondente, oppure null se assente
     */
    public Campo getCampoSpecifico(String nomeCampo) {
        if (nomeCampo == null) return null;
        return campiSpecifici.stream()
                .filter(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Aggiunge un campo specifico alla categoria.
     * 
     * @param campo il campo da aggiungere
     * @return true se aggiunto con successo, false se già presente o nullo
     * @pre campo != null
     * @post result == true ==> haCampoSpecifico(campo.getNome())
     * @post result == false ==> dimensione invariata
     */
    public boolean aggiungiCampoSpecifico(Campo campo) {
        assert checkInvariant() : "Invariante violato prima di aggiungiCampoSpecifico";
        if (campo == null) return false;

        if (haCampoSpecifico(campo.getNome())) {
            return false;
        }

        int dimensioneIniziale = campiSpecifici.size();
        campiSpecifici.add(campo);

        assert campiSpecifici.size() == dimensioneIniziale + 1 : "Postcondizione: dimensione errata dopo aggiunta";
        assert haCampoSpecifico(campo.getNome()) : "Postcondizione: campo non presente dopo aggiunta";
        assert checkInvariant() : "Invariante violato dopo aggiungiCampoSpecifico";

        return true;
    }

    /**
     * Rimuove un campo specifico dato il suo nome.
     * 
     * @param nomeCampo nome del campo da rimuovere
     * @return true se rimosso con successo, false se assente o nullo
     * @post result == true ==> !haCampoSpecifico(nomeCampo)
     */
    public boolean rimuoviCampoSpecifico(String nomeCampo) {
        assert checkInvariant() : "Invariante violato prima di rimuoviCampoSpecifico";
        if (nomeCampo == null) return false;

        boolean rimosso = campiSpecifici.removeIf(c -> c.getNome().equalsIgnoreCase(nomeCampo.trim()));

        if (rimosso) {
            assert !haCampoSpecifico(nomeCampo) : "Postcondizione: campo ancora presente dopo rimozione";
        }
        assert checkInvariant() : "Invariante violato dopo rimuoviCampoSpecifico";

        return rimosso;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria categoria = (Categoria) o;
        return nome.equalsIgnoreCase(categoria.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome.toLowerCase());
    }

    @Override
    public String toString() {
        return nome + (!descrizione.isEmpty() ? " - " + descrizione : "");
    }
}
