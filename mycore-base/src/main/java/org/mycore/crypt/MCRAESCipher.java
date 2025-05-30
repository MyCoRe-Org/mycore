/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.crypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRProperty;

public class MCRAESCipher extends MCRCipher {

    private static final Logger LOGGER = LogManager.getLogger();

    private String keyFile;

    private SecretKey secretKey;

    private Cipher encryptCipher;

    private Cipher decryptCipher;

    @MCRProperty(name = "KeyFile")
    public void setKeyFile(String path) {
        keyFile = path;
    }

    public MCRAESCipher() {
        secretKey = null;
        encryptCipher = null;
        decryptCipher = null;
    }

    @Override
    public void init(String id) throws MCRCryptKeyFileNotFoundException, InvalidKeyException {
        cipherID = id;

        try {
            LOGGER.info("Get key from file {}.", keyFile);
            String encodedKey = Files.readString(FileSystems.getDefault().getPath(keyFile));
            byte[] decodedKey = java.util.Base64.getDecoder().decode(encodedKey);
            LOGGER.info("Set secret key");
            secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            encryptCipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            decryptCipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (NoSuchFileException e) {
            MCRCryptKeyFileNotFoundException fileNotFoundException = new MCRCryptKeyFileNotFoundException(
                "Keyfile " + keyFile
                    + " not found. Generate new one with CLI command or copy file to path.");
            fileNotFoundException.initCause(e);
            throw fileNotFoundException;
        } catch (IllegalArgumentException e) {
            throw new InvalidKeyException("Error while decoding key from keyFile " + keyFile + "!", e);
        } catch (IOException e) {
            throw new MCRException("Can't read keyFile " + keyFile + ".", e);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new MCRCryptCipherConfigurationException(
                "The algorithm AES/ECB/PKCS5PADDING ist not provided by this Java version."
                    + "Update Java or configure an other chipher in mycore.properties.",
                e);
        }
    }

    @Override
    public boolean isInitialised() {
        return (secretKey != null && encryptCipher != null && decryptCipher != null);
    }

    @Override
    public void reset() {
        secretKey = null;
        encryptCipher = null;
        decryptCipher = null;
    }

    @Override
    public void generateKeyFile() throws FileAlreadyExistsException {
        try {
            LOGGER.info("generate Key File");
            String cryptKey = generateKey();
            Path keyPath = FileSystems.getDefault().getPath(keyFile);
            Files.createDirectories(keyPath.getParent());
            Files.writeString(keyPath, cryptKey, StandardOpenOption.CREATE_NEW);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRCryptCipherConfigurationException(
                "Error while generating keyfile: The configured algorithm is not available.", e);
        } catch (FileAlreadyExistsException e) {
            FileAlreadyExistsException fileAlreadyExistsException = new FileAlreadyExistsException(keyFile, null,
                "A crypt key shouldn't be generated if it allready exists. "
                    + " If you are aware of the consequences use overwriteKeyFile().");
            fileAlreadyExistsException.initCause(e);
            throw fileAlreadyExistsException;
        } catch (IOException e) {
            throw new MCRException("Error while write key to file.", e);
        }
    }

    @Override
    public void overwriteKeyFile() {
        try {
            LOGGER.info("overwrite Key File");
            String cryptKey = generateKey();
            Path keyPath = FileSystems.getDefault().getPath(keyFile);
            Files.createDirectories(keyPath.getParent());
            Files.writeString(keyPath, cryptKey);
        } catch (NoSuchAlgorithmException e) {
            throw new MCRCryptCipherConfigurationException(
                "Error while generating keyfile. The configured algorithm is not available.", e);
        } catch (IOException e) {
            throw new MCRException("Error while write key to file.", e);
        }
    }

    private String generateKey() throws NoSuchAlgorithmException {
        SecretKey tmpSecretKey = KeyGenerator.getInstance("AES").generateKey();
        return java.util.Base64.getEncoder().encodeToString(tmpSecretKey.getEncoded());
    }

    @Override
    protected String encryptImpl(String text) throws MCRCryptCipherConfigurationException {
        byte[] encryptedBytes = encryptImpl(text.getBytes(StandardCharsets.UTF_8));
        return java.util.Base64.getEncoder().encodeToString(encryptedBytes);
    }

    @Override
    protected String decryptImpl(String text) throws MCRCryptCipherConfigurationException {
        byte[] encryptedBytes = java.util.Base64.getDecoder().decode(text);
        byte[] decryptedBytes = decryptImpl(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    @Override
    protected byte[] encryptImpl(byte[] bytes) throws MCRCryptCipherConfigurationException {
        try {
            return encryptCipher.doFinal(bytes);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new MCRCryptCipherConfigurationException("Can't encrypt value - wrong configuration.", e);
        }
    }

    @Override
    protected byte[] decryptImpl(byte[] bytes) throws MCRCryptCipherConfigurationException {
        try {
            return decryptCipher.doFinal(bytes);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new MCRCryptCipherConfigurationException("Can't decrypt value - "
                + " possible issues: corrupted crypted value, wrong configuration or bad key.", e);
        }
    }

}
