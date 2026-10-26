package it.unibs.ing.iniziative.ui;

import it.kibo.fp.lib.InputData;
import it.unibs.ing.iniziative.model.Campo;
import it.unibs.ing.iniziative.model.TipoCampo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility per l'acquisizione sicura di dati da console con validazione rigorosa dei formati.
 * Delega le primitive di lettura alla libreria Kibo (it.kibo.fp.lib.InputData).
 */
public class InputDati {
    private static final char RISPOSTA_SI = 'S';
    private static final char RISPOSTA_NO = 'N';

    public static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("d/M/uuuu");
    public static final DateTimeFormatter FORMATTER_ORA = DateTimeFormatter.ofPattern("H:m");

    public static String leggiStringa(String messaggio) {
        return InputData.readString(messaggio, false);
    }

    public static String leggiStringaNonVuota(String messaggio) {
        return InputData.readNonEmptyString(messaggio, false);
    }

    public static char leggiChar(String messaggio) {
        return InputData.readChar(messaggio);
    }

    public static char leggiUpperChar(String messaggio, String ammissibili) {
        return Character.toUpperCase(InputData.readChar(messaggio, ammissibili));
    }

    public static int leggiIntero(String messaggio) {
        return InputData.readInteger(messaggio);
    }

    public static int leggiInteroPositivo(String messaggio) {
        while (true) {
            String str = leggiStringaNonVuota(messaggio).trim();
            try {
                int n = Integer.parseInt(str);
                if (n > 0) {
                    return n;
                }
                System.out.println("Attenzione: il numero deve essere un intero positivo strettamente maggiore di 0. Riprova.");
            } catch (NumberFormatException e) {
                System.out.println("Attenzione: il dato inserito non è nel formato corretto. È richiesto un numero intero (es. 20). Riprova.");
            }
        }
    }

    public static int leggiInteroNonNegativo(String messaggio) {
        while (true) {
            String str = leggiStringaNonVuota(messaggio).trim();
            try {
                int n = Integer.parseInt(str);
                if (n >= 0) {
                    return n;
                }
                System.out.println("Attenzione: il numero non può essere negativo. Riprova.");
            } catch (NumberFormatException e) {
                System.out.println("Attenzione: il dato inserito non è nel formato corretto. È richiesto un numero intero. Riprova.");
            }
        }
    }

    public static int leggiInteroConMinimo(String messaggio, int minimo) {
        return InputData.readIntegerWithMinimum(messaggio, minimo);
    }

    public static int leggiIntero(String messaggio, int minimo, int massimo) {
        return InputData.readIntegerBetween(messaggio, minimo, massimo);
    }

    public static Integer leggiInteroOpzionale(String messaggio, int min) {
        while (true) {
            String line = leggiStringa(messaggio).trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                int valore = Integer.parseInt(line);
                if (valore >= min) {
                    return valore;
                }
                System.out.println("Attenzione: il valore deve essere maggiore o uguale a " + min + ". Riprova.");
            } catch (NumberFormatException e) {
                System.out.println("Attenzione: il dato inserito non è nel formato corretto. Inserisci un numero intero (o premi Invio per saltare).");
            }
        }
    }

    public static double leggiDouble(String messaggio) {
        return InputData.readDouble(messaggio);
    }

    public static double leggiDoubleNonNegativo(String messaggio) {
        while (true) {
            String str = leggiStringaNonVuota(messaggio).trim();
            try {
                double d = Double.parseDouble(str.replace(',', '.'));
                if (d >= 0.0) {
                    return d;
                }
                System.out.println("Attenzione: l'importo non può essere un valore negativo. Riprova.");
            } catch (NumberFormatException e) {
                System.out.println("Attenzione: il dato inserito non è nel formato corretto. È richiesto un valore numerico (es. 10.50 o 0). Riprova.");
            }
        }
    }

    public static double leggiDoubleConMinimo(String messaggio, double minimo) {
        return InputData.readDoubleWithMinimum(messaggio, minimo);
    }

    public static Double leggiDoubleOpzionale(String messaggio, double min) {
        while (true) {
            String line = leggiStringa(messaggio).trim().replace(',', '.');
            if (line.isEmpty()) {
                return null;
            }
            try {
                double valore = Double.parseDouble(line);
                if (valore >= min) {
                    return valore;
                }
                System.out.println("Attenzione: il valore deve essere maggiore o uguale a " + min + ". Riprova.");
            } catch (NumberFormatException e) {
                System.out.println("Attenzione: il dato inserito non è nel formato corretto. Inserisci un numero decimale (o premi Invio per saltare).");
            }
        }
    }

    public static LocalDate leggiData(String messaggio) {
        while (true) {
            String str = leggiStringaNonVuota(messaggio).trim();
            try {
                return LocalDate.parse(str, FORMATTER_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("Attenzione: formato data non valido. Inserire una data valida nel formato gg/mm/aaaa (es. 15/10/2026). Riprova.");
            }
        }
    }

    public static LocalDate leggiDataOpzionale(String messaggio) {
        while (true) {
            String line = leggiStringa(messaggio).trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(line, FORMATTER_DATA);
            } catch (DateTimeParseException e) {
                System.out.println("Attenzione: formato data non valido. Usa il formato gg/mm/aaaa (o premi Invio per saltare). Riprova.");
            }
        }
    }

    public static LocalTime leggiOra(String messaggio) {
        while (true) {
            String str = leggiStringaNonVuota(messaggio).trim();
            try {
                return LocalTime.parse(str, FORMATTER_ORA);
            } catch (DateTimeParseException e) {
                System.out.println("Attenzione: formato ora non valido. Inserire un'ora valida nel formato hh:mm (es. 20:30). Riprova.");
            }
        }
    }

    public static LocalTime leggiOraOpzionale(String messaggio) {
        while (true) {
            String line = leggiStringa(messaggio).trim();
            if (line.isEmpty()) {
                return null;
            }
            try {
                return LocalTime.parse(line, FORMATTER_ORA);
            } catch (DateTimeParseException e) {
                System.out.println("Attenzione: formato ora non valido. Usa il formato hh:mm (o premi Invio per saltare). Riprova.");
            }
        }
    }

    public static boolean yesOrNo(String messaggio) {
        String prompt = messaggio + " (" + RISPOSTA_SI + "/" + RISPOSTA_NO + "): ";
        char risposta = leggiUpperChar(prompt, "" + RISPOSTA_SI + RISPOSTA_NO + Character.toLowerCase(RISPOSTA_SI) + Character.toLowerCase(RISPOSTA_NO));
        return risposta == RISPOSTA_SI;
    }

    public static Boolean leggiBooleanOpzionale(String messaggio) {
        while (true) {
            String input = leggiStringa(messaggio + " (S/N, Invio per saltare): ").trim().toUpperCase();
            if (input.isEmpty()) {
                return null;
            }
            if (input.equals("S")) return true;
            if (input.equals("N")) return false;
            System.out.println("Attenzione: inserisci 'S', 'N' oppure premi Invio.");
        }
    }

    /**
     * Permette all'utente di selezionare uno dei tipi di dato supportati per i campi
     * tramite il menu Kibo.
     */
    public static TipoCampo scegliTipoCampo() {
        TipoCampo[] tipi = TipoCampo.values();
        String[] voci = new String[tipi.length];
        for (int i = 0; i < tipi.length; i++) {
            voci[i] = tipi[i].getDescrizione();
        }
        MyMenu menuTipo = new MyMenu("SELEZIONE TIPO DI DATO", voci, false);
        int scelta = menuTipo.scegli();
        return tipi[scelta - 1];
    }

    /**
     * Acquisisce da console il valore adatto per il campo indicato nella creazione di una proposta,
     * rispettandone rigorosamente il TipoCampo e il vincolo di obbligatorietà.
     * In caso di formato errato, mostra il messaggio di errore e permette all'utente di riprovare.
     */
    public static Object leggiValorePerCampo(Campo campo) {
        String etichetta = campo.getNome() + " [" + campo.getTipo().getDescrizione() + 
                (campo.isObbligatorio() ? ", Obbligatorio" : ", Facoltativo") + "]";
        if (!campo.getDescrizione().isEmpty()) {
            etichetta += "\n  Nota: " + campo.getDescrizione();
        }
        System.out.println("-> " + etichetta);

        switch (campo.getTipo()) {
            case STRINGA:
                if (campo.isObbligatorio()) {
                    return leggiStringaNonVuota("  Valore: ");
                } else {
                    String str = leggiStringa("  Valore (premi Invio per lasciare vuoto): ");
                    return str.isEmpty() ? null : str;
                }

            case INTERO:
                if (campo.getNome().equalsIgnoreCase("Numero di partecipanti")) {
                    return leggiInteroPositivo("  Numero partecipanti (Intero > 0): ");
                } else if (campo.isObbligatorio()) {
                    return leggiInteroNonNegativo("  Valore intero (>= 0): ");
                } else {
                    return leggiInteroOpzionale("  Valore intero (>= 0, Invio per saltare): ", 0);
                }

            case DECIMALE:
                if (campo.isObbligatorio()) {
                    return leggiDoubleNonNegativo("  Importo / Valore decimale (>= 0): ");
                } else {
                    return leggiDoubleOpzionale("  Importo / Valore decimale (>= 0, Invio per saltare): ", 0.0);
                }

            case DATA:
                if (campo.isObbligatorio()) {
                    return leggiData("  Data (gg/mm/aaaa): ");
                } else {
                    return leggiDataOpzionale("  Data (gg/mm/aaaa, Invio per saltare): ");
                }

            case ORA:
                if (campo.isObbligatorio()) {
                    return leggiOra("  Ora (hh:mm): ");
                } else {
                    return leggiOraOpzionale("  Ora (hh:mm, Invio per saltare): ");
                }

            case BOOLEANO:
                if (campo.isObbligatorio()) {
                    return yesOrNo("  Valore");
                } else {
                    return leggiBooleanOpzionale("  Valore");
                }

            default:
                return leggiStringa("  Valore: ");
        }
    }
}
