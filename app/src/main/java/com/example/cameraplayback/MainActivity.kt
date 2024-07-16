package com.example.cameraplayback

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraplayback.databinding.LayoutActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: LayoutActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}