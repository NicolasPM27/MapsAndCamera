package com.example.taller2_nicolaspadilla

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.MediaController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller2_nicolaspadilla.databinding.ActivityCameraGalleryBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.util.logging.Logger

class CameraGalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraGalleryBinding
    private lateinit var pictureImagePath: Uri
    companion object {
        val TAG: String = CameraGalleryActivity::class.java.name
    }
    private val logger = Logger.getLogger(TAG)
    // Permission handler
    private val getSimplePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
        updateUI(it)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_camera_gallery)
        binding = ActivityCameraGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonTake.setOnClickListener{
            verifyPermissions(this, android.Manifest.permission.CAMERA, "El permiso es necesario para tomar fotos y videos")
        }
        binding.buttonGallery.setOnClickListener{
            val pickGalleryImage = Intent(Intent.ACTION_PICK)
            if(binding.switch1.isChecked){
                pickGalleryImage.type = "video/*"
                galleryActivityResultLauncher.launch(pickGalleryImage)
            }else if(!binding.switch1.isChecked){
                pickGalleryImage.type = "image/*"
                galleryActivityResultLauncher.launch(pickGalleryImage)
            }
        }
    }

    //-------------------------------PERMISSIONS--------------------------------
    // Verify permission to access contacts info
    private fun verifyPermissions(context: Context, permission: String, rationale: String) {
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                updateUI(true)
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // We display a snackbar with the justification for the permission, and once it disappears, we request it again.
                val snackbar = Snackbar.make(binding.root, rationale, Snackbar.LENGTH_LONG)
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar, event: Int) {
                        if (event == DISMISS_EVENT_TIMEOUT) {
                            getSimplePermission.launch(permission)
                        }
                    }
                })
                snackbar.show()
            }
            else -> {
                getSimplePermission.launch(permission)
            }
        }
    }
    // Update activity behavior and actions according to result of permission request
    private fun updateUI(permission : Boolean, ) {
        if(permission){
                if(binding.switch1.isChecked){
                    dipatchTakeVideoIntent()
                }else if(!binding.switch1.isChecked){
                    dipatchTakePictureIntent()
                }
                logger.info("Permission granted")
        }else{
            logger.warning("Permission denied")
        }
    }
    //---------------------------------PICTURE---------------------------------
   private fun dipatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Crear el archivo donde debería ir la foto
        var imageFile: File? = null
        try {
            imageFile = createImageFile()
        } catch (ex: IOException) {
            logger.warning(ex.message)
        }
        // Continua si el archivo ha sido creado exitosamente
        if (imageFile != null) {
            // Guardar un archivo: Ruta para usar con ACTION_VIEW intents
            pictureImagePath = FileProvider.getUriForFile(this,"com.example.android.fileprovider", imageFile)
            logger.info("Ruta: ${pictureImagePath}")
            Log.d("URI CREADA", pictureImagePath.toString())
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureImagePath)
            cameraActivityResultLauncher.launch(takePictureIntent)
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        //Crear un nombre de archivo de imagen
        val timeStamp: String = System.currentTimeMillis().toString()
        val imageFileName = "${timeStamp}.jpg"
        val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),imageFileName)
        return imageFile
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode == RESULT_OK) {
                // Handle camera result
                binding.videoView.visibility = View.INVISIBLE
                binding.imageView.visibility = View.VISIBLE
                binding.imageView.setImageURI(pictureImagePath)
                binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                binding.imageView.adjustViewBounds = true
                logger.info("Image capture successfully.")
            } else {
                logger.warning("Image capture failed.")
            }
        }

    //---------------------------------VIDEO---------------------------------

    private fun dipatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        // Set maximum video duration in seconds
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
        // Set video quality (0: low, 1: high)
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        videoActivityResultLauncher.launch(takeVideoIntent)
    }
    private val videoActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        val videoViewContainer= binding.videoView
        if (result.resultCode == RESULT_OK) {
            // Handle camera result
            binding.imageView.visibility = View.INVISIBLE
            binding.videoView.visibility = View.VISIBLE
            videoViewContainer.setVideoURI(result.data!!.data)
            videoViewContainer.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
            videoViewContainer.setMediaController(MediaController(this))
            videoViewContainer.start()
            videoViewContainer.setZOrderOnTop(true)
        }
        else {
            logger.warning("Video capture failed.")
        }
    }
//----------------------------------CAMERA-------------------------------------
private val galleryActivityResultLauncher =
    registerForActivityResult(
        ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle gallery result
            pictureImagePath = result.data!!.data!!
            val fileType = contentResolver.getType(pictureImagePath)
            if(fileType!!.startsWith("image/")) {
                binding.videoView.visibility = View.INVISIBLE
                binding.imageView.visibility = View.VISIBLE
                binding.imageView.setImageURI(pictureImagePath)
                binding.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                binding.imageView.adjustViewBounds = true
                logger.info("Image capture successfully.")
            }else if(fileType.startsWith("video/")){
                val videoViewContainer= binding.videoView
                binding.imageView.visibility = View.INVISIBLE
                videoViewContainer.visibility = View.VISIBLE
                videoViewContainer.setVideoURI(result.data!!.data)
                videoViewContainer.foregroundGravity = View.TEXT_ALIGNMENT_CENTER
                videoViewContainer.setMediaController(MediaController(this))
                videoViewContainer.start()
                videoViewContainer.setZOrderOnTop(true)
                Logger.getLogger(TAG).info("Video capture successfully.")
            }
        } else {
            logger.warning("Image capture failed.")
        }
    }
}