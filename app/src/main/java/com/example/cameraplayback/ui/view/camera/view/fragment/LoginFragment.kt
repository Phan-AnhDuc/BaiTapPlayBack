package com.example.cameraplayback.ui.view.camera.view.fragment

import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.cameraplayback.R
import com.example.cameraplayback.databinding.FragmentLoginBinding
import com.example.cameraplayback.databinding.FragmentMainBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.uid.text = Editable.Factory.getInstance().newEditable("VNTTA-017631-MVEGE")
        binding.password.text = Editable.Factory.getInstance().newEditable("qsL6eC8n")
        binding.buttonLogin.setOnClickListener {
            val uid = binding.uid.text.toString()
            val password = binding.password.text.toString()
            val bundle = Bundle().apply {
                putString("uid", uid)
                putString("password", password)
            }
            findNavController().navigate(R.id.action_login_to_mainFragment, bundle)
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}