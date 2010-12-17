package org.mycore.datamodel.ifs2;

public class StoreAlreadyExistsException extends Exception {
    public StoreAlreadyExistsException(String msg) {
        super(msg);
    }
}