/**
 
 É concedida permissão a qualquer pessoa que obtenha uma cópia do código fonte, sendo que o código fonte fornecido não tem qualquer garantia expressa ou implícita, em nenhum caso autores deste código, ou titulares dos diretos autorais são responsáveis por qualquer reivindicação, danos, ou quaisquer responsabilidades decorrente de conexão ou com o uso deste código fonte em qualquer segmento, negócios ou outros softwares
 
 */

package br.org.cip.howto_r2c3_integracao_arquivo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
	private static final int BUFFER_SIZE = 4096;
	
	public static void gzipFile(String srcFilePath, String targetFilepath) throws IOException {
		byte[] buffer = new byte[1024];

		GZIPOutputStream gzipOuputStream = new GZIPOutputStream(new FileOutputStream(targetFilepath));
		FileInputStream fileInput = new FileInputStream(srcFilePath);
		int bytes_read;

		while ((bytes_read = fileInput.read(buffer)) > 0) {
			gzipOuputStream.write(buffer, 0, bytes_read);
		}

		fileInput.close();
		gzipOuputStream.finish();
		gzipOuputStream.close();
		
		long fileLengthBeforePadding = new File(targetFilepath).length();
		final int PADDING_LENGTH = 8;
		
		long paddingLength = PADDING_LENGTH - (fileLengthBeforePadding % PADDING_LENGTH);
        if (paddingLength == PADDING_LENGTH) {
            paddingLength = 0;
        }
        
        //Se for necessário fazer padding
        if(paddingLength != 0) {
        	try(java.io.RandomAccessFile randomAccessFile = new java.io.RandomAccessFile(targetFilepath,"rw")) {
        		//JVM completa com zeros ao final do arquivo quando se estabelece o tamanho do arquivo
        		randomAccessFile.setLength(fileLengthBeforePadding + paddingLength);
        	}
        }
	}

	public static void unzipFile(String zipFilePath, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.createNewFile();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
	}
	
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
