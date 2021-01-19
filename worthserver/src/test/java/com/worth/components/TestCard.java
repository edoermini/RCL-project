package com.worth.components;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Vector;

import static org.junit.Assert.*;

public class TestCard {

    @Test
    public void testGetName() {
        Card c = new Card("test", "test");

        assertEquals("test", c.getName());
    }

    @Test
    public void testGetDescription() {
        Card c = new Card("test", "test");

        assertEquals("test", c.getDescription());
    }

    @Test
    public void testGetHistory() {
        Card c = new Card("test", "test");

        assertEquals(CardState.TODO, c.getHistory().get(0));
        assertNotEquals(CardState.INPROGRESS, c.getHistory().get(0));
    }

    @Test
    public void testUpdateHistory() {
        Card c = new Card("test", "test");
        c.updateHistory(CardState.INPROGRESS);

        List<CardState> history = c.getHistory();

        assertEquals(CardState.TODO, history.get(0));
        assertEquals(CardState.INPROGRESS, history.get(1));
    }

    @Test
    void testGetState() {
        Card c = new Card("test", "test");
        assertEquals(CardState.TODO, c.getState());

        c.updateHistory(CardState.INPROGRESS);
        assertEquals(CardState.INPROGRESS, c.getState());
    }
}
