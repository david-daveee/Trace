package com.podhod.app;
import org.junit.Test;import static org.junit.Assert.*;
public class GmailAddressTest {
 @Test public void normalizesCaseAndWhitespace(){assertEquals("ace@gmail.com",GmailAddress.normalize(" Ace@GMAIL.COM "));}
 @Test public void preservesExactAlias(){assertEquals("ace.test+gym@gmail.com",GmailAddress.normalize("ace.test+gym@gmail.com"));}
 @Test public void rejectsPathsAndNonGmail(){for(String input:new String[]{"a/b@gmail.com","a@gmail.com/extra","@gmail.com","a@example.com","a b@gmail.com","",null})try{GmailAddress.normalize(input);fail("Accepted invalid address");}catch(IllegalArgumentException expected){}}
}
