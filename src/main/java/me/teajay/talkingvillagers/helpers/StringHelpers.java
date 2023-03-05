package me.teajay.talkingvillagers.helpers;

public class StringHelpers {
    public static String removeNumbers(String s) {
        int i = s.length();
        while (i > 0 && Character.isDigit(s.charAt(i - 1))) {
            i--;
        }
        return s.substring(0, i);
    }
}
