package com.example.pruebafinal.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pruebafinal.Entity.Lugar

@Dao
interface LugarDao {

    @Query("SELECT * FROM lugar ORDER BY ordenVisita")
    fun getAllLugares() : List<Lugar>

    @Query("Select COUNT(*) from lugar")
    fun contar(): Int

    @Query("select * from lugar where id = :id")
    fun findLugarById(id:Int) : Lugar

    @Insert
    fun insertarLugar(lugar: Lugar): Long

    @Update
    fun actualizarLugar(lugar: Lugar)

    @Delete
    fun eliminarLugar(lugar: Lugar)
}