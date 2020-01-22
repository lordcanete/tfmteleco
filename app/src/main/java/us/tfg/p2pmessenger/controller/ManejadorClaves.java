package us.tfg.p2pmessenger.controller;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import us.tfg.p2pmessenger.util.Base64;

import static us.tfg.p2pmessenger.controller.Controlador.ALGORITMO_FIRMA;


/**
 * Para crear una entrada para el almacen de claves,
 * sera necesario indicar
 * CN=TFG Domingo ,
 * OU=Departamentode telematica,
 * O=Escuela Tecnica Superior de Ingenieria,
 * C=ES
 * (These refer to the subject's Common Name, Organizational Unit, Organization, and Country.)
 * en el campo del issuer.
 *
 * Clase con metodos estaticos para cifrar, descrifrar,
 * convertir a cadena y obtener el objeto clave contenido
 * en unaa cadena.
 */
public class ManejadorClaves
{
    /**
     * Constructor de la clase
     */
    public ManejadorClaves()
    {
    }

    /**
     * Devuelve la version hexadecimal de una clave
     * @param key
     * @return
     */
    public static String imprimirClave(Key key) {
        return "Public Key: " + getHexString(key.getEncoded());
    }

    /**
     * Convierte una cadena de bytes a hexadecimal
     * @param b bytes a convertir
     * @return resultado hexadecimal
     */
    private static String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    /**
     * Genera un objeto de clave a partir de los bytes
     * de una cadena.
     * @param claveCodificada contenido de la clave
     * @param tipo tipo de la clave (para no generar
     *             a partir de un certificado, una
     *             clave AES)
     * @return La clave publica en formato objeto
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey leeClavePublica(byte[] claveCodificada,String tipo) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PublicKey pk = null;
        X509EncodedKeySpec spec = new X509EncodedKeySpec(claveCodificada);
        KeyFactory kf = KeyFactory.getInstance(tipo);
        pk = kf.generatePublic(spec);
        return pk;
    }

    /**
     * Genera un objeto certificado X509 a partir de
     * una cadena de texto y el tipo
     * @param certificadoCodificado certificado codificado
     *                              en cadena de texto
     * @param tipo tipo del certificado
     * @return objeto certificado
     * @throws IOException
     * @throws CertificateException
     */
    public static X509Certificate leeCertificado(String certificadoCodificado,String tipo) throws IOException, CertificateException
    {
        CertificateFactory certFactory = CertificateFactory.getInstance(tipo);

        X509Certificate cert= null;
        InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(certificadoCodificado));
        cert = (X509Certificate) certFactory.generateCertificate(in);
        return cert;
    }

    /**
     * Genera a partir de un objeto certificado la representacion PEM del mismo, para
     * ser serializado y enviado por la red.
     * @param certificado
     * @return
     * @throws CertificateEncodingException
     */
    public static String imprimeCertificado(X509Certificate certificado) throws CertificateEncodingException
    {
        //String cert_begin = "-----BEGIN CERTIFICATE-----\n";
        //String end_cert = "-----END CERTIFICATE-----";

        byte[] codificado= certificado.getEncoded();

        String cert_cont= Base64.getEncoder().encodeToString(codificado);
        //return cert_begin + cert_cont + end_cert;
        return cert_cont;
    }

    /**
     * Genera un objeto de clave privada a partir de un
     * array de bytes. El tipo es para no crear un tipo
     * de clave a partir de otra.
     * @param claveCodificada clave que se desea obtener
     * @param tipo tipo de la clave pasada como parametro
     * @return el objeto clave privada
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey leeClavePrivada(byte[] claveCodificada,String tipo)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PrivateKey pk = null;
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(claveCodificada);
        KeyFactory kf = KeyFactory.getInstance(tipo);
        pk = kf.generatePrivate(privSpec);
        return pk;
    }

    /**
     * Genera una entrada para el KeyStore a partir de un
     * par de claves publica - privada. Se generara un
     * certificado a partir de la clave publica autofirmado.
     * @param cert
     * @param priv
     * @return
     * @throws GeneralSecurityException
     */
    public static KeyStore.PrivateKeyEntry entryFromKeys(PublicKey cert,PrivateKey priv) throws GeneralSecurityException
    {
        Date validFrom = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.YEAR, 10);
        Date validTo=c.getTime();
        String issuer = "CN=TFG Domingo , OU=Departamento de telematica, O=Escuela Tecnica Superior de Ingenieria, C=ES";

        String subject = issuer;
        String subjectAltName = null;
        String subjectIPAddress = null;//"127.0.0.1";

        X509Certificate x509Certificate = generateV3Certificate(
                new X500Principal(issuer), new X500Principal(subject),
                false, false, subjectAltName, subjectIPAddress,
                cert, priv, validFrom, validTo, ALGORITMO_FIRMA);
        x509Certificate.checkValidity(new Date());
        x509Certificate.verify(cert);

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = x509Certificate;
        //        KeyStore keyStoreSigningKey = createKeyStore(storePassword);

        KeyStore.PrivateKeyEntry entrada= new KeyStore.PrivateKeyEntry(priv,chain);

        //        KeyStore.ProtectionParameter protectionParameter=new KeyStore.PasswordProtection(keyPassword);
        //        keyStoreSigningKey.setEntry(alias,entrada,protectionParameter);
        //        return keyStoreSigningKey;
        return entrada;
    }

    /**
     * Encripta una clave simetrica (AES) con una asimetrica (RSA)
     * y la codifica en formato {@link Base64} para su posterior serializado
     * @param sk clave simetrica a codificar
     * @param pk clave publica con la que encriptar
     * @return la clave simetrica cifrada y codificada como {@link String}
     * @throws Exception
     */
    public static String encriptaClaveSimetrica(SecretKey sk,Key pk) throws Exception
    {
        byte[] enc=sk.getEncoded();
        //String simetricaBase64= Base64.getEncoder().encodeToString(enc);
        //return ManejadorClaves.encrypt(simetricaBase64,pk);

        Cipher encryptCipher = Cipher.getInstance(pk.getAlgorithm(),new BouncyCastleProvider());
        encryptCipher.init(Cipher.WRAP_MODE, pk);
        byte[] cipherText = encryptCipher.wrap(sk);

        return Base64.getEncoder().encodeToString(cipherText);

    }

    /**
     * Desencripta una clave simetrica (AES) con una asimetrica (RSA)
     * a partir del formato en {@link Base64} y lo convierte a un
     * objeto de clave simetrica.
     * @param claveSimetricaCifrada
     * @param clavePrivada
     * @param tipoClave
     * @return
     * @throws Exception
     */
    public static SecretKey desencriptaClaveSimetrica(String claveSimetricaCifrada,Key clavePrivada,String tipoClave) throws Exception
    {

        byte[] bytes = Base64.getDecoder().decode(claveSimetricaCifrada);

        Cipher decriptCipher = Cipher.getInstance(clavePrivada.getAlgorithm(),new BouncyCastleProvider());
        decriptCipher.init(Cipher.UNWRAP_MODE, clavePrivada);



        SecretKey sk = (SecretKey) decriptCipher.unwrap(bytes,tipoClave,Cipher.SECRET_KEY);
        return sk;
    }

    /**
     * Genera una nueva clave simetrica de la longitud proporcionada.
     * El algoritmo de la clave es AES
     * @param algoritmo
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static SecretKey generaClaveSimetrica(String algoritmo) throws NoSuchAlgorithmException
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algoritmo);
        keyGenerator.init(128,new SecureRandom());
        return keyGenerator.generateKey();
    }


    /**
     * Metodo de encriptacion. Recibe una cadena de texto claro y un objeto
     * Certificado o clave simetrica con la que cifrar. El resultado, cifrado,
     * estara expresado en {@link Base64}.
     *
     * Metodo encontrado en http://niels.nu/blog/2016/java-rsa.html
     * @param plainText
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static String encrypt(String plainText, Key publicKey) throws Exception {
        Cipher encryptCipher = Cipher.getInstance(publicKey.getAlgorithm(),new BouncyCastleProvider());
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(cipherText);
    }


    /**
     * Metodo de desencriptado. Recibe una cadena cifrada en
     * formato {@link Base64} y una clave privada o una clave
     * simetrica y devuelve una cadena de texto claro.
     *
     * Metodo encontrado en http://niels.nu/blog/2016/java-rsa.html
     * @param cipherText
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String decrypt(String cipherText, Key privateKey) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(cipherText);

        Cipher decriptCipher = Cipher.getInstance(privateKey.getAlgorithm(), new BouncyCastleProvider());
        decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(decriptCipher.doFinal(bytes), StandardCharsets.UTF_8);
    }



    // ------------------ obtenido de --- https://stackoverflow.com/
    // questions/6692671/how-to-generate-a-certificate-chain-to-be-
    // sent-as-an-argument-to-keystore-privat   --------------
    /**
     * Este metodo y los que quedan por debajo fueron tomados de
     * <a href="https://stackoverflow.com/questions/6692671/how-to-generate-a-certificate-chain-to-be-sent-as-an-argument-to-keystore-privat">
     *     aqui</a>
     * y tienen como finalidad generar un par de claves, y firmarlas,
     * de manera que se tenga un certificado a partir de ellas y poder
     * almacenar la pareja en un objeto de tipo {@link KeyStore}.
     *
     * Cada metodo adjunta su descripcion original en ingles
     * @param keyAlgorithm key algorithm
     * @param keyLength key length
     * @param signatureAlgorithm signature algorithm
     * @return KeyStore with the certificate and private key
     * @throws GeneralSecurityException
     */
    public static KeyStore.PrivateKeyEntry generaEntrada(String keyAlgorithm, int keyLength, String signatureAlgorithm)
            throws GeneralSecurityException
    {
        Date validFrom = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.YEAR, 10);
        Date validTo=c.getTime();
        String issuer = "CN=TFG Domingo , OU=Departamento de telematica, O=Escuela Tecnica Superior de Ingenieria, C=ES";
        return generaEntrada(keyAlgorithm,keyLength,signatureAlgorithm,issuer,validFrom,validTo);
    }

    /**
     * Create new Certificate Authority.
     * @param keyAlgorithm key algorithm
     * @param keyLength key length
    //     * @param storePassword store password
    //     * @param keyPassword private key password
    //     * @param alias alias in key store
     * @param signatureAlgorithm signature algorithm
     * @param issuer issuer
     * @param validFrom certificate validity first date
     * @param validTo certificate validity last date
     * @return KeyStore with the certificate and private key
     * @throws GeneralSecurityException
     */
    public static KeyStore.PrivateKeyEntry generaEntrada(String keyAlgorithm, int keyLength,
                                                         String signatureAlgorithm, String issuer, Date validFrom, Date validTo)
            throws GeneralSecurityException {
        String subject = issuer;
        String subjectAltName = null;
        String subjectIPAddress = null;//"127.0.0.1";
        KeyPair keyPair = generateKeyPair(keyAlgorithm, keyLength);
        X509Certificate x509Certificate = generateV3Certificate(
                new X500Principal(issuer), new X500Principal(subject),
                false, false, subjectAltName, subjectIPAddress,
                keyPair.getPublic(), keyPair.getPrivate(), validFrom, validTo, signatureAlgorithm);
        x509Certificate.checkValidity(new Date());
        x509Certificate.verify(keyPair.getPublic());

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = x509Certificate;
        //        KeyStore keyStoreSigningKey = createKeyStore(storePassword);

        KeyStore.PrivateKeyEntry entrada= new KeyStore.PrivateKeyEntry(keyPair.getPrivate(),chain);

        //        KeyStore.ProtectionParameter protectionParameter=new KeyStore.PasswordProtection(keyPassword);
        //        keyStoreSigningKey.setEntry(alias,entrada,protectionParameter);
        //        return keyStoreSigningKey;
        return entrada;
    }

    /**
     * Generate RCA 1024bit private and public keys pair
     *
     * @param algorithm the standard string name of the algorithm. i.e. "RSA"
     * @param keySize algorithm-specific metric, such as modulus length, specified in number of bits. i.e. 1024,2048,4096 for RSA
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair(String algorithm, int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        kpg.initialize(keySize);
        return kpg.generateKeyPair();
    }

    /**
     * Create new key store
     *
     * @param storePassword
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public static KeyStore createKeyStore(char[] storePassword, String tipo) throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
        // Instantiate KeyStore
        KeyStore keyStore = KeyStore.getInstance(tipo);

        // Load keystore
        try {
            keyStore.load(null, storePassword);
        } catch (IOException e) { //theoretically should never happen
            throw new KeyStoreException(e);
        }

        return keyStore;
    }


    /**
     * Creates a new key pair and self-signed certificate.
     * example params: keyAlgName = "RSA", sigAlgName = "SHA1WithRSA", keysize = 2048
     * Example: x500Name=new X500Name(commonName, organizationalUnit, organization, city, state, country);
     * @param keyStore
     * @param alias
     * @param keyPass
     * @param keyAlgName
     * @param sigAlgName
     * @param keysize
     * @param principal
     * @param startDate
     * @param validityDays
     * @return KeyStore object
     * @throws Exception
     */
    public static KeyStore generateKeyPair(KeyStore keyStore, String alias, char[] keyPass,
                                           String keyAlgName, String sigAlgName, int keysize,
                                           X500Principal principal, Date startDate, int validityDays)
            throws Exception {
        KeyStore keyStore2 = keyStore;
        if (keyStore2 == null) {
            keyStore2 = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore2.load(null, null);
        }

        if (keyStore2.containsAlias(alias)) {
            MessageFormat form = new MessageFormat("Key pair not generated, alias <alias> already exists");
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        X509Certificate[] chain = new X509Certificate[1];

        //CertAndKeyGen keyPair = new CertAndKeyGen(keyAlgName, sigAlgName);
        //keyPair.generate(keysize);
        //X500Name x500Name=new X500Name(commonName, organizationalUnit, organization, city, state, country);
        //chain[0] = keyPair.getSelfCertificate(x500Name, startDate, (long)validityDays*24*3600);
        KeyPair keyPair = generateKeyPair(keyAlgName, keysize);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(startDate);
        cal.add(Calendar.DATE, validityDays);
        Date endDate = cal.getTime();
        chain[0] = generateV3Certificate(principal, principal, false, true, null, null, keyPair.getPublic(), keyPair.getPrivate(), startDate, endDate, sigAlgName);

        keyStore2.setKeyEntry(alias, keyPair.getPrivate(), keyPass, chain);
        return keyStore2;
    }

    /**
     * Generate V3 Certificate.
     * @param issuer issuer
     * @param subject subject
     * @param useForServerAuth use for server auth flag
     * @param useForClientAuth use for client auth flag
     * @param subjectAltName subject alt name
     * @param subjectIPAssress subject IP address
     * @param publicKey public key
     * @param privateKey private key
     * @param from certificate validity first date
     * @param to certificate validity last date
     * @param signatureAlgorithm signature algorithm
     * @return X509Certificate object
     * @throws GeneralSecurityException GeneralSecurityException
     */
    public static X509Certificate generateV3Certificate(X500Principal issuer, X500Principal subject,
                                                        boolean useForServerAuth, boolean useForClientAuth,
                                                        String subjectAltName, String subjectIPAssress, PublicKey publicKey, PrivateKey privateKey,
                                                        Date from, Date to, String signatureAlgorithm) throws GeneralSecurityException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        certGen.setSerialNumber(new BigInteger(UUID.randomUUID().toString().replaceAll("-", ""), 16));
        certGen.setSubjectDN(subject);
        certGen.setIssuerDN(issuer);
        certGen.setNotBefore(from);
        certGen.setNotAfter(to);
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm(signatureAlgorithm);

        certGen.addExtension(X509Extensions.BasicConstraints, true, issuer.equals(subject) ? new BasicConstraints(1) : new BasicConstraints(false));
        if (!issuer.equals(subject)) {
            certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature
                    | KeyUsage.keyEncipherment | (useForServerAuth ? KeyUsage.keyCertSign | KeyUsage.cRLSign : 0)));
        }
        if (useForServerAuth) {
            certGen.addExtension(X509Extensions.ExtendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        }
        if (useForClientAuth) {
            certGen.addExtension(X509Extensions.ExtendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
        }
        if (subjectAltName != null) {
            certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new GeneralNames(
                    new GeneralName(GeneralName.rfc822Name, subjectAltName)));
        }
        if (subjectIPAssress != null) {
            certGen.addExtension(X509Extensions.SubjectAlternativeName, true, new GeneralNames(
                    new GeneralName(GeneralName.iPAddress, subjectIPAssress)));
        }

        return certGen.generate(privateKey);
    }

    // ------------------ fin de --- https://stackoverflow.com/
    // questions/6692671/how-to-generate-a-certificate-chain-to-be-
    // sent-as-an-argument-to-keystore-privat   --------------

    // --------- obtenido de http://niels.nu/blog/2016/java-rsa.html --------------

}
