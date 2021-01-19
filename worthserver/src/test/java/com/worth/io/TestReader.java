package com.worth.io;

import com.worth.components.Card;
import com.worth.components.CardState;
import com.worth.components.Project;
import com.worth.components.User;
import com.worth.exceptions.card.CardAlreadyExistsException;
import com.worth.exceptions.card.CardNotFoundException;
import com.worth.exceptions.card.IllegalCardMovementException;
import com.worth.exceptions.user.UserAlreadyMemberException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class TestReader {

    @BeforeEach
    public void createDB() {
        Writer.createDB();
    }

    @AfterEach
    public void clean() throws IOException {

        File file = new File("./db");
        delete(file);
    }

    @Test
    public void testRestoreUsers() {

        List<User> users = Reader.restoreUsers();

        assertEquals(0, users.size());

        User u1 = new User("user1", "test");
        User u2 = new User("user2", "test");

        Writer.addUser(u1);
        Writer.addUser(u2);

        users = Reader.restoreUsers();

        assertEquals(2, users.size());
        assertEquals(u1.getUserName(), users.get(0).getUserName());
        assertEquals(u2.getUserName(), users.get(1).getUserName());
    }


    @Test
    public void testRestoreProjects() throws CardAlreadyExistsException, CardNotFoundException, IllegalCardMovementException, UserAlreadyMemberException {

        List<User> users = new ArrayList<>();
        users.add(new User("user", "test"));

        List<Project> projects = Reader.restoreProjects();

        assertEquals(0, projects.size());

        Project p1 = new Project("proj1", "0.0.0.0");

        p1.addCard("card1", "desc");
        p1.addCard("card2", "desc");
        p1.addCard("card3", "desc");
        p1.addCard("card4", "desc");

        p1.moveCard("card2", CardState.INPROGRESS);

        p1.moveCard("card3", CardState.INPROGRESS);
        p1.moveCard("card3", CardState.TOBEREVISED);

        p1.moveCard("card4", CardState.INPROGRESS);
        p1.moveCard("card4", CardState.TOBEREVISED);
        p1.moveCard("card4", CardState.DONE);

        p1.addMember(users.get(0).getUserName());

        Project p2 = new Project("proj2", "0.0.0.0");

        p2.addCard("card1", "desc");
        p2.addCard("card2", "desc");
        p2.addCard("card3", "desc");
        p2.addCard("card4", "desc");


        p2.moveCard("card2", CardState.INPROGRESS);

        p2.moveCard("card3", CardState.INPROGRESS);
        p2.moveCard("card3", CardState.TOBEREVISED);

        p2.moveCard("card4", CardState.INPROGRESS);
        p2.moveCard("card4", CardState.TOBEREVISED);
        p2.moveCard("card4", CardState.DONE);

        p2.addMember(users.get(0).getUserName());

        Writer.addProject(p1);
        Writer.addProject(p2);

        projects = Reader.restoreProjects();

        check(projects.get(0), users);
    }

    private void check(Project p, List<User> users) throws CardNotFoundException {
        assertEquals(p.getCard("card1").getState(), CardState.TODO);
        assertEquals(p.getCard("card2").getState(), CardState.INPROGRESS);
        assertEquals(p.getCard("card3").getState(), CardState.TOBEREVISED);
        assertEquals(p.getCard("card4").getState(), CardState.DONE);

        assertEquals(p.getCardState("card1"), CardState.TODO);
        assertEquals(p.getCardState("card2"), CardState.INPROGRESS);
        assertEquals(p.getCardState("card3"), CardState.TOBEREVISED);
        assertEquals(p.getCardState("card4"), CardState.DONE);

        assertEquals(p.getCard("card1").getHistory(), Arrays.asList(CardState.TODO));
        assertEquals(p.getCard("card2").getHistory(), Arrays.asList(CardState.TODO, CardState.INPROGRESS));
        assertEquals(p.getCard("card3").getHistory(), Arrays.asList(CardState.TODO, CardState.INPROGRESS, CardState.TOBEREVISED));
        assertEquals(p.getCard("card4").getHistory(), Arrays.asList(CardState.TODO, CardState.INPROGRESS, CardState.TOBEREVISED, CardState.DONE));

        assertEquals(p.getMembers().get(0), users.get(0).getUserName());
    }

    // Deletes a file or a directory and its children.
    private static void delete(File file) throws IOException {

        for (File childFile : file.listFiles()) {

            if (childFile.isDirectory()) {
                delete(childFile);
            } else {
                if (!childFile.delete()) {
                    throw new IOException();
                }
            }
        }

        if (!file.delete()) {
            throw new IOException();
        }
    }
}
