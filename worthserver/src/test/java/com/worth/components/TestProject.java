package com.worth.components;

import com.worth.exceptions.card.CardAlreadyExistsException;
import com.worth.exceptions.card.CardNotFoundException;
import com.worth.exceptions.card.IllegalCardMovementException;
import com.worth.exceptions.user.UserAlreadyMemberException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static org.junit.Assert.*;

public class TestProject {

    @Test
    public void testGetName() {
        Project p = new Project("project", "0.0.0.0");

        assertEquals("project", p.getName());
        assertNotEquals("project1", p.getName());
    }

    @Test
    public void testGetChatIp() {
        Project p = new Project("project", "0.0.0.0");

        assertEquals("0.0.0.0", p.getChatIp());
        assertNotEquals("0.0.0.1", p.getChatIp());
    }

    @Test
    public void testGetMembers() throws UserAlreadyMemberException {
        User u = new User("test", "test");
        Project p = new Project("project","0.0.0.0");
        p.addMember(u.getUserName());

        assertEquals(u.getUserName(), p.getMembers().get(0));
    }

    @Test
    public void testAddCard() throws CardAlreadyExistsException, CardNotFoundException {
        String cardName = "card1";
        String cardDesc = "test";

        Project p = new Project("project", "0.0.0.0");
        p.addCard(cardName, cardDesc);

        assertEquals(cardName, p.getCard(cardName).getName());
        assertEquals(cardDesc, p.getCard(cardName).getDescription());

        Assertions.assertThrows(CardAlreadyExistsException.class, () -> {
            p.addCard(cardName, cardDesc);
        });
    }

    @Test
    public void testGetCard() throws CardAlreadyExistsException, CardNotFoundException {

        Project p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.addCard("card2", "test");

        assertEquals("card1", p.getCard("card1").getName());
        assertEquals("card2", p.getCard("card2").getName());

        Assertions.assertThrows(CardNotFoundException.class, () -> {
            p.getCard("card3");
        });
    }

    @Test
    public void testGetCards() throws CardAlreadyExistsException, CardNotFoundException, IllegalCardMovementException {
        Project p = new Project("project", "0.0.0.0");

        p.addCard("card1", "test");
        p.addCard("card2", "test");
        p.addCard("card3", "test");
        p.addCard("card4", "test");

        p.moveCard("card2", CardState.INPROGRESS);

        p.moveCard("card3", CardState.INPROGRESS);
        p.moveCard("card3", CardState.TOBEREVISED);

        List<String> cards = p.getCards();

        assertEquals("card1", cards.get(0));
        assertEquals("card4", cards.get(1));
        assertEquals("card2", cards.get(2));
        assertEquals("card3", cards.get(3));
    }

    @Test
    public void testGetCardHistory() throws CardAlreadyExistsException, CardNotFoundException, IllegalCardMovementException{
        Project p = new Project("project", "0.0.0.0");
        p.addCard("card", "test");

        p.moveCard("card", CardState.INPROGRESS);
        p.moveCard("card", CardState.DONE);

        List<CardState> ch = p.getCardHistory("card");

        assertEquals(CardState.TODO, ch.get(0));
        assertEquals(CardState.INPROGRESS, ch.get(1));
        assertEquals(CardState.DONE, ch.get(2));
        assertEquals(3, ch.size());

        assertEquals(null, p.getCardHistory("card1"));

    }

    @Test
    public void testGetCardState() throws CardAlreadyExistsException{
        Project p = new Project("project", "0.0.0.0");
        p.addCard("card", "test");

        assertEquals(CardState.TODO, p.getCardState("card"));
        assertEquals(null, p.getCardState("card1"));
    }

