package com.apex.ams.config.server.env;

import com.apex.ams.utils.CryptoUtil;
import com.apex.ams.utils.PlainKey;
import org.springframework.security.crypto.codec.Hex;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.SecureRandom;

public class AESEncryptor implements  EnvEncryptor {
    private static final int blockSize = 16;
    private EncryptProperties encryptProperties;

    public AESEncryptor(EncryptProperties encryptProperties){
        this.encryptProperties = encryptProperties;
    }

    @Override
    public String encrypt(String text) throws Exception {
        byte rnd[] = new byte[blockSize];
        SecureRandom random = new SecureRandom();
        random.nextBytes(rnd);
        byte bytes[] = CryptoUtil.aes(text.getBytes("UTF-8"),
                encodePwd(encryptProperties.getKey(), new String(Hex.encode(rnd))), Cipher.ENCRYPT_MODE);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(1024 );
        bout.write(rnd.length);
        bout.write(rnd);
        bout.write(bytes);
        bout.close();

        return CryptoUtil.base64Encode(bout.toByteArray());
    }

    @Override
    public String decrypt(String text) throws Exception{
        byte[] buf = CryptoUtil.base64Decode(text) ;
        int klen = buf[0];
        byte key[] = new byte[klen];
        System.arraycopy(buf, 1, key, 0, klen);
        byte enc[] = new byte[buf.length - klen -1];
        System.arraycopy(buf, 1+klen, enc, 0, enc.length);
        byte bytes[] = CryptoUtil.aes(enc,
                encodePwd(encryptProperties.getKey(), new String(Hex.encode(key))), Cipher.DECRYPT_MODE);
        return new String(bytes,"UTF-8");
    }

    private byte[] encodePwd(String pwd, String salt) throws Exception {
        Key key = new PlainKey(salt);
        Mac mac = Mac.getInstance("HmacMD5");
        mac.init(key);
        byte[] digest = mac.doFinal(pwd.getBytes());
        return digest;
    }


}
