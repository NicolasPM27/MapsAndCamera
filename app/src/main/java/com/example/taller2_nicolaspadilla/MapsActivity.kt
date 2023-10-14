package com.example.taller2_nicolaspadilla
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.example.taller2_nicolaspadilla.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.io.IOException
import java.util.logging.Logger
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private val TAG = MapsActivity::class.java.name
        //Bogota Colombia
        const val lowerLeftLatitude = 4.490247911971994
        const val lowerLeftLongitude = -74.08836562217077
        const val upperRightLatitude = 4.764056787739693
        const val upperRightLongitude = -73.83254337782923
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var mMapInitialized = false

    lateinit var sensorManager: SensorManager
    lateinit var lightSensor: Sensor
    lateinit var lightSensorListener: SensorEventListener

    private var locationList : MutableList<LatLng> = mutableListOf()
    private var polyline: Polyline? = null
    private var marker: Marker? = null

    private lateinit var mGeocoder: Geocoder
    private var currentMarker: Marker? = null
    private var currentPolyline : Polyline? = null

    private val logger = Logger.getLogger(TAG)

    //Permission Variables
    private val LOCATION_PERMISSION_ID = 103
    //Location variables
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    var mCurrentLocation: Location? = null

    //Constraints
    private val REQUEST_CHECK_SETTINGS = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize the sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        //Initialize the sensor listener
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if(mMapInitialized) {
                    if (event.values[0] < 200) {
                        Log.i("MAPS", "DARK MAP " + event.values[0])
                        mMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                this@MapsActivity,
                                R.raw.style_black
                            )
                        )
                    } else {
                        Log.i("MAPS", "LIGHT MAP " + event.values[0])
                        mMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                this@MapsActivity,
                                R.raw.style_retro
                            )
                        )
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
        }
        //Initialize the geocoder
        mGeocoder = Geocoder(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Location variables initialization
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        //Permission request
        requestPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
            "El permiso es necesario para acceder a la localización",
            LOCATION_PERMISSION_ID
        )

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                logger.info("Location update received: $location")
                if(location!= null){
                    mCurrentLocation = location
                    val newLocation = LatLng(location.latitude, location.longitude)
                    locationList.add(newLocation)
                    if(locationList.size>1){
                        if(polyline != null){
                            polyline!!.remove()
                        }
                        val options = PolylineOptions()
                            .addAll(locationList)
                            .width(20f)
                            .color(R.color.BrightPurple)
                        polyline = mMap.addPolyline(options)
                    }
                    val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_person, null)
                    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable!!.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    val smallMarker = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
                    val groundZero = LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
                    if(binding.switch1.isChecked){
                        Log.d("SWITCH", "SWITCH IS CHECKED")
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    mCurrentLocation!!.latitude,
                                    mCurrentLocation!!.longitude
                                ), 20f
                            )
                        )
                    }
                    marker?.remove()
                    marker=  mMap.addMarker(MarkerOptions().position(groundZero)
                        .alpha(0.7f)
                        .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                    )

                }
            }
        }
        turnOnLocationAndStartUpdates()
        binding.textInputEditText.setOnEditorActionListener{v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEND){
                findAddress()
            }
            false
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMapInitialized = true
        mMap.setOnMapLongClickListener { latLng ->

            findAddress(latLng)
        }
    }
//-------------------Location Aware------------------------
override fun onResume() {
    super.onResume()
    startLocationUpdates()
    sensorManager.registerListener(
        lightSensorListener,
        lightSensor,
        SensorManager.SENSOR_DELAY_NORMAL
    )
}

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(lightSensorListener)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient!!.requestLocationUpdates(mLocationRequest!!, mLocationCallback!!, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
    }

    private fun requestPermission(
        context: Activity,
        permission: String,
        justification: String,
        idCode: Int
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                Toast.makeText(context, justification, Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(context, arrayOf(permission), idCode)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_ID -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(
                        this,
                        "Ya hay permiso para acceder a la localización",
                        Toast.LENGTH_LONG
                    ).show()
                    turnOnLocationAndStartUpdates()
                }
                return
            }
        }
    }

    private fun turnOnLocationAndStartUpdates() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            mLocationRequest!!
        )
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(
            this
        ) { locationSettingsResponse: LocationSettingsResponse? ->
            startLocationUpdates() // Todas las condiciones para recibiir localizaciones
        }
        task.addOnFailureListener(this) { e ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                CommonStatusCodes.RESOLUTION_REQUIRED ->                         // Location setttings are not satisfied, but this can be fixed by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                        val resolvable = e as ResolvableApiException
                        resolvable.startResolutionForResult(
                            this@MapsActivity,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error
                    }

                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == RESULT_OK) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(
                        this,
                        "Sin acceso a localización. Hardware deshabilitado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun findAddress() {
        val addressString = binding.textInputEditText.text.toString()
        if (addressString.isNotEmpty()) {
            if(currentMarker != null){
                currentMarker?.remove()
            }
            try {
                val addresses = mGeocoder.getFromLocationName(
                    addressString, 2,
                    lowerLeftLatitude, lowerLeftLongitude,
                    upperRightLatitude, upperRightLongitude
                )

                if (addresses != null && !addresses.isEmpty()) {
                    val addressResult = addresses[0]
                    val position = LatLng(addressResult.latitude, addressResult.longitude)
                    currentMarker=mMap.addMarker(
                        MarkerOptions().position(position)
                            .title(addressString)
                            .snippet(addressResult.getAddressLine(0))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    getDirections(LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude), position)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
                } else {
                    Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "La dirección está vacía", Toast.LENGTH_SHORT).show()
        }
    }
    private fun findAddress(latLng: LatLng) {
        val geocoder = Geocoder(this)
        if(currentMarker != null){
            currentMarker?.remove()
        }
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && !addresses.isEmpty()) {
                val address = addresses[0]
                val addressString = address.getAddressLine(0)
                currentMarker=mMap.addMarker(
                    MarkerOptions().position(latLng)
                        .title(addressString)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
                getDirections(LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude), latLng)
                // El valor de 'addressString' ahora contendrá la dirección basada en las coordenadas del clic largo.
            } else {
                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun getDirections(startLatLng: LatLng, endLatLng: LatLng) {
        val apiKey = "API_KEY" // Reemplaza con tu clave de API de Google Maps
        val context = GeoApiContext.Builder().apiKey(apiKey).build()

        val request = DirectionsApi.newRequest(context)
            .origin("${startLatLng.latitude},${startLatLng.longitude}")
            .destination("${endLatLng.latitude},${endLatLng.longitude}")
            .mode(TravelMode.DRIVING) // Puedes cambiar el modo de viaje si lo deseas
            .await()

        handleDirectionsResult(request)
    }

    private fun handleDirectionsResult(result: DirectionsResult) {
        // Procesar los resultados, como mostrar la ruta en el mapa
        if (result.routes.isNotEmpty()) {
            val overviewPolyline = result.routes[0].overviewPolyline
            val path = PolyUtil.decode(overviewPolyline.encodedPath)

            // Dibuja la ruta en el mapa utilizando los puntos del camino (path)
            currentPolyline?.remove()
            val polylineOptions = PolylineOptions()
                .addAll(path)
                .width(20f) // Ancho de la línea
                .color(R.color.Red) // Color de la línea
            currentPolyline= mMap.addPolyline(polylineOptions)
        }
    }


}