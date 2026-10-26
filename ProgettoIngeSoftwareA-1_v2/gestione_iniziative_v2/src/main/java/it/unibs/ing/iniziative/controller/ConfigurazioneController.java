package it.unibs.ing.iniziative.controller;

import it.unibs.ing.iniziative.model.Bacheca;
import it.unibs.ing.iniziative.model.Campo;
import it.unibs.ing.iniziative.model.Categoria;
import it.unibs.ing.iniziative.model.Configuratore;
import it.unibs.ing.iniziative.model.ConfigurazioneSistema;
import it.unibs.ing.iniziative.model.Proposta;
import it.unibs.ing.iniziative.model.StatoProposta;
import it.unibs.ing.iniziative.model.TipoCampo;
import it.unibs.ing.iniziative.model.Utente;
import it.unibs.ing.iniziative.storage.DatiSistema;
import it.unibs.ing.iniziative.storage.GestorePersistenza;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller applicativo per la Versione 2 (Architettura MVC).
 * Disaccoppia la logica di business e di controllo dall'interfaccia utente (CLI o futura GUI),
 * coordinando le operazioni sul modello di dominio (categorie, campi, proposte, bacheca) e sulla persistenza.
 */
public class ConfigurazioneController {

    private final DatiSistema dati;
    // Proposte valide create nella sessione corrente ma non ancora pubblicate in bacheca (non persistenti)
    private final List<Proposta> proposteValideInSessione;

    /**
     * Costruttore: riceve l'istanza di DatiSistema su cui operare.
     * @param dati il contenitore radice dello stato applicativo
     */
    public ConfigurazioneController(DatiSistema dati) {
        assert dati != null : "Precondizione violata: dati non può essere nullo";
        this.dati = dati;
        this.proposteValideInSessione = new ArrayList<>();
        assert this.dati.checkInvariant() : "Invariante di DatiSistema violato";
    }

    // -------------------------------------------------------------
    // AUTENTICAZIONE E UTENZE
    // -------------------------------------------------------------

    /**
     * Esegue il login dell'utente.
     */
    public Utente login(String username, String password) {
        if (username == null || password == null) return null;
        return dati.getGestoreUtenti().login(username, password);
    }

    /**
     * Registra un nuovo configuratore personale o aggiorna le credenziali.
     */
    public Configuratore registraNuovoConfiguratore(Configuratore utenteCorrente, String nuovoUsername, String nuovaPassword) {
        if (utenteCorrente == null || nuovoUsername == null || nuovaPassword == null) return null;
        return dati.getGestoreUtenti().cambiaCredenziali(utenteCorrente, nuovoUsername, nuovaPassword);
    }

    // -------------------------------------------------------------
    // GESTIONE CAMPI BASE (V1)
    // -------------------------------------------------------------

    /**
     * Verifica se i campi base sono già stati fissati in modo persistente.
     */
    public boolean isCampiBaseFissati() {
        return dati.getConfigurazioneSistema().isCampiBaseImpostati();
    }

    /**
     * Fissa in modo definitivo gli 8 campi base scelti dal configuratore al primo avvio.
     * @return true se fissati; false se già fissati o elenco non valido
     */
    public boolean fissaCampiBase(List<Campo> campi) {
        return dati.getConfigurazioneSistema().fissaCampiBase(campi);
    }

    /**
     * Restituisce la vista immutabile dei campi base.
     */
    public List<Campo> getCampiBase() {
        return dati.getConfigurazioneSistema().getCampiBase();
    }

    // -------------------------------------------------------------
    // GESTIONE CAMPI COMUNI (V1)
    // -------------------------------------------------------------

    public List<Campo> getCampiComuni() {
        return dati.getConfigurazioneSistema().getCampiComuni();
    }

    public boolean haCampoBase(String nome) {
        return dati.getConfigurazioneSistema().haCampoBase(nome);
    }

    public boolean haCampoComune(String nome) {
        return dati.getConfigurazioneSistema().haCampoComune(nome);
    }

    public boolean haCampoSpecificoInQualcheCategoria(String nome) {
        return dati.haCampoSpecificoInQualcheCategoria(nome);
    }

    public Campo getCampoComune(String nome) {
        return dati.getConfigurazioneSistema().getCampoComune(nome);
    }

    /**
     * Aggiunge un nuovo campo comune verificando l'assenza di collisioni
     * con campi base, altri campi comuni o campi specifici di categorie esistenti.
     */
    public boolean aggiungiCampoComune(String nome, String descrizione, boolean obbligatorio, TipoCampo tipo) {
        if (nome == null || nome.trim().isEmpty() || tipo == null) {
            return false;
        }
        if (haCampoBase(nome) || haCampoComune(nome) || haCampoSpecificoInQualcheCategoria(nome)) {
            return false;
        }
        Campo nuovo = new Campo(nome, descrizione, obbligatorio, tipo);
        return dati.getConfigurazioneSistema().aggiungiCampoComune(nuovo);
    }

    /**
     * Rimuove un campo comune dato il nome.
     */
    public boolean rimuoviCampoComune(String nome) {
        return dati.getConfigurazioneSistema().rimuoviCampoComune(nome);
    }

    /**
     * Modifica l'obbligatorietà di un campo comune esistente.
     */
    public boolean modificaObbligatorietaCampoComune(String nome, boolean nuovoStato) {
        Campo c = getCampoComune(nome);
        if (c == null) return false;
        c.setObbligatorio(nuovoStato);
        return true;
    }

    // -------------------------------------------------------------
    // GESTIONE CATEGORIE E CAMPI SPECIFICI (V1)
    // -------------------------------------------------------------

    public List<Categoria> getCategorie() {
        return dati.getCategorie();
    }

    public boolean esisteCategoria(String nome) {
        return dati.esisteCategoria(nome);
    }

    public Categoria getCategoria(String nome) {
        return dati.getCategoria(nome);
    }

    /**
     * Crea e aggiunge una nuova categoria (inizialmente vuota).
     */
    public boolean creaCategoria(String nome, String descrizione) {
        if (nome == null || nome.trim().isEmpty() || esisteCategoria(nome)) {
            return false;
        }
        Categoria c = new Categoria(nome, descrizione);
        return dati.aggiungiCategoria(c);
    }

    /**
     * Aggiunge una categoria già istanziata (eventualmente con campi specifici).
     */
    public boolean aggiungiCategoria(Categoria cat) {
        return dati.aggiungiCategoria(cat);
    }

    /**
     * Rimuove una categoria e a cascata tutti i suoi campi specifici associati.
     */
    public boolean rimuoviCategoria(String nome) {
        return dati.rimuoviCategoria(nome);
    }

    public boolean isNomeCampoPresenteInCategoria(Categoria cat, String nomeCampo) {
        return dati.isNomeCampoPresenteInCategoria(cat, nomeCampo);
    }

    /**
     * Aggiunge un campo specifico a una categoria.
     */
    public boolean aggiungiCampoSpecifico(String nomeCategoria, String nomeCampo, String descrizione, boolean obbligatorio, TipoCampo tipo) {
        Categoria cat = getCategoria(nomeCategoria);
        if (cat == null || nomeCampo == null || nomeCampo.trim().isEmpty() || tipo == null) {
            return false;
        }
        if (isNomeCampoPresenteInCategoria(cat, nomeCampo)) {
            return false;
        }
        Campo c = new Campo(nomeCampo, descrizione, obbligatorio, tipo);
        return cat.aggiungiCampoSpecifico(c);
    }

    /**
     * Rimuove un campo specifico da una categoria.
     */
    public boolean rimuoviCampoSpecifico(String nomeCategoria, String nomeCampo) {
        Categoria cat = getCategoria(nomeCategoria);
        if (cat == null || nomeCampo == null) return false;
        return cat.rimuoviCampoSpecifico(nomeCampo);
    }

    /**
     * Modifica l'obbligatorietà di un campo specifico.
     */
    public boolean modificaObbligatorietaCampoSpecifico(String nomeCategoria, String nomeCampo, boolean nuovoStato) {
        Categoria cat = getCategoria(nomeCategoria);
        if (cat == null || nomeCampo == null) return false;
        Campo c = cat.getCampoSpecifico(nomeCampo);
        if (c == null) return false;
        c.setObbligatorio(nuovoStato);
        return true;
    }

    /**
     * Restituisce la scheda completa dei campi di una categoria (Base + Comuni + Specifici).
     */
    public List<Campo> getTuttiICampiCategoria(Categoria cat) {
        return dati.getTuttiICampiCategoria(cat);
    }

    // -------------------------------------------------------------
    // GESTIONE PROPOSTE DI INIZIATIVA E BACHECA (V2)
    // -------------------------------------------------------------

    /**
     * Restituisce la vista immutabile delle proposte valide create nella sessione corrente
     * e non ancora pubblicate in bacheca.
     */
    public List<Proposta> getProposteValideInSessione() {
        return Collections.unmodifiableList(proposteValideInSessione);
    }

    /**
     * Valida una proposta rispetto a tutti i vincoli di dominio.
     * Se valida, le assegna automaticamente lo stato VALIDA e la registra nella sessione corrente.
     * 
     * @param proposta la proposta compilata
     * @param dataRiferimento la data odierna per la validazione
     * @return lista di eventuali errori (vuota se la proposta è VALIDA)
     */
    public List<String> validaERegistraPropostaInSessione(Proposta proposta, LocalDate dataRiferimento) {
        if (proposta == null) {
            return List.of("Proposta nulla.");
        }
        List<Campo> campiAttesi = getTuttiICampiCategoria(proposta.getCategoria());
        List<String> errori = proposta.validaVincoli(dataRiferimento, campiAttesi);
        if (errori.isEmpty() && proposta.getStato() == StatoProposta.VALIDA) {
            if (!proposteValideInSessione.contains(proposta)) {
                proposteValideInSessione.add(proposta);
            }
        }
        return errori;
    }

    /**
     * Pubblica in bacheca una proposta valida, facendola passare nello stato APERTA,
     * rimuovendola dalle proposte temporanee di sessione e inserendola nell'archivio persistente.
     * 
     * @param proposta la proposta valida da pubblicare
     * @param dataPubblicazione la data di pubblicazione
     * @return true se pubblicata con successo in bacheca, false altrimenti
     */
    public boolean pubblicaPropostaInBacheca(Proposta proposta, LocalDate dataPubblicazione) {
        if (proposta == null) return false;
        if (!proposta.pubblicaInBacheca(dataPubblicazione)) {
            return false;
        }
        boolean aggiunta = dati.getBacheca().aggiungiPropostaAperta(proposta);
        if (aggiunta) {
            proposteValideInSessione.remove(proposta);
        }
        return aggiunta;
    }

    public Bacheca getBacheca() {
        return dati.getBacheca();
    }

    public List<Proposta> getPropostePerCategoria(Categoria cat) {
        return dati.getBacheca().getPropostePerCategoria(cat);
    }

    // -------------------------------------------------------------
    // PERSISTENZA
    // -------------------------------------------------------------

    /**
     * Salva lo stato applicativo sul file persistente.
     */
    public void salvaDati() throws IOException {
        GestorePersistenza.salvaDati(dati);
    }

    public DatiSistema getDati() {
        return dati;
    }

    public ConfigurazioneSistema getConfigurazioneSistema() {
        return dati.getConfigurazioneSistema();
    }
}
