package com.example.pruebafinal

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pruebafinal.Entity.Lugar
import com.example.pruebafinal.db.AppDataBase
import com.example.pruebafinal.exception.SinPermisoException
import com.example.pruebafinal.util.Pantalla
import com.example.pruebafinal.viewmodel.AppVM
import com.example.pruebafinal.viewmodel.CameraAppViewModel
import com.example.pruebafinal.viewmodel.FormRegistroVM
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        val lugar = Lugar(0, "prueba1", 1, "R.drawable.no_imagen",
            "2222.33", "34555.44", "80000", "30000", null )
        val lugar2 = Lugar(0,"testing 2", 2, "R.drawable.no_imagen",
            "2222.33", "34555.44", "90000", "40000", null )

        lifecycleScope.launch(Dispatchers.IO) {
            val lugarDao = AppDataBase.getInstance(this@MainActivity).lugarDao()
            lugarDao.insertarLugar(lugar)
            lugarDao.insertarLugar(lugar2)
        }

        val stringsLanguaje = mapOf("listaLuagares" to resources.getString( R.string.lista_lugares ),
            "agregarLugar" to resources.getString( R.string.agregar_lugar ),
            "lugarVacaciones" to resources.getString( R.string.lugar_vacaciones ),
            "imagen" to resources.getString( R.string.imagen ),
            "latitud" to resources.getString( R.string.latitud ),
            "longitud" to resources.getString( R.string.longitud ),
            "orden" to resources.getString( R.string.orden ),
            "costo_alojamiento" to resources.getString( R.string.costo_alojamiento ),
            "costo_traslados" to resources.getString( R.string.costo_traslados ),
            "comentarios" to resources.getString( R.string.comentarios )
        )


        setContent {
            AppUI(cameraController, stringsLanguaje)
        }
    }
}


@Composable
fun ListadoLugaresVacacionesUI(cameraAppViewModel:CameraAppViewModel,stringsLanguaje: Map<String, String>){
    val (seleccion, setSeleccion) = remember {
        mutableStateOf<Lugar?>(null) }

    val contexto = LocalContext.current
    val(lugares, setLugares) = remember { mutableStateOf(emptyList<Lugar>()) }

    LaunchedEffect(Unit){
        withContext( Dispatchers.IO ){
            val dao = AppDataBase.getInstance(contexto).lugarDao()
            setLugares( dao.getAllLugares() )
        }
    }

    if( seleccion == null ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(lugares) {
                ItemLugarUI(it) {
                    setSeleccion(it)
                }
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.height(80.dp))
                    Button(onClick = {
                        cameraAppViewModel.cambiarPantallaForm()
                    }) {
                        Text(stringsLanguaje.get("agregarLugar").toString())
                    }
                }
            }
        }
    } else {
        DetalleLugarUI(seleccion) {
            setSeleccion(null)
        }
    }
}

@Composable
fun ItemLugarUI(lugar:Lugar, onClick:() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Spacer(modifier = Modifier.width(30.dp))
/*        Image(
            painter = painterResource(id = lugar.imagenReferenciaId),
            contentDescription = lugar.nombre
        )*/
        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(lugar.nombre )
            Text("Costo de alojamiento por noche: " + lugar.costoAlojamiento)
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Ubicacion"
            )
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Editar"
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar"
            )
        }


    }

}

@Composable
fun DetalleLugarUI(lugar:Lugar, onClose: () -> Unit) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(lugar.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Text(": ${lugar.costoAlojamiento}")
        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = { onClose() }) {
            Text("Cerrar")
        }
    }
}


@Composable
fun AppUI(cameraController: CameraController, stringsLanguaje: Map<String, String>) {
    val contexto = LocalContext.current
    val formRegistroVM: FormRegistroVM = viewModel()
    val cameraAppViewModel:CameraAppViewModel = viewModel()

    //cameraAppViewModel.cambiarPantallaLista()
    when(cameraAppViewModel.pantalla.value) {
        Pantalla.LISTA -> {
            ListadoLugaresVacacionesUI(cameraAppViewModel,stringsLanguaje)
        }
        Pantalla.FORM -> {
            PantallaFormUI(
                stringsLanguaje,
                formRegistroVM,
                tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantallaFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.CAMERA))
                },
                irAListaOnClick = {
                    cameraAppViewModel.cambiarPantallaLista()

                },
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
fun PantallaFormUI(stringsLanguaje: Map<String, String>,
    formRegistroVM:FormRegistroVM,
    tomarFotoOnClick:() -> Unit = {},
    irAListaOnClick:() -> Unit = {}
) {
    val contexto = LocalContext.current
    val lugarDao = AppDataBase.getInstance(contexto).lugarDao()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(onClick = {
            irAListaOnClick()
        }) {
            Text( stringsLanguaje.get("listaLuagares").toString() )
        }
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("lugarVacaciones").toString()
                ) },
            value = formRegistroVM.nombre.value,
            onValueChange = {formRegistroVM.nombre.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("imagen").toString()
                ) },
            value = formRegistroVM.imagenReferenciaId.value,
            onValueChange = {formRegistroVM.imagenReferenciaId.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("latitud").toString()
                ) },
            value = formRegistroVM.latitud.value,
            onValueChange = {formRegistroVM.latitud.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("longitud").toString()
                ) },
            value = formRegistroVM.longitud.value,
            onValueChange = {formRegistroVM.longitud.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("orden").toString()
                ) },
            value = formRegistroVM.ordenVisita.value,
            onValueChange = {formRegistroVM.ordenVisita.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("costo_alojamiento").toString()
                ) },
            value = formRegistroVM.costoAlojamiento.value,
            onValueChange = {formRegistroVM.costoAlojamiento.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )

        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("costo_traslados").toString()
                ) },
            value = formRegistroVM.costoTransporte.value,
            onValueChange = {formRegistroVM.costoTransporte.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            label = {
                Text(stringsLanguaje.get("comentarios").toString()
                ) },
            value = formRegistroVM.comentariosAdicionales.value,
            onValueChange = {formRegistroVM.comentariosAdicionales.value = it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )


        Spacer(Modifier.height(10.dp))
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
                    contentDescription = "Imagen Del lugar visitado: ${formRegistroVM.nombre.value}"
                )
            }
        }
        //Text("La ubicación es: lat: ${formRegistroVM.latitud.value} y long: ${formRegistroVM.longitud.value}")
        Button(onClick = {

            var lugarGuardar = Lugar(0, formRegistroVM.nombre.value,
                formRegistroVM.ordenVisita.value.toInt(),
                formRegistroVM.imagenReferenciaId.value,
                formRegistroVM.latitud.value,
                formRegistroVM.longitud.value,
                formRegistroVM.costoAlojamiento.value,
                formRegistroVM.costoTransporte.value,
                formRegistroVM.comentariosAdicionales.value)
            lugarDao.insertarLugar(lugarGuardar)
        }) {
            Text("Guardar")
        }

        //MapaOsmUI(formRegistroVM.latitud.value, formRegistroVM.longitud.value)



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