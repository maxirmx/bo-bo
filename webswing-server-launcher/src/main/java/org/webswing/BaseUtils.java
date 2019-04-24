package org.webswing;

import com.swimap.external.cbb.baseutil.AESCipher;
import com.swimap.external.cbb.baseutil.CipherException;

import java.security.Key;

/**
 * Created by j00290887 on 2017/10/17.
 */
public class BaseUtils {
    public static char[] encrypt(char[] var0){
        try {
            return AESCipher.encrypt(var0, AESCipher.defaultKey(), "AES/CBC/NoPadding");
        } catch (CipherException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static char[] decrypt(char[] var0){
        try {
            return AESCipher.decrypt(var0, AESCipher.defaultKey(), "AES/CBC/NoPadding");
        } catch (CipherException e) {
            e.printStackTrace();
        }
        return null;
    }

}
