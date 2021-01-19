package com.worth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worth.components.Card;
import com.worth.components.CardState;
import com.worth.components.Project;
import com.worth.exceptions.OperationNotAllowedException;
import com.worth.exceptions.WrongPasswordException;
import com.worth.exceptions.card.CardAlreadyExistsException;
import com.worth.exceptions.card.CardNotFoundException;
import com.worth.exceptions.card.IllegalCardMovementException;
import com.worth.exceptions.card.InvalidCardStateException;
import com.worth.exceptions.project.ProjectAlreadyExistsException;
import com.worth.exceptions.project.ProjectNotFoundException;
import com.worth.exceptions.user.UserAlreadyLoggedInException;
import com.worth.exceptions.user.UserAlreadyLoggedOutException;
import com.worth.exceptions.user.UserAlreadyMemberException;
import com.worth.exceptions.user.UserNotFoundException;
import com.worth.managers.ProjectsManager;
import com.worth.managers.UsersManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerThread implements Runnable {
    private final ProjectsManager projects;
    private final UsersManager users;
    private String userName;
    private final Socket socket;


    public ServerThread(ProjectsManager projects, UsersManager users, Socket socket) {
        this.projects = projects;
        this.users = users;
        this.socket = socket;
        this.userName = null;

        try {
            // 5 minutes of timeout
            // if the client is inactive for more than
            // 5 minutes the connection will be closed
            // and user will be logged out
            this.socket.setSoTimeout(5 * 60 * 1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try (
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter outToClient = new PrintWriter(socket.getOutputStream())
        ){

            String command;

            while(true) {

                try {
                    command = inFromClient.readLine();
                } catch (SocketTimeoutException e) {
                    // passed 5 minutes waiting for user input
                    // closing safely

                    if (userName != null) {
                        this.users.logout(userName);
                    }

                    break;
                }

                // client quit
                if (command.equals("quit")) {
                    break;
                }

                String response = processRequest(command);
                System.out.println(response);
                outToClient.println(response);
                outToClient.flush();
            }

            this.socket.close();

        } catch (IOException | UserAlreadyLoggedOutException | UserNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String processRequest(String command) throws JsonProcessingException {
        String[] splittedCommand = command.split("%");
        int op;

        try {
            op = Integer.parseInt(splittedCommand[0]);
        } catch (NumberFormatException e) {
            return "1%Unknown operation";
        }

        switch (op) {

            case 0: // LOGIN

                try {
                    this.users.login(
                            splittedCommand[1], // user's username
                            splittedCommand[2]); // user's password
                } catch (UserAlreadyLoggedInException e) {
                    return "4%" + e.getMessage();
                } catch (UserNotFoundException e) {
                    return "2%" + e.getMessage();
                } catch (WrongPasswordException e) {
                    return "3%" + e.getMessage();
                }

                this.userName = splittedCommand[1];

                // creating relations (project's name, project's ip) to send to the client
                HashMap<String, String> projectsChatIPs = new HashMap<>();

                // listing projects chat ip
                for (Project p : projects.listProjectsOf(this.userName)) {
                    projectsChatIPs.put(p.getName(), p.getChatIp());
                }

                // creating relations (user's name, status) to send to the client
                Map<String, Boolean> usersList = users.getUsersList();

                return "0%Logged in successfully%" +
                        new ObjectMapper().writeValueAsString(projectsChatIPs) + "%" +
                        new ObjectMapper().writeValueAsString(usersList);

            case 1: // LOGOUT

                try {
                    this.users.logout(
                            splittedCommand[1]); // user's username
                    this.userName = null;
                } catch (UserNotFoundException e) {
                    return "2%" + e.getMessage();
                } catch (UserAlreadyLoggedOutException e) {
                    return "4%" + e.getMessage();
                }

                return "0%Logged out successfully";

            case 2: // LIST-PROJECTS

                List<Project> projectsList = projects.listProjectsOf(this.userName);
                ArrayList<String> projectsName = new ArrayList<>();

                for (Project p : projectsList) {
                    projectsName.add(p.getName());
                }

                return "0%" + new ObjectMapper().writeValueAsString(projectsName);

            case 3: // CREATE-PROJECT

                try {
                    this.projects.createProject(
                            splittedCommand[1], // project's name
                            this.userName);
                } catch (ProjectAlreadyExistsException e) {
                    return "5%" + e.getMessage();
                }

                return "0%Project successfully created";

            case 4: // ADD-MEMBER

                if (!this.users.exists(splittedCommand[2])) {
                    return "2%User " + splittedCommand[2] + " doesn't exist";
                }

                try {
                    this.projects.addMember(
                            splittedCommand[1], // project's name
                            splittedCommand[2], // new member's name
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                } catch (UserAlreadyMemberException e) {
                    return "2%" + e.getMessage();
                }

                return "0%User successfully added";

            case 5: // SHOW-MEMBERS

                List<String> membersList;

                try {
                    membersList = this.projects.showMembers(
                            splittedCommand[1], // project's name
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                }

                return "0%" + new ObjectMapper().writeValueAsString(membersList);

            case 6: // SHOW-CARDS

                List<String> cardsList;

                try {
                    cardsList = this.projects.showCards(
                            splittedCommand[1], // project's name
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                }

                return "0%" + new ObjectMapper().writeValueAsString(cardsList);

            case 7: // SHOW-CARD

                Card card;

                try {
                    card = this.projects.showCard(
                            splittedCommand[1], // project's name
                            splittedCommand[2], // card's name
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                } catch (CardNotFoundException e) {
                    return "7%" + e.getMessage();
                }

                return "0%" + card.getName() + "%" + card.getDescription() + "%" + card.getState();

            case 8: // ADD-CARD

                try {
                    this.projects.addCard(
                            splittedCommand[1], // project's name
                            splittedCommand[2], // card's name
                            splittedCommand[3], // card's description
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                } catch (CardAlreadyExistsException e) {
                    return "7%" + e.getMessage();
                }

                return "0%Card successfully added";

            case 9: // MOVE-CARD

                try {
                    this.projects.moveCard(
                            splittedCommand[1], // project's name
                            splittedCommand[2], // card's name
                            CardState.fromString(splittedCommand[3]), // to list
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                } catch (CardNotFoundException | IllegalCardMovementException | InvalidCardStateException e) {
                    return "7%" + e.getMessage();
                }

                return "0%Card successfully moved";

            case 10: // GET-CARD-HISTORY

                List<CardState> history;

                try {
                    history = this.projects.getCardHistory(
                            splittedCommand[1], // project's name
                            splittedCommand[2], // card's name
                            this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                }

                if (history == null) {
                    return "7%Card " + splittedCommand[2] + " doesn't exist in project " + splittedCommand[1];
                }

                return "0%" + new ObjectMapper().writeValueAsString(history);

            case 11: // CANCEL-PROJECT

                try {
                    this.projects.cancelProject(splittedCommand[1], this.userName);
                } catch (ProjectNotFoundException e) {
                    return "5%" + e.getMessage();
                } catch (OperationNotAllowedException e) {
                    return "6%" + e.getMessage();
                }

                return "0%project successfully deleted";

            default:
                return "1%Unknown operation";
        }
    }
}
