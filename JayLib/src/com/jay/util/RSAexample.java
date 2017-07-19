package com.jay.util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAexample {

 public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
	 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

     byte[] input = "abcdefg hijklmn".getBytes();
     Cipher cipher = Cipher.getInstance("RSA/None/NoPadding", "BC");
     SecureRandom random = new SecureRandom();
     KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");

     generator.initialize(128, random); // ���⿡���� 128 bit Ű�� �����Ͽ���
     KeyPair pair = generator.generateKeyPair();
     Key pubKey = pair.getPublic();  // Kb(pub) ����Ű
     Key privKey = pair.getPrivate();// Kb(pri) ����Ű

     // ����Ű�� �����Ͽ� ��ȣȭ
     cipher.init(Cipher.ENCRYPT_MODE, pubKey);
     byte[] cipherText = cipher.doFinal(input);
     System.out.println("cipher: ("+ cipherText.length +")"+ new String(cipherText));
     
     // ����Ű�� �������ִ��ʿ��� ��ȣȭ
     cipher.init(Cipher.DECRYPT_MODE, privKey);
     byte[] plainText = cipher.doFinal(cipherText);
     System.out.println("plain : " + new String(plainText));
 }
}