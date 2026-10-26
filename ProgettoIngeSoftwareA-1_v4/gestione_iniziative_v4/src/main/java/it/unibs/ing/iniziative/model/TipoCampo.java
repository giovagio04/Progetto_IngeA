package it.unibs.ing.iniziative.model;

/**
 * Tipi di dato supportati per i campi delle iniziative.
 */
public enum TipoCampo {
    STRINGA("Stringa / Testo"),
    INTERO("Numero Intero"),
    DATA("Data (gg/mm/aaaa)"),
    ORA("Ora (hh:mm)"),
    DECIMALE("Numero Decimale / Importo"),
    BOOLEANO("Booleano (Sì/No)");

    private final String descrizione;

    TipoCampo(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    /**
     * Verifica se il valore inserito rispetta il formato previsto per questo tipo di dato.
     * 
     * @param valore stringa da validare
     * @return true se il formato è conforme, false altrimenti
     */
    public boolean validaFormato(String valore) {
        if (valore == null || valore.trim().isEmpty()) {
            return false;
        }
        String v = valore.trim();
        switch (this) {
            case STRINGA:
                return true;
            case INTERO:
                try {
                    int n = Integer.parseInt(v);
                    return n > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            case DECIMALE:
                try {
                    double d = Double.parseDouble(v.replace(',', '.'));
                    return d >= 0.0;
                } catch (NumberFormatException e) {
                    return false;
                }
            case DATA:
                try {
                    java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.ofPattern("d/M/uuuu"));
                    return true;
                } catch (java.time.format.DateTimeParseException e) {
                    return false;
                }
            case ORA:
                try {
                    java.time.LocalTime.parse(v, java.time.format.DateTimeFormatter.ofPattern("H:m"));
                    return true;
                } catch (java.time.format.DateTimeParseException e) {
                    return false;
                }
            case BOOLEANO:
                return v.equalsIgnoreCase("S") || v.equalsIgnoreCase("N")
                        || v.equalsIgnoreCase("SI") || v.equalsIgnoreCase("NO")
                        || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false");
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return descrizione;
    }
}
