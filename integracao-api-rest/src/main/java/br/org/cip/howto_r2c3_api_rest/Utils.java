/**
 
 É concedida permissão a qualquer pessoa que obtenha uma cópia do código fonte, sendo que o código fonte fornecido não tem qualquer garantia expressa ou implícita, em nenhum caso autores deste código, ou titulares dos diretos autorais são responsáveis por qualquer reivindicação, danos, ou quaisquer responsabilidades decorrente de conexão ou com o uso deste código fonte em qualquer segmento, negócios ou outros softwares
 
 */

package br.org.cip.howto_r2c3_api_rest;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;

import okhttp3.OkHttpClient;

public class Utils {
	public static OkHttpClient getUnsafeOkHttpClient() {
		try {
			final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			} };

			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			return new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
					.hostnameVerifier(new HostnameVerifier() {
						@Override
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					}).build();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isSignatureValid(String detachedJws, String body) throws CertificateException, java.text.ParseException, IOException {
		final String cipCertificate = IOUtils.readInputStreamToString(Utils.class.getClassLoader().getResourceAsStream("cip.cer"));
		
		final PublicKey PUBLIC_KEY = X509CertUtils.parse(cipCertificate).getPublicKey();
		try {
			String[] splittedJws = detachedJws.split("\\.");
			
			String jws = splittedJws[0] + "." +  Base64URL.encode(body) + "." + splittedJws[2];
			
			SignedJWT signedJWT = SignedJWT.parse(jws);
			JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) PUBLIC_KEY);
			return signedJWT.verify(verifier);
		} catch (JOSEException e) {
			return false;
		}
	}
	
	public static String signRequest(String requestBody,
			String identificadorRequisicao)
			throws JOSEException, IOException, CertificateEncodingException, NoSuchAlgorithmException {
		ClassLoader classLoader = Utils.class.getClassLoader();
		
		final String privateKey = IOUtils.readInputStreamToString(classLoader.getResourceAsStream("87654321_priv_decrypted.pem"));
		final X509Certificate certificate = X509CertUtils.parse(IOUtils.readInputStreamToString(classLoader.getResourceAsStream("87654321.cer")));
		
		final String certificateThumbPrint256 = getThumbprint(certificate);
		final String certificateSerialHex = StringUtils.leftPad(certificate.getSerialNumber().toString(16), 32, '0');
		final String dataReferencia = "2021-03-17";
		final String ispbPrincipal = "87654321";
		final String ispbAdministrado = "87654321";
		
		JWK jwk = JWK.parseFromPEMEncodedObjects(privateKey);
		JWSSigner signer = new RSASSASigner(jwk.toRSAKey());

		JWSObject jwsObject = new JWSObject(
				new JWSHeader.Builder(JWSAlgorithm.RS256)
					.x509CertSHA256Thumbprint(new Base64URL(certificateThumbPrint256))
					.keyID(certificateSerialHex)
					.customParam("http://www.cip-bancos.org.br/identificador-requisicao", identificadorRequisicao)
					.customParam("http://www.cip-bancos.org.br/data-referencia", dataReferencia)
					.customParam("http://www.cip-bancos.org.br/identificador-emissor-principal", ispbPrincipal)
					.customParam("http://www.cip-bancos.org.br/identificador-emissor-administrado", ispbAdministrado)
					.build(), 
				new Payload(requestBody));

		jwsObject.sign(signer);
		
		return jwsObject.serialize(true);
	}
	
	private static String getThumbprint(X509Certificate cert) 
            throws NoSuchAlgorithmException, CertificateEncodingException {
		return X509CertUtils.computeSHA256Thumbprint(cert).toString();
    }
	
	public static String toHexString(byte[] bytes) {
		StringBuffer sb = new StringBuffer(bytes.length * 2);
		for (int i=0; i<bytes.length; i++) {
			sb.append(toHexString(bytes[i]));
		}
		if (sb.length()>0 && sb.charAt(sb.length()-1)==' ')
		{
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}
	
	private static String toHexString(byte b) {
		return String.valueOf(convertDigit((int) (b >> 4))) +  
			String.valueOf(convertDigit((int) (b & 0x0f)));
	}
	
	private static char convertDigit(int value) {
		value &= 0x0f;
		if (value >= 10)
			return ((char) (value - 10 + 'a'));
		return ((char) (value + '0'));
	}
}
