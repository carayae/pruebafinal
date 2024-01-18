package com.example.pruebafinal

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pruebafinal.exception.SinPermisoException
import com.example.pruebafinal.util.Pantalla
import com.example.pruebafinal.viewmodel.AppVM
import com.example.pruebafinal.viewmodel.CameraAppViewModel
import com.example.pruebafinal.viewmodel.FormRegistroVM
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime


class MainActivity : ComponentActivity() {

    val cameraAppVm: CameraAppViewModel by viewModels()
    val appVM: AppVM by viewModels()
    lateinit var cameraController: LifecycleCameraController


    val lanzadorPermisos = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        when {
            (it[android.Manifest.permission.ACCESS_FINE_LOCATION]
                ?: false) or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false) -> {
                Log.v("callback RequestMultiplePermissions", "permisoubicacion granted")
                cameraAppVm.onPermisoUbicacionOk()
            }

            (it[android.Manifest.permission.CAMERA] ?: false) -> {
                Log.v("callback RequestMultiplePermissions", "permisocamara granted")
                cameraAppVm.onPermisoCamaraOk()
            }

            else -> {
            }
        }
    }

    private fun setupCamara() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector =
            CameraSelector.DEFAULT_BACK_CAMERA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraAppVm.lanzadorPermisos = lanzadorPermisos
        setupCamara()

        setContent {
            AppUI(cameraController)
        }
    }
}



@Composable
fun AppUI(cameraController: CameraController) {
    val contexto = LocalContext.current
    val formRegistroVM: FormRegistroVM = viewModel()
    val cameraAppViewModel:CameraAppViewModel = viewModel()
    when(cameraAppViewModel.pantalla.value) {
        Pantalla.FORM -> {
            PantallaFormUI(
                formRegistroVM,
                tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantallaFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.CAMERA))
                },
                actualizarUbicacionOnClick = {
                    cameraAppViewModel.onPermisoUbicacionOk = {
                        conseguirUbicacion(contexto) {
                            formRegistroVM.latitud.value = it.latitude
                            formRegistroVM.longitud.value = it.longitude
                        }
                    }
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            )
        }
        Pantalla.CAMARA -> {
            PantallaFotoUI(formRegistroVM, cameraAppViewModel,
                cameraController)
        }
        else -> {
            Log.v("AppUI()", "when else, no debería entrar aquí")
        }
    }
}

@Composable
fun PantallaFormUI(
    formRegistroVM:FormRegistroVM,
    tomarFotoOnClick:() -> Unit = {},
    actualizarUbicacionOnClick:() -> Unit = {}
) {
    val contexto = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            label = { Text("Lugar de vacaciones") },
            value = formRegistroVM.lugar.value,
            onValueChange = {formRegistroVM.lugar.value = it},
            modifier = Modifier
                .fillMaxWidth().padding(horizontal = 10.dp)
        )
        Text("Fotografía del lugar:")
        Button(onClick = {
            tomarFotoOnClick()
        }) {
            Text("Tomar Fotografía")
        }
        formRegistroVM.fotoLugar.value?.also {
            Box(Modifier.size(200.dp, 100.dp)) {
                Image(
                    painter = BitmapPainter(uri2imageBitmap(it,
                        contexto)),
                    contentDescription = "Imagen Del lugar visitado ${formRegistroVM.lugar.value}"
                )
            }
        }
        Text("La ubicación es: lat: ${formRegistroVM.latitud.value} y long: ${formRegistroVM.longitud.value}")
        Button(onClick = {
            actualizarUbicacionOnClick()
        }) {
            Text("Actualizar Ubicación")
        }
        Spacer(Modifier.height(100.dp))
        MapaOsmUI(formRegistroVM.latitud.value,
            formRegistroVM.longitud.value)
    }
}

@Composable
fun PantallaFotoUI(formRegistroVM:FormRegistroVM, appViewModel:
CameraAppViewModel, cameraController: CameraController) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    Button(onClick = {
        tomarFotografia(
            cameraController,
            crearArchivoImagenPrivado(contexto),
            contexto
        ) {
            formRegistroVM.fotoLugar.value = it
            appViewModel.cambiarPantallaForm()
        }
    }) {
        Text("Tomar foto")
    }
}


/*@Composable
fun AppUI(appVM: AppVM,lanzadorPermisos: ActivityResultLauncher<Array<String>>){

    val contexto = LocalContext.current

    Column() {
        Button(onClick = {

            appVM.permisoUbicacionOk = {
                conseguirUbicacion(contexto) {
                    appVM.latitud.value = it.latitude
                    appVM.longitud.value = it.longitude
                }
            }

            lanzadorPermisos.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

        }) {
            Text("Conseguir Ubicación")
        }

        Text("Lat: ${appVM.latitud.value}  Long: ${appVM.longitud.value}")
        Spacer(Modifier.height(100.dp))
        AndroidView(
            factory = {
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    org.osmdroid.config.Configuration.getInstance().userAgentValue = contexto.packageName
                    controller.setZoom(15.0)
                }
            }, update = {
                it.overlays.removeIf{true}
                it.invalidate()

                val geoPoint = GeoPoint(appVM.latitud.value, appVM.longitud.value)
                it.controller.animateTo(geoPoint)

                val marcador = Marker(it)
                marcador.position = geoPoint
                marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                it.overlays.add(marcador)
            }
        )
    }
}*/


fun conseguirUbicacion(contexto:Context, onSuccess:(ubicacion:Location) -> Unit){

    try {
        val servicio = LocationServices.getFusedLocationProviderClient(contexto)
        val tarea = servicio.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
        tarea.addOnSuccessListener {
            onSuccess( it )
        }
    }catch (se: SecurityException){
        throw SinPermisoException("Sin permisos de ubicación")
    }
}

fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime
    .now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)

fun crearArchivoImagenPrivado(contexto: Context): File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${generarNombreSegunFechaHastaSegundo()}.jpg"
)

fun tomarFotografia(cameraController: CameraController, archivo:File,
                    contexto:Context, imagenGuardadaOk:(uri:Uri)->Unit) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(outputFileOptions,
        ContextCompat.getMainExecutor(contexto), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults:
                                      ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                            imagenGuardadaOk(it)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        })
}


fun uri2imageBitmap(uri:Uri, contexto:Context) =
    BitmapFactory.decodeStream(
        contexto.contentResolver.openInputStream(uri)
    ).asImageBitmap()

@Composable
fun MapaOsmUI(latitud:Double, longitud:Double) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            MapView(it).also {
                it.setTileSource(TileSourceFactory.MAPNIK)
                Configuration.getInstance().userAgentValue =
                    contexto.packageName
            }
        }, update = {
            it.overlays.removeIf { true }
            it.invalidate()
            it.controller.setZoom(18.0)
            val geoPoint = GeoPoint(latitud, longitud)
            it.controller.animateTo(geoPoint)
            val marcador = Marker(it)
            marcador.position = geoPoint
            marcador.setAnchor(Marker.ANCHOR_CENTER,
                Marker.ANCHOR_CENTER)
            it.overlays.add(marcador)
        }
    )
}