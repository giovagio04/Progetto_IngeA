# Guida Completa alla Logica del Programma – Versione 1 (V1)

Questo documento spiega in modo dettagliato l'architettura, il flusso di esecuzione, le regole di dominio e il funzionamento di ogni classe della **Versione 1** del progetto di *Ingegneria del Software A* (Prof.ssa Marina Zanella, A.A. 2025-2026).

---

## 1. Obiettivo della Versione 1

La **Versione 1** costituisce il nucleo di configurazione (*back-end*) del sistema per l'organizzazione di iniziative ricreative/culturali.
In questa versione:
- **Unico attore previsto:** il **Configuratore** (non esistono né l'attore Fruitore, né le proposte in bacheca, che verranno introdotti rispettivamente nelle versioni V3 e V2).
- **Compiti del Configuratore in V1:**
  1. Effettuare il primo accesso con le credenziali predefinite (`config` / `config`) e registrare immediatamente le proprie **credenziali personali univoche**.
  2. Al primissimo avvio del sistema, completare gli **8 Campi Base obbligatori** previsti dalla specifica, inserendo il nome da attribuire a ciascuno degli 8 ruoli predefiniti. Una volta salvati, diventano **immutabili** per tutta la vita dell'applicazione.
  3. Definire, modificare (obbligatorietà) o rimuovere i **Campi Comuni** (condivisi da tutte le categorie).
  4. Creare e rimuovere le **Categorie** di iniziative.
  5. Definire, modificare (obbligatorietà) o rimuovere i **Campi Specifici** associati a ciascuna categoria.
  6. Visualizzare in qualsiasi momento le categorie e tutti i campi (base, comuni e specifici).
  7. Mantenere tutti i dati in **forma persistente** tra un'esecuzione e la successiva tramite serializzazione su file (`dati_sistema.dat`).

---

## 2. Architettura del Sistema (Pattern MVC)

Per soddisfare il requisito non funzionale della consegna (*"progettare l'architettura interna in modo da ridurre gli effetti collaterali se in futuro il sistema di interazione testuale fosse sostituito da una GUI"*), il progetto è suddiviso in **4 package disaccoppiati**:

```
it.unibs.ing.iniziative
│
├── Main.java                          --> Entry-point: bootstrap, login e avvio
├── TestSuiteCompleta.java             --> Suite di collaudo automatico (16 test intensivi)
│
├── ui/ (View / Presentation Layer)
│   ├── MenuConfiguratore.java         --> Interfaccia a riga di comando (CLI)
│   ├── InputDati.java                 --> Acquisizione sicura dell'input da tastiera
│   └── MyMenu.java                    --> Gestione e stampa dei menu testuali
│
├── controller/ (Controller Layer)
│   └── ConfigurazioneController.java  --> Logica applicativa e coordinamento tra View e Model
│
├── model/ (Domain Model Layer)
│   ├── Utente.java                    --> Classe astratta per le credenziali utente
│   ├── Configuratore.java             --> Configuratore (con flag credenziali predefinite)
│   ├── GestoreUtenti.java             --> Autenticazione, unicità username e cambio credenziali
│   ├── RuoloCampoBase.java            --> Enum dei ruoli dei campi base previsti dalla specifica
│   ├── Campo.java                     --> Rappresentazione di un campo (Base, Comune o Specifico)
│   ├── TipoCampo.java                 --> Enum dei tipi di dato (Stringa, Intero, Data, Ora, ecc.)
│   ├── Categoria.java                 --> Categoria di iniziative e relativi campi specifici
│   └── ConfigurazioneSistema.java     --> Gestione dei campi globali (Campi Base e Campi Comuni)
│
└── storage/ (Persistence Layer)
    ├── DatiSistema.java               --> Contenitore radice (Root Aggregate) dello stato del sistema
    └── GestorePersistenza.java        --> Salvataggio e caricamento su file binario (dati_sistema.dat)
```

### Perché questa separazione è importante all'orale?
La classe `MenuConfiguratore` (View) **non modifica mai direttamente le liste del modello**, ma invoca i metodi di `ConfigurazioneController`. Se domani si volesse sostituire la console con una finestra grafica (JavaFX o Swing), basterebbe sostituire `MenuConfiguratore` lasciando invariati `controller`, `model` e `storage`.

---

## 3. Flusso di Esecuzione Passo-Passo

### Fase 1: Avvio e Caricamento Persistenza (`Main.java`)
1. All'avvio, `Main` invoca `GestorePersistenza.caricaDati()`.
2. Se il file `dati_sistema.dat` esiste, ricostruisce l'intero oggetto `DatiSistema` salvato nella sessione precedente.
3. Se il file non esiste (primo avvio assoluto), crea un nuovo `DatiSistema()` vergine, al cui interno:
   - `GestoreUtenti` contiene l'utenza predefinita di fabbrica `config` / `config` (con `usaCredenzialiPredefinite = true`).
   - `ConfigurazioneSistema` ha la lista dei campi base vuota (`campiBaseImpostati = false`) e la lista dei campi comuni vuota.
   - La lista delle categorie è vuota.

### Fase 2: Login e Primo Accesso del Configuratore
1. Il sistema chiede `Username` e `Password`.
2. Un nuovo configuratore accede digitando `config` / `config`.
3. Il sistema rileva `config.isUsaCredenzialiPredefinite() == true` e **obbliga** l'utente a scegliere subito un `Nuovo Username personale` e una `Nuova Password personale`.
4. `GestoreUtenti.cambiaCredenziali(...)` verifica che:
   - Il nuovo username non sia vuoto.
   - Il nuovo username non sia di nuovo `"config"`.
   - Il nuovo username non sia già usato da un altro configuratore registrato (controllo *case-insensitive*).
5. Viene creato un nuovo oggetto `Configuratore(nuovoUsername, nuovaPassword, false)` e salvato immediatamente su disco. L'utenza `config`/`config` resta disponibile in `GestoreUtenti` per permettere in futuro la registrazione di ulteriori configuratori autorizzati (come previsto dalla specifica).

### Fase 3: Compilazione degli 8 Campi Base da parte del Configuratore con Controllo di Formato (Solo al Primissimo Avvio)
1. Subito dopo l'autenticazione, `Main` controlla `if (!controller.isCampiBaseFissati())`.
2. Se i campi base non sono ancora stati fissati, viene lanciato `menu.eseguiImpostazioneManualeCampiBase()`.
3. Il flusso guida il configuratore attraverso gli **8 campi base obbligatori** previsti dalla specifica (`RuoloCampoBase`), eseguendo il controllo rigoroso sul formato di ciascun tipo e consentendo di riprovare in caso di errore:
   - **`Titolo` (Stringa):** testo non vuoto (es. *"Festa di Primavera"*).
   - **`Numero di partecipanti` (Intero):** verifica che sia un numero intero strettamente positivo (> 0). In caso di stringhe non numeriche (es. *"dieci"*) o negative, segnala l'errore di formato e richiede il dato.
   - **`Termine ultimo di iscrizione` (Data `gg/mm/aaaa`):** verifica sintattica e semantica della data tramite `DateTimeFormatter` (`LocalDate`). In caso di data inesistente (es. *"31/02/2026"*) o testo generico, segnala l'errore e richiede il dato.
   - **`Luogo` (Stringa):** indirizzo o ritrovo dei partecipanti (testo non vuoto).
   - **`Data` (Data `gg/mm/aaaa`):** oltre al formato data valido, verifica il vincolo di dominio esplicito della specifica: **deve essere successiva di almeno due giorni** rispetto al *Termine ultimo di iscrizione*. In caso contrario, stampa l'errore e permette di reinserirla.
   - **`Ora` (Ora `hh:mm`):** verifica del formato orario `LocalTime` (24 ore). Errori di formato (es. *"25:70"*) vengono bloccati con richiesta di riprova.
   - **`Quota individuale` (Decimale):** verifica numerica con supporto sia per virgola che per punto decimale (es. `15.50` o `0`). Se negativa o non numerica, segnala l'errore e richiede il dato.
   - **`Data conclusiva` (Data `gg/mm/aaaa`):** oltre al formato data valido, verifica il vincolo di dominio: **non può cronologicamente precedere la data di inizio** (*Data*). In caso contrario, stampa l'errore e permette di reinserirla.
4. Ciascun campo viene istanziato come `Campo` con il proprio nome canonico di specifica, la descrizione, l'obbligatorietà (`true`), il tipo di dato e il valore compilato validato (`valore`).
5. Alla fine, il sistema mostra il riepilogo completo degli 8 campi base compilati e chiama `controller.fissaCampiBase(campi)`, che blocca definitivamente l'elenco (`campiBaseImpostati = true`) e salva subito su file (`dati_sistema.dat`).
6. **In tutti gli avvii successivi**, poiché `isCampiBaseFissati()` è `true`, questa procedura di setup iniziale viene saltata automaticamente e i campi base non potranno mai più essere alterati.

### Fase 4: Menu Operativo del Configuratore (`MenuConfiguratore.java`)
Il configuratore accede al menu principale con 4 sezioni:
1. **Visualizza Campi Base (Immutabili):** mostra l'elenco dei campi base fissati al primo avvio.
2. **Gestisci Campi Comuni:** sottomenu per visualizzare, aggiungere, rimuovere o cambiare l'obbligatorietà dei campi comuni.
3. **Gestisci Categorie e Campi Specifici:** sottomenu per visualizzare le categorie, crearne di nuove (con possibilità di inserire contestualmente i campi specifici), rimuovere una categoria (eliminando a cascata i suoi campi specifici), oppure aggiungere/rimuovere/modificare campi specifici di una categoria esistente.
4. **Visualizza Configurazione Completa:** stampa il riepilogo generale di Campi Base, Campi Comuni e, per ogni Categoria registrata, la scheda completa di tutti i suoi campi (Base + Comuni + Specifici).

Ogni singola modifica effettuata nei menu viene salvata immediatamente su disco tramite `salvaStatoPersistente()`, oltre al salvataggio finale all'uscita (`0 - Esci`).

---

## 4. Regole di Dominio e Vincoli di Unicità

La specifica stabilisce che:
> *"Ciascuna categoria è dotata di un nome che la individua univocamente."*
> *"Relativamente a ogni singola categoria, ogni campo è dotato di un nome che lo individua univocamente."*
> *"Fissare i campi comuni [...] può indifferentemente precedere o seguire la creazione di categorie."*

Per garantire matematicamente questi vincoli in qualunque ordine il configuratore operi:
1. **Unicità delle Categorie:** non possono esistere due categorie con lo stesso nome (confronto *case-insensitive*, es. `"Sport"` e `"sport"` sono considerati uguali).
2. **Aggiunta di un Campo Comune:** un nuovo campo comune viene **rifiutato** se il suo nome coincide (case-insensitive) con:
   - uno dei **Campi Base**;
   - un altro **Campo Comune** già presente;
   - un **Campo Specifico** già presente in una qualsiasi **Categoria esistente** (`haCampoSpecificoInQualcheCategoria`). In questo modo, anche se il configuratore crea prima una categoria con il campo specifico `"Certificato"` e poi tenta di creare un campo comune `"Certificato"`, il sistema blocca la collisione.
3. **Aggiunta di un Campo Specifico a una Categoria `C`:** viene **rifiutato** se il suo nome coincide con:
   - uno dei **Campi Base**;
   - uno dei **Campi Comuni**;
   - un altro **Campo Specifico** già presente nella **stessa categoria `C`**.
   - *Nota importante:* Due categorie diverse (es. `"Sport"` e `"Arte"`) **possono** invece avere entrambe un campo specifico con lo stesso nome (es. `"Guida"`), poiché l'unicità è relativa a ogni singola categoria.

---

## 5. Design by Contract (Precondizioni, Postcondizioni, Invarianti)

La consegna evidenzia nei Requisiti Non Funzionali:
> *"Requisito non prescrittivo ma importante in sede di valutazione è l’impiego di precondizioni, postcondizioni e invarianti di classe entro il codice Java."*

Tutte le classi del modello e dello storage implementano il **Design by Contract** tramite:
- Annotazioni Javadoc formali (`@inv`, `@pre`, `@post`).
- Metodo pubblico `checkInvariant()` su ogni classe di dominio (`Campo`, `Categoria`, `ConfigurazioneSistema`, `Utente`, `GestoreUtenti`, `DatiSistema`).
- Istruzioni `assert` di Java all'inizio (precondizione + invariante) e alla fine (postcondizione + invariante) dei metodi mutatori.

| Classe | Invariante di Classe (`checkInvariant()`) |
| :--- | :--- |
| **`Campo`** | `nome != null && !nome.trim().isEmpty() && tipo != null && descrizione != null` |
| **`Categoria`** | `nome` non vuoto, `campiSpecifici` non nullo, nessun campo nullo o invalido, nessun nome duplicato (case-insensitive) in `campiSpecifici`. |
| **`ConfigurazioneSistema`** | Liste non nulle; se `campiBaseImpostati == true` contiene esattamente gli 8 campi corrispondenti ai ruoli previsti in `RuoloCampoBase` con i rispettivi tipi, tutti obbligatori (se non fissati la lista è vuota); nessuna collisione di nome tra `campiBase` e `campiComuni` né duplicati interni. |
| **`Utente` / `Configuratore`** | `username` non nullo/vuoto e `password` non nulla/vuota. |
| **`GestoreUtenti`** | Mappa `utenti` non nulla e non vuota; ogni chiave coincide con `utente.getUsername().toLowerCase()`. |
| **`DatiSistema`** | `gestoreUtenti`, `configurazioneSistema` e `categorie` validi; nessun duplicato tra le categorie; nessun campo specifico di alcuna categoria collide con i campi globali (base o comuni). |

---

## 6. Come Compilare ed Eseguire

Dalla cartella radice `ProgettoIngeSoftwareA-1_v1_final`:

### Compilazione:
```powershell
$files = Get-ChildItem -Path "gestione_iniziative_v1\src\main\java" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -cp "lib\kibofplib-1.0.jar" -d "gestione_iniziative_v1\out" $files
```

### Esecuzione dell'Applicazione (`Main`):
```powershell
java -ea -cp "gestione_iniziative_v1\out;lib\kibofplib-1.0.jar" it.unibs.ing.iniziative.Main
```

### Esecuzione della Suite di Test (`TestSuiteCompleta` con asserzioni DBC attive `-ea`):
```powershell
java -ea -cp "gestione_iniziative_v1\out;lib\kibofplib-1.0.jar" it.unibs.ing.iniziative.TestSuiteCompleta
```
