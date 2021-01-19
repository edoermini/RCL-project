package com.worth.managers;

import com.worth.components.User;
import com.worth.exceptions.OperationNotAllowedException;
import com.worth.exceptions.project.ProjectAlreadyExistsException;
import com.worth.exceptions.project.ProjectNotFoundException;
import com.worth.io.Writer;
import com.worth.rmi.callback.CallbackServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class TestProjectsManager {

    @BeforeEach
    public void createDB() {
        Writer.createDB();
    }

    @Test
    public void testCreateProject() throws ProjectAlreadyExistsException {
        ProjectsManager pm = new ProjectsManager(new ArrayList<>(), new CallbackServer());
        User u = new User("user", "test");

        pm.createProject("project", u.getUserName());
        assertEquals("project", pm.listProjects().get(0));

        Assertions.assertThrows(ProjectAlreadyExistsException.class, () -> {
            pm.createProject("project", u.getUserName());
        });

    }

    @Test
    public void testCancelProject()
            throws ProjectAlreadyExistsException, OperationNotAllowedException, ProjectNotFoundException {
        ProjectsManager pm = new ProjectsManager(new ArrayList<>(), new CallbackServer());
        User u1 = new User("user1", "test");
        User u2 = new User("user2", "test");

        pm.createProject("project", u1.getUserName());

        Assertions.assertThrows(OperationNotAllowedException.class, () -> {
            pm.cancelProject("project", u2.getUserName());
        });

        pm.cancelProject("project", u1.getUserName());

        assertEquals(0, pm.listProjects().size());

        Assertions.assertThrows(ProjectNotFoundException.class, () -> {
            pm.cancelProject("project", u1.getUserName());
        });
    }
}
