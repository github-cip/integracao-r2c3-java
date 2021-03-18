/**
 
 É concedida permissão a qualquer pessoa que obtenha uma cópia do código fonte, sendo que o código fonte fornecido não tem qualquer garantia expressa ou implícita, em nenhum caso autores deste código, ou titulares dos diretos autorais são responsáveis por qualquer reivindicação, danos, ou quaisquer responsabilidades decorrente de conexão ou com o uso deste código fonte em qualquer segmento, negócios ou outros softwares
 
 */

package br.org.cip.howto_r2c3_integracao_arquivo;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.io.IOUtils;

public class DecryptUtils {
	public static void verifySignDecrypt(String srcFilePath, String targetFilepath) throws Exception {
		byte[] cipheredContent = IOUtils.toByteArray(new FileInputStream(srcFilePath));

		ByteArrayInputStream bais = new ByteArrayInputStream(cipheredContent);
		
		// Descriptografa a chave simétrica
		// C01 - Tamanho total do cabeçalho
		byte[] c01_headerSize = new byte[2];
		bais.read(c01_headerSize, 0, 2);
		printByteArray("(C01) Tamanho do Cabeçalho", c01_headerSize);
		
		// C02 - Versão do protocolo
		byte c02_version = (byte) bais.read();
		printByte("(C02) Versão Protocolo", c02_version);
		
		// C03 - Indicação de tratamento especial
		byte c03_errorCode = (byte) bais.read();
		printByte("(C03) Codigo Erro", c03_errorCode);
		
		// C04 - Código de erro
		byte c04_treatment = (byte) bais.read();
		printByte("(C04) Tratamento Especial", c04_treatment);
		
		// C05 Reservado para uso futuro
		byte c05_reserved = (byte) bais.read();
		printByte("(C05) Reservado Uso Futuro", c05_reserved);
		
		// C06 - Algoritmo da chave assimétrica do destino
		byte c06_symmAlgReceiver = (byte) bais.read();
		printByte("(C06) Algoritimo chave simetrica destino", c06_symmAlgReceiver);
		
		// C07 - Algoritmo da chave simétrica
		byte c07_symmKeyAlg = (byte) bais.read();
		printByte("(C07) Algoritimo chave simétrica", c07_symmKeyAlg);
		
		// C08 - Algortmo da chave assimétrica local
		byte c08_symmAlgSender = (byte) bais.read();
		printByte("(C08) Algoritimo chave simétrica local", c08_symmAlgSender);
		
		// C09 - Algoritmo de hash
		byte c09_hashAlg = (byte) bais.read();
		printByte("(C09) Algoritimo hash", c09_hashAlg);
		
		// C10 - PC do certificado digital do destino
		byte c10_certAuthReceiver = (byte) bais.read();
		
		// C11 - Série do certificado digital do destino
		byte[] c11_certSerialNumberReceiver = new byte[32];
		bais.read(c11_certSerialNumberReceiver, 0, 32);
		String receiverSerialNum = new String(c11_certSerialNumberReceiver);
		System.out.println("(C11) Receiver Serial Number: " + receiverSerialNum);
		
		// C12 - PC do certificado digital da Instituição
		byte c12_certAuthSender = (byte) bais.read();
		
		// C13 - série do certificado digital da Instituição
		byte[] c13_certSerialNumberSender = new byte[32];
		bais.read(c13_certSerialNumberSender, 0, 32);
		String senderSerialNum = new String(c13_certSerialNumberSender);
		System.out.println("(C13) Sender Serial Number: " + senderSerialNum);
		
		PublicKey senderPublicKey = getPublicKey("sender");
		
		// C14 - Buffer da chave simétrica
		byte[] c14_symmetricKeyContent = new byte[256];
		bais.read(c14_symmetricKeyContent, 0, 256);
		printByteArray("(C14) CHAVE SIMETRICA CRIPTOGRAFADA: ", c14_symmetricKeyContent);
		c14_symmetricKeyContent = Arrays.copyOf(c14_symmetricKeyContent, 256);

		// C15 - Buffer da assinatura
		byte[] c15_encodedSignature = new byte[256];
		bais.read(c15_encodedSignature, 0, 256);
		printByteArray("(C15) ASSINATURA CRIPTOGRAFADA: ", c15_encodedSignature);
		c15_encodedSignature = Arrays.copyOf(c15_encodedSignature, 256);
		
		// Área de dados (arquivo criptografado).
		int dataLength = cipheredContent.length - 588;
		byte[] encryptedData = new byte[dataLength];
		bais.read(encryptedData, 0, dataLength);
		System.out.println("Área de Dados: " + dataLength + " byte(s).");
		System.out.println(String.format("Multiplo de 8? %s", dataLength % 8 == 0));
		
		PrivateKey receiverPrivateKey = getPrivateKey("receiver");
		Cipher asymCipher = Cipher.getInstance("RSA"); // igual a "RSA/ECB/PKCS1PADDING"
		asymCipher.init(Cipher.UNWRAP_MODE, receiverPrivateKey);
	    SecretKey symmKey = (SecretKey)asymCipher.unwrap(c14_symmetricKeyContent, "DESede", Cipher.SECRET_KEY);
	    System.out.println(symmKey.getFormat());

	    // Descriptografa o conteúdo do arquivo com a chave simétrica
		byte[] decriptedData = decodeContent(symmKey, encryptedData);
		System.out.println("Arquivo decriptografado com sucesso.");
		
		// Salva o arquivo decriptografado em arquivo.
		String outputFile = targetFilepath;
		IOUtils.write(decriptedData, new FileOutputStream(outputFile));
		System.out.println(outputFile + " salvo com sucesso. " + decriptedData.length + " byte(s).");
		
		// Valida a assinatura do arquivo.
		boolean isSignatureValid = verifySignature(senderPublicKey, decriptedData, c15_encodedSignature);
        System.out.println("ASSINATURA VALIDA? " + isSignatureValid);
	}
	
