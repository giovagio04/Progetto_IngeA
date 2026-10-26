package it.unibs.ing.iniziative.storage;

import java.io.*;

/**
 * Classe utility per il salvataggio e il caricamento dei dati di sistema su file binario.
 */
public class GestorePersistenza {

    private static final String FILE_DATI = "dati_sistema.dat";

    public static void salvaDati(DatiSistema dati) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_DATI))) {
            oos.writeObject(dati);
        }
    }

    public static DatiSistema caricaDati() throws IOException, ClassNotFoundException {
        File file = new File(FILE_DATI);
        if (!file.exists()) {
            return new DatiSistema();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (DatiSistema) ois.readObject();
        }
    }
}
