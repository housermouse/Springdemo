package com.apex.ams.config.server.env;

public interface EnvEncryptor {
    String encrypt(String text) throws Exception;

    String decrypt(String text) throws Exception;
}
