package com.podhod.app;
import java.util.Locale;
final class GmailAddress {
 static String normalize(String input){String email=input==null?"":input.trim().toLowerCase(Locale.ROOT);if(email.length()>254||!email.matches("[a-z0-9][a-z0-9._+\\-]*@gmail\\.com"))throw new IllegalArgumentException("Enter a Gmail address, e.g. name@gmail.com");return email;}
 static final class NotFound extends Exception {}
}