	private static byte[] decodeContent(SecretKey key, byte[] data)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
		AlgorithmParameterSpec params = new IvParameterSpec(key.getEncoded(), 0, 8);
		cipher.init(Cipher.DECRYPT_MODE, key, params);
		return cipher.doFinal(data);
	}

	private static boolean verifySignature(PublicKey pk, byte[] data, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initVerify(pk);
        signer.update(data);
        return signer.verify(signature);
	}
	
	/**
	 * Carrega chave privada do repositório.
	 * @param serialNumber
	 * @return
	 * @throws Exception
	 */
	private static PrivateKey getPrivateKey(String serialNumber) throws Exception {
		CertificateUtil.CertificateInfo certInfo = CertificateUtil.getCertificateInfo(serialNumber);
		return certInfo.getKeyPair().getPrivate();
	}

	private static PublicKey getPublicKey(String serialNumber) throws Exception {
		//CertificateUtil.CertificateInfo certInfo = CertificateUtil.getCertificateInfo("sender");
		CertificateUtil.CertificateInfo certInfo = CertificateUtil.getCertificateInfo(serialNumber);
		return certInfo.getCertificate().getPublicKey();
		
	}

	public static void printByteArray(String description, byte[] content) {
		System.out.println(description +": [" +toHexString(content)+ "]");
	}
	
	public static void printByte(String description, byte content) {
		System.out.println(description +": [" +toHexString(content)+ "]");
	}
	
	public static String toHexString(byte b) {
		return String.valueOf(convertDigit((int) (b >> 4))) +  
			String.valueOf(convertDigit((int) (b & 0x0f)));
	}

	public static String toHexString(byte[] bytes) {
		StringBuffer sb = new StringBuffer(bytes.length * 2);
		for (int i=0; i<bytes.length; i++) {
			sb.append(toHexString(bytes[i]));
			sb.append(" ");
		}
		if (sb.length()>0 && sb.charAt(sb.length()-1)==' ')
		{
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}

	private static char convertDigit(int value) {
		value &= 0x0f;
		if (value >= 10)
			return ((char) (value - 10 + 'a'));
		return ((char) (value + '0'));
	}
}
