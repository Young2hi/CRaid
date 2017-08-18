package com.jay.cipher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.jay.util.CommonConst;

public class SymmetricCipherFactory {
	
	private static String KEY_FILENAME = System.getProperty(CommonConst.USER_DIR_PROP_KEY)+File.separator+CommonConst.LIB_DIR+File.separator+CommonConst.SECRET_KEY_FILE;
	
	/**
	 * 1. 256bit AES(Rijndael)Ű��  ����
	 * 2. PBE�� �̿��Ͽ� ������ Ű�� ��ȣȭ�Ͽ� ���Ͽ� ����
	 */
	private static void createKey(char[] password) {
		System.out.println("Generating a RijnDael key...");
		// AES ��Ī Ű ������ ��ü ����
		KeyGenerator keyGenerator = null;
		try {
			keyGenerator = KeyGenerator.getInstance("Rijndael");
		} catch (NoSuchAlgorithmException e) {}
		// AES Ű ����
		keyGenerator.init(256);
		Key key = keyGenerator.generateKey();
		System.out.println("Done generating the key");
		
		// PBE ����
		// salt ����
		byte[] salt = new byte[8];
		SecureRandom random = new SecureRandom();
		random.nextBytes(salt);
		
		// password�� PBEKeySpec ���� 
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
		SecretKeyFactory keyFactory = null;;
		SecretKey pbeKey = null;
		try {
			keyFactory = SecretKeyFactory.getInstance("PBEWithSHAAndTwofish-CBC");
			pbeKey = keyFactory.generateSecret(pbeKeySpec);
		} catch (NoSuchAlgorithmException e) {} 
		catch (InvalidKeySpecException e) {}
		// PBEParameterSpec ����
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, CommonConst.ITERATION_CNT);
		// Cipher ���� �� �ʱ�ȭ
		Cipher cipher = null;
		byte[] encryptedKeyBytes = null;
		try {
			cipher = Cipher.getInstance("PBEWithSHAAndTwofish-CBC");
			cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
			
			// AESŰ ��ȣȭ
			encryptedKeyBytes = cipher.doFinal(key.getEncoded());
		} catch (NoSuchAlgorithmException e) {} 
		catch (NoSuchPaddingException e) {} 
		catch (InvalidKeyException e) {} 
		catch (InvalidAlgorithmParameterException e) {}
		catch (IllegalBlockSizeException e) {} 
		catch (BadPaddingException e) {}
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(KEY_FILENAME);
			//salt�� ��ȣȭ�� Ű ����Ʈ�� ����.
			fos.write(salt);
			fos.write(encryptedKeyBytes);
			fos.close();
		} catch (FileNotFoundException e) {} 
		catch (IOException e) {}
	}
	
	private static Key loadKey(char[] password) throws Exception {
		// ���Ϸκ��� �о����
		FileInputStream fis = new FileInputStream(KEY_FILENAME);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i = 0;
		while((i=fis.read()) != -1)
			baos.write(i);
		fis.close();
		byte[] saltAndKeyBytes = baos.toByteArray();
		baos.close();
		// salt�� �и���. Base64 ���ڵ��� ���� �ʾұ⿡ 8byte �״�� ������
		byte[] salt = new byte[8];
		System.arraycopy(saltAndKeyBytes, 0, salt, 0, 8);
		
		// key�� �и���
		int length = saltAndKeyBytes.length - 8;
		byte[] encryptedKeyBytes = new byte[length];
		System.arraycopy(saltAndKeyBytes, 8, encryptedKeyBytes, 0, length);
		
		// PBE ����
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHAAndTwofish-CBC");
		SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, CommonConst.ITERATION_CNT);
		// Cipher ���� �� �ʱ�ȭ
		Cipher cipher = Cipher.getInstance("PBEWithSHAAndTwofish-CBC");
		cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
		// Ű ��ȣȭ
		byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);
		SecretKeySpec key = new SecretKeySpec(decryptedKeyBytes, "Rijndael");
		
		return key;
	}

	private static void encrypt(char[] passowrd, String fileInput, String fileOutput) throws Exception {
		// key �ε�
		System.out.println("Loading the key.");
		Key key = loadKey(passowrd);
		System.out.println("Loaded the key.");
		
		// ������ ����
		Cipher cipher = Cipher.getInstance("Rijndael/CBC/PKCS5Padding");
		System.out.println("Initializing SecureRandom...");
		// SecureRandom �����Ͽ� IV �ʱ�ȭ
		SecureRandom random = new SecureRandom();
		byte[] iv = new byte[16];
		random.nextBytes(iv);
		// �Է� ���ϰ� ��� ������ stream�� ���� 
		FileInputStream fis = new FileInputStream(fileInput);
		FileOutputStream fos = new FileOutputStream(fileOutput);
		
		// ���� ������Ͽ� iv�� ����.
		fos.write(iv);
		// IvParameterSpec�� �����ϰ� �����۸� ���� �� �ʱ�ȭ�Ѵ�.
		IvParameterSpec spec = new IvParameterSpec(iv);
		System.out.println("Initializing the cipher...");
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		// ��� ��Ʈ���� �����۸� ���ڷ� ������ ��Ʈ���� �����Ѵ�. 
		CipherOutputStream cos = new CipherOutputStream(fos, cipher);
		
		System.out.println("Encrypting the file");
		// �Է� ���Ϸκ��� �о�鿩 ������ ��Ʈ������ ����.
		int theByte = 0;
		while((theByte = fis.read()) != -1)
			cos.write(theByte);
		// ��Ʈ���� ��� �ݾ��ش�.
		fis.close();
		cos.close();
	}
	
	private static void decrypt(char[] password, String fileInput, String fileOutput) throws Exception {
		// key �ε�
		System.out.println("Loading the key");
		Key key = loadKey(password);
		System.out.println("Loaded the key.");
		// ������ ����
		Cipher cipher = Cipher.getInstance("Rijndael/CBC/PKCS5Padding");
		// �Է� ���ϰ� ��� ������ stream�� ���� 
		FileInputStream fis = new FileInputStream(fileInput);
		FileOutputStream fos = new FileOutputStream(fileOutput);
		// �Է����Ͽ��� �ʱ�ȭ ���� 16byte�� �о���δ�. 
		// Base64���ڵ��� ���� �ʾ����Ƿ� ���̴� �״�δ�.
		byte[] iv = new byte[16];
		fis.read(iv);
		// IvParameterSpec ���� �� ������ �ʱ�ȭ
		IvParameterSpec spec = new IvParameterSpec(iv);
		System.out.println("Initializing the cipher...");
		cipher.init(Cipher.DECRYPT_MODE, key, spec);
		// �Է����ϰ� �����۸� ���ڷ� ������ ��Ʈ���� �����Ѵ�.
		CipherInputStream cis = new CipherInputStream(fis, cipher);
		System.out.println("Decrypting the file...");
		// �Է����Ϸ� ���� �о�鿩 ������Ͽ� ����.
		int theByte = 0;
		while((theByte = cis.read()) != -1)
			fos.write(theByte);
		// ��Ʈ���� ��� �ݾ��ش�.
		cis.close();
		fos.close();
	}
}
