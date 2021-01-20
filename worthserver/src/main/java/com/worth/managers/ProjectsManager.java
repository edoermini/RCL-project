package com.worth.managers;

import com.worth.components.Card;
import com.worth.components.CardState;
import com.worth.components.Project;
import com.worth.exceptions.*;
import com.worth.exceptions.card.CardAlreadyExistsException;
import com.worth.exceptions.card.CardNotFoundException;
import com.worth.exceptions.card.IllegalCardMovementException;
import com.worth.exceptions.card.InvalidCardStateException;
import com.worth.exceptions.project.ProjectAlreadyExistsException;
import com.worth.exceptions.project.ProjectNotFoundException;
import com.worth.exceptions.user.UserAlreadyMemberException;
import com.worth.io.Writer;
import com.worth.rmi.callback.CallbackServer;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class ProjectsManager {
    private final HashMap<String, Project> projects;
    private final ArrayList<String> usedIPs;
    private final CallbackServer callback;

    public ProjectsManager(List<Project> projects, CallbackServer callback) {

        this.projects = new HashMap<>();
        this.usedIPs = new ArrayList<>();
        this.callback = callback;

        for (Project p : projects) {
            this.projects.put(p.getName(), p);
        }
    }

    /**
     * Adds a project with given name and member
     *
     * @param projectName the project's name
     * @param user the project's member
     * @throws ProjectAlreadyExistsException if a project with the same name already exists
     */
    public synchronized void createProject(String projectName, String user) throws ProjectAlreadyExistsException {

        String ip = generateIP();
        Project p = new Project(projectName, ip);

        try {
            p.addMember(user);
        } catch (UserAlreadyMemberException e) {
            // if user is already a member project already exists,
            // so ProjectAlreadyExistsException exception will be thrown
        }

        this.usedIPs.add(ip);

        /*
         * putIfAbsent returns null if there was no mapping for the key,
         * the value associated to the key if there was a mapping
         */
        if (this.projects.putIfAbsent(projectName, p) != null) {
            throw new ProjectAlreadyExistsException("Project " + projectName + " already exists");
        }

        // notifies to the user the project's ip
        this.callback.notifyProjectIp(user, p.getName(), p.getChatIp());

        Writer.addProject(p);
    }

    /**
     * Removes given project
     *
     * @param projectName the project to remove
     * @param user the user requesting the remove
     * @throws ProjectNotFoundException if there isn't a project with given name
     * @throws OperationNotAllowedException if the project doesn't have given user as member
     */
    public synchronized void cancelProject(String projectName, String user)
            throws ProjectNotFoundException, OperationNotAllowedException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        if (!p.isFinished()) {
            throw new OperationNotAllowedException("Project " + projectName + " has some cards not in done list");
        }

        this.projects.remove(projectName);

        // removing project's ip from used ips
        String ip = p.getChatIp();
        this.usedIPs.remove(ip);

        // removing project from filesystem
        Writer.delProject(p);

        // sending message to into project's multicast chat
        this.sendMsg(user, "deleted project", p.getChatIp());

        // notifies to the user the project's ip
        this.callback.notifyDeletedProject(p.getMembers(), p.getName());
    }

    /**
     * Adds a card to a project
     *
     * @param projectName the project's name
     * @param cardName the card's name
     * @param description the card's description
     * @param user the user requesting the action
     * @throws ProjectNotFoundException if there isn't a project with given name
     * @throws OperationNotAllowedException if the project doesn't have given user as member
     * @throws CardAlreadyExistsException if cards already exists in the project
     */
    public synchronized void addCard(String projectName, String cardName, String description, String user)
            throws ProjectNotFoundException, OperationNotAllowedException, CardAlreadyExistsException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        p.addCard(cardName, description);

        // adding card to filesystem
        try {
            Writer.updateCard(p, p.getCard(cardName));
        } catch (CardNotFoundException e) {
            e.printStackTrace();
        }

        // sending message to into project's multicast chat
        this.sendMsg(user, "added card " + cardName, p.getChatIp());
    }

    /**
     * Searches the specific card that belongs to a specific project
     *
     * @param projectName the project that contains the card
     * @param cardName the card's name
     * @param user the user requesting the action
     * @return the card with given name that belongs to given project
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws OperationNotAllowedException if user is not a project's member
     * @throws CardNotFoundException if card with given name doesn't exist in project
     */
    public synchronized Card showCard(String projectName, String cardName, String user)
            throws ProjectNotFoundException, OperationNotAllowedException, CardNotFoundException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        return p.getCard(cardName);
    }

    /**
     * Returns all the cards relative to a project
     *
     * @param projectName the project from which to get the cards
     * @param user the user requesting the action
     * @return the list of cards relative to given project
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws OperationNotAllowedException if user is not a project's member
     */
    public synchronized List<String> showCards(String projectName, String user)
            throws ProjectNotFoundException, OperationNotAllowedException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        return p.getCards();
    }

    /**
     * Moves a card to given position, updating it's history
     *
     * @param projectName the project's name
     * @param cardName the card to move
     * @param dst the movement's destination
     * @param user the user requesting the action
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws OperationNotAllowedException if user is not a project's member
     * @throws CardNotFoundException if card with given name doesn't exist in given project
     * @throws IllegalCardMovementException if movement in given destination is not permitted
     * @throws InvalidCardStateException if destination card's state is invalid (= null)
     */
    public synchronized void moveCard(String projectName, String cardName, CardState dst, String user)
            throws ProjectNotFoundException, OperationNotAllowedException, CardNotFoundException, IllegalCardMovementException, InvalidCardStateException {

        if (dst == null) {
            throw new InvalidCardStateException("Invalid card state");
        }

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        p.moveCard(cardName, dst);

        // adding card's movement to filesystem
        try {
            Writer.updateCard(p, p.getCard(cardName));
        } catch (CardNotFoundException e) {
            e.printStackTrace();
        }

        // sending message to into project's multicast chat
        this.sendMsg(user, "moved card " + cardName + " into " + dst, p.getChatIp());

    }

    /**
     * Returns the history of a project's card
     *
     * @param projectName the project's name
     * @param cardName the card's name
     * @param user the user requesting the action
     * @return the history of a project's card if card exists in the project, null otherwise
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws OperationNotAllowedException if user is not a project's member
     */
    public synchronized List<CardState> getCardHistory(String projectName, String cardName, String user)
            throws ProjectNotFoundException, OperationNotAllowedException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        return p.getCardHistory(cardName);
    }

    /**
     * Adds a member to a project
     *
     * @param projectName the project's name
     * @param newMember the member to add
     * @param user the user requesting the action
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws OperationNotAllowedException if user is not a project's member
     * @throws UserAlreadyMemberException if the user to add is already a member
     */
    public synchronized void addMember(String projectName, String newMember, String user)
            throws ProjectNotFoundException, OperationNotAllowedException, UserAlreadyMemberException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        p.addMember(newMember);

        // notifies to the new user the project's ip
        this.callback.notifyProjectIp(newMember, p.getName(), p.getChatIp());

        // adding project's member in filesystem
        Writer.updateMembers(p);

        // sending message to into project's multicast chat
        this.sendMsg(user, "added " + newMember + " as member", p.getChatIp());
    }

    /**
     * Returns the list of the members of a project
     *
     * @param projectName the project's name
     * @param user the user requesting the action
     * @return the list of the members belonging to the project
     * @throws ProjectNotFoundException if project doesn't exist
     * @throws OperationNotAllowedException if user is not a project's member
     */
    public synchronized List<String> showMembers(String projectName, String user)
            throws ProjectNotFoundException, OperationNotAllowedException {

        if (!this.projects.containsKey(projectName)) {
            throw new ProjectNotFoundException("Project " + projectName + " doesn't exist");
        }

        Project p = this.projects.get(projectName);

        if (!p.isMember(user)) {
            throw new OperationNotAllowedException("User " + user + " is not a member of project " + projectName);
        }

        return p.getMembers();
    }

    /**
     * Returns the projects list
     *
     * @return the projects list
     */
    public synchronized List<String> listProjects() {

        Set<String> projectsEnum = this.projects.keySet();

        return new ArrayList<>(projectsEnum);
    }

    /**
     * Returns all projects of given user
     *
     * @return all projects of given user
     */
    public synchronized List<Project> listProjectsOf(String user) {

        ArrayList<Project> projects = new ArrayList<>();

        for (Project p : this.projects.values()) {
            if (p.isMember(user)) {
                projects.add(p);
            }
        }

        return projects;
    }

    private void sendMsg(String user, String action, String chatIp) {
        InetAddress ia = null;
        MulticastSocket ms;

        InetSocketAddress dategroup = new InetSocketAddress(chatIp, 6662);

        try {
            ms = new MulticastSocket(6662);
            ms.setReuseAddress(true);
            ms.joinGroup(dategroup, NetworkInterface.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            ia = InetAddress.getByName(chatIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        byte[] msg = ("[WORTH]: " + user + " " + action).getBytes();
        DatagramPacket dp = new DatagramPacket(msg, msg.length, ia, 6662);

        try {
            ms.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ms.close();
    }

    private synchronized String generateIP() {
        Random r = new Random();

        String ip = (r.nextInt((239 - 224) + 1) + 224) + "." +
                r.nextInt(256) + "." +
                r.nextInt(256) + "." +
                r.nextInt(256);

        while (this.usedIPs.contains(ip)) {
            ip = (r.nextInt((239 - 224) + 1) + 224) + "." +
                    r.nextInt(256) + "." +
                    r.nextInt(256) + "." +
                    r.nextInt(256);
        }

        return ip;
    }

}
