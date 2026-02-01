package com.tuempresa.gestorfuneraria

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class RespaldoServicioActivity : AppCompatActivity() {

    private val listaNombres = ArrayList<String>()
    private val listaEmails = ArrayList<String>()
    private lateinit var db: FirebaseFirestore
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_respaldo_servicio)

        db = FirebaseFirestore.getInstance()
        spinner = findViewById(R.id.spinnerChoferRespaldo)

        val etDifunto = findViewById<EditText>(R.id.etDifuntoRespaldo)
        val etAcompanante = findViewById<EditText>(R.id.etAcompananteRespaldo)
        val etLugar = findViewById<EditText>(R.id.etLugarRespaldo)
        val etFecha = findViewById<EditText>(R.id.etFechaRespaldo)
        val etHora = findViewById<EditText>(R.id.etHoraRespaldo)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarRespaldo)

        cargarChoferes()
        configurarFechaHora(etFecha, etHora)

        btnGuardar.setOnClickListener {
            if (listaEmails.isEmpty()) return@setOnClickListener

            val difunto = etDifunto.text.toString()
            val lugar = etLugar.text.toString()

            if (difunto.isEmpty() || lugar.isEmpty()) {
                Toast.makeText(this, "Falta Difunto o Lugar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Datos del Chofer
            val index = spinner.selectedItemPosition
            val emailChofer = listaEmails[index]
            val nombreChofer = listaNombres[index]

            // CREAMOS EL RESPALDO
            val respaldo = hashMapOf(
                "difunto" to difunto,
                "cementerio" to lugar, // Usamos el campo cementerio para mantener compatibilidad
                "acompanante" to etAcompanante.text.toString(), // Guardamos al acompañante
                "fecha" to etFecha.text.toString(),
                "hora" to etHora.text.toString(),
                "staff_nombre" to nombreChofer,
                "staff_email" to emailChofer,
                "direccion_retiro" to "Registro Manual (Oficina)", // Dato por defecto

                // LA CLAVE MÁGICA:
                "estado" to "FINALIZADO ✅",

                "tipo_registro" to "RESPALDO_MANUAL", // Para saber que fue creado por admin
                "timestamp" to System.currentTimeMillis()
            )

            // Guardamos en la colección normal "servicios" para que salga en los reportes generales
            db.collection("servicios").add(respaldo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Respaldo Guardado Correctamente 📂", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cargarChoferes() {
        db.collection("usuarios").whereEqualTo("rol", "STAFF").get()
            .addOnSuccessListener { documents ->
                listaNombres.clear()
                listaEmails.clear()
                for (doc in documents) {
                    val nombre = doc.getString("nombre") ?: "Sin Nombre"
                    listaNombres.add(nombre)
                    listaEmails.add(doc.id)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaNombres)
                spinner.adapter = adapter
            }
    }

    private fun configurarFechaHora(etF: EditText, etH: EditText) {
        val c = Calendar.getInstance()
        etF.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d -> etF.setText("$d/${m + 1}/$y") },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        etH.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> etH.setText(String.format("%02d:%02d", h, m)) },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
    }
}