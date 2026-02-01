package com.tuempresa.gestorfuneraria

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CrearServicioActivity : AppCompatActivity() {

    private val listaNombresPantalla = ArrayList<String>()
    private val listaEmailsOculta = ArrayList<String>()
    private lateinit var spinnerChoferes: Spinner
    private lateinit var db: FirebaseFirestore

    // Variable para saber si estamos editando (si es null, estamos creando)
    private var idEditar: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_servicio)

        db = FirebaseFirestore.getInstance()

        // Referencias
        val tvTitulo = findViewById<TextView>(R.id.tvTituloPantalla) // Asegúrate de tener un ID en el título del XML, si no, búscalo por texto o déjalo
        // Nota: Si no tienes ID en el título del XML (TextView "Nueva Asignación"), ignora la línea de arriba o agrégale ID.

        val etDifunto = findViewById<EditText>(R.id.etDifunto)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etRetiro = findViewById<EditText>(R.id.etRetiro)
        val etCementerio = findViewById<EditText>(R.id.etCementerio)
        val etFecha = findViewById<EditText>(R.id.etFecha)
        val etHora = findViewById<EditText>(R.id.etHora)
        val etObservaciones = findViewById<EditText>(R.id.etObservaciones)
        spinnerChoferes = findViewById(R.id.spinnerChoferes)
        val btnGuardar = findViewById<Button>(R.id.btnConfirmarServicio)

        try { findViewById<ImageButton>(R.id.btnVolverAsignar).setOnClickListener { finish() } } catch (e: Exception) {}

        configurarFechaHora(etFecha, etHora)

        // 1. VERIFICAR SI VENIMOS A EDITAR
        if (intent.hasExtra("ID_DOCUMENTO")) {
            idEditar = intent.getStringExtra("ID_DOCUMENTO")

            // Cambiamos textos visuales
            btnGuardar.text = "ACTUALIZAR SERVICIO ✏️"

            // Rellenamos los campos con lo que recibimos
            etDifunto.setText(intent.getStringExtra("difunto"))
            etTelefono.setText(intent.getStringExtra("telefono"))
            etRetiro.setText(intent.getStringExtra("retiro"))
            etCementerio.setText(intent.getStringExtra("cementerio"))
            etFecha.setText(intent.getStringExtra("fecha"))
            etHora.setText(intent.getStringExtra("hora"))
            etObservaciones.setText(intent.getStringExtra("obs"))
        }

        // 2. CARGAR CHOFERES (Y seleccionar el correcto si estamos editando)
        cargarChoferesEnSpinner(intent.getStringExtra("staff_email"))

        // 3. GUARDAR O ACTUALIZAR
        btnGuardar.setOnClickListener {
            if (listaEmailsOculta.isEmpty()) {
                Toast.makeText(this, "No hay choferes cargados", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (etDifunto.text.isEmpty() || etRetiro.text.isEmpty() || etCementerio.text.isEmpty()) {
                Toast.makeText(this, "Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val indexSeleccionado = spinnerChoferes.selectedItemPosition
            val emailChofer = listaEmailsOculta[indexSeleccionado]
            val nombreChofer = listaNombresPantalla[indexSeleccionado]

            val datosServicio = hashMapOf<String, Any>(
                "difunto" to etDifunto.text.toString(),
                "telefonoContacto" to etTelefono.text.toString(),
                "direccion_retiro" to etRetiro.text.toString(),
                "cementerio" to etCementerio.text.toString(),
                "fecha" to etFecha.text.toString(),
                "hora" to etHora.text.toString(),
                "observaciones" to etObservaciones.text.toString(),
                "staff_email" to emailChofer,
                "staff_nombre" to nombreChofer
                // No tocamos 'estado' ni 'timestamp' al editar para no reiniciar el flujo
            )

            if (idEditar != null) {
                // --- MODO EDICIÓN: ACTUALIZAR ---
                db.collection("servicios").document(idEditar!!).update(datosServicio)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Servicio Actualizado ✅", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // --- MODO CREACIÓN: NUEVO ---
                datosServicio["estado"] = "PENDIENTE" // Solo al crear es pendiente
                datosServicio["timestamp"] = System.currentTimeMillis()

                db.collection("servicios").add(datosServicio)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Asignado correctamente ✅", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al crear", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun cargarChoferesEnSpinner(emailPreseleccionado: String?) {
        db.collection("usuarios").whereEqualTo("rol", "STAFF").get()
            .addOnSuccessListener { documents ->
                listaNombresPantalla.clear()
                listaEmailsOculta.clear()

                var posicionASeleccionar = 0
                var contador = 0

                for (doc in documents) {
                    val email = doc.id
                    val nombre = doc.getString("nombre") ?: "Sin Nombre"
                    val disponible = doc.getBoolean("disponible") ?: false
                    val emoji = if (disponible) "🟢" else "🔴"

                    listaNombresPantalla.add("$nombre $emoji")
                    listaEmailsOculta.add(email)

                    // Si estamos editando y este es el chofer que ya tenía asignado, guardamos su posición
                    if (email == emailPreseleccionado) {
                        posicionASeleccionar = contador
                    }
                    contador++
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaNombresPantalla)
                spinnerChoferes.adapter = adapter

                // Magia: Seleccionamos automáticamente al chofer original
                spinnerChoferes.setSelection(posicionASeleccionar)
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