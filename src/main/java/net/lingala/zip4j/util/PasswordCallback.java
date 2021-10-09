package net.lingala.zip4j.util;

@FunctionalInterface
public interface PasswordCallback {

    char[] getPassword();
}
