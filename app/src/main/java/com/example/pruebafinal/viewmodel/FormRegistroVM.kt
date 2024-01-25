package com.example.pruebafinal.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FormRegistroVM : ViewModel(){
    val nombre = mutableStateOf("")
    val latitud = mutableStateOf("")
    val longitud = mutableStateOf("")
    val fotoLugar =  mutableStateOf<Uri?>(null)
    var ordenVisita = mutableStateOf("")
    var imagenReferenciaId = mutableStateOf("")
    var costoAlojamiento = mutableStateOf("")
    var costoTransporte = mutableStateOf("")
    var comentariosAdicionales = mutableStateOf("")
}