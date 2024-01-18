package com.example.pruebafinal.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FormRegistroVM : ViewModel(){
    val lugar = mutableStateOf("")
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    val fotoLugar = mutableStateOf<Uri?>(null)
}