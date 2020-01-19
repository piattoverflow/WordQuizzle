package server.gamelogic;

import server.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Dizionario {
    private ArrayList<String> dizionario;
    private static Dizionario instance;

    public ArrayList<String> getDizionario() {
        return dizionario;
    }

    private Dizionario() {
        this.dizionario = new ArrayList<>();
        loadDictionaryFromFile(Settings.FILE_DIZIONARIO);
    }

    public static synchronized Dizionario getInstance() {
        if (instance == null) instance = new Dizionario();
        return instance;
    }

    private void loadDictionaryFromFile(String path) {//Apre in lettura il file del dizionario (se c'è)
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                this.dizionario.add(currentLine);//Aggiungi al dizionario la riga letta
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<HashMap<String, String>> getNwordsFromDictionary(int n) {
        if (n > this.dizionario.size())
            throw new IllegalArgumentException("Il dizionario ha meno parole di quelle richieste");

        ArrayList<HashMap<String, String>> nParoleEstratte = new ArrayList<>();
        Collections.shuffle(this.dizionario); //Serve per randomizzare le parole visto che estraggo da 0 a N

        for (int i = 0; i < n; i++) {
            HashMap<String, String> ita_eng = new HashMap<>();
            ita_eng.put(dizionario.get(i), null);
            nParoleEstratte.add(ita_eng); //aggiungo le N paroli alla nuova Lista di coppie <Parola, Traduzione>
        }

        return nParoleEstratte;
    }
}