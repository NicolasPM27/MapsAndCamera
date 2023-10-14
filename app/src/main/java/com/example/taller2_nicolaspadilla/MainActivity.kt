package com.example.taller2_nicolaspadilla

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import  com.example.taller2_nicolaspadilla.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.frame1.setOnClickListener(){
            intent = Intent(this, CameraGalleryActivity::class.java)
            startActivity(intent)
        }
        binding.frame2.setOnClickListener(){
            intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

    }
}