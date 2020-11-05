package com.example.ucode;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class User implements Serializable {
    private int id, tokens, lives, toxic;
    private double level, assessorMark;
    private String firstName, lastName, username, email, location, adventure, photoUrl, phone, workplace;
    private boolean notifications_mail, notifications_push, notifications_slack;
    private ArrayList<Object[]> skills = new ArrayList<>();

    public void setProfileData(int id, String username, String firstName, String lastName, String email,
                               String location, String adventure, double level, String photoUrl, String phone, String workplace,
                               int tokens, int lives, double assessorMark, int toxic, Boolean notifications_mail,
                               boolean notifications_push, boolean notifications_slack, ArrayList<Object[]> skills) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.location = location;
        this.adventure = adventure;
        this.level = level;
        this.photoUrl = "https://lms.ucode.world/api/" + photoUrl;
        this.phone = phone;
        this.workplace = workplace;
        this.tokens = tokens;
        this.lives = lives;
        this.assessorMark = assessorMark;
        this.toxic = toxic;
        this.notifications_mail = notifications_mail;
        this.notifications_push = notifications_push;
        this.notifications_slack = notifications_slack;
        this.skills = skills;
    }

    public int ID() { return this.id; }
    public String USERNAME() { return this.username; }
    public String FIRST_NAME() { return this.firstName; }
    public String LAST_NAME() { return this.lastName; }
    public String EMAIL() { return this.email; }
    public String LOCATION() { return this.location; }
    public String ADVENTURE() { return this.adventure; }
    public double LEVEL() { return this.level; }
    public String PHOTO_URL() { return this.photoUrl; }
    public String PHONE() { return this.phone; }
    public String WORKPLACE() { return this.workplace; }
    public int TOKENS() { return this.tokens; }
    public int LIVES() { return this.lives; }
    public double ASSESSOR_MARK() { return this.assessorMark; }
    public int TOXIC() { return this.toxic; }
    public boolean NOTIFICATIONS_MAIL() { return this.notifications_mail; }
    public boolean NOTIFICATIONS_PUSH() { return this.notifications_push; }
    public boolean NOTIFICATIONS_SLACK() { return this.notifications_slack; }
    public ArrayList<Object[]> SKILLS() { return this.skills; }
}
