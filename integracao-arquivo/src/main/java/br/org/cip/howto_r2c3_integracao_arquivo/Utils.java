/**
 
 É concedida permissão a qualquer pessoa que obtenha uma cópia do código fonte, sendo que o código fonte fornecido não tem qualquer garantia expressa ou implícita, em nenhum caso autores deste código, ou titulares dos diretos autorais são responsáveis por qualquer reivindicação, danos, ou quaisquer responsabilidades decorrente de conexão ou com o uso deste código fonte em qualquer segmento, negócios ou outros softwares
 
 */

package br.org.cip.howto_r2c3_integracao_arquivo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.io.IOUtils;

public class Utils {
	public static void sendToCip(String string) throws IOException, UnrecoverableKeyException, InvalidKeyException, NoSuchAlgorithmException, CertificateException, KeyStoreException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, SignatureException, InvalidAlgorithmParameterException, InterruptedException {
		System.out.println("Arquivo enviado à CIP. Aguardando arquivo de resposta...");
		Thread.sleep(2000);
		String xml = 
				  "<?xml version=\"1.0\"?>\r\n"
				  + "		<ADDADOC xmlns=\"http://www.bcb.gov.br/ARQ/AGEN001.xsd\">\r\n"
				  + "		    <BCARQ>\r\n"
				  + "		        <NomArq>AGEN001_92894922_20210203_00020</NomArq>\r\n"
				  + "		        <NumCtrlEmis>20210203000000050465</NumCtrlEmis>\r\n"
				  + "		        <ISPBEmissor>17423302</ISPBEmissor>\r\n"
				  + "		        <ISPBDestinatario>92894922</ISPBDestinatario>\r\n"
				  + "		        <DtHrDDA>2021-02-03T15:04:45</DtHrDDA>\r\n"
				  + "		        <IndrFlagFim>N</IndrFlagFim>\r\n"
				  + "		        <DtMovto>2021-02-03</DtMovto>\r\n"
				  + "		    </BCARQ>    <SISARQ>\r\n"
				  + "		    <AGEN001>\r\n"
				  + "		        <ISPBEmissor>17423302</ISPBEmissor>\r\n"
				  + "		        <ISPBDestinatario>92894922</ISPBDestinatario>\r\n"
				  + "		        <MsgECO>AGEN001 from 17423302 to 92894922</MsgECO>\r\n"
				  + "		    </AGEN001>\r\n"
				  + "		    </SISARQ></ADDADOC>";
		
		
		//salva xml num arquivo
		IOUtils.write(xml.getBytes(), new FileOutputStream("resp.xml"));

		//2. Compactar esse posicional usando o algoritmo “gzip” do padrão ZIP (implementado no Unix pelo gzip, 
		//em Java pelo java.util.zip, em C pelo zlib, etc)
		ZipUtils.gzipFile("resp.xml", "resp.gz");
		
		//3. Assinar e encriptar o arquivo e mensagens utilizando um framework de criptografia padrão SPB - “5 Especificações para Segurança de Mensagens e Arquivos” do Manual de Segurança da RSFN.
		EncryptUtils.signEncrypt("resp.gz", "resp.gz.dat");
	}
}
