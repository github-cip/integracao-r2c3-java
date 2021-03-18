/**
 
 É concedida permissão a qualquer pessoa que obtenha uma cópia do código fonte, sendo que o código fonte fornecido não tem qualquer garantia expressa ou implícita, em nenhum caso autores deste código, ou titulares dos diretos autorais são responsáveis por qualquer reivindicação, danos, ou quaisquer responsabilidades decorrente de conexão ou com o uso deste código fonte em qualquer segmento, negócios ou outros softwares
 
 */

package br.org.cip.howto_r2c3_integracao_arquivo;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

public class EncryptUtils {
	private static final int HEADER_SIZE = 588;
	private static final int SERIAL_NUMBER_SIZE = 32;
	private static final int STRING_SERIAL_NUMBER_SIZE = 16;
	private static final int BUFFER_SIZE = 256;
	private static final int PADDING_SIZE = 8;
	
	public static void signEncrypt(String srcFilePath, String targetFilepath) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, KeyStoreException, NoSuchProviderException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, SignatureException, InvalidAlgorithmParameterException {
		// Arquivo de entrada
		// ATENÇÃO: o arquivo deve estar compactado em GZIP e com padding de zeros para
		// tornar o tamanho múltiplo de 8.
		File inputPath = new File(srcFilePath);

		// Arquivo de saída criptografado
		File outputPath = new File(targetFilepath);

		OutputStream outputStream = new FileOutputStream(outputPath);

		CertificateUtil.CertificateInfo senderCert = CertificateUtil.getCertificateInfo("sender");
		CertificateUtil.CertificateInfo receiverCert = CertificateUtil.getCertificateInfo("receiver");

		DataOutputStream dos = new DataOutputStream(outputStream);

		// c01 - Header Size
		dos.writeShort(HEADER_SIZE);

		// c02 - Protocol Version
		dos.writeByte(2);

		// c03 - Error Code
		dos.writeByte(0);

		// c04 - Special Treatment Indicator:
		// 1 - Mensagem relativa a segurança ou que utiliza um certificado digital ainda
		// não ativado (caso da GEN0006);
		// 2 - Mensagem não cifrada para o destinatário (somente nos casos de
		// "broadcast" público, isto é, mensagens sem destinatário específico);
		// 3 - Mensagem não cifrada que pode ser relativa a segurança (nos casos das
		// mensagens GEN0004);
		// 4 - Indicativo de arquivo não compactado, normalmente gerado como resposta a
		// uma mensagem;
		// 6 - Indicativo de arquivo não compactado, sem cifragem, normalmente de uso
		// público;
		// 8 - Indicativo de arquivo compactado segundo o padrão Zip.
		// 10 - Indicativo de arquivo compactado segundo o padrão Zip, sem cifragem,
		// normalmente de uso público;
		dos.writeByte(8);

		// C05 - Reservado
		dos.writeByte(0);

		// C06 - destAsymmetricAlgorithm:
		// 01H: RSA com 1024 bits
		// 02H: RSA com 2048 bits
		dos.writeByte(2);

		// C07 - symmetricAlgorithm:
		// 01H: Triple-DES com 168 bits (3 x 56 bits) (Vide 5.1.3)
		dos.writeByte(1);

		// C08 - asymmetricAlgorithm :
		// 01H: RSA com 1024 bits
		// 02H: RSA com 2048 bits
		dos.writeByte(2);

		// C09 - hashAlgorithm: 02H: SHA-1, 03H: SHA-256
		dos.writeByte(3);

		// C10 - destCertCa:
		// 01H: SPB-Serpro
		// 02H: SPB-Certisign
		// 03H: Pessoas Físicas
		// 04H: SPB-Serasa
		// 05H: SPB-CAIXA
		dos.writeByte(4);

		// C11 - destination Certificate Serial Number
		dos.write(canonicalSerialNumber(receiverCert.getCertificate()).getBytes(), 0, SERIAL_NUMBER_SIZE);

		// C12 - signatureCertCa
		// 01H: SPB-Serpro
		// 02H: SPB-Certisign
		// 03H: Pessoas Físicas
		// 04H: SPB-Serasa
		// 05H: SPB-CAIXA
		dos.writeByte(4);

		// C13 - signature Certificate Serial Number - Local
		dos.write(canonicalSerialNumber(senderCert.getCertificate()).getBytes(), 0, SERIAL_NUMBER_SIZE);

		// C14 - encryptedSymmetricKey
		Key secretKey = generateSecretKey();
		dos.write(cipherKey(secretKey, receiverCert.getCertificate(), BUFFER_SIZE), 0, BUFFER_SIZE);

		// C15 - messageSignature
		byte[] messageSignature = generateSignatute(senderCert, inputPath);
		dos.write(Arrays.copyOf(messageSignature, BUFFER_SIZE), 0, BUFFER_SIZE);

		cipherInputFile(secretKey, inputPath, outputStream);

		outputStream.flush();
		outputStream.close();
	}
	
	private static void cipherInputFile(Key secretKey, File inputPath, OutputStream outputStream)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IOException {
		Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
		AlgorithmParameterSpec cipherParams = new IvParameterSpec(secretKey.getEncoded(), 0, PADDING_SIZE);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, cipherParams);
		OutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);

		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		InputStream inputStream = new FileInputStream(inputPath);
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			cipherOutputStream.write(buffer, 0, bytesRead);
		}
		inputStream.close();
		cipherOutputStream.close();
	}

	private static byte[] generateSignatute(CertificateUtil.CertificateInfo senderCert, File inputPath)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(senderCert.getKeyPair().getPrivate());
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		InputStream inputStream = new FileInputStream(inputPath);
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			signature.update(buffer, 0, bytesRead);
		}
		inputStream.close();
		return signature.sign();
	}

	private static Key generateSecretKey() throws NoSuchAlgorithmException {
		KeyGenerator generator = KeyGenerator.getInstance("DESede");
		generator.init(SecureRandom.getInstance("SHA1PRNG"));
		return generator.generateKey();
	}

	private static byte[] cipherKey(Key secretKey, X509Certificate destCert, int keySize)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException {
		Cipher asymCipher = Cipher.getInstance("RSA");
		asymCipher.init(Cipher.WRAP_MODE, destCert);
		return Arrays.copyOf(asymCipher.wrap(secretKey), keySize);
	}

	private static String canonicalSerialNumber(X509Certificate cert) {
		return canonicalSerialNumber(cert.getSerialNumber().toString(STRING_SERIAL_NUMBER_SIZE));
	}

	private static String canonicalSerialNumber(String serialNumber) {
		if (serialNumber == null)
			return null;
		String sNumber = serialNumber.toUpperCase();
		int numberOfZeroes = SERIAL_NUMBER_SIZE - sNumber.length();
		if (numberOfZeroes <= 0)
			return sNumber;
		else
			return String.format("%0" + numberOfZeroes + "d%s", 0, sNumber);
	}
}
