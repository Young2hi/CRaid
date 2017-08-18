package com.jay.cipher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.jay.util.CommonConst;

public class AsymmetricCipherFactory {
	private static void createKey(String password) throws Exception {

        // RSA Ű �� ����

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        //����Ű�� ���Ͽ� ����.
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        FileOutputStream fos = new FileOutputStream("c:\\publicKey");
        fos.write(publicKeyBytes);
        fos.close();

        // ���� Ű�� ��ȣȭ�� �Ŀ� ���Ͽ� ����.
        byte[] privateKeyBytes = passwordEncrypt(password.toCharArray(), keyPair.getPrivate().getEncoded());
        fos = new FileOutputStream("c:\\privateKey");
        fos.write(publicKeyBytes);
        fos.close();
  }

  private static byte[] passwordEncrypt(char[] password, byte[] plaintext) throws Exception {
        // salt ����
        byte[] salt = new byte[9];
        Random random = new Random();
        random.nextBytes(salt);

        // PBE Ű�� ������ ����
        PBEKeySpec keySpec = new PBEKeySpec(password);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHAAndTwofish-CBC");
        SecretKey key = keyFactory.generateSecret(keySpec);
        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, CommonConst.ITERATION_CNT);

        Cipher cipher = Cipher.getInstance("PBEWithSHAAndTwofish-CBC");
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

        byte[] cipherText = cipher.doFinal(plaintext);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(salt);
        baos.write(cipherText);

        return baos.toByteArray();
  }

  private static void encrypt(String fileInput) throws Exception {
        String publicKeyFileName = "c:\\publicKey";

        // ���� Ű�� ����� ���Ϸκ��� keyByte�� ����Ʈ �迭�� �����Ѵ�.
        FileInputStream fis = new FileInputStream(publicKeyFileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int theByte = 0;
        while((theByte = fis.read()) != -1)
               baos.writeTo(baos);
        fis.close();
        byte[] keyBytes = baos.toByteArray();
        baos.close();

        // ���ڵ��� Ű�� RSA ���� Ű�� �ν��Ͻ��� �ٲ۴�.
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        String fileOutput = fileInput + CommonConst.CURRENT_DIR + CommonConst.ENCRYPTED;
        DataOutputStream output = new DataOutputStream(new FileOutputStream(fileOutput));

        // RSA ���� Ű�� �̿��Ͽ� ���� Ű�� ��ȣȭ�� �����۸� �����Ѵ�.
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // ���� Ű ����
        KeyGenerator rijndaelKeyGenerator = KeyGenerator.getInstance("Rijndael");
        rijndaelKeyGenerator.init(256);
        Key rijndaelKey = rijndaelKeyGenerator.generateKey();

        // RSA �����۸� �̿��Ͽ� ���� Ű�� ��ȣȭ �ϰ� ���Ͽ� �����Ѵ�.
        // Ű�� ����, ���ڵ��� ���� Ű �����̴�.
        byte[] encodedKeyBytes = rsaCipher.doFinal(rijndaelKey.getEncoded());
        output.writeInt(encodedKeyBytes.length);
        output.write(encodedKeyBytes);

        // �ʱ�ȭ ����
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);

        //IV�� ���Ͽ� ����
        output.write(iv);

        //IV�� ������ ���� Ű�� �̿��Ͽ� ������ ������ ��ȣȭ�Ѵ�.
        IvParameterSpec spec = new IvParameterSpec(iv);
        Cipher symmetricCipher = Cipher.getInstance("Rijndael/CBC/PKCS5Padding");
        symmetricCipher.init(Cipher.ENCRYPT_MODE, rijndaelKey, spec);
        CipherOutputStream cos = new CipherOutputStream(output, symmetricCipher);

        FileInputStream input = new FileInputStream(fileInput);
        theByte = 0;
        while((theByte = input.read()) != -1)
               cos.write(theByte);

        input.close();
        cos.close();
        return;
  }
  
  private static byte[] passwordDecrypt(char[] password, byte[] ciphertext) throws Exception {

      // salt�� �д´�. ����Ű�� 8byte salt�� ����ߴ�.

      byte[] salt = new byte[8];
      ByteArrayInputStream bais = new ByteArrayInputStream(ciphertext);
      bais.read(salt, 0 ,8);

      byte[] remainingCiphertext = new byte[ciphertext.length-8];
      bais.read(remainingCiphertext, 0, ciphertext.length-8);

      //PBE �����۸� �����Ͽ� ���� Ű�� �����Ѵ�. 
      PBEKeySpec keySpec = new PBEKeySpec(password);
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHAAndTwofish-CBC");
      SecretKey key = keyFactory.generateSecret(keySpec);
      PBEParameterSpec paramSpec = new PBEParameterSpec(salt, CommonConst.ITERATION_CNT);
      Cipher cipher = Cipher.getInstance("PBEWithSHAAndTwofish-CBC");

      // Ű ��ȣȭ
      cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
      return cipher.doFinal(remainingCiphertext);
	}
	
	
	
	private static void decrypt(String password, String fileInput) throws Exception {
	
	      String privateKeyFilename = "c:\\privateKey";
	
	      // ���Ϸκ��� ���� Ű�� �о���δ�.
	      FileInputStream fis = new FileInputStream(privateKeyFilename);
	      ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
	      int theByte = 0;
	      while((theByte = fis.read()) != -1)
	             baos.write(theByte);
	
	      fis.close();
	      byte[] keyByte = baos.toByteArray();
	      baos.close();
	
	       // ��ȣȭ�� ���� Ű ����Ʈ�� �����Ѵ�.
	      keyByte = passwordDecrypt(password.toCharArray(), keyByte);
	
	      // RSA ���� Ű�� �����Ѵ�.
	      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyByte);
	      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	      PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
	
	      
	
	      // ���� Ű�� �̿��Ͽ� �����۸� �����ϰ� ���� Ű�� ��ȣȭ�Ѵ�
	
	      Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
	      DataInputStream dis = new DataInputStream(new FileInputStream(fileInput));
	
	      byte[] encryptedKeyBytes = new byte[dis.readInt()];
	      dis.readFully(encryptedKeyBytes);
	
	      rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
	      byte[] rijndaelKeyByte = rsaCipher.doFinal(encryptedKeyBytes);
	
	      SecretKey rijndaelKey = new SecretKeySpec(rijndaelKeyByte, "Rijndael");
	
	      byte[] iv = new byte[16];
	      dis.readFully(iv);
	
	      IvParameterSpec spec = new IvParameterSpec(iv);
	      
	      Cipher cipher = Cipher.getInstance("Rijndael/CBC/PKCS5padding");
	      cipher.init(Cipher.DECRYPT_MODE, rijndaelKey, spec);
	      CipherInputStream cis = new CipherInputStream(dis, cipher);
	
	      FileOutputStream fos = new FileOutputStream(fileInput + CommonConst.CURRENT_DIR + CommonConst.DECRYPTED);
	
	      theByte = 0;
	      while((theByte = cis.read()) != -1)
	             fos.write(theByte);
	      
	      cis.close();
	      fos.close();
	
	      return;      
	}


}
