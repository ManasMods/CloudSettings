package com.github.manasmods.cloudsettings.exception;

public class AuthenticationFailedException extends IllegalStateException {
    public AuthenticationFailedException(String path) {
        super(String.format("Could not authenticate for path %s", path));
    }
}
