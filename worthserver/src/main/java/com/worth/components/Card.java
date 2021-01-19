package com.worth.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Vector;

@JsonIgnoreProperties({"state"})
public class Card {

    private String name;
    private String description;
    private Vector<CardState> history;

    /**
     * Creates a card with given name and description
     *
     * @param name the card's name
     * @param description the card's description
     */
    public Card(String name, String description) {
        this.name = name;
        this.description = description;
        this.history = new Vector<>();

        this.history.add(CardState.TODO);
    }

    /**
     * Creates a card with given name and void description
     *
     * @param name the card's name
     */
    public Card(String name) {
        this.name = name;
        this.description = "";
        this.history = new Vector<>();

        this.history.add(CardState.TODO);
    }

    public Card() {
        this.history = new Vector<>();
    }

    /**
     * Makes a deep copy of given card
     *
     * @param c the card to copy
     */
    public Card(Card c) {
        this.name = c.name;
        this.description = c.description;
        this.history = new Vector<>(c.history);
    }

    /**
     * Returns the card's name
     *
     * @return the card's name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the card's description
     *
     * @return the card's description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the card's sates history
     *
     * @return the card's sates history
     */
    public List<CardState> getHistory() {
        return new Vector<>(this.history);
    }

    /**
     * Sets card's name
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets card's description
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets card's history
     *
     * @param history the history
     */
    public void setHistory(Vector<CardState> history) {
        this.history = new Vector<>(history);
    }

    /**
     * Adds a new card's state in states history
     *
     * @param state the new card's state
     */
    public void updateHistory(CardState state) {
        this.history.add(state);
    }

    /**
     * Returns the current card state
     *
     * @return the current card state
     */
    public CardState getState() {
        return this.history.get(this.history.size()-1);
    }
}
