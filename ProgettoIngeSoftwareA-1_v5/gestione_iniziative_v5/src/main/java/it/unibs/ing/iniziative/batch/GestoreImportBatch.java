package it.unibs.ing.iniziative.batch;

import it.unibs.ing.iniziative.model.*;
import it.unibs.ing.iniziative.storage.DatiSistema;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Gestore dell'importazione batch da file di testo strutturato (Versione 5).
 * Consente al configuratore di importare gli ingressi del back-end dell'applicazione
 * (categorie, campi comuni, campi specifici e nuove proposte di iniziativa)
 * in modalità batch attraverso uno o più file di input, mantenendo operativa
 * anche la modalità interattiva e applicando tutti i controlli di congruenza e DBC.
 * 
 * @inv dati != null && dati.checkInvariant()
 * @inv configuratore != null && configuratore.checkInvariant()
 */
public class GestoreImportBatch {

    private DatiSistema dati;
    private Configuratore configuratore;

    public GestoreImportBatch(DatiSistema dati, Configuratore configuratore) {
        assert dati != null : "Precondizione violata: dati non può essere nullo";
        assert configuratore != null : "Precondizione violata: configuratore non può essere nullo";
        this.dati = dati;
        this.configuratore = configuratore;
        assert checkInvariant() : "Invariante violato in GestoreImportBatch";
    }

    public boolean checkInvariant() {
        return dati != null && dati.checkInvariant()
                && configuratore != null && configuratore.checkInvariant();
    }

    /**
     * Importa gli elementi contenuti in un file batch.
     * 
     * @param file file sorgente da leggere
     * @param dataRiferimento data odierna per la verifica dei vincoli temporali
     * @return esito dell'importazione con conteggi ed eventuali errori/avvisi
     */
    public EsitoImportazione importaDaFile(File file, LocalDate dataRiferimento) {
        assert checkInvariant() : "Invariante violato prima di importaDaFile";
        EsitoImportazione esito = new EsitoImportazione();
        if (file == null || !file.exists() || !file.canRead()) {
            esito.aggiungiErrore("Impossibile accedere al file specificato: " + (file != null ? file.getPath() : "nullo"));
            return esito;
        }

        if (dataRiferimento == null) {
            dataRiferimento = LocalDate.now();
        }

        List<BloccoBatch> blocchi = leggiBlocchi(file, esito);
        for (BloccoBatch blocco : blocchi) {
            switch (blocco.getTipo().toUpperCase()) {
                case "CAMPO_COMUNE":
                    elaboraCampoComune(blocco, esito);
                    break;
                case "CATEGORIA":
                    elaboraCategoria(blocco, esito);
                    break;
                case "PROPOSTA":
                    elaboraProposta(blocco, esito, dataRiferimento);
                    break;
                default:
                    esito.aggiungiAvviso("Tipo di sezione sconosciuta alla riga " + blocco.getRigaInizio() + ": [" + blocco.getTipo() + "]");
                    break;
            }
        }

        assert checkInvariant() : "Invariante violato dopo importaDaFile";
        assert esito.checkInvariant() : "Invariante di EsitoImportazione violato";
        return esito;
    }

    // =========================================================================
    // 1. ELABORAZIONE CAMPO COMUNE
    // =========================================================================
    private void elaboraCampoComune(BloccoBatch blocco, EsitoImportazione esito) {
        String nome = blocco.getValore("nome");
        if (nome == null || nome.trim().isEmpty()) {
            esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Campo comune senza 'nome'.");
            return;
        }

        if (dati.isNomeCampoGlobalePresente(nome)) {
            esito.aggiungiAvviso("Riga " + blocco.getRigaInizio() + ": Campo '" + nome + "' già presente tra i campi base o comuni. Ignorato.");
            return;
        }
        if (dati.haCampoSpecificoInQualcheCategoria(nome)) {
            esito.aggiungiAvviso("Riga " + blocco.getRigaInizio() + ": Campo '" + nome + "' già presente come campo specifico in una categoria. Ignorato.");
            return;
        }

        String desc = blocco.getValoreOrDefault("descrizione", "");
        boolean obbligatorio = Boolean.parseBoolean(blocco.getValoreOrDefault("obbligatorio", "false"));
        TipoCampo tipo = parseTipoCampo(blocco.getValoreOrDefault("tipo", "STRINGA"));

        Campo c = new Campo(nome.trim(), desc.trim(), obbligatorio, tipo);
        if (dati.getConfigurazioneSistema().aggiungiCampoComune(c)) {
            esito.incrementaCampiComuni();
        } else {
            esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Errore durante l'aggiunta del campo comune '" + nome + "'.");
        }
    }

    // =========================================================================
    // 2. ELABORAZIONE CATEGORIA E RELATIVI CAMPI SPECIFICI
    // =========================================================================
    private void elaboraCategoria(BloccoBatch blocco, EsitoImportazione esito) {
        String nomeCat = blocco.getValore("nome");
        if (nomeCat == null || nomeCat.trim().isEmpty()) {
            esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Categoria priva di 'nome'.");
            return;
        }

        Categoria categoriaEsistente = dati.getCategoria(nomeCat);
        Categoria cat;
        if (categoriaEsistente == null) {
            String descCat = blocco.getValoreOrDefault("descrizione", "");
            cat = new Categoria(nomeCat.trim(), descCat.trim());
            dati.aggiungiCategoria(cat);
            esito.incrementaCategorie();
        } else {
            cat = categoriaEsistente;
            esito.aggiungiAvviso("Riga " + blocco.getRigaInizio() + ": Categoria '" + nomeCat + "' già presente nel sistema. Aggiungo eventuali campi specifici.");
        }

        // Elaborazione eventuali campi specifici contenuti nel blocco
        List<BloccoBatch> sottoBlocchi = blocco.getSottoBlocchi();
        for (BloccoBatch sb : sottoBlocchi) {
            if ("CAMPO_SPECIFICO".equalsIgnoreCase(sb.getTipo())) {
                String nomeCampo = sb.getValore("nome");
                if (nomeCampo == null || nomeCampo.trim().isEmpty()) {
                    esito.aggiungiErrore("Riga " + sb.getRigaInizio() + ": Campo specifico per categoria '" + cat.getNome() + "' senza nome.");
                    continue;
                }
                if (dati.isNomeCampoPresenteInCategoria(cat, nomeCampo)) {
                    esito.aggiungiAvviso("Riga " + sb.getRigaInizio() + ": Campo '" + nomeCampo + "' già presente nella categoria '" + cat.getNome() + "'. Ignorato.");
                    continue;
                }

                String descCampo = sb.getValoreOrDefault("descrizione", "");
                boolean obb = Boolean.parseBoolean(sb.getValoreOrDefault("obbligatorio", "false"));
                TipoCampo tipo = parseTipoCampo(sb.getValoreOrDefault("tipo", "STRINGA"));

                Campo cs = new Campo(nomeCampo.trim(), descCampo.trim(), obb, tipo);
                if (cat.aggiungiCampoSpecifico(cs)) {
                    esito.incrementaCampiSpecifici();
                } else {
                    esito.aggiungiErrore("Riga " + sb.getRigaInizio() + ": Impossibile aggiungere campo specifico '" + nomeCampo + "'.");
                }
            }
        }
    }

    // =========================================================================
    // 3. ELABORAZIONE NUOVA PROPOSTA DI INIZIATIVA
    // =========================================================================
    private void elaboraProposta(BloccoBatch blocco, EsitoImportazione esito, LocalDate dataRiferimento) {
        String nomeCat = blocco.getValore("categoria");
        if (nomeCat == null || nomeCat.trim().isEmpty()) {
            esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Proposta priva dell'indicazione di 'categoria'.");
            return;
        }

        Categoria cat = dati.getCategoria(nomeCat);
        if (cat == null) {
            esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Categoria prevista '" + nomeCat + "' non trovata nel sistema. Impossibile creare proposta.");
            return;
        }

        List<Campo> tuttiCampi = dati.getTuttiICampiCategoria(cat);
        Proposta nuovaProposta = new Proposta(cat, configuratore);

        // Assegnazione valori dai campi definiti nel blocco
        for (Campo c : tuttiCampi) {
            String rawVal = blocco.getValore(c.getNome());
            if (rawVal != null && !rawVal.trim().isEmpty()) {
                Object parsed = parseValore(c, rawVal.trim(), esito, blocco.getRigaInizio());
                if (parsed != null) {
                    nuovaProposta.assegnaValore(c, parsed);
                }
            }
        }

        // Validazione rigorosa dei vincoli
        List<String> erroriVincoli = nuovaProposta.validaVincoli(dataRiferimento, tuttiCampi);
        if (!erroriVincoli.isEmpty()) {
            esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Proposta per categoria '" + cat.getNome() + "' non valida per violazione vincoli: " + String.join("; ", erroriVincoli));
            return;
        }

        // La proposta è valida!
        esito.incrementaProposteValide();
        esito.aggiungiPropostaValida(nuovaProposta);

        // Controllo se è richiesta la pubblicazione immediata in bacheca
        boolean pubblicaSubito = Boolean.parseBoolean(blocco.getValoreOrDefault("pubblica_subito", "false"));
        if (pubblicaSubito) {
            if (nuovaProposta.pubblicaInBacheca(dataRiferimento)) {
                dati.getBacheca().aggiungiPropostaAperta(nuovaProposta);
                dati.getArchivioProposte().registraProposta(nuovaProposta);
                esito.incrementaPropostePubblicate();
            } else {
                esito.aggiungiErrore("Riga " + blocco.getRigaInizio() + ": Impossibile pubblicare la proposta in bacheca (Termine ultimo di iscrizione non successivo alla data di pubblicazione).");
            }
        }
    }

