package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rappresenta lo spazio personale privato di un fruitore contenente le notifiche ricevute.
 * Le notifiche sono persistenti tra le sessioni e possono essere cancellate selettivamente dal fruitore.
 * 
 * Invarianti di classe:
 * - La lista notifiche non deve essere nulla e non deve contenere elementi nulli o invalidi.
 * - contatoreId >= 1.
 * 
 * @inv notifiche != null && contatoreId >= 1
 * @inv forall n in notifiche: n != null && n.checkInvariant()
 */
public class SpazioPersonale implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Notifica> notifiche;
    private int contatoreId;

    /**
     * Costruttore: inizializza uno spazio personale vuoto.
     * 
     * @post isVuoto() && checkInvariant()
     */
    public SpazioPersonale() {
        this.notifiche = new ArrayList<>();
        this.contatoreId = 1;
        assert checkInvariant() : "Invariante violato in SpazioPersonale";
    }

    public boolean checkInvariant() {
        if (notifiche == null || contatoreId < 1) {
            return false;
        }
        for (Notifica n : notifiche) {
            if (n == null || !n.checkInvariant()) {
                return false;
            }
        }
        return true;
    }

    public List<Notifica> getNotifiche() {
        return Collections.unmodifiableList(notifiche);
    }

    public boolean isVuoto() {
        return notifiche.isEmpty();
    }

    public int getNumeroNotifiche() {
        return notifiche.size();
    }

    /**
     * Aggiunge una nuova notifica allo spazio personale.
     * 
     * @param testo contenuto del messaggio
     * @param data data di emissione
     */
    public void aggiungiNotifica(String testo, LocalDate data) {
        assert checkInvariant() : "Invariante violato prima di aggiungiNotifica";
        if (testo != null && !testo.trim().isEmpty()) {
            notifiche.add(new Notifica(contatoreId++, testo.trim(), data));
        }
        assert checkInvariant() : "Invariante violato dopo aggiungiNotifica";
    }

    /**
     * Rimuove selettivamente una notifica dato il suo ID.
     * 
     * @param id identificativo della notifica da cancellare
     * @return true se trovata e rimossa, false altrimenti
     */
    public boolean rimuoviNotifica(int id) {
        assert checkInvariant() : "Invariante violato prima di rimuoviNotifica";
        boolean rimossa = notifiche.removeIf(n -> n.getId() == id);
        assert checkInvariant() : "Invariante violato dopo rimuoviNotifica";
        return rimossa;
    }

    public void visualizzaNotifiche() {
        System.out.println("\n=======================================================");
        System.out.println("              SPAZIO PERSONALE - NOTIFICHE             ");
        System.out.println("=======================================================");
        if (notifiche.isEmpty()) {
            System.out.println("Nessuna notifica presente nello spazio personale.");
        } else {
            for (Notifica n : notifiche) {
                System.out.println(n.toString());
            }
        }
        System.out.println("=======================================================");
    }
}
