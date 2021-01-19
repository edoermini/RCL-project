package com.worth.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worth.components.Card;
import com.worth.components.Project;
import com.worth.components.User;
import com.worth.exceptions.card.CardNotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Writer {
    private static final String projectsDir = "db/projects";
    private static final String usersDir = "db/users";

    /**
     * Creates all base folders (db/projects, db/users)
     */
    public static void createDB() {
        // creating db folders them doesn't exists

        String base = "db";
        File dbDir = new File(base);
        dbDir.mkdir();

        File projects = new File(projectsDir);
        projects.mkdir();

        File users = new File(usersDir);
        users.mkdir();

    }

    /**
     * Creates new folder in database root with project's name and adds
     * a .meta folder inside it which contains project's name, ip and members
     *
     * @param p the project
     */
    public static void addProject(Project p) {
        String projDirName = projectsDir + "/" + p.getName();
        File projDir = new File(projDirName);

        // creating project dir
        projDir.mkdir();

        // creating .meta dir inside project dir
        File metaDir = new File(projDir, ".meta");
        metaDir.mkdir();

        // creating file containing project's chat ip
        File projIpFile = new File(metaDir, "ip");
        try {
            projIpFile.createNewFile();

            // writing project's ip
            FileOutputStream fos = new FileOutputStream(projIpFile);
            fos.write(p.getChatIp().getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // creating file containing project's members
        File projMembersFile = new File(metaDir, "members");
        try {
            projMembersFile.createNewFile();

            // writing project's members
            new ObjectMapper().writeValue(projMembersFile, p.getMembers());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // writing cards
        for (String c : p.getCards()) {
            try {
                updateCard(p, p.getCard(c));
            } catch (CardNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes all files and folders relative to project
     *
     * @param p project to delete
     */
    public static void delProject(Project p) {

        // opening project folder
        String projDirName = projectsDir + "/" + p.getName();
        File projDir = new File(projDirName);

        deleteDir(projDir);
    }

    /**
     * Updates project's members list in filesystem
     *
     * @param p the project
     */
    public static void updateMembers(Project p) {
        String projDirName = projectsDir + "/" + p.getName();

        File projDir = new File(projDirName);
        File metaDir = new File(projDir, ".meta");
        File projMembersFile = new File(metaDir, "members");

        try {
            projMembersFile.createNewFile();

            // writing project's members
            new ObjectMapper().writeValue(projMembersFile, p.getMembers());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a file corresponding given card inside project's folder
     * if card doesn't exists, updates existing card otherwise
     *
     * @param p the project containing the card
     * @param c the card
     */
    public static void updateCard(Project p, Card c) {

        // opening project folder
        String projDirName = projectsDir + "/" + p.getName();
        File projDir = new File(projDirName);

        // opening card file
        File cardFile = new File(projDir, c.getName());

        // creating card file
        try {
            cardFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Writing card c to file
        try {
            new ObjectMapper().writeValue(cardFile, c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates new file inside users folder representing given user
     *
     * @param u the user to add
     */
    public static void addUser(User u) {
        String userFileName = usersDir + "/" + u.getUserName();
        File userFile = new File(userFileName);

        try {
            userFile.createNewFile();

            // writing project's members
            new ObjectMapper().writeValue(userFile, u);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // deletes a directory deleting first each file inside it recursively
    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if(files != null) {
            for (final File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
    }

}
