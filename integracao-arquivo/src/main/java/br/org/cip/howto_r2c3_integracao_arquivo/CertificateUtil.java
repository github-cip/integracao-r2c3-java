/**
 
 É concedida permissão a qualquer pessoa que obtenha uma cópia do código fonte, sendo que o código fonte fornecido não tem qualquer garantia expressa ou implícita, em nenhum caso autores deste código, ou titulares dos diretos autorais são responsáveis por qualquer reivindicação, danos, ou quaisquer responsabilidades decorrente de conexão ou com o uso deste código fonte em qualquer segmento, negócios ou outros softwares
 
 */

package br.org.cip.howto_r2c3_integracao_arquivo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateUtil {
	public static CertificateInfo getCertificateInfo(String codigoIspb) throws NoSuchAlgorithmException,
			CertificateException, IOException, UnrecoverableKeyException, KeyStoreException, NoSuchProviderException {
		final String password = "123";
		InputStream inputStream = new FileInputStream(new File(codigoIspb + ".p12"));
		KeyStore keystore = KeyStore.getInstance("PKCS12");
		keystore.load(inputStream, password.toCharArray());
		
		String ispb = "12345678";
		if("receiver".equalsIgnoreCase(codigoIspb)) {
			ispb = "04391007";
		}
		return extractKeyPair(keystore, ispb, password.toCharArray());
	}

	private static CertificateInfo extractKeyPair(KeyStore keystore, String alias, char[] password)
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		Key key = keystore.getKey(alias, password);
		if (key instanceof PrivateKey) {
			X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
			PublicKey publicKey = cert.getPublicKey();
			return new CertificateUtil.CertificateInfo(cert, new KeyPair(publicKey, (PrivateKey) key));
		}
		return null;
	}

	public static class CertificateInfo {

		private X509Certificate certificate;
		private KeyPair keyPair;

		public CertificateInfo(X509Certificate certificate, KeyPair keyPair) {
			this.certificate = certificate;
			this.keyPair = keyPair;
		}

		public X509Certificate getCertificate() {
			return certificate;
		}

		public KeyPair getKeyPair() {
			return keyPair;
		}
	}
}
