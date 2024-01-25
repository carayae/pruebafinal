package com.example.pruebafinal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pruebafinal.Entity.Lugar

@Database(entities = [Lugar::class], version = 4)
abstract class AppDataBase : RoomDatabase() {

    abstract fun lugarDao(): LugarDao

    companion object{
        @Volatile
        private var BASE_DATOS: AppDataBase? = null

        fun getInstance(contexto: Context) : AppDataBase{
            return BASE_DATOS ?: synchronized(this){
                Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDataBase::class.java,
                    "lugares4.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { BASE_DATOS = it }
            }
        }
    }
}