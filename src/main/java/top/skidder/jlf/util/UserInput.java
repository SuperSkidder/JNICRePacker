package top.skidder.jlf.util;

import java.util.Scanner;

public class UserInput {
    Scanner scanner = new Scanner(System.in);

    public String input(String s){
        System.out.println("[Input] " + s+ ": ");
        return scanner.nextLine();
    }
}
