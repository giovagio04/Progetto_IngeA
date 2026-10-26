package it.unibs.ing.iniziative.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Associa un campo (fotografato al momento della creazione della proposta)
 * al valore concreto attribuitogli in una proposta di iniziativa.
 * 
 * Invarianti di classe:
 * - Il riferimento al campo non deve essere nullo e deve rispettare il proprio invariante.
 * 
 * @inv campo != null && campo.checkInvariant()
 */
public class ValoreCampo implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMATTER_ORA = DateTimeFormatter.ofPattern("HH:mm");

    private final Campo campo;
    private Object valore;

    /**
     * Costruisce un'associazione tra uno snapshot immutabile del campo e il valore compilato.
     * 
     * @param campo il campo di riferimento
     * @param valore il valore assegnato (può essere null per i campi facoltativi)
     * @pre campo != null && campo.checkInvariant()
     * @post checkInvariant()
     */
    public ValoreCampo(Campo campo, Object valore) {
        assert campo != null && campo.checkInvariant() : "Precondizione violata: il campo non può essere nullo o invalido";
        // Copia difensiva del campo per isolare la proposta da modifiche future a campi comuni/specifici
        this.campo = new Campo(campo.getNome(), campo.getDescrizione(), campo.isObbligatorio(), campo.getTipo(), campo.getValore());
        this.valore = valore;

        assert checkInvariant() : "Invariante violato in ValoreCampo";
    }

    /**
     * Verifica l'invariante di classe.
     */
    public boolean checkInvariant() {
        return campo != null && campo.checkInvariant();
    }

    public Campo getCampo() {
        return campo;
    }

    public Object getValore() {
        return valore;
    }

    public void setValore(Object valore) {
        assert checkInvariant() : "Invariante violato prima di setValore";
        this.valore = valore;
        assert checkInvariant() : "Invariante violato dopo setValore";
    }

    public boolean isCompilato() {
        if (valore == null) return false;
        if (valore instanceof String) {
            return !((String) valore).trim().isEmpty();
        }
        return true;
    }

    public String asString() {
        return valore != null ? valore.toString() : null;
    }

    public Integer asInteger() {
        if (valore instanceof Integer) {
            return (Integer) valore;
        }
        if (valore instanceof Number) {
            return ((Number) valore).intValue();
        }
        if (valore instanceof String) {
            try {
                return Integer.parseInt(((String) valore).trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public Double asDouble() {
        if (valore instanceof Double) {
            return (Double) valore;
        }
        if (valore instanceof Number) {
            return ((Number) valore).doubleValue();
        }
        if (valore instanceof String) {
            try {
                return Double.parseDouble(((String) valore).trim().replace(',', '.'));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public LocalDate asLocalDate() {
        if (valore instanceof LocalDate) {
            return (LocalDate) valore;
        }
        if (valore instanceof String) {
            try {
                return LocalDate.parse(((String) valore).trim(), DateTimeFormatter.ofPattern("d/M/uuuu"));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public LocalTime asLocalTime() {
        if (valore instanceof LocalTime) {
            return (LocalTime) valore;
        }
        if (valore instanceof String) {
            try {
                return LocalTime.parse(((String) valore).trim(), DateTimeFormatter.ofPattern("H:m"));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public Boolean asBoolean() {
        if (valore instanceof Boolean) {
            return (Boolean) valore;
        }
        if (valore instanceof String) {
            String s = ((String) valore).trim();
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("s") || s.equalsIgnoreCase("si") || s.equalsIgnoreCase("sì")) {
                return true;
            }
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no")) {
                return false;
            }
        }
        return null;
    }

    public String getValoreFormattato() {
        if (!isCompilato()) {
            return "(Non compilato)";
        }
        if (valore instanceof LocalDate) {
            return ((LocalDate) valore).format(FORMATTER_DATA);
        }
        if (valore instanceof LocalTime) {
            return ((LocalTime) valore).format(FORMATTER_ORA);
        }
        if (valore instanceof Boolean) {
            return ((Boolean) valore) ? "Sì" : "No";
        }
        if (valore instanceof Double) {
            return String.format(java.util.Locale.ITALY, "%.2f €", (Double) valore);
        }
        return valore.toString();
    }

    @Override
    public String toString() {
        return campo.getNome() + ": " + getValoreFormattato();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValoreCampo that = (ValoreCampo) o;
        return Objects.equals(campo, that.campo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campo);
    }
}
