# R2C3 - Integração - API REST

Esse repositório possui exemplos de integração com o sistema R2C3 utilizando API REST.

## Requisitos

* Java 11
* Maven
* Mockoon (para mockar o backend)

## Características da API REST do R2C3

* Todas as requisições e respostas são assinadas digitalmente;
* Será utilizado UTF-8 como codificação para o 'body' das requisições e respostas;
* O tamanho limite de payload de requisição é de 400Kb;

## Segurança

### Assinatura da Requisição

O padrão da assinatura digital  para API REST é o JWS (JSON Web Signature). Seguem alguns detalhes:
* A assinatura digital deverá ser realizada com RSASSA-PKCS1-v1_5 SHA-256
* O JWS deve ser representado no modo compacto e separado (detached). Referência: https://tools.ietf.org/html/rfc7515 (apêndice F)

### Campos do JOSE header:

* **x5t#S256** 
	* campo público correspondente ao thumbprint sha256 em base64url do certificado digital utilizado para assinar a mensagem
* **kid** 
	* campo público contendo o serial id (em hexadecimal) do requisitante com zeros a esquerda; por exemplo: 0000DA7B6F02EFA1EDDA741E78FF3508;
	* 32 caracteres em maiúsculo
* **alg**
	* campo público contendo o valor RS256
* **http://www.cip-bancos.org.br/identificador-requisicao** 
	* campo privado contendo uma identificação única da requisição com até 40 caracteres
	* Esta identificação deve ser única para o emissor e a data informados.
* **http://www.cip-bancos.org.br/identificador-requisicao-relacionada** 
	* campo privado contendo uma identificação única da requisição relacionada, este campo será preenchido pela CIP quando realizar requisições para os participantes quando relacionadas à uma requisição anterior recebida pela CIP, o campo terá o tamanho de até 40 caracteres
	* Campo não obrigatório.
* **http://www.cip-bancos.org.br/data-referencia**
	* campo privado contendo a data de referência da requisição
* **http://www.cip-bancos.org.br/identificador-emissor-principal** 
	* campo privado contendo o identificador do emissor principal da requisição
* **http://www.cip-bancos.org.br/identificador-emissor-administrado**
	* campo privado contendo o identificador do emissor administrado da requisição
* **http://www.cip-bancos.org.br/identificador-emissor-principal-relacionado**
	* campo privado contendo a identificação do emissor principal da requisição que originou esta resposta, este campo será preenchido pela CIP quando realizar requisições para os participantes quando relacionadas à uma requisição anterior recebida pela CIP, o campo terá o tamanho de até 8 caracteres
	* Campo não obrigatório.
* **http://www.cip-bancos.org.br/identificador-emissor-administrado-relacionado**
	* campo privado contendo a identificação do emissor administrado da requisição que originou esta resposta, este campo será preenchido pela CIP quando realizar requisições para os participantes quando relacionadas à uma requisição anterior recebida pela CIP, o campo terá o tamanho de até 8 caracteres
	* Campo não obrigatório.
	
## Implementação exemplo

Nos nossos exemplos utilizamos algumas bibliotecas para facilitar o entendimento de uso da API. São elas:

* **nimbus-jose-jwt**: Para abstrair a geração do JWS 
* **okhttp**: Para abstrair as requisições HTTP 

### Fonte

src/main/java/br/org/cip/howto_r2c3_api_rest/App.java

### Requisitos

É necessário ter o mockoon instalado e em execução para rodar nosso projeto exemplo. Ele simulará a API do R2C3. O mockoon escutará na porta 3001.

**Para instalar o CLI:**

```
$ npm install -g @mockoon/cli 
```

Mais detalhes da instalação: https://github.com/mockoon/cli#installation

**Para iniciar o mockoon-cli:**

```
$ mockoon-cli start --data ./mockoon.json
```

*Observação*: Caso necessite alterar os valores do mock, utilize o mockoon 'ui': https://github.com/mockoon/mockoon

### Execução

```
$ mvn clean compile exec:java -Dexec.mainClass="br.org.cip.howto_r2c3_api_rest.App"
```

A saída deverá ser:

```
Http Get Request - Sucesso: {"msg":"oi"}
Http Post Request - Sucesso: {"msg":"oi"}
```

### Detalhes

#### Geração do JWS:

Como falado anteriormente, todas as requisições devem ser assinadas. Segue um exemplo de como gerar uma assinatura.

```
String certificatePrivateKey = "-----BEGIN PRIVATE KEY-----MIIJQgIBA....";
		
//em base64
String certificateThumbPrint256 = "ZmE2MThkMjAyMWU0ZDI1NWRkY2FkYWIzODcwMDM1YzcwOWU0ODQ3MA=="; 

//completar com zeros a esquerda
String certificateSerialHex = "507e166079a8eb93";

String dataReferencia = new Date().toString();
String ispbPrincipal = "sender";
String ispbAdministrado = "87654321";
String identificadorRequisicao = "sender111111111234567"

JWK jwk = JWK.parseFromPEMEncodedObjects(certificatePrivateKey);
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

//Guardar JWS abaixo
String jws = jwsObject.serialize(true)
```

#### Exemplo de uma requisição GET:
```
final String HOST = "https://apihext.cipr2c3.org.br";
Request request = new Request.Builder()
	.url(HOST + "/api/v1/ferramentas/eco?msg=teste")
	.addHeader("x-jws-signature", jws) //jws foi gerado no passo anterior
	.build();

try (Response response = client.newCall(request).execute()) {
	System.out.println(response.body().string());
}
```

#### Exemplo de uma requisição POST
```
final String HOST = "https://apihext.cipr2c3.org.br";

final String JSON = "{ \"identdConjUniddRecbvl\": \"233\" }";

RequestBody body = RequestBody.create(JSON, MediaType.get("application/json; charset=utf-8"));

  Request request = new Request.Builder()
      .url(HOST + "/v1/financiadora/conjuntos-unidades-recebiveis")
      .addHeader("x-jws-signature", jws) //jws foi gerado anteriormente
      .post(body)
      .build();
  try (Response response = client.newCall(request).execute()) {
    return response.body().string();
  }
```

#### Exemplo de verificação de assinatura da resposta
```
String detachedJws = response.header("x-jws-signature");

String[] splittedJws = detachedJws.split("\\.");

String jws = splittedJws[0] + "." +  Base64.getEncoder().encodeToString(body.getBytes()) + "." + splittedJws[2];

SignedJWT signedJWT = SignedJWT.parse(jws);
JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) PUBLIC_KEY);
boolean isJwsOk = signedJWT.verify(verifier);
```

## Documentação oficial

* [Portal do desenvolvedor]("https://developer.cip-bancos.org.br/")