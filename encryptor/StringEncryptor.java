// Source code is decompiled from a .class file using FernFlower decompiler.
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class StringEncryptor {
   private static final String ALGORITHM = "AES";
   private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
   private static final int KEY_SIZE_BYTES = 32;
   private static final int IV_SIZE_BYTES = 16;

   public StringEncryptor() {
   }

   public static void main(String[] var0) {
      if (var0.length != 2) {
         System.err.println("Error: Invalid number of arguments.");
         System.err.println("Usage: java StringEncryptor <stringToEncrypt> <passphrase>");
         System.exit(1);
      }

      String var1 = var0[0];
      String var2 = var0[1];
      if (var1 == null || var1.isEmpty()) {
         System.err.println("Error: String to encrypt cannot be empty.");
         System.exit(1);
      }

      if (var2 == null || var2.isEmpty()) {
         System.err.println("Error: Passphrase cannot be empty.");
         System.exit(1);
      }

      try {
         byte[] var3 = new byte[16];
         SecureRandom var4 = new SecureRandom();
         var4.nextBytes(var3);
         SecretKey var5 = deriveKey(var2, var3);
         byte[] var6 = generateIv();
         IvParameterSpec var7 = new IvParameterSpec(var6);
         Cipher var8 = Cipher.getInstance("AES/CBC/PKCS5Padding");
         var8.init(1, var5, var7);
         byte[] var9 = var1.getBytes(StandardCharsets.UTF_8);
         byte[] var10 = var8.doFinal(var9);
         byte[] var11 = new byte[var3.length + 16 + var10.length];
         System.arraycopy(var3, 0, var11, 0, var3.length);
         System.arraycopy(var6, 0, var11, var3.length, 16);
         System.arraycopy(var10, 0, var11, var3.length + 16, var10.length);
         String var12 = Base64.getEncoder().encodeToString(var11);
         System.out.println(var12);
      } catch (Exception var13) {
         System.err.println("Encryption failed: " + var13.getMessage());
         System.exit(1);
      }

   }

   private static SecretKey deriveKey(String var0, byte[] var1) throws Exception {
      PBEKeySpec var2 = new PBEKeySpec(var0.toCharArray(), var1, 65536, 256);
      SecretKeyFactory var3 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] var4 = var3.generateSecret(var2).getEncoded();
      return new SecretKeySpec(var4, "AES");
   }

   private static byte[] generateIv() {
      byte[] var0 = new byte[16];
      Random var1 = new Random(System.currentTimeMillis());
      var1.nextBytes(var0);
      return var0;
   }
}