    @Test
    public void moveCard() throws CardAlreadyExistsException, CardNotFoundException, IllegalCardMovementException {
        Project p;




        /* Movement from TODO list testing */


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");


        //testing todo -> inprogress movement, expecting success
        p.moveCard("card1", CardState.INPROGRESS);
        assertEquals(CardState.INPROGRESS, p.getCard("card1").getHistory().get(1));
        assertEquals(CardState.INPROGRESS, p.getCardState("card1"));


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        Project todoTestP = p;


        // testing todo -> todo movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            todoTestP.moveCard("card1", CardState.TODO);
        });
        assertEquals(CardState.TODO, p.getCardState("card1"));


        // testing todo -> toberevised movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            todoTestP.moveCard("card1", CardState.TOBEREVISED);
        });
        assertEquals(CardState.TODO, p.getCardState("card1"));


        // testing todo -> done movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            todoTestP.moveCard("card1", CardState.DONE);
        });
        assertEquals(CardState.TODO, p.getCardState("card1"));




        /* Movement from INPROGRESS list testing */


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);


        // testing inprogress -> toberevised movement, expecting success
        p.moveCard("card1", CardState.TOBEREVISED);
        assertEquals(CardState.TOBEREVISED, p.getCard("card1").getHistory().get(2));
        assertEquals(CardState.TOBEREVISED, p.getCardState("card1"));


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);


        // testing inprogress -> done movement, expecting success
        p.moveCard("card1", CardState.DONE);
        assertEquals(CardState.DONE, p.getCard("card1").getHistory().get(2));
        assertEquals(CardState.DONE, p.getCardState("card1"));


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);
        Project inProgressTestP = p;


        // testing inprogress -> todo movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            inProgressTestP.moveCard("card1", CardState.TODO);
        });
        assertEquals(CardState.INPROGRESS, p.getCardState("card1"));


        // testing inprogress -> inprogress movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            inProgressTestP.moveCard("card1", CardState.INPROGRESS);
        });
        assertEquals(CardState.INPROGRESS, p.getCardState("card1"));




        /* Movement from TOBEREVISED list testing */


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);
        p.moveCard("card1", CardState.TOBEREVISED);


        // testing toberevised -> done movement, expecting success
        p.moveCard("card1", CardState.DONE);
        assertEquals(CardState.DONE, p.getCard("card1").getHistory().get(3));
        assertEquals(CardState.DONE, p.getCardState("card1"));


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);
        p.moveCard("card1", CardState.TOBEREVISED);


        // testing toberevised -> inprogress movement, expecting success
        p.moveCard("card1", CardState.INPROGRESS);
        assertEquals(CardState.INPROGRESS, p.getCard("card1").getHistory().get(3));
        assertEquals(CardState.INPROGRESS, p.getCardState("card1"));


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);
        p.moveCard("card1", CardState.TOBEREVISED);
        Project toBeRevisedTestP = p;


        // testing toberevised -> todo movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            toBeRevisedTestP.moveCard("card1", CardState.TODO);
        });
        assertEquals(CardState.TOBEREVISED, p.getCardState("card1"));


        // testing toberevised -> toberevised movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            toBeRevisedTestP.moveCard("card1", CardState.TOBEREVISED);
        });
        assertEquals(CardState.TOBEREVISED, p.getCardState("card1"));




        /* Movement from DONE list testing */


        // initializing project p
        p = new Project("project", "0.0.0.0");
        p.addCard("card1", "test");
        p.moveCard("card1", CardState.INPROGRESS);
        p.moveCard("card1", CardState.TOBEREVISED);
        p.moveCard("card1", CardState.DONE);
        Project doneTestP = p;


        // testing done -> todo movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            doneTestP.moveCard("card1", CardState.TODO);
        });
        assertEquals(CardState.DONE, p.getCardState("card1"));


        // testing done -> inprogress movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            doneTestP.moveCard("card1", CardState.INPROGRESS);
        });
        assertEquals(CardState.DONE, p.getCardState("card1"));


        // testing done -> toberevised movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            doneTestP.moveCard("card1", CardState.TOBEREVISED);
        });
        assertEquals(CardState.DONE, p.getCardState("card1"));


        // testing done -> done movement, expecting error
        Assertions.assertThrows(IllegalCardMovementException.class, () -> {
            doneTestP.moveCard("card1", CardState.DONE);
        });
        assertEquals(CardState.DONE, p.getCardState("card1"));
    }

    @Test
    public void testAddMember() throws UserAlreadyMemberException {
        User u = new User("test", "test");

        Project p = new Project("project", "0.0.0.0");
        p.addMember(u.getUserName());

        assertEquals(u.getUserName(), p.getMembers().get(0));

        Assertions.assertThrows(UserAlreadyMemberException.class, () -> {
            p.addMember(u.getUserName());
        });
    }

    @Test
    public void testIsMember() throws UserAlreadyMemberException {
        User u1 = new User("test", "test");
        User u2 = new User("test1", "test1");

        Project p = new Project("project", "0.0.0.0");
        p.addMember(u1.getUserName());

        assertTrue(p.isMember(u1.getUserName()));
        assertFalse(p.isMember(u2.getUserName()));
    }

    @Test
    public void testIsFinished()
            throws CardAlreadyExistsException, CardNotFoundException, IllegalCardMovementException {

        Project p = new Project("project", "0.0.0.0");

        assertTrue(p.isFinished());

        p.addCard("card", "test");

        assertFalse(p.isFinished());

        p.moveCard("card", CardState.INPROGRESS);
        p.moveCard("card", CardState.TOBEREVISED);
        p.moveCard("card", CardState.DONE);

        assertTrue(p.isFinished());
    }
}
