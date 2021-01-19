package com.worth.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worth.components.Card;
import com.worth.components.Project;
import com.worth.components.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Reader {

    /**
     * Returns the users list saved previously into filesystem
     *
     * @return the users list saved previously into filesystem
     */
    public static List<User> restoreUsers() {
        String usersDir = "db/users";

        ArrayList<User> usersList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        File dir = new File(usersDir);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {

            // each userFile is a jackson serialized User object
            for (File userFile : directoryListing) {

                try {
                    usersList.add(mapper.readValue(userFile, User.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        return usersList;
    }

    /**
     * Returns the projects list saved previously into filesystem
     *
     * @return the projects list saved previously into filesystem
     */
    public static List<Project> restoreProjects() {
        String projectsDir = "db/projects";

        ArrayList<Project> projectList = new ArrayList<>();
        File dir = new File(projectsDir);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {

            // each directory contains meta info of project Object
            // inside .meta directory
            // each file inside projDir directory is a jackson serialized Card object
            for (File projDir : directoryListing) {

                // creating project instance
                String ip = getProjIp(projDir);
                Project p = new Project(projDir.getName(), ip);

                // adding members to project
                List<String> members = getProjMembers(projDir);
                p.addMembers(members);

                // adding cards to project
                List<Card> cards = getProjCards(projDir);
                p.addCards(cards);

                projectList.add(p);
            }
        }

        return projectList;
    }

    private static String getProjIp(File projDir) {
        File ipFile = new File(projDir, ".meta/ip");
        String ip = null;

        try {
            ip = new Scanner(ipFile).nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return ip;
    }

    private static List<String> getProjMembers(File projDir) {
        File membersFile = new File(projDir, ".meta/members");
        List<String> members = null;

        // deserializing project members name list
        try {
            members = Arrays.asList(new ObjectMapper().readValue(membersFile, String[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return members;
    }

    private static List<Card> getProjCards(File projDir) {
        ArrayList<Card> cards = new ArrayList<>();
        File[] cardsListing = projDir.listFiles();

        if (cardsListing != null) {

            // iterating over files inside project directory
            for (File cardFile : cardsListing) {

                if (cardFile.isFile()) {
                    // is card file

                    // deserializing card file
                    try {
                        cards.add(new ObjectMapper().readValue(cardFile, Card.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        return cards;
    }

}
