package com.example.cameraplayback.ui.view

import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.cameraplayback.BuildConfig
import com.example.cameraplayback.R
import com.example.cameraplayback.databinding.FragmentMainBinding
import com.example.cameraplayback.ui.view.camera.viewmodel.MainViewModel
import com.example.cameraplayback.utils.CryptoAES
import java.util.Objects
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var uid: String
    private lateinit var password: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        mainViewModel.fetchDevice(193113)
        uid = arguments?.getString("uid").toString()
        password = arguments?.getString("password").toString()
        addViewListener()
        addObserver()
        onCommonViewLoaded()
        return binding.root
    }

    private fun onCommonViewLoaded() {
//        mainViewModel.prepareAndSetUpCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addViewListener() {
        val bundle = Bundle().apply {
            putString("uid", uid)
            putString("password", password)
        }
        binding.playback.setOnClickListener {
            Log.d("ducpa", "${decrypt("pGq4mIuuMQikINtDNYN2YA==")}")
           findNavController().navigate(R.id.action_mainFragment_to_playbackCameraFragment, bundle)

        }
    }

    private fun addObserver() {
        mainViewModel.device.observe(viewLifecycleOwner, Observer { device ->
            // Xử lý kết quả trên main thread
            Log.d("MainActivity", "Device: $device")
            binding.tvNameCamera.text = uid
            binding.tvStatus.text = if (device.status == "0") "Trực tuyến" else "Ngoại tuyến"
            binding.tvPass.text = password
            mainViewModel.setDataCamera(device)
        })

        mainViewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            Log.e("MainActivity", "Error: $errorMessage")
        })
    }

    fun decrypt(textDecrypt: String?): String? {
        try {
            val iv = CryptoAES.decrypt3DES(BuildConfig.IV)
            val ivParameterSpec = IvParameterSpec(iv.toByteArray())
            val key = CryptoAES.decrypt3DES(BuildConfig.KEY_SECRET)
            val keyspec = SecretKeySpec(key.toByteArray(), "AES")
            val cipher = Cipher.getInstance(BuildConfig.CIPHER_INSTANCE)
            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivParameterSpec)
            return if (Build.VERSION.SDK_INT >= 26) {
                String(cipher.doFinal(java.util.Base64.getDecoder().decode(textDecrypt)))
            } else {
                String(cipher.doFinal(Base64.decode(textDecrypt, Base64.DEFAULT)))
            }
        } catch (exception: Exception) {
            Objects.requireNonNull(exception.message)?.let { Log.d("AES decrypt fail:", it) }
        }
        return ""
    }


}

