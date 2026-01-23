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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_servicio)

        // 1. CONECTAMOS TODOS LOS ELEMENTOS DEL DISEÑO (FIND VIEW BY ID)
        val etDifunto = findViewById<EditText>(R.id.etDifunto)
        val etTelefono = findViewById<EditText>(R.id.etTelefono) // Nuevo
        val etCementerio = findViewById<EditText>(R.id.etCementerio)
        val etFecha = findViewById<EditText>(R.id.etFecha)
        val etHora = findViewById<EditText>(R.id.etHora) // Nuevo
        val spinnerStaff = findViewById<Spinner>(R.id.spinnerStaff)
        val etObservaciones = findViewById<EditText>(R.id.etObservaciones)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnAtras = findViewById<ImageButton>(R.id.btnAtras)

        // Botón atrás
        btnAtras.setOnClickListener { finish() }

        // 2. CARGAR LISTA DE EMPLEADOS (STAFF) EN EL SPINNER
        cargarStaffEnSpinner(spinnerStaff)

        // 3. CONFIGURAR EL CALENDARIO (DATE PICKER)
        etFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                // El mes empieza en 0, así que sumamos 1
                val fechaFormateada = "$day/${month + 1}/$year"
                etFecha.setText(fechaFormateada)
            }
            DatePickerDialog(this, dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 4. CONFIGURAR EL RELOJ (TIME PICKER) - NUEVO
        etHora.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                // Formato con ceros a la izquierda (ej: 09:05 en vez de 9:5)
                val horaFormateada = String.format("%02d:%02d", hour, minute)
                etHora.setText(horaFormateada)
            }
            // El 'true' al final es para usar formato 24 horas
            TimePickerDialog(this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 5. BOTÓN GUARDAR
        btnGuardar.setOnClickListener {
            val textoDifunto = etDifunto.text.toString()
            val textoCementerio = etCementerio.text.toString()
            val textoFecha = etFecha.text.toString()
            val textoHora = etHora.text.toString() // Nuevo
            val textoTelefono = etTelefono.text.toString() // Nuevo
            val textoObs = etObservaciones.text.toString()

            // Verificamos que se haya seleccionado un chofer
            val staffSeleccionado = if (spinnerStaff.selectedItem != null) spinnerStaff.selectedItem.toString() else ""

            if (textoDifunto.isNotEmpty() && textoCementerio.isNotEmpty() && textoFecha.isNotEmpty() && staffSeleccionado.isNotEmpty()) {

                // GUARDAR EN FIREBASE
                val db = FirebaseFirestore.getInstance()

                val nuevoServicio = hashMapOf(
                    "difunto" to textoDifunto,
                    "telefonoContacto" to textoTelefono, // Guardamos el teléfono
                    "cementerio" to textoCementerio,
                    "fecha" to textoFecha,
                    "hora" to textoHora, // Guardamos la hora
                    "staff_email" to staffSeleccionado,
                    "observaciones" to textoObs,
                    "estado" to "PENDIENTE"
                )

                db.collection("servicios")
                    .add(nuevoServicio)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Servicio Asignado Correctamente ✅", Toast.LENGTH_LONG).show()
                        finish() // Cierra la pantalla y vuelve al menú
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función auxiliar para llenar el Spinner
    private fun cargarStaffEnSpinner(spinner: Spinner) {
        val db = FirebaseFirestore.getInstance()
        val listaStaff = ArrayList<String>()

        db.collection("usuarios")
            .whereEqualTo("rol", "STAFF") // Solo traemos a los choferes
            .get()
            .addOnSuccessListener { documentos ->
                for (doc in documentos) {
                    listaStaff.add(doc.id) // El ID es el email
                }

                // Creamos el adaptador para mostrar la lista en el Spinner
                val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaStaff)
                spinner.adapter = adaptador
            }
    }
}