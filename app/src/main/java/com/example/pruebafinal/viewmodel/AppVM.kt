package com.example.pruebafinal.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.pruebafinal.util.Pantalla

class AppVM : ViewModel(){
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    var permisoUbicacionOk:() -> Unit = {}

    val pantallaActual = mutableStateOf(Pantalla.FORM)
    var onPermisoCamaraOk:() -> Unit = {}
}