package com.example.ucode;

public class Cluster {
    public final static char MAC = '*';
    public final static char NON_MAC = 'I';
    public final static char SPACE = ' ';
    public final static int MAX_WIDTH = 20;

    public final static int E1 = 0x01;
    public final static int E2 = 0x02;
    public final static int E3 = 0x03;
    public final static int C1 = 0x04;

    private final static String[] e1_map = {
            "**                **",
            "****         *** ***",
            "****         *** ***",
            "*****I***I*****I ***",
            "***** ********** ***",
            "***** ********** ***",
            "*****I***I*****I ***",
            "***** ********** ***",
            "***** ********** ***",
            "*****I***I*****I ***",
            "***** ********** ***",
            "***** ********** ***"
    };
    private final static String[] e2_map = {
            "**               ***",
            "****          ******",
            "****          ******",
            "*****I **I*** I*****",
            "****** ****** ******",
            "****** ****** ******",
            "*****I **I***       ",
            "****** ******       ",
            "****** ******       ",
            "*****I **I***       ",
            "****** ******       ",
            "****** ******       ",
            "*****I **I***       ",
            "******              ",
            "******              "
    };
    private final static String[] e3_map = {
            "             ***    ",
            "             ***    ",
            "    *** **** ***    ",
            "    *** **** ***    ",
            "    *** **** ***    ",
            "    *** **** ***    ",
            "    *** **** ***    ",
            "    *** **** ***    ",
            "    *** **** ***    "
    };
    private final static String[] c1_map = {
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******  *****   ",
            "    ******          ",
            "    ******          "
    };
    private final static int e1_location = 1;
    private final static int e2_location = 1;
    private final static int e3_location = 1;
    private final static int c1_location = 5;
    private final static int e1_floor = 1;
    private final static int e2_floor = 2;
    private final static int e3_floor = 3;
    private final static int c1_floor = 1;

    public static String[] getMap(int cluster_id) {
        switch (cluster_id) {
            case E1: return e1_map;
            case E2: return e2_map;
            case E3: return e3_map;
            case C1: return c1_map;
        }
        return new String[0];
    }

    public static int getLocation(int cluster_id) {
        switch (cluster_id) {
            case E1: return e1_location;
            case E2: return e2_location;
            case E3: return e3_location;
            case C1: return c1_location;
        }
        return 0;
    }

    public static int getFloor(int cluster_id) {
        switch (cluster_id) {
            case E1: return e1_floor;
            case E2: return e2_floor;
            case E3: return e3_floor;
            case C1: return c1_floor;
        }
        return 0;
    }
}
