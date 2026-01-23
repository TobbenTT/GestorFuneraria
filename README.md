# 👑 Gestor Funeraria - Rey David

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-RealTime-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Android Studio](https://img.shields.io/badge/Android_Studio-Iguana-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white)
![Google Maps](https://img.shields.io/badge/Google_Maps-API-4285F4?style=for-the-badge&logo=google-maps&logoColor=white)
![WhatsApp](https://img.shields.io/badge/WhatsApp-Integration-25D366?style=for-the-badge&logo=whatsapp&logoColor=white)

👋 **¡Bienvenido al repositorio de Gestor Funeraria Rey David!**

**Rey David** es una aplicación móvil nativa diseñada para optimizar la logística y gestión de servicios funerarios. A diferencia de una agenda tradicional, esta solución integra **Base de Datos en Tiempo Real** y **Geolocalización** para coordinar al personal administrativo y a los conductores de carrozas fúnebres de manera eficiente.

El sistema implementa una **Arquitectura Cliente-Servidor** robusta utilizando **Firebase Firestore** como backend NoSQL y **Firebase Auth** para la seguridad basada en roles (RBAC).

→ ¡Dale una ⭐ a este repositorio si te gusta el proyecto!

---

## ✨ Características Principales

### 🔐 Seguridad y Roles
* **Login Diferenciado:** Acceso seguro con validación de credenciales.
* **Roles Jerárquicos:**
    * 👮‍♂️ **Administrador:** Crea servicios, gestiona la flota, elimina usuarios y visualiza todo el historial.
    * 🚗 **Staff (Chofer):** Solo visualiza sus tareas asignadas y reporta estados.
* **Bloqueo Remoto:** Sistema de seguridad que impide el acceso a empleados desvinculados aunque tengan la contraseña.

### 📡 Gestión en Tiempo Real
* **Asignación Instantánea:** Los servicios creados por el administrador aparecen inmediatamente en el dispositivo del chofer sin necesidad de recargar.
* **Estados de Servicio:** Flujo de trabajo controlado (Pendiente -> Finalizado ✅).
* **Limpieza Automática:** Algoritmo inteligente que detecta y archiva servicios finalizados hace más de 30 días para optimizar el rendimiento.

### 🗺️ Integración Externa
* **Navegación GPS:** Botón directo "IR AL MAPA" que conecta con **Google Maps/Waze** usando las coordenadas o dirección del cementerio.
* **Comunicación Directa:** Integración con la **API de WhatsApp** para contactar a los familiares del difunto con un solo toque.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Kotlin (Android Nativo).
* **Backend:** Google Firebase (Firestore Database).
* **Autenticación:** Firebase Authentication.
* **Imágenes:** Glide (Gestión de fotos de perfil en la nube).
* **Diseño:** XML Layouts con Material Design Components.

---

## ⚠️ Instalación y Compilación

Este proyecto utiliza servicios de Google que requieren claves privadas. Para compilar este proyecto localmente:

1.  Clona el repositorio.
2.  Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
3.  Descarga tu propio archivo `google-services.json`.
4.  Colócalo en la carpeta `app/` del proyecto.
5.  Sincroniza Gradle y ejecuta en un emulador o dispositivo físico.

---

## 👨‍💻 Autor

Desarrollado por **[David Cabezas]** - Estudiante de Ingeniería en Informática (INACAP).
* 🌐 Portafolio: [tobben.site](https://tobben.site)
