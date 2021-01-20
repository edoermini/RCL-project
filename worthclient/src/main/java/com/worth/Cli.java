package com.worth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worth.chat.Chat;
import com.worth.rmi.callback.CallbackServerInterface;
import com.worth.rmi.callback.ClientEvent;
import com.worth.rmi.callback.ClientEventInterface;
import com.worth.rmi.registration.Registration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Cli {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";

    private final int servicePort;

    // saves users and their status (false=offline,true=online)
    // modified by callback functions
    private final ConcurrentHashMap<String, Boolean> usersList;

    // contains relations (projects, chat)
    // used to send and read messages from projects chats
    // callback functions adds new relations in this data structure
    private final ConcurrentHashMap<String, Chat> chats;

    // thread pool for chat readers threads
    // used by callback function to add or stop threads
    private final ExecutorService threadPool;

    // rmi object for registration
    Registration registration;

    // callback objects
    CallbackServerInterface callback;
    ClientEventInterface callbackStub;
    ClientEventInterface clientCallback;

    public Cli(int servicePort, int registryPort) {
        this.servicePort = servicePort;

        this.usersList = new ConcurrentHashMap<>();
        this.chats = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();

        // getting rmi registry
        Registry reg = null;
        try {
            reg = LocateRegistry.getRegistry(registryPort);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // setting registration stub
        try {
            this.registration = (Registration) reg.lookup("REGISTRATION-SERVICE");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        // setting up callback system
        try {
            this.callback = (CallbackServerInterface) reg.lookup("CALLBACK-SERVICE");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        this.clientCallback = new ClientEvent(usersList, chats, threadPool);

        try {
            this.callbackStub = (ClientEventInterface) UnicastRemoteObject.exportObject(clientCallback, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void runCli() {
        String prompt = "%sworth$ ";
        String command = "";
        String userName = "";
        Scanner sc = new Scanner(System.in);

        String responseStr;
        String[] response;
        int respCode;

        this.printLogo();
        System.out.println(ANSI_YELLOW + "Type help to see available commands" + ANSI_RESET);

        try (
                Socket socket = new Socket("localhost", servicePort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
                ) {

            while (!command.equals("quit")) {

                // printing prompt
                if (userName.equals("")) {
                    System.out.printf(prompt, userName);
                } else {
                    System.out.printf(prompt, userName + "@");
                }

                String cmd = sc.nextLine();

                // splitting input
                String[] splitted = cmd.split(" ");
                command = splitted[0].trim();

                switch (command) {

                    case "login": // LOGIN

                        if (!userName.equals("")) {
                            System.out.println(parseCode(4) + " User logged in, logout before login");
                            break;
                        }

                        if (splitted.length < 3) {
                            System.out.println(parseCode(1) + " You have to specify user and password");
                            break;
                        }

                        // sending to server
                        out.println("0%" + splitted[1] + "%" + splitted[2]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // first element is the response code (integer)
                        respCode = Integer.parseInt(response[0]);

                        // printing response
                        System.out.println(parseCode(respCode) + " " + response[1]);

                        if (respCode == 0) {
                            // success

                            //starting chat threads for each project
                            Map<String, String> projectsChatIPs = new ObjectMapper().readValue(response[2], new TypeReference<Map<String, String>>(){});


                            for (Map.Entry<String, String> project : projectsChatIPs.entrySet()) {
                                Chat cr = new Chat(project.getValue());
                                this.threadPool.execute(cr);

                                // adding project -> chatReader mappings
                                this.chats.put(project.getKey(), cr);
                            }

                            // saving users and status
                            Map<String, Boolean> usersList = new ObjectMapper().readValue(response[3], new TypeReference<Map<String, Boolean>>(){});
                            for (Map.Entry<String, Boolean> user : usersList.entrySet()) {
                                this.usersList.put(user.getKey(), user.getValue());
                            }

                            // registering to callbacks
                            callback.registerForEvents(splitted[1], this.callbackStub);

                            userName = splitted[1];
                        }

                        break;

                    case "logout": // LOGOUT

                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        // sending request
                        out.println("1%" + userName);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        if (respCode == 0) {
                            // success

                            // unregistering to callbacks
                            callback.unregisterForEvents(userName);

                            userName = "";

                            // stopping chat readers threads
                            for (Chat chat : this.chats.values()) {
                                chat.stop();
                            }
                        }

                        // printing response
                        System.out.println(parseCode(respCode) + " " + response[1]);

                        break;


                    case "showps": // LIST-PROJECTS
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        // sending request
                        out.println("2");
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        if (respCode == 0) {
                            // success

                            List<String> projects = new ObjectMapper().readValue(response[1], new TypeReference<List<String>>(){});
                            for (String p : projects) {
                                System.out.println(p);
                            }
                        } else {
                            // printing error
                            System.out.println(parseCode(respCode) + " " + response[1]);
                        }

                        break;


                    case "createp": // CREATE-PROJECT

                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 2) {
                            System.out.println(parseCode(1) + " You have to specify project's name");
                            break;
                        }

                        // sending request
                        out.println("3%" + splitted[1]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        System.out.println(parseCode(respCode) + " " + response[1]);

                        break;

                    case "addm": // ADD-MEMBER
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 3) {
                            System.out.println(parseCode(1) + " You have to specify project's name and new member's name");
                            break;
                        }

                        // sending request
                        out.println("4%" + splitted[1] + "%" + splitted[2]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        System.out.println(parseCode(respCode) + " " + response[1]);

                        break;


                    case "showms": // SHOW-MEMBERS
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 2) {
                            System.out.println(parseCode(1) + " You have to specify project's name");
                            break;
                        }

                        // sending request
                        out.println("5%" + splitted[1]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        if (respCode == 0) {
                            // success

                            List<String> members = new ObjectMapper().readValue(response[1], new TypeReference<List<String>>(){});
                            for (String m : members) {
                                System.out.println(m);
                            }
                        } else {
                            // printing error
                            System.out.println(parseCode(respCode) + " " + response[1]);
                        }

                        break;

                    case "showcs": // SHOW-CARDS
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 2) {
                            System.out.println(parseCode(1) + " You have to specify project's name");
                            break;
                        }

                        // sending request
                        out.println("6%" + splitted[1]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        if (respCode == 0) {
                            // success

                            List<String> cards = new ObjectMapper().readValue(response[1], new TypeReference<List<String>>(){});
                            for (String c : cards) {
                                System.out.println(c);
                            }
                        } else {
                            // printing error
                            System.out.println(parseCode(respCode) + " " + response[1]);
                        }

                        break;

                    case "showc": // SHOW-CARD
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 3) {
                            System.out.println(parseCode(1) + " You have to specify project's name and card's name");
                            break;
                        }

                        // sending request
                        out.println("7%" + splitted[1] + "%" + splitted[2]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        if (respCode == 0) {
                            // success

                            System.out.println("Card's name: " + response[1]);
                            System.out.println("Card's description: " + response[2]);
                            System.out.println("Card's list: " + response[3]);
                        } else {
                            System.out.println(parseCode(respCode) + " " + response[1]);
                        }

                        break;

                    case "addc": // ADD-CARD
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 4) {
                            System.out.println(parseCode(1) + " You have to specify project's name, card's name and card's description");
                            break;
                        }

                        StringBuilder desc = new StringBuilder();

                        for (int i = 3; i < splitted.length; i++) {
                            desc.append(splitted[i]).append(" ");
                        }

                        // sending request
                        out.println("8%" + splitted[1] + "%" + splitted[2] + "%" + desc);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        System.out.println(parseCode(respCode) + " " + response[1]);

                        break;


                    case "movec": // MOVE-CARD
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 4) {
                            System.out.println(parseCode(1) + " You have to specify project's name, card's name and to list");
                            break;
                        }

                        // sending request
                        out.println("9%" + splitted[1] + "%" + splitted[2] + "%" +  splitted[3]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        System.out.println(parseCode(respCode) + " " + response[1]);

                        break;

                    case "getch": // GET-CARD-HISTORY
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 3) {
                            System.out.println(parseCode(1) + " You have to specify project's name and card's name");
                            break;
                        }

                        // sending request
                        out.println("10%" + splitted[1] + "%" + splitted[2]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        if (respCode == 0) {
                            // success

                            List<String> cardHistory = new ObjectMapper().readValue(response[1], new TypeReference<List<String>>(){});
                            for (String c : cardHistory) {
                                System.out.println(c);
                            }
                        } else {
                            // printing error
                            System.out.println(parseCode(respCode) + " " + response[1]);
                        }

                        break;

                    case "delp": // CANCEL-PROJECT
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 2) {
                            System.out.println(parseCode(1) + " You have to specify project's name");
                            break;
                        }

                        System.out.println("Deleting project " + splitted[1] + "...");

                        // sending request
                        out.println("11%" + splitted[1]);
                        out.flush();

                        // reading response
                        responseStr = in.readLine();

                        if (responseStr == null) {
                            // server timeout

                            timeout(userName);
                            command = "quit"; // for exiting from while
                            break;
                        }

                        // splitting response
                        response = responseStr.split("%");

                        // converting response code to int
                        respCode = Integer.parseInt(response[0]);

                        System.out.println(parseCode(respCode) + " " + response[1]);

                        break;

                    case "readchat": // READ-CHAT
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 2) {
                            System.out.println(parseCode(1) + " You have to specify project's name");
                            break;
                        }

                        List<String> messages = this.readChat(splitted[1]);

                        if (messages == null) {
                            // project not found

                            System.out.println(parseCode(5) + " Project " + splitted[1] + " doesn't exist");
                            break;
                        }

                        // printing messages
                        for (String message : messages) {
                            System.out.println(message);
                        }

                        break;

                    case "sendmsg": // SEND-MESSAGE
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        if (splitted.length < 3) {
                            System.out.println(parseCode(1) + " You have to specify project's name and message");
                            break;
                        }

                        StringBuilder msg = new StringBuilder();

                        for (int i = 2; i < splitted.length; i++) {
                            msg.append(splitted[i]).append(" ");
                        }

                        System.out.println(sendChatMsg(splitted[1], msg.toString(), userName));
                        break;

                    case "register": // REGISTER
                        if (!userName.equals("")) {
                            System.out.println(parseCode(4) + " User logged in, log out before registering a new account");
                            break;
                        }

                        if (splitted.length < 3) {
                            System.out.println(parseCode(1) + " You have to specify username and password");
                            break;
                        }

                        String result = this.register(splitted[1], splitted[2]);

                        System.out.println(result);

                        break;

                    case "listus": // LIST-USERS
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        this.listUsers();
                        break;

                    case "listous": // LIST-ONLINE-USERS
                        if (userName.equals("")) {
                            System.out.println(parseCode(4) + " User not logged in");
                            break;
                        }

                        this.listOnlineUsers();
                        break;

                    case "help": // HELP
                        printHelp();
                        break;

                    case "quit":
                        if (!userName.equals("")) {
                            System.out.println(parseCode(4) + " User logged in, log out before quit");
                            command = "";
                            break;
                        }

                        // sending request
                        out.println("quit");
                        out.flush();

                        break;

                    case "":
                        break;

                    default:
                        System.out.println(parseCode(1) + " Unknown operation");
                        break;
                }

            }

            // cleaning
            UnicastRemoteObject.unexportObject(this.clientCallback, true);
            this.threadPool.shutdown();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void printHelp() {
        System.out.println("Users commands:");
        System.out.println("- register [username] [password]        registers user to worth");
        System.out.println("- login [username] [password]           login user to worth");
        System.out.println("- logout                                logout user to worth");
        System.out.println("- listus                                prints all users and their status");
        System.out.println("- listous                               prints only online users");
        System.out.println();

        System.out.println("Projects commands:");
        System.out.println("- showps                                shows user's projects");
        System.out.println("- createp [project]                     creates new project");
        System.out.println("- delp [project]                        deletes project");
        System.out.println("- addm [project] [username]             adds a member to project");
        System.out.println("- showms [project]                      shows all project's members");
        System.out.println();

        System.out.println("Cards commands:");
        System.out.println("- showcs [project]                      shows all project's cards");
        System.out.println("- showc [project] [card]                shows card information (name, description, list)");
        System.out.println("- addc [project] [card] [description]   adds a card to project");
        System.out.println("- movec [project] [card] [list]         moves card into specified list");
        System.out.println("                                            [list] = todo | inprogress | toberevised | done");
        System.out.println("- getch [project] [card]                prints the card's history");
        System.out.println();

        System.out.println("Chat commands:");
        System.out.println("- readchat [project]                    prints received messages from project's chat");
        System.out.println("- sendmsg [project] [message]           sends a message into project's chat");
        System.out.println();

        System.out.println("- quit                                  terminates the client");
        System.out.println("- help                                  prints this message");
    }

    private void printLogo() {
        System.out.println();
        System.out.println("██╗    ██╗ ██████╗ ██████╗ ████████╗██╗  ██╗");
        System.out.println("██║    ██║██╔═══██╗██╔══██╗╚══██╔══╝██║  ██║");
        System.out.println("██║ █╗ ██║██║   ██║██████╔╝   ██║   ███████║");
        System.out.println("██║███╗██║██║   ██║██╔══██╗   ██║   ██╔══██║");
        System.out.println("╚███╔███╔╝╚██████╔╝██║  ██║   ██║   ██║  ██║");
        System.out.println(" ╚══╝╚══╝  ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝");
        System.out.println();
    }

    private String parseCode(int respCode) {
        switch (respCode) {
            case 0:
                return ANSI_GREEN + "SUCCESS:" + ANSI_RESET;
            case 1:
                return ANSI_RED + "SYNTAX ERROR:" + ANSI_RESET;
            case 2:
                return ANSI_RED + "USER ERROR:" + ANSI_RESET;
            case 3:
                return ANSI_RED + "PASSWORD ERROR:" + ANSI_RESET;
            case 4:
                return ANSI_RED + "LOGIN/OUT ERROR:" + ANSI_RESET;
            case 5:
                return ANSI_RED + "PROJECT ERROR:" + ANSI_RESET;
            case 6:
                return ANSI_RED + "PERMISSIONS ERROR:" + ANSI_RESET;
            case 7:
                return ANSI_RED + "CARD ERROR:" + ANSI_RESET;
        }

        return null;
    }

    private String sendChatMsg(String projectName, String message, String user) {
        Chat projChat;

        projChat = chats.get(projectName);

        // project deleted and all messages read
        if (projChat == null) {
            return parseCode(5) + " Project " + projectName + " doesn't exist";
        }

        // prject deleted but there are messages to read
        if (projChat.isDeleted()) {
            return parseCode(5) + " Project " + projectName + " has been deleted";
        }

        projChat.sendChatMsg(user, message);

        return parseCode(0) + " Message sent correctly";
    }

    private List<String> readChat(String projectName) {
        Chat cr;
        List<String> messages;

        cr = this.chats.get(projectName);

        if (cr == null) {
            return null;
        }

        messages = cr.getMessages();

        if (cr.isDeleted()) {
            this.chats.remove(projectName);
        }

        return messages;
    }

    private void listUsers() {
        synchronized (this.usersList) {
            for (Map.Entry<String, Boolean> user : this.usersList.entrySet()) {
                System.out.println(user.getKey() + ": "
                        + (user.getValue() ? ANSI_GREEN + "online" + ANSI_RESET : ANSI_RED + "offline" + ANSI_RESET));
            }
        }
    }

    private void listOnlineUsers() {
        synchronized (this.usersList) {
            for (Map.Entry<String, Boolean> user : this.usersList.entrySet()) {
                if (user.getValue()) {
                    System.out.println(user.getKey());
                }
            }
        }
    }

    private String register(String userName, String password) throws RemoteException {
        String[] response = registration.register(userName, password).split("%");

        // converting response code to int
        int respCode = Integer.parseInt(response[0]);

        return parseCode(respCode) + " " + response[1];
    }

    private void timeout(String userName) {
        System.out.println();
        System.out.println(ANSI_RED + "Server timeout occurred" + ANSI_RESET);

        if (!userName.equals("")) {
            try {
                this.callback.unregisterForEvents(userName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // stopping chat readers threads
        for (Chat chat : this.chats.values()) {
            chat.stop();
        }
    }

}
