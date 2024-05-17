package twomillions.plugin.advancedwish.utils.texts;

import lombok.experimental.UtilityClass;
import twomillions.plugin.advancedwish.annotations.JsInteropJavaType;
import twomillions.plugin.advancedwish.utils.exceptions.ExceptionUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 字符串加密和解密工具类，使用 AES 算法进行加密和解密。
 *
 * @author 2000000
 * @date 2023/2/19
 */
@UtilityClass
@JsInteropJavaType
public class StringEncrypter {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY = "Abfudi2!@#dfHsdg";
    private static final String IV_PARAMETER = "Abfudi2!@#dfHsdg";

    /**
     * 对给定的字符串进行加密，并返回加密后的字符串。
     *
     * @param str 要加密的字符串
     * @return 加密后的字符串，如果加密失败则返回 null
     */
    public static String encrypt(String str) {
        try {
            // 创建 AES 加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            IvParameterSpec ivParameter = new IvParameterSpec(IV_PARAMETER.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameter);

            // 加密字符串并使用 Base64 编码
            byte[] encryptedBytes = cipher.doFinal(str.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception exception) {
            return ExceptionUtils.throwEncrypt(exception);
        }
    }

    /**
     * 对给定的加密字符串进行解密，并返回解密后的字符串。
     *
     * @param encryptedStr 要解密的字符串
     * @return 解密后的字符串，如果解密失败则返回 null
     */
    public static String decrypt(String encryptedStr) {
        try {
            // 创建 AES 解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            IvParameterSpec ivParameter = new IvParameterSpec(IV_PARAMETER.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameter);

            // 解密 Base64 编码后的字符串
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedStr);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (Exception exception) {
            return ExceptionUtils.throwEncrypt(exception);
        }
    }
}

