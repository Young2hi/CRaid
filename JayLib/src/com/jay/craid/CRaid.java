package com.jay.craid;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.jay.raid.RaidController;
import com.jay.util.CommonConst;
import com.jay.util.CommonUtil;
import com.jay.util.CryptoUtils;
import com.jay.util.FileHandler;
import com.jay.util.MetaCraid;

public class CRaid {

	public void splitFile(String sSourceFilePath, ArrayList<Integer> aSplitRatio, boolean doEncrypt, boolean doRaid) {
		// TODO Auto-generated method stub
		try {
    		MetaCraid meta = splitOperation(sSourceFilePath, aSplitRatio, doEncrypt, doRaid);
    		if(doRaid) {
	    		if(RaidController.backup(sSourceFilePath, meta))
	    			FileHandler.writeSerEncFile(meta, CommonConst.META_FILE_PATH, CommonConst.META_FILE_NAME);
	    		else throw new Exception("Failed RAID");
    		}else FileHandler.writeSerEncFile(meta, CommonConst.META_FILE_PATH, CommonConst.META_FILE_NAME);
    		System.out.println(meta);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void mergeFile(String sMetaFilePath, String sTargetFilePath) {
		// TODO Auto-generated method stub
		try {
    		MetaCraid meta = (MetaCraid)FileHandler.readSerEncFile(sMetaFilePath); 
//    		System.out.println(meta);
    		if(meta.isRaidType()) {
	    		if(RaidController.recover(sTargetFilePath, meta))
	    			mergeOperation(meta, sTargetFilePath, meta.getOperationType());
	    		else throw new Exception("Failed Recover from RAID");
    		}else mergeOperation(meta, sTargetFilePath, meta.getOperationType());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
     * @param sSourcePath
     * @param sSplitRatio
     * @param isEncrypt
     * @param doRaid
     * @return
     */
    private MetaCraid splitOperation(String sSourcePath, ArrayList<Integer> sSplitRatio, boolean isEncrypt, boolean doRaid) {
    	MetaCraid meta = null;
    	ArrayList<String> splitFileNames = null;
    	BufferedOutputStream bw = null;	
		RandomAccessFile raf = null;
		File sourceFile = null;
		File encryptedFile = null;
		String encryptedFilePath = null;
		
		try{
			encryptedFilePath = sSourcePath;
			if(isEncrypt) encryptedFilePath = sSourcePath+CommonConst.CURRENT_DIR+CommonConst.ENCRYPTED;
			sourceFile = new File(sSourcePath);
			encryptedFile = new File(encryptedFilePath);
			splitFileNames = new ArrayList<String>();
			
			meta = new MetaCraid();
			meta.setOriginFileType(CommonConst.BINARY);
	    	meta.setOriginFilePath(sSourcePath);
	    	meta.setId(CommonUtil.makeUniqueTimeID(CommonConst.ENC_BYTE_16));
	    	meta.setOperationType(isEncrypt);
	    	meta.setSecretKey(CryptoUtils.generateRandomSecretKey(CommonConst.AES));
	    	meta.setRaidType(doRaid);

	    	if(isEncrypt)CryptoUtils.encrypt(meta.getSecretKey(), sourceFile, encryptedFile);
    		raf = new RandomAccessFile(encryptedFilePath, "r");
    		long sourceSize = raf.length();
            sourceSize = raf.length();
            calcSplitRatio(sourceSize, sSplitRatio, doRaid);
            meta.setSplitRatio(sSplitRatio);
//System.out.println("sSplitRatio:"+sSplitRatio);            
            int iSplitCnt = sSplitRatio.size();
            int maxReadBufferSize = (int) sourceSize;
            if (maxReadBufferSize  > 1024*8) maxReadBufferSize = 1024*8;
//System.out.println("iSplitCnt:"+iSplitCnt);			
//System.out.println("maxReadBufferSize:"+maxReadBufferSize);

            long bytesPerSplit = 0 ;
            String aTempFileName = null;
            for(int destIx=0; destIx < iSplitCnt; destIx++) {
            	bytesPerSplit = sSplitRatio.get(destIx);
//System.out.println(destIx+":bytesPerSplit:"+bytesPerSplit);
            	      	
            	aTempFileName = sSourcePath.substring(0,sSourcePath.lastIndexOf("\\")+1)+CommonUtil.makeUniqueID(24);
     			bw = new BufferedOutputStream(new FileOutputStream(aTempFileName));
     			
     			if(bytesPerSplit > maxReadBufferSize) {
                    long numReads = bytesPerSplit/maxReadBufferSize;
//System.out.println(destIx+":numReads:"+numReads);                    
                    long numRemainingRead = bytesPerSplit % maxReadBufferSize;
//System.out.println(destIx+":numRemainingRead:"+numRemainingRead);
                    for(int i=0; i<numReads; i++) {
                        FileHandler.readWrite(raf, bw, maxReadBufferSize);
                    }
                    if(numRemainingRead > 0) {
                    	FileHandler.readWrite(raf, bw, numRemainingRead);
                    }
                }else {
                	FileHandler.readWrite(raf, bw, bytesPerSplit);
                }
                bw.flush();
                splitFileNames.add(aTempFileName);
            }
            
            long remainingBytes = sourceSize - raf.getFilePointer();
//System.out.println("Last:remainingBytes:"+remainingBytes);
            if(remainingBytes > 0) {
//            	aTempFileName = sSourcePath.substring(0,sSourcePath.lastIndexOf("\\")+1)+CommonUtil.makeUniqueID(24);
//            	bw = new BufferedOutputStream(new FileOutputStream(aTempFileName));
            	
            	byte[] buf = new byte[(int) remainingBytes];
                int val = raf.read(buf);
                if(val != -1) {
                	meta.setRemainingBytes(buf);
                } 
            	
//            	readWrite(raf, bw, remainingBytes);
//                bw.flush();
//                splitFileNames.add(aTempFileName);
//                sSplitRatio.set(sSplitRatio.size(), (int) remainingBytes);
            }
            meta.setSplitFileNames(splitFileNames);
    	}catch(Exception e){
    		e.printStackTrace();
    	}finally {
            try {
                bw.close();
                raf.close();
                if(isEncrypt) encryptedFile.delete();
            } catch (IOException ex) {
            	ex.printStackTrace();
            }
        }
    	return meta;
    }

    /**
     * @param fileLength
     * @param sSplitRatio
     * @param doRaid
     */
    private static void calcSplitRatio(long fileLength, ArrayList<Integer> sSplitRatio, boolean doRaid){
    	int totalLength = 0;
    	int itemCnt = sSplitRatio.size();
    	int unitLength = 0;
// System.out.println("fileLength:"+fileLength); 
// System.out.println("itemCnt:"+itemCnt);
    	for(int i=0;i<itemCnt;i++) {
    		totalLength += sSplitRatio.get(i);
    	} 
// System.out.println("totalLength:"+totalLength);   	
    	if(doRaid) {
    		unitLength = (int) (fileLength / itemCnt);        	
    	}else{
    		if(totalLength % itemCnt == 0) unitLength = (int) (fileLength / totalLength);
        	else unitLength = (int) ((fileLength/totalLength)+1);
    	}
// System.out.println("unitLength:"+unitLength);
    	for(int i=0;i<itemCnt;i++) {
    		sSplitRatio.set(i, (doRaid?1:sSplitRatio.get(i)) * unitLength);
    	}
// System.out.println("sSplitRatio:"+sSplitRatio);    	
    }
    
    /**
     * @param meta
     * @param sOutPutFilePath
     * @param doEncrypt
     */
    public static void mergeOperation(MetaCraid meta, String sOutPutFilePath, boolean doEncrypt){
    	System.out.println(meta);
        ArrayList<String> aSplitFileList = meta.getSplitFileNames();
//  System.out.println("aSplitFileList:"+aSplitFileList);  	
        FileOutputStream fOs = null;
        try{
        	File outputFile = new File(sOutPutFilePath);
        	fOs = new FileOutputStream(outputFile);
        	for(int destIx=0; destIx < aSplitFileList.size() ; destIx++) {
            	RandomAccessFile raf = new RandomAccessFile((String)aSplitFileList.get(destIx), "r");
            	FileHandler.readWrite(raf, fOs);
            }
        	if(meta.getRemainingBytes() != null) fOs.write(meta.getRemainingBytes());
            fOs.flush();
            fOs.close();
            if(doEncrypt)CryptoUtils.decrypt(meta.getSecretKey(), outputFile, outputFile);
        }catch(Exception ex){
        	ex.printStackTrace();
        }finally{
			try {
				if(fOs != null) fOs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
}
