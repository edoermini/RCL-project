package com.worth.components;

import com.worth.exceptions.card.CardAlreadyExistsException;
import com.worth.exceptions.card.CardNotFoundException;
import com.worth.exceptions.card.IllegalCardMovementException;
import com.worth.exceptions.user.UserAlreadyMemberException;

import java.util.*;

public class Project {
    private final String name;
    private final HashMap<String, Card> todo;
    private final HashMap<String, Card> inProgress;
    private final HashMap<String, Card> toBeRevised;
    private final HashMap<String, Card> done;
    private final ArrayList<String> members;
    private final String chatIp;

    public Project(String name, String chatIp) {
        this.name = name;
        this.chatIp = chatIp;
        this.members = new ArrayList<>();
        this.todo = new HashMap<>();
        this.inProgress = new HashMap<>();
        this.toBeRevised = new HashMap<>();
        this.done = new HashMap<>();
    }

    /**
     * Returns the project's name
     *
     * @return the project's name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the string representing the chat ip
     *
     * @return the string representing the chat ip
     */
    public String getChatIp() {
        return this.chatIp;
    }

    /**
     * Returns the list of all project's members
     *
     * @return the list of all project's members
     */
    public List<String> getMembers() {
        return new Vector<>(this.members);
    }

    /**
     * Adds card with given name and description to project
     * and initializes it's state in TODO
     *
     * @param cardName the card's name
     * @param description the card's description
     * @throws CardAlreadyExistsException if card already exists in the project
     */
    public void addCard(String cardName, String description) throws CardAlreadyExistsException {

        Card c = new Card(cardName, description);

        // checking if card is already in project
        if (this.todo.containsKey(cardName) ||
                this.inProgress.containsKey(cardName) ||
                this.toBeRevised.containsKey(cardName) ||
                this.done.containsKey(cardName))
        {
            throw new CardAlreadyExistsException("Card " + cardName + " already exists in project " + this.name);
        }

        this.todo.put(cardName, c);
    }

    /**
     * Returns the deep copy of the card with given name if exists,
     * throws an exception if card doesn't exist
     *
     * @param cardName the card's name
     * @return the Card with given name
     * @throws CardNotFoundException if card with given name doesn't exist in project
     */
    public Card getCard(String cardName) throws CardNotFoundException {


        if (this.todo.containsKey(cardName)) {
            return new Card(this.todo.get(cardName));

        } else if (this.inProgress.containsKey(cardName)) {
            return new Card(this.inProgress.get(cardName));

        } else if (this.toBeRevised.containsKey(cardName)) {
            return new Card(this.toBeRevised.get(cardName));

        } else if (this.done.containsKey(cardName)) {
            return new Card(this.done.get(cardName));
        }

        throw new CardNotFoundException("Card " + cardName + " doesn't exist in project " + this.name);
    }

    /**
     * Returns the list of all the cards in the project
     *
     * @return the list of all the cards in the project
     */
    public List<String> getCards() {
        ArrayList<String> cards = new ArrayList<>();

        for (Card c : this.todo.values()) {
            cards.add(c.getName());
        }

        for (Card c : this.inProgress.values()) {
            cards.add(c.getName());
        }

        for (Card c : this.toBeRevised.values()) {
            cards.add(c.getName());
        }

        for (Card c : this.done.values()) {
            cards.add(c.getName());
        }

        return cards;
    }

    /**
     * Returns the history of a card
     *
     * @param cardName the card's name
     * @return the card's history if card exists, null otherwise
     */
    public List<CardState> getCardHistory(String cardName) {

        if (this.todo.containsKey(cardName)) {
            return this.todo.get(cardName).getHistory();

        } else if (this.inProgress.containsKey(cardName)) {
            return this.inProgress.get(cardName).getHistory();

        } else if (this.toBeRevised.containsKey(cardName)) {
            return this.toBeRevised.get(cardName).getHistory();

        } else if (this.done.containsKey(cardName)) {
            return this.done.get(cardName).getHistory();
        }

        return null;
    }

