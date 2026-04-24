package io.github.narendrakumar2259.networkinspector.crypto

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

data class CaKeyPair(
    val privateKey: PrivateKey,
    val certificate: X509Certificate
)

class CertificateAuthority(private val context: Context) {

    companion object {
        private const val CA_CERT_FILE = "ca_certificate.crt"
        private const val CA_KEY_FILE = "ca_private_key.key"
    }

    // Returns existing CA or generates a new one
    fun getOrCreateCA(): CaKeyPair {
        val existing = loadCA()
        if (existing != null) return existing

        val newCA = generateCA()
        saveCA(newCA)
        return newCA
    }

    fun generateCA(): CaKeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()

        val name = X500Name("CN=Network Inspector CA")
        val serialNumber = BigInteger(64, SecureRandom())
        val startDate = Date(System.currentTimeMillis())
        val expiryDate = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)

        val builder = JcaX509v3CertificateBuilder(
            name, serialNumber, startDate, expiryDate, name, keyPair.public
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        return CaKeyPair(keyPair.private, certificate)
    }

    fun generateSiteCertificate(
        caCertificate: X509Certificate,
        caPrivateKey: PrivateKey,
        domain: String
    ): CaKeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val keyPair = generator.generateKeyPair()

        val issuer = X500Name(caCertificate.subjectX500Principal.name)
        val subject = X500Name("CN=$domain")
        val serialNumber = BigInteger(64, SecureRandom())
        val startDate = Date(System.currentTimeMillis())
        val expiryDate = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)

        val builder = JcaX509v3CertificateBuilder(
            issuer, serialNumber, startDate, expiryDate, subject, keyPair.public
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPrivateKey)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        return CaKeyPair(keyPair.private, certificate)
    }

    private fun saveCA(caKeyPair: CaKeyPair) {
        // Save certificate bytes to file
        File(context.filesDir, CA_CERT_FILE).writeBytes(caKeyPair.certificate.encoded)

        // Save private key bytes to file
        File(context.filesDir, CA_KEY_FILE).writeBytes(caKeyPair.privateKey.encoded)
    }

    private fun loadCA(): CaKeyPair? {
        val certFile = File(context.filesDir, CA_CERT_FILE)
        val keyFile = File(context.filesDir, CA_KEY_FILE)

        // If either file doesn't exist, no saved CA
        if (!certFile.exists() || !keyFile.exists()) return null

        return try {
            // Rebuild certificate from bytes
            val certFactory = CertificateFactory.getInstance("X.509")
            val certificate = certFactory.generateCertificate(
                FileInputStream(certFile)
            ) as X509Certificate

            // Rebuild private key from bytes
            val keyBytes = keyFile.readBytes()
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

            CaKeyPair(privateKey, certificate)
        } catch (e: Exception) {
            null
        }
    }

    fun installCA() {
        val caKeyPair = getOrCreateCA()

        // Save to Downloads folder
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val certFile = File(downloadsDir, "network_inspector_ca.crt")
        certFile.writeBytes(caKeyPair.certificate.encoded)
    }
}