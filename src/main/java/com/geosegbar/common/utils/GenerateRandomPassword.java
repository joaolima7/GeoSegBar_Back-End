package com.geosegbar.common.utils;

import java.util.Random;

public class GenerateRandomPassword {
    public static String execute() {
    String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
    String numbers = "0123456789";
    String specialChars = "!@#$%^&*()-_=+";
    
    String allChars = upperCaseLetters + lowerCaseLetters + numbers + specialChars;
    
    Random random = new Random();
    StringBuilder password = new StringBuilder();
    
    password.append(upperCaseLetters.charAt(random.nextInt(upperCaseLetters.length())));
    password.append(lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length())));
    password.append(numbers.charAt(random.nextInt(numbers.length())));
    password.append(specialChars.charAt(random.nextInt(specialChars.length())));
    
    for (int i = 0; i < 4; i++) {
        password.append(allChars.charAt(random.nextInt(allChars.length())));
    }
    
    char[] passwordArray = password.toString().toCharArray();
    for (int i = 0; i < passwordArray.length; i++) {
        int j = random.nextInt(passwordArray.length);
        char temp = passwordArray[i];
        passwordArray[i] = passwordArray[j];
        passwordArray[j] = temp;
    }
    
    return new String(passwordArray);
    }
}
