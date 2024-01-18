package com.example.pruebafinal.viewmodel

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.pruebafinal.util.Pantalla

class CameraAppViewModel : ViewModel() {
    val pantalla = mutableStateOf(Pantalla.FORM)
    // callbacks
    var onPermisoCamaraOk : () -> Unit = {}
    var onPermisoUbicacionOk: () -> Unit = {}
    // lanzador permisos
    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null
    fun cambiarPantallaFoto(){ pantalla.value = Pantalla.CAMARA }
    fun cambiarPantallaForm(){ pantalla.value = Pantalla.FORM }
}
