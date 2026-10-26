package it.unibs.ing.iniziative.ui;

import it.kibo.fp.lib.Menu;

/**
 * Wrapper per la classe Menu fornita dalla libreria Kibo (it.kibo.fp.lib).
 * Gestisce l'incapsulamento della visualizzazione e della selezione delle voci.
 */
public class MyMenu {
    private final Menu menu;

    public MyMenu(String titolo, String[] voci) {
        this(titolo, voci, true);
    }

    public MyMenu(String titolo, String[] voci, boolean useExitEntry) {
        this.menu = new Menu(titolo, voci, useExitEntry, true, true);
    }

    public int scegli() {
        return this.menu.choose();
    }

    public int choose() {
        return this.menu.choose();
    }

    public static void clearConsole() {
        Menu.clearConsole();
    }
}
