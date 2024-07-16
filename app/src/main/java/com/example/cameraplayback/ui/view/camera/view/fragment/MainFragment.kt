package com.example.cameraplayback.ui.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.cameraplayback.R
import com.example.cameraplayback.databinding.FragmentMainBinding
import com.example.cameraplayback.ui.view.camera.viewmodel.MainViewModel

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        mainViewModel.fetchDevice(193113)
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
        binding.playback.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_playbackCameraFragment)
        }
    }

    private fun addObserver() {
        mainViewModel.device.observe(viewLifecycleOwner, Observer { device ->
            // Xử lý kết quả trên main thread
            Log.d("MainActivity", "Device: $device")
            binding.tvNameCamera.text = device.uid
            binding.tvStatus.text = if (device.status == "0") "Online" else "Offline"
            mainViewModel.setDataCamera(device)
        })

        mainViewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            Log.e("MainActivity", "Error: $errorMessage")
        })
    }
}

