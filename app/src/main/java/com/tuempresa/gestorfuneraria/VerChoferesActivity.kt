package com.tuempresa.gestorfuneraria

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // <--- Importante para la alerta
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class VerChoferesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_servicios_admin) // Reusamos el layout base

        val contenedor = findViewById<LinearLayout>(R.id.contenedorGlobal)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolverAdmin)

        btnVolver.setOnClickListener { finish() }

        // Cambiamos el título visualmente (Opcional, pero se ve mejor)
        // Puedes buscar el TextView del título si quieres, o dejarlo así.

        cargarChoferesEnVivo(contenedor)
    }

    private fun cargarChoferesEnVivo(contenedor: LinearLayout) {
        val db = FirebaseFirestore.getInstance()

        db.collection("usuarios")
            .whereEqualTo("rol", "STAFF")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                contenedor.removeAllViews()

                if (snapshots != null) {
                    for (doc in snapshots) {
                        val email = doc.id
                        val disponible = doc.getBoolean("disponible") ?: false
                        val urlFoto = doc.getString("fotoUrl") ?: ""

                        // Inflamos la tarjeta
                        val vista = LayoutInflater.from(this).inflate(R.layout.item_chofer, contenedor, false)

                        val tvEmail = vista.findViewById<TextView>(R.id.tvEmailChofer)
                        val tvTexto = vista.findViewById<TextView>(R.id.tvEstadoTexto)
                        val bolita = vista.findViewById<android.view.View>(R.id.viewEstadoColor)
                        val imgPerfil = vista.findViewById<android.widget.ImageView>(R.id.imgFotoPerfil)

                        tvEmail.text = email

                        // Colores
                        if (disponible) {
                            tvTexto.text = "DISPONIBLE"
                            tvTexto.setTextColor(android.graphics.Color.parseColor("#00C853"))
                            bolita.background.setTint(android.graphics.Color.parseColor("#00C853"))
                        } else {
                            tvTexto.text = "OCUPADO / NO TURNO"
                            tvTexto.setTextColor(android.graphics.Color.RED)
                            bolita.background.setTint(android.graphics.Color.RED)
                        }

                        // Cargar Foto
                        if (urlFoto.isNotEmpty()) {
                            imgPerfil.clearColorFilter()
                            imgPerfil.imageTintList = null
                            Glide.with(this).load(urlFoto).circleCrop().into(imgPerfil)
                        } else {
                            imgPerfil.setImageResource(R.drawable.baseline_account_circle_24)
                            imgPerfil.setColorFilter(android.graphics.Color.BLACK)
                        }

                        // --- NUEVO: BORRAR USUARIO CON CLICK LARGO ---
                        vista.setOnLongClickListener {
                            mostrarAlertaBorrar(email)
                            true // "true" significa que consumimos el evento (para que no haga click normal también)
                        }
                        // ---------------------------------------------

                        contenedor.addView(vista)
                    }
                }
            }
    }

    // Función para mostrar la pregunta de seguridad
    private fun mostrarAlertaBorrar(emailChofer: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar a este Chofer?")
            .setMessage("Estás a punto de eliminar a:\n$emailChofer\n\nSe borrarán sus datos y no aparecerá en la lista.")
            .setPositiveButton("ELIMINAR") { _, _ ->
                borrarChoferDeFirebase(emailChofer)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun borrarChoferDeFirebase(email: String) {
        val db = FirebaseFirestore.getInstance()

        // Borramos el documento de la colección "usuarios"
        db.collection("usuarios").document(email).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Chofer eliminado correctamente 🗑️", Toast.LENGTH_SHORT).show()
                // No hace falta recargar manual, el SnapshotListener lo hará solo
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }
}