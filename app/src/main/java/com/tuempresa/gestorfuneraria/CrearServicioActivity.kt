package com.tuempresa.gestorfuneraria

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CrearServicioActivity : AppCompatActivity() {

    private val listaNombresPantalla = ArrayList<String>()
    private val listaEmailsOculta = ArrayList<String>()
    private lateinit var spinnerChoferes: Spinner
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_servicio)

        db = FirebaseFirestore.getInstance()

        val etDifunto = findViewById<EditText>(R.id.etDifunto)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etRetiro = findViewById<EditText>(R.id.etRetiro) // NUEVO CAMPO
        val etCementerio = findViewById<EditText>(R.id.etCementerio)
        val etFecha = findViewById<EditText>(R.id.etFecha)
        val etHora = findViewById<EditText>(R.id.etHora)
        val etObservaciones = findViewById<EditText>(R.id.etObservaciones)
        spinnerChoferes = findViewById(R.id.spinnerChoferes)
        val btnGuardar = findViewById<Button>(R.id.btnConfirmarServicio)

        try { findViewById<ImageButton>(R.id.btnVolverAsignar).setOnClickListener { finish() } } catch (e: Exception) {}

        configurarFechaHora(etFecha, etHora)
        cargarChoferesEnSpinner()

        btnGuardar.setOnClickListener {
            if (listaEmailsOculta.isEmpty()) {
                Toast.makeText(this, "No hay choferes disponibles", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validamos que estén las dos direcciones
            if (etDifunto.text.isEmpty() || etRetiro.text.isEmpty() || etCementerio.text.isEmpty()) {
                Toast.makeText(this, "Falta Difunto, Retiro o Cementerio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val indexSeleccionado = spinnerChoferes.selectedItemPosition
            val emailChofer = listaEmailsOculta[indexSeleccionado]
            val nombreChofer = listaNombresPantalla[indexSeleccionado]

            val nuevoServicio = hashMapOf(
                "difunto" to etDifunto.text.toString(),
                "telefonoContacto" to etTelefono.text.toString(),
                "direccion_retiro" to etRetiro.text.toString(), // GUARDAMOS EL RETIRO
                "cementerio" to etCementerio.text.toString(),
                "fecha" to etFecha.text.toString(),
                "hora" to etHora.text.toString(),
                "observaciones" to etObservaciones.text.toString(),
                "staff_email" to emailChofer,
                "staff_nombre" to nombreChofer,
                "estado" to "PENDIENTE", // Estado inicial
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("servicios").add(nuevoServicio)
                .addOnSuccessListener {
                    Toast.makeText(this, "Asignado correctamente ✅", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cargarChoferesEnSpinner() {
        db.collection("usuarios").whereEqualTo("rol", "STAFF").get()
            .addOnSuccessListener { documents ->
                listaNombresPantalla.clear()
                listaEmailsOculta.clear()
                for (doc in documents) {
                    val email = doc.id
                    val nombre = doc.getString("nombre") ?: "Sin Nombre"
                    val disponible = doc.getBoolean("disponible") ?: false
                    val estadoEmoji = if (disponible) "🟢" else "🔴"
                    listaNombresPantalla.add("$nombre $estadoEmoji")
                    listaEmailsOculta.add(email)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaNombresPantalla)
                spinnerChoferes.adapter = adapter
            }
    }

    private fun configurarFechaHora(etFecha: EditText, etHora: EditText) {
        val c = Calendar.getInstance()
        etFecha.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d -> etFecha.setText("$d/${m + 1}/$y") },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        etHora.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> etHora.setText(String.format("%02d:%02d", h, m)) },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
    }
}