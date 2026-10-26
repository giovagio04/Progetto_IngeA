package it.unibs.ing.iniziative.ui;

import it.kibo.fp.lib.InputData;
import it.unibs.ing.iniziative.model.TipoCampo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility per l'acquisizione sicura di dati da console con validazione rigorosa dei formati.
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
        return InputData.readIntegerWithMinimum(messaggio, 0);
    }

    public static int leggiInteroConMinimo(String messaggio, int minimo) {
        return InputData.readIntegerWithMinimum(messaggio, minimo);
    }

    public static int leggiIntero(String messaggio, int minimo, int massimo) {
        return InputData.readIntegerBetween(messaggio, minimo, massimo);
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

    public static boolean yesOrNo(String messaggio) {
        String prompt = messaggio + " (" + RISPOSTA_SI + "/" + RISPOSTA_NO + "): ";
        char risposta = leggiUpperChar(prompt, "" + RISPOSTA_SI + RISPOSTA_NO + Character.toLowerCase(RISPOSTA_SI) + Character.toLowerCase(RISPOSTA_NO));
        return risposta == RISPOSTA_SI;
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
}
