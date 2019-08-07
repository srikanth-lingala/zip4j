package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
/**
* 最好把这个类放到.callback包下
*/
public interface UncatchException {
    void wrongPassword(ZipException e);
}