    // =========================================================================
    // HELPER DI PARSING E LETTURA BLOCCHI
    // =========================================================================
    private Object parseValore(Campo c, String raw, EsitoImportazione esito, int riga) {
        try {
            switch (c.getTipo()) {
                case STRINGA:
                    return raw;
                case INTERO:
                    return Integer.parseInt(raw);
                case DECIMALE:
                    return Double.parseDouble(raw.replace(',', '.'));
                case DATA:
                    return LocalDate.parse(raw, ValoreCampo.FORMATTER_DATA);
                case ORA:
                    return LocalTime.parse(raw, ValoreCampo.FORMATTER_ORA);
                case BOOLEANO:
                    return raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("s") || raw.equalsIgnoreCase("si");
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            esito.aggiungiErrore("Riga " + riga + ": Valore '" + raw + "' non valido per il campo '" + c.getNome() + "' (tipo atteso: " + c.getTipo().getDescrizione() + ").");
        }
        return null;
    }

    private TipoCampo parseTipoCampo(String s) {
        if (s == null) return TipoCampo.STRINGA;
        String val = s.trim().toUpperCase();
        for (TipoCampo tc : TipoCampo.values()) {
            if (tc.name().equalsIgnoreCase(val) || tc.getDescrizione().equalsIgnoreCase(val)) {
                return tc;
            }
        }
        return TipoCampo.STRINGA;
    }

    private List<BloccoBatch> leggiBlocchi(File file, EsitoImportazione esito) {
        List<BloccoBatch> radice = new ArrayList<>();
        BloccoBatch bloccoCorrente = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int numRiga = 0;

            while ((line = br.readLine()) != null) {
                numRiga++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                    continue; // Commenti o righe vuote
                }

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    String tipoSezione = trimmed.substring(1, trimmed.length() - 1).trim();
                    BloccoBatch nuovoBlocco = new BloccoBatch(tipoSezione, numRiga);

                    if ("CAMPO_SPECIFICO".equalsIgnoreCase(tipoSezione) && bloccoCorrente != null && "CATEGORIA".equalsIgnoreCase(bloccoCorrente.getTipo())) {
                        bloccoCorrente.aggiungiSottoBlocco(nuovoBlocco);
                    } else {
                        radice.add(nuovoBlocco);
                        bloccoCorrente = nuovoBlocco;
                    }
                } else if (trimmed.contains(":")) {
                    int sep = trimmed.indexOf(':');
                    String chiave = trimmed.substring(0, sep).trim();
                    String valore = trimmed.substring(sep + 1).trim();

                    if (bloccoCorrente != null) {
                        if (!bloccoCorrente.getSottoBlocchi().isEmpty() && "CATEGORIA".equalsIgnoreCase(bloccoCorrente.getTipo())) {
                            BloccoBatch ultimoSotto = bloccoCorrente.getSottoBlocchi().get(bloccoCorrente.getSottoBlocchi().size() - 1);
                            ultimoSotto.aggiungiAttributo(chiave, valore);
                        } else {
                            bloccoCorrente.aggiungiAttributo(chiave, valore);
                        }
                    } else {
                        esito.aggiungiAvviso("Riga " + numRiga + ": Proprietà '" + chiave + "' fuori da una sezione [BLOCCO]. Ignorata.");
                    }
                }
            }
        } catch (Exception e) {
            esito.aggiungiErrore("Errore di lettura I/O del file: " + e.getMessage());
        }

        return radice;
    }

    /**
     * Struttura dati interna per rappresentare una sezione o blocco del file batch.
     */
    public static class BloccoBatch {
        private String tipo;
        private int rigaInizio;
        private Map<String, String> attributi;
        private List<BloccoBatch> sottoBlocchi;

        public BloccoBatch(String tipo, int rigaInizio) {
            this.tipo = tipo;
            this.rigaInizio = rigaInizio;
            this.attributi = new LinkedHashMap<>();
            this.sottoBlocchi = new ArrayList<>();
        }

        public String getTipo() { return tipo; }
        public int getRigaInizio() { return rigaInizio; }
        public List<BloccoBatch> getSottoBlocchi() { return sottoBlocchi; }

        public void aggiungiAttributo(String chiave, String valore) {
            attributi.put(chiave.toLowerCase(), valore);
        }

        public void aggiungiSottoBlocco(BloccoBatch sb) {
            sottoBlocchi.add(sb);
        }

        public String getValore(String chiave) {
            if (chiave == null) return null;
            return attributi.get(chiave.toLowerCase());
        }

        public String getValoreOrDefault(String chiave, String def) {
            String v = getValore(chiave);
            return (v != null) ? v : def;
        }
    }
}
