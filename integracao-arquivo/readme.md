# R2C3 - Integração - arquivos

Esse repositório possui um exemplo de integração com o sistema R2C3 utilizando o canal de arquivos.

# Requisitos

* Java 11
* Maven

# Passos

Os passos necessários para que um Participante realize troca de arquivos com a CIP, por meio do Connect:Direct ou CFT, são os seguintes:

1.	Construir uma requisição, em formato definido no manual de leiautes;
1.	Compactar esse posicional usando o algoritmo “gzip” do padrão ZIP (implementado no Unix pelo gzip, em Java pelo java.util.zip, em C pelo zlib, etc).
1.	Assinar e encriptar o arquivo e mensagens utilizando um framework de criptografia padrão SPB - “5 Especificações para Segurança de Mensagens e Arquivos” do Manual de Segurança da RSFN.;
1.	Enviar para a CIP esse arquivo da requisição, já em formato SPB encriptado e assinado, utilizando o Connect:Direct ou XFB usando o modo de transferências binário;
1.	Após o processamento desse arquivo de requisição, a CIP enviará de volta um arquivo de resposta, também em formato SPB encriptado e assinado;
1.	O arquivo de resposta em formato SPB encriptado e assinado deve ser agora transformado em posicional compactado, utilizando novamente o mesmo framework de criptografia padrão SPB;
1.	O posicional compactado deve ser agora descompactado, usando a operação inversa do algoritmo “gzip” do padrão ZIP;
1.	A resposta posicional está agora disponível, para ser processada pelo sistema do Participante, de acordo com o formato de resposta definido no manual de leiautes.

# Exemplo completo:

_/src/main/java/br/org/cip/howto_r2c3_integracao_arquivo/App.java_

```
System.out.println("1. Construindo uma requisição, em formato definido no manual de leiautes");
String xml = 
            "<?xml version=\"1.0\"?>\r\n"
        + "<ADDADOC xmlns=\"http://www.bcb.gov.br/ARQ/AGEN001.xsd\">\r\n"
        + "    <BCARQ>\r\n"
        + "        <NomArq>AGEN001_92894922_20210201_00021</NomArq>\r\n"
        + "        <NumCtrlEmis>20210201000000024460</NumCtrlEmis>\r\n"
        + "        <ISPBEmissor>17423302</ISPBEmissor>\r\n"
        + "        <ISPBDestinatario>92894922</ISPBDestinatario>\r\n"
        + "        <DtHrDDA>2021-02-01T15:04:27</DtHrDDA>\r\n"
        + "        <IndrFlagFim>N</IndrFlagFim>\r\n"
        + "        <DtMovto>2021-02-01</DtMovto>\r\n"
        + "    </BCARQ>    "
        + "	   <SISARQ>\r\n"
        + "    	   <AGEN001>\r\n"
        + "        		<ISPBEmissor>17423302</ISPBEmissor>\r\n"
        + "        		<ISPBDestinatario>92894922</ISPBDestinatario>\r\n"
        + "        		<MsgECO>AGEN001 from 17423302 to 92894922</MsgECO>\r\n"
        + "    	   </AGEN001>\r\n"
        + "    </SISARQ>"
        + "</ADDADOC>";
IOUtils.write(xml.getBytes(), new FileOutputStream("req.xml"));

System.out.println("2. Compactando esse posicional usando o algoritmo “gzip” do padrão ZIP (implementado no Unix pelo gzip, em Java pelo java.util.zip, em C pelo zlib, etc)");
ZipUtils.gzipFile("req.xml", "req.gz");

System.out.println("3. Assinando e encriptando o arquivo e mensagens utilizando um framework de criptografia padrão SPB - “5 Especificações para Segurança de Mensagens e Arquivos” do Manual de Segurança da RSFN.");
EncryptUtils.signEncrypt("req.gz", "req.gz.dat");

System.out.println("4. Enviando para a CIP esse arquivo da requisição, já em formato SPB encriptado e assinado, utilizando o Connect:Direct ou XFB usando o modo de transferências binário.");
Utils.sendToCip("req.gz.dat");

System.out.println("5. Após o processamento desse arquivo de requisição, a CIP enviará de volta um arquivo de resposta, também em formato SPB encriptado e assinado considerar como arquivo de resposta, neste exemplo, o arquivo \"resp.gz.dat\"");

System.out.println("6. O arquivo de resposta em formato SPB encriptado e assinado deve ser agora transformado em posicional compactado, utilizando novamente o mesmo framework de criptografia padrão SPB");
System.out.println("Verificando assinatura do arquivo de resposta e iniciando decriptação");
DecryptUtils.verifySignDecrypt("resp.gz.dat", "resp.gz");

System.out.println("7. O posicional compactado deve ser agora descompactado, usando a operação inversa do algoritmo “gzip” do padrão ZIP;");
System.out.println("Descompactando arquivo para o formato xml");
ZipUtils.unzipFile("resp.gz", "resp.xml");

System.out.println("8. A resposta posicional está agora disponível, para ser processada pelo sistema do Participante, de acordo com o formato de resposta definido no manual de leiautes.");
System.out.println("Printando xml de resposta");
try (BufferedReader br = new BufferedReader(new FileReader("resp.xml"))) {
    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }
}
```

## Execução

```
$ mvn clean compile exec:java -Dexec.mainClass="br.org.cip.howto_r2c3_integracao_arquivo.App"
```

## Documentação oficial

* [Portal do desenvolvedor](# "https://developer.cip-bancos.org.br/")