    /**
     * Returns the current card's state
     *
     * @param cardName the card's name
     * @return the current card's state if card exists, null otherwise
     */
    public CardState getCardState(String cardName) {

        if (this.todo.containsKey(cardName)) {
            return CardState.TODO;

        } else if (this.inProgress.containsKey(cardName)) {
            return CardState.INPROGRESS;

        } else if (this.toBeRevised.containsKey(cardName)) {
            return CardState.TOBEREVISED;

        } else if (this.done.containsKey(cardName)) {
            return CardState.DONE;
        }

        return null;
    }

    /**
     * Moves given card to given position, updating it's history
     *
     * @param cardName the card's name to move
     * @param dst the destination state
     * @throws IllegalCardMovementException if movement in given destination is not permitted
     * @throws CardNotFoundException if card with given name doesn't exist in project
     */
    public void moveCard(String cardName, CardState dst) throws IllegalCardMovementException, CardNotFoundException {

        CardState state = this.getCardState(cardName);

        if (state == null) {
            throw new CardNotFoundException("Card " + cardName + "doesn't exist in project " + this.name);
        }


        switch (state) {

            case TODO:
                // card to move is in todo list

                if (dst == CardState.INPROGRESS) {
                    // todo --> inProgress
                    this.move(cardName, this.todo, this.inProgress, CardState.INPROGRESS);

                } else {
                    throw new IllegalCardMovementException("Can't move a card from TODO to " + dst);
                }
                break;

            case INPROGRESS:
                // card to move is in inProgress list

                if (dst == CardState.TOBEREVISED) {
                    // inProgress --> toBeRevised
                    this.move(cardName, this.inProgress, this.toBeRevised, CardState.TOBEREVISED);

                } else if (dst == CardState.DONE) {
                    // inProgress --> done
                    this.move(cardName, this.inProgress, this.done, CardState.DONE);

                } else {
                    throw new IllegalCardMovementException("Can't move a card from INPROGRESS to " + dst);
                }
                break;

            case TOBEREVISED:
                // card to move is in toBeRevised list

                if (dst == CardState.DONE) {
                    // toBeRevised --> done
                    this.move(cardName, this.toBeRevised, this.done, CardState.DONE);

                } else if (dst == CardState.INPROGRESS) {
                    // toBeRevised --> inProgress
                    this.move(cardName, this.toBeRevised, this.inProgress, CardState.INPROGRESS);

                } else {
                    throw new IllegalCardMovementException("Can't move a card from TOBEREVISED to " + dst);
                }
                break;

            case DONE:
                // card to move is in done list
                throw new IllegalCardMovementException("Can't move a card from DONE to " + dst);
        }
    }

    /**
     * Sets given user as a member of the project
     *
     * @param user the user to set as member of the project
     * @throws UserAlreadyMemberException if given user is already a member
     */
    public void addMember(String user) throws UserAlreadyMemberException {

        if (this.isMember(user)) {
            throw new UserAlreadyMemberException("User " + user + " is already a member of project " + this.name);
        }

        this.members.add(user);
    }

    /**
     * Checks if given user is a member of the project
     *
     * @param user the user
     * @return true if given user is a member, false otherwise
     */
    public boolean isMember(String user) {

        for (String member : this.members) {
            if (member.equals(user)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if all cards are in done list or there are no cards in projects
     *
     * @return true if all cards are in done list or there are no cards in project, false otherwise
     */
    public boolean isFinished() {

        return ((this.todo.size() == 0) &&
                (this.inProgress.size() == 0) &&
                (this.toBeRevised.size()== 0));
    }

    private void move(String cardName, HashMap<String, Card> src, HashMap<String, Card> dst, CardState dstState) {

        dst.put(cardName, src.get(cardName));
        src.remove(cardName);

        // updating card states history
        dst.get(cardName).updateHistory(dstState);
    }

    // methods used only by Reader class

    public void addCards(List<Card> cards) {
        for (Card c : cards) {

            switch (c.getState()) {
                case TODO:
                    this.todo.put(c.getName(), c);
                    break;

                case INPROGRESS:
                    this.inProgress.put(c.getName(), c);
                    break;

                case TOBEREVISED:
                    this.toBeRevised.put(c.getName(), c);
                    break;

                case DONE:
                    this.done.put(c.getName(), c);
                    break;
            }
        }
    }

    public void addMembers(List<String> members) {
        for (String user : members) {
            this.members.add(user);
        }
    }
}
