package com.urbanblade.mobile.ui.account

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Actividad 09 (Mi cuenta, déficit 2): las contraseñas escritas en Mi cuenta usaban
 * `rememberSaveable`, que las copia al estado guardado de la actividad (Bundle). Ese estado lo
 * conserva el sistema al rotar la pantalla o cuando Android mata la app en segundo plano, así que
 * una contraseña a medio escribir sale de la memoria de la pantalla.
 *
 * Es una prueba estática: revisa el código fuente de la interfaz y falla si algún campo secreto
 * vuelve a declararse con `rememberSaveable`.
 */
class SecretFieldsNotSavedTest {

    private val secretNames = Regex("pass|secret|contrase|clave|\\bpin\\b|cvv|current|confirmation", RegexOption.IGNORE_CASE)
    private val declaration = Regex("""va[rl]\s+(\w+)\s+by\s+rememberSaveable""")

    private fun uiSources(): List<File> {
        val root = File("src/main/java/com/urbanblade/mobile/ui")
        assertTrue("no se encontró ${root.absolutePath}", root.isDirectory)
        return root.walkTopDown().filter { it.extension == "kt" }.toList()
    }

    /** Campos que guardan una contraseña o secreto en el estado guardado de la pantalla. */
    private fun savedSecrets(): List<String> = uiSources().flatMap { file ->
        file.readLines().mapIndexedNotNull { i, line ->
            val name = declaration.find(line)?.groupValues?.get(1)
            if (name != null && secretNames.containsMatchIn(name)) "${file.name}:${i + 1}  $name" else null
        }
    }

    @Test
    fun `ninguna contrasena se guarda en el estado guardado de la pantalla`() {
        val found = savedSecrets()
        assertTrue("campos secretos con rememberSaveable:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun `el detector reconoce las cuatro declaraciones que habia en Mi cuenta`() {
        val sample = listOf(
            "    var current by rememberSaveable { mutableStateOf(\"\") }",
            "    var password by rememberSaveable { mutableStateOf(\"\") }",
            "    var confirmation by rememberSaveable { mutableStateOf(\"\") }",
            "    var secret by rememberSaveable { mutableStateOf(\"\") }"
        )
        val hits = sample.count { line -> declaration.find(line)?.groupValues?.get(1)?.let { secretNames.containsMatchIn(it) } == true }
        assertTrue(hits == 4)
    }

    @Test
    fun `los campos normales si pueden usar rememberSaveable`() {
        val line = "    var name by rememberSaveable(profile) { mutableStateOf(profile.name) }"
        assertTrue(declaration.find(line) == null || !secretNames.containsMatchIn(declaration.find(line)!!.groupValues[1]))
    }
}
