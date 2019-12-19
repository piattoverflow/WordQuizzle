package server;

import errori.*;
import server.storage.Storage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class Worker implements Runnable {

    private Socket client;
    private BufferedOutputStream outToClient;
    private Utente socUser;

    public Worker(Socket c){
        this.client = c;
    }
    
    public void run() {
        try{
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outToClient = new BufferedOutputStream(client.getOutputStream());

            String message;

            while((message = inFromClient.readLine()) != null){
                StringTokenizer tokenizedLine = new StringTokenizer(message);
                String pw, nickUtente, nickAmico;
                switch (tokenizedLine.nextToken()){
                    case "LOGIN":
                        nickUtente = tokenizedLine.nextToken();
                        pw = tokenizedLine.nextToken();

                        login(nickUtente, pw);
                        break;
                    case "LOGOUT":
                        nickUtente = tokenizedLine.nextToken();

                        logout(nickUtente);
                        break;
                    case "ADD_FRIEND":
                        nickUtente = tokenizedLine.nextToken();
                        nickAmico = tokenizedLine.nextToken();

                        aggiungi_amico(nickUtente, nickAmico);
                        break;
                    case "LISTA_AMICI":
                        nickUtente = tokenizedLine.nextToken();
                        lista_amici(nickUtente);
                        break;
                    case "SFIDA":
                        //TODO UDP request
                        break;
                    case "MOSTRA_SCORE":
                        nickUtente = tokenizedLine.nextToken();
                        mostra_punteggio(nickUtente);
                        break;
                    case "MOSTRA_CLASSIFICA":
                        nickUtente = tokenizedLine.nextToken();
                        mostra_classifica(nickUtente);
                        break;
                }
            }

            client.close();
            UtentiConnessi.getInstance().setConnected(socUser.getNickname(), false);
            inFromClient.close();
            System.out.println("Client closed connection");
        }catch (IOException ecc){

        }
    }

    public void login(String nickUtente, String password) {
        if(nickUtente == null || nickUtente.length() == 0) throw new IllegalArgumentException();
        if(password == null || password.length() == 0) throw new IllegalArgumentException();
        if(UtentiConnessi.getInstance().isConnected(nickUtente)) throw new UserAlreadyLoggedIn();

        Utente profilo = UtentiConnessi.getInstance().getUser(nickUtente);

        try{
            if(profilo == null) throw  new UserDoesntExists("L'utente inserito non esiste");
            if(!profilo.getPassword().equals(password)) throw new WrongPassword("Password errata");
        }catch (UserDoesntExists e){
            sendResponseToClient(e.getMessage());
            return;
        }catch (WrongPassword e2){
            sendResponseToClient(e2.getMessage());
            return;
        }

        UtentiConnessi.getInstance().setConnected(nickUtente, true);
        this.socUser = profilo;
        sendResponseToClient("Login eseguito con successo");
    }
    
    public void logout(String nickUtente) {
        if(nickUtente == null || nickUtente.length() == 0) throw new IllegalArgumentException();
        if(!UtentiConnessi.getInstance().isConnected(nickUtente)) throw new UserAlreadyLoggedIn();

        UtentiConnessi.getInstance().setConnected(nickUtente, false);
        sendResponseToClient("Logout eseguito con successo");
    }

    public void aggiungi_amico(String nickUtente, String nickAmico) {
        if(nickUtente == null || nickUtente.length() == 0) throw new IllegalArgumentException();
        if(nickAmico == null || nickAmico.length() == 0) throw new IllegalArgumentException("nickAmico arrato");

        UtentiConnessi connectedUSers = UtentiConnessi.getInstance();
        Utente profileRichiedente = connectedUSers.getUser(nickUtente);
        Utente profileAmico = connectedUSers.getUser(nickAmico);

        if(profileRichiedente == null || profileAmico == null) throw new FriendNotFound("Amico non trovato");

        try{
            profileRichiedente.addFriend(profileAmico.getNickname());
        }catch (FriendAlreadyExists e){
            sendResponseToClient(e.getMessage());
            return;
        }catch (IllegalArgumentException e2){
            sendResponseToClient("Nome amico non valido");
            return;
        }

        sendResponseToClient("Amicizia " + nickUtente + "-" + nickAmico + " creata");
    }

    public void lista_amici(String nickUtente) {
        if(nickUtente == null || nickUtente.length() == 0) throw new IllegalArgumentException("nickUtente arrato");

        Utente profilo = UtentiConnessi.getInstance().getUser(nickUtente);
        if(profilo == null) throw new UserDoesntExists("L'utente cercato non esiste");

        ConcurrentHashMap<String,String> listaAmici = profilo.getListaAmici();
        String listaAmiciAsJson = Storage.concurrentMapToJSON(listaAmici);
        sendResponseToClient(listaAmiciAsJson);//TODO checkkare se passare una lista ad un Object lo fa crashare
    }

    public void sfida(String nickUtente, String nickAmico) {

    }
    
    public void mostra_punteggio(String nickUtente) {
        if(nickUtente == null || nickUtente.length() == 0) throw new IllegalArgumentException("nickUtente arrato");

        Utente profilo = UtentiConnessi.getInstance().getUser(nickUtente);
        if(profilo == null) throw new UserDoesntExists("L'utente cercato non esiste");

        sendResponseToClient("Punteggio: " + profilo.getPunteggioTotale());
    }
    
    public void mostra_classifica(String nickUtente) {
        if(nickUtente == null || nickUtente.length() == 0) throw new IllegalArgumentException("nickUtente arrato");

        Utente profilo = UtentiConnessi.getInstance().getUser(nickUtente);
        if(profilo == null) throw new UserDoesntExists("L'utente cercato non esiste");


    }

    private void sendResponseToClient(String testo){
        if(testo == null) throw new IllegalArgumentException();
        try{
            outToClient.write(testo.getBytes(StandardCharsets.UTF_8), 0, testo.length());
            outToClient.flush();
        }catch (IOError | IOException ecc){
            ecc.printStackTrace();
        }
    }
}