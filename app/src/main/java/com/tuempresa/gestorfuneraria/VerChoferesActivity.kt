package com.tuempresa.gestorfuneraria

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class VerChoferesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_choferes) // Asegúrate de que este layout exista

        val btnVolver = findViewById<ImageButton>(R.id.btnVolverChoferes)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorChoferes)

        btnVolver.setOnClickListener { finish() }

        cargarChoferes(contenedor)
    }

    private fun cargarChoferes(contenedor: LinearLayout) {
        val db = FirebaseFirestore.getInstance()

        // Escuchamos en tiempo real para ver si cambian su estado (Disponible/Ocupado)
        db.collection("usuarios")
            .whereEqualTo("rol", "STAFF")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error al cargar", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                contenedor.removeAllViews()

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots.documents) {

                        // 1. OBTENER DATOS
                        val email = doc.id
                        val nombre = doc.getString("nombre") ?: "Sin Nombre Registrado"
                        val isDisponible = doc.getBoolean("disponible") ?: false

                        // 2. CREAR TARJETA (Visualmente bonito)
                        val card = CardView(this)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(0, 0, 0, 24)
                        card.layoutParams = params
                        card.radius = 16f
                        card.cardElevation = 6f
                        card.setContentPadding(32, 32, 32, 32)

                        // Contenido de la tarjeta
                        val layoutInterno = LinearLayout(this)
                        layoutInterno.orientation = LinearLayout.VERTICAL

                        // A. NOMBRE (Grande y Dorado)
                        val tvNombre = TextView(this)
                        tvNombre.text = nombre
                        tvNombre.textSize = 18f
                        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD)
                        tvNombre.setTextColor(Color.parseColor("#FFD700")) // Dorado
                        layoutInterno.addView(tvNombre)

                        // B. CORREO (Pequeño y Gris)
                        val tvEmail = TextView(this)
                        tvEmail.text = email
                        tvEmail.textSize = 14f
                        tvEmail.setTextColor(Color.DKGRAY)
                        tvEmail.setPadding(0, 4, 0, 16)
                        layoutInterno.addView(tvEmail)

                        // C. ESTADO (Con color)
                        val tvEstado = TextView(this)
                        if (isDisponible) {
                            tvEstado.text = "🟢 DISPONIBLE"
                            tvEstado.setTextColor(Color.parseColor("#00C853")) // Verde
                        } else {
                            tvEstado.text = "🔴 OCUPADO / NO DISPONIBLE"
                            tvEstado.setTextColor(Color.parseColor("#D32F2F")) // Rojo
                        }
                        tvEstado.textSize = 14f
                        tvEstado.setTypeface(null, android.graphics.Typeface.BOLD)
                        layoutInterno.addView(tvEstado)

                        // D. BOTÓN ELIMINAR (Opcional, por si despides a alguien)
                        val btnEliminar = Button(this)
                        btnEliminar.text = "Desvincular Chofer"
                        btnEliminar.textSize = 10f
                        btnEliminar.setBackgroundColor(Color.TRANSPARENT)
                        btnEliminar.setTextColor(Color.RED)
                        btnEliminar.gravity = Gravity.END
                        btnEliminar.setOnClickListener {
                            confirmarEliminacion(email)
                        }
                        layoutInterno.addView(btnEliminar)

                        card.addView(layoutInterno)
                        contenedor.addView(card)
                    }
                } else {
                    val aviso = TextView(this)
                    aviso.text = "No hay choferes registrados."
                    contenedor.addView(aviso)
                }
            }
    }

    private fun confirmarEliminacion(email: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Despedir empleado?")
            .setMessage("Se eliminará el acceso de $email al sistema permanentemente.")
            .setPositiveButton("ELIMINAR") { _, _ ->
                FirebaseFirestore.getInstance().collection("usuarios").document(email).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Chofer eliminado", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}