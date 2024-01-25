package com.example.pruebafinal.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class Lugar( @PrimaryKey(autoGenerate = true) val id:Int,
                 var nombre:String,
                 var ordenVisita:Int,
                 var imagenReferenciaId: String,
                 var latitud:String,
                 var longitud:String,
                 var costoAlojamiento:String,
                 var costoTransporte:String,
                 var comentariosAdicionales:String? ) : Serializable