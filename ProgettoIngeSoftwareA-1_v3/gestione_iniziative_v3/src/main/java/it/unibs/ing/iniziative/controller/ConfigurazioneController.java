package it.unibs.ing.iniziative.controller;

import it.unibs.ing.iniziative.model.ArchivioProposte;
import it.unibs.ing.iniziative.model.Bacheca;
import it.unibs.ing.iniziative.model.Campo;
import it.unibs.ing.iniziative.model.Categoria;
import it.unibs.ing.iniziative.model.Configuratore;
import it.unibs.ing.iniziative.model.ConfigurazioneSistema;
import it.unibs.ing.iniziative.model.Fruitore;
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
 * Controller applicativo per la Versione 3 (Architettura MVC).
 * Estende il controller di V1 e V2 aggiungendo la gestione dei Fruitori,
 * delle iscrizioni alle proposte aperte, dell'ArchivioProposte storico,
 * dello Spazio Personale e dell'aggiornamento automatico delle scadenze allo scadere della mezzanotte.
 */
public class ConfigurazioneController {

    private final DatiSistema dati;
    // Proposte valide create nella sessione corrente ma non ancora pubblicate in bacheca (non persistenti)
    private final List<Proposta> proposteValideInSessione;

    public ConfigurazioneController(DatiSistema dati) {
        assert dati != null : "Precondizione violata: dati non può essere nullo";
        this.dati = dati;
        this.proposteValideInSessione = new ArrayList<>();
        assert this.dati.checkInvariant() : "Invariante di DatiSistema violato";
    }

    public boolean checkInvariant() {
        return dati != null && dati.checkInvariant() && proposteValideInSessione != null;
    }

    // -------------------------------------------------------------
    // AUTENTICAZIONE E UTENZE (V1 + V3)
    // -------------------------------------------------------------

    public Utente login(String username, String password) {
        if (username == null || password == null) return null;
        return dati.getGestoreUtenti().login(username, password);
    }

    public boolean esisteUsername(String username) {
        return dati.getGestoreUtenti().esisteUsername(username);
    }

    public Configuratore registraNuovoConfiguratore(Configuratore utenteCorrente, String nuovoUsername, String nuovaPassword) {
        if (utenteCorrente == null || nuovoUsername == null || nuovaPassword == null) return null;
        return dati.getGestoreUtenti().cambiaCredenziali(utenteCorrente, nuovoUsername, nuovaPassword);
    }

    public Fruitore registraFruitore(String username, String password) {
        return dati.getGestoreUtenti().registraFruitore(username, password);
    }

    public Fruitore getFruitore(String username) {
        return dati.getGestoreUtenti().getFruitore(username);
    }

    // -------------------------------------------------------------
    // GESTIONE CAMPI BASE (V1)
    // -------------------------------------------------------------

    public boolean isCampiBaseFissati() {
        return dati.getConfigurazioneSistema().isCampiBaseImpostati();
    }

    public boolean fissaCampiBase(List<Campo> campi) {
        return dati.getConfigurazioneSistema().fissaCampiBase(campi);
    }

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

    public boolean rimuoviCampoComune(String nome) {
        return dati.getConfigurazioneSistema().rimuoviCampoComune(nome);
    }

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

    public boolean creaCategoria(String nome, String descrizione) {
        if (nome == null || nome.trim().isEmpty() || esisteCategoria(nome)) {
            return false;
        }
        Categoria c = new Categoria(nome, descrizione);
        return dati.aggiungiCategoria(c);
    }

    public boolean aggiungiCategoria(Categoria cat) {
        return dati.aggiungiCategoria(cat);
    }

    public boolean rimuoviCategoria(String nome) {
        return dati.rimuoviCategoria(nome);
    }

    public boolean isNomeCampoPresenteInCategoria(Categoria cat, String nomeCampo) {
        return dati.isNomeCampoPresenteInCategoria(cat, nomeCampo);
    }

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

    public boolean rimuoviCampoSpecifico(String nomeCategoria, String nomeCampo) {
        Categoria cat = getCategoria(nomeCategoria);
        if (cat == null || nomeCampo == null) return false;
        return cat.rimuoviCampoSpecifico(nomeCampo);
    }

    public boolean modificaObbligatorietaCampoSpecifico(String nomeCategoria, String nomeCampo, boolean nuovoStato) {
        Categoria cat = getCategoria(nomeCategoria);
        if (cat == null || nomeCampo == null) return false;
        Campo c = cat.getCampoSpecifico(nomeCampo);
        if (c == null) return false;
        c.setObbligatorio(nuovoStato);
        return true;
    }

    public List<Campo> getTuttiICampiCategoria(Categoria cat) {
        return dati.getTuttiICampiCategoria(cat);
    }

    // -------------------------------------------------------------
    // GESTIONE PROPOSTE E BACHECA (V2)
    // -------------------------------------------------------------

    public List<Proposta> getProposteValideInSessione() {
        return Collections.unmodifiableList(proposteValideInSessione);
    }

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
     * Pubblica in bacheca una proposta valida (VALIDA -> APERTA), la registra nell'ArchivioProposte
     * storico e la rimuove dalle proposte temporanee di sessione.
     */
    public boolean pubblicaPropostaInBacheca(Proposta proposta, LocalDate dataPubblicazione) {
        if (proposta == null) return false;
        if (!proposta.pubblicaInBacheca(dataPubblicazione)) {
            return false;
        }
        boolean aggiunta = dati.getBacheca().aggiungiPropostaAperta(proposta);
        if (aggiunta) {
            dati.getArchivioProposte().registraProposta(proposta);
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
    // ARCHIVIO PROPOSTE, ISCRIZIONI E SCADENZE (V3)
    // -------------------------------------------------------------

    public ArchivioProposte getArchivioProposte() {
        return dati.getArchivioProposte();
    }

    public void aggiornaStatiEScadenze(LocalDate dataOggi) {
        dati.aggiornaStatiEScadenze(dataOggi);
    }

    /**
     * Iscrive un fruitore a una proposta aperta in bacheca.
     * @return null se iscrizione avvenuta con successo, messaggio di errore altrimenti
     */
    public String iscriviFruitoreAProposta(Proposta proposta, String usernameFruitore, LocalDate dataCorrente) {
        if (proposta == null) return "Proposta non valida.";
        return proposta.iscriviFruitore(usernameFruitore, dataCorrente);
    }

    public List<Proposta> getPropostePerFruitore(String usernameFruitore) {
        return dati.getArchivioProposte().getPropostePerFruitore(usernameFruitore);
    }

    public boolean rimuoviNotificaFruitore(Fruitore fruitore, int idNotifica) {
        if (fruitore == null) return false;
        return fruitore.getSpazioPersonale().rimuoviNotifica(idNotifica);
    }

    // -------------------------------------------------------------
    // PERSISTENZA
    // -------------------------------------------------------------

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
