package it.kibo.fp.lib;

import java.io.InputStream;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * The class <strong>InputData</strong> can read a specific data type inserted
 * in input by the user. It also allows the possibility to make controls on the
 * data inserted.
 *
 * @author Alessandro Muscio
 * @version 1.0
 */
public final class InputData {
  private InputData() {
    throw new UnsupportedOperationException();
  }

  private static InputStream lastIn = null;
  private static Scanner reader = null;

  private static final String RED_ATTENTION = AnsiColors.RED + "Attention!" + AnsiColors.RESET;
  private static final String ALPHANUMERIC_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";
  private static final String ALPHANUMERIC_CHARACTERS_ERROR = RED_ATTENTION
      + "\nOnly alphanumeric characters are allowed.";
  private static final String EMPTY_STRING_ERROR = RED_ATTENTION + "\nNo characters were inserted.";
  private static final String ALLOWED_CHARACTERS_ERROR = RED_ATTENTION + "\nThe only allowed characters are: %s";
  private static final String INTEGER_FORMAT_ERROR = RED_ATTENTION
      + "\nThe inserted data is in a incorrect format. An integer is required.";
  private static final String DOUBLE_FORMAT_ERROR = RED_ATTENTION
      + "\nThe inserted data is in a incorrect format. A double is required.";
  private static final String MINIMUM_ERROR = RED_ATTENTION + "\nA value greater equal than %.2f is required.";
  private static final String MAXIMUM_ERROR = RED_ATTENTION + "\nA value less equal than %.2f is required.";
  private static final String YES_ANSWERS = "yY";
  private static final String NO_ANSWERS = "nN";

  private static synchronized Scanner getReader() {
    if (reader == null || System.in != lastIn) {
      lastIn = System.in;
      reader = new Scanner(System.in);
    }
    return reader;
  }

  private static boolean hasAlphanumericCharacters(String message) {
    for (int i = 0; i < message.length(); i++) {
      char currentChar = message.charAt(i);
      if (ALPHANUMERIC_CHARACTERS.indexOf(currentChar) == -1)
        return false;
    }
    return true;
  }

  public static String readString(String message, boolean alphanumeric) {
    boolean isAlphanumeric;
    String read;

    if (alphanumeric) {
      do {
        System.out.print(message);
        read = getReader().nextLine().trim();
        isAlphanumeric = hasAlphanumericCharacters(read);
        if (!isAlphanumeric)
          System.out.println(ALPHANUMERIC_CHARACTERS_ERROR);
      } while (!isAlphanumeric);
    } else {
      System.out.print(message);
      read = getReader().nextLine().trim();
    }

    return read;
  }

  public static String readNonEmptyString(String message, boolean alphanumeric) {
    boolean isStringEmpty = true;
    String read;

    do {
      read = readString(message, alphanumeric);
      if (isStringEmpty = read.isBlank())
        System.out.println(EMPTY_STRING_ERROR);
    } while (isStringEmpty);

    return read;
  }

  public static char readChar(String message, String allowed) {
    boolean isAllowed = false;
    String read;
    char readChar;

    do {
      read = readNonEmptyString(message, false);
      readChar = read.charAt(0);
      if (allowed.indexOf(readChar) != -1)
        isAllowed = true;
      else
        System.out.println(String.format(ALLOWED_CHARACTERS_ERROR, Arrays.toString(allowed.toCharArray())));
    } while (!isAllowed);

    return readChar;
  }

  public static char readChar(String message) {
    String read = readNonEmptyString(message, false);
    return read.charAt(0);
  }

  public static boolean readYesOrNo(String question) {
    question = question + "? [" + YES_ANSWERS.charAt(1) + "/" + NO_ANSWERS.charAt(0) + "] ";
    char valoreLetto = readChar(question);
    return YES_ANSWERS.indexOf(valoreLetto) != -1;
  }

  private static void flushReader() {
    // Gestito automaticamente tramite nextLine()
  }

  public static int readInteger(String message) {
    boolean isInteger;
    int read = 0;

    do {
      try {
        System.out.print(message);
        read = Integer.parseInt(getReader().nextLine().trim());
        isInteger = true;
      } catch (NumberFormatException | InputMismatchException e) {
        System.out.println(INTEGER_FORMAT_ERROR);
        isInteger = false;
      }
    } while (!isInteger);

    return read;
  }

  public static int readIntegerWithMinimum(String message, int min) {
    boolean isAboveMin = false;
    int read;

    do {
      read = readInteger(message);
      if (read >= min)
        isAboveMin = true;
      else
        System.out.println(String.format(MINIMUM_ERROR, (double) min));
    } while (!isAboveMin);

    return read;
  }

  public static int readIntegerWithMaximum(String message, int max) {
    boolean isBelowMax = false;
    int read;

    do {
      read = readInteger(message);
      if (read <= max)
        isBelowMax = true;
      else
        System.out.println(String.format(MAXIMUM_ERROR, (double) max));
    } while (!isBelowMax);

    return read;
  }

  public static int readIntegerBetween(String message, int min, int max) {
    boolean isBetweenMinMax = false;
    int read;

    do {
      read = readInteger(message);
      if (read < min)
        System.out.println(String.format(MINIMUM_ERROR, (double) min));
      else if (read > max)
        System.out.println(String.format(MAXIMUM_ERROR, (double) max));
      else
        isBetweenMinMax = true;
    } while (!isBetweenMinMax);

    return read;
  }

  public static double readDouble(String message) {
    boolean isDouble;
    double read = Double.NaN;

    do {
      System.out.print(message);
      try {
        read = Double.parseDouble(getReader().nextLine().trim());
        isDouble = true;
      } catch (NumberFormatException | InputMismatchException e) {
        System.out.println(DOUBLE_FORMAT_ERROR);
        isDouble = false;
      }
    } while (!isDouble);

    return read;
  }

  public static double readDoubleWithMinimum(String message, double min) {
    boolean isAboveMin = false;
    double read;

    do {
      read = readDouble(message);
      if (read >= min)
        isAboveMin = true;
      else
        System.out.println(String.format(MINIMUM_ERROR, min));
    } while (!isAboveMin);

    return read;
  }

  public static double readDoubleWithMaximum(String message, double max) {
    boolean isBelowMax = false;
    double read;

    do {
      read = readDouble(message);
      if (read <= max)
        isBelowMax = true;
      else
        System.out.println(String.format(MAXIMUM_ERROR, max));
    } while (!isBelowMax);

    return read;
  }

  public static double readDoubleBetween(String message, double min, double max) {
    boolean isBetweenMinMax = false;
    double read;

    do {
      read = readDouble(message);
      if (read < min)
        System.out.println(String.format(MINIMUM_ERROR, min));
      else if (read > max)
        System.out.println(String.format(MAXIMUM_ERROR, max));
      else
        isBetweenMinMax = true;
    } while (!isBetweenMinMax);

    return read;
  }
}