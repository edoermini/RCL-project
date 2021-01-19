package com.worth.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worth.components.Card;
import com.worth.components.CardState;
import com.worth.components.Project;
import com.worth.components.User;
import com.worth.exceptions.card.CardAlreadyExistsException;
import com.worth.exceptions.user.UserAlreadyMemberException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.junit.Assert.*;

public class TestWriter {
    private static String projectsBase = "db/projects";
    private static String usersBase = "db/users";


    @AfterEach
    public void clean() throws IOException {

        File file = new File("./db");
        delete(file);
    }

    @BeforeEach
    public void createDB() {
        Writer.createDB();
    }

    @Test
    public void testAddProject() throws IOException, UserAlreadyMemberException, CardAlreadyExistsException {

        User u = new User("user", "test");
        Project p = new Project("test", "0.0.0.0");
        p.addMember(u.getUserName());

        p.addCard("card", "card");

        Writer.addProject(p);

        Path path;

        path = Paths.get(projectsBase + "/test/.meta/ip");
        assertTrue(Files.exists(path));
        assertEquals("0.0.0.0", Files.readString(path));

        path = Paths.get(projectsBase + "/test/.meta/members");
        assertTrue(Files.exists(path));

        List<String> members = new ObjectMapper().readValue(path.toFile(), new TypeReference<List<String>>() {});

        assertEquals("user", members.get(0));
        assertTrue(members.size() == 1);

        path = Paths.get(projectsBase + "/test/card");
        assertTrue(Files.exists(path));

        Card c = new ObjectMapper().readValue(path.toFile(), Card.class);
        assertEquals("card", c.getName());
        assertEquals("card", c.getDescription());
        assertEquals(Arrays.asList(CardState.TODO), c.getHistory());
    }

    @Test
    public void testDelProject() throws IOException {
        Project p = new Project("test", "0.0.0.0");

        Writer.addProject(p);
        Writer.delProject(p);

        assertFalse(Files.exists(Paths.get(projectsBase + "/test")));
    }

    @Test
    public void testUpdateMembers() throws IOException, UserAlreadyMemberException {
        User u = new User("user1", "test");
        Project p = new Project("test", "0.0.0.0");
        p.addMember(u.getUserName());

        Writer.addProject(p);

        p.addMember("user2");

        Writer.updateMembers(p);

        File membersFile = new File(projectsBase + "/test/.meta/members");

        List<String> members = Arrays.asList(new ObjectMapper().readValue(membersFile, String[].class));

        assertEquals(2, members.size());
        assertEquals("user1", members.get(0));
        assertEquals("user2", members.get(1));
    }

    @Test
    public void testUpdateCard() throws IOException {
        Project p = new Project("test", "0.0.0.0");

        Writer.addProject(p);

        // testing card creation

        Card c1 = new Card("test", "test");
        Writer.updateCard(p, c1);

        File cardFile = new File(projectsBase + "/test/test");
        Card c2 = new ObjectMapper().readValue(cardFile, Card.class);

        assertEquals(c2.getName(), c1.getName());
        assertEquals(c2.getDescription(), c1.getDescription());
        assertEquals(c2.getHistory().get(0), c1.getHistory().get(0));

        // testing card update

        Vector<CardState> history = new Vector<>();
        history.add(CardState.TODO);
        history.add(CardState.INPROGRESS);

        c1.setHistory(history);
        Writer.updateCard(p, c1);

        c2 = new ObjectMapper().readValue(cardFile, Card.class);

        assertEquals(CardState.INPROGRESS, c2.getHistory().get(1));
    }

    @Test
    public void testAddUser() throws IOException{

        User u = new User("test", "test");
        Writer.addUser(u);

        User u2 = new ObjectMapper().readValue(new File("db/users/test"), User.class);

        assertEquals(u.getUserName(), u2.getUserName());
        assertEquals(u.getStatus(), u2.getStatus());
        assertEquals(u.getPassword(), u2.getPassword());
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
