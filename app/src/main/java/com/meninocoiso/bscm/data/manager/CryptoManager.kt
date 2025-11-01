package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val keysetAlias = "bscm_auth_keyset"
    private val prefsFileName = "bscm_auth_encrypted_prefs"
    
    private val aead: Aead by lazy {
        initializeTink()
    }
    
    private fun initializeTink(): Aead {
        try {
            // Inicializa Tink
            AeadConfig.register()
            
            // Cria ou recupera o keyset protegido pelo Android Keystore
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, keysetAlias, prefsFileName)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://bscm_master_key")
                .build()
                .keysetHandle
            
            return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Falha ao inicializar criptografia", e)
        }
    }
    
    /**
     * Criptografa dados usando AES-GCM via Tink
     */
    fun encrypt(data: String): String {
        return try {
            val ciphertext = aead.encrypt(data.toByteArray(), null)
            Base64.encodeToString(ciphertext, Base64.DEFAULT)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Falha ao criptografar dados", e)
        }
    }
    
    /**
     * Descriptografa dados usando AES-GCM via Tink
     */
    fun decrypt(encryptedData: String): String {
        return try {
            val ciphertext = Base64.decode(encryptedData, Base64.DEFAULT)
            val plaintext = aead.decrypt(ciphertext, null)
            String(plaintext)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Falha ao descriptografar dados", e)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Dados criptografados inválidos", e)
        }
    }
    
    /**
     * Verifica se os dados podem ser descriptografados (validação de integridade)
     */
    fun isValidEncryptedData(encryptedData: String): Boolean {
        return try {
            decrypt(encryptedData)
            true
        } catch (e: Exception) {
            false
        }
    }
}