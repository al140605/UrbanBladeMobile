package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.urbanblade.mobile.core.validation.FormLimits
import com.urbanblade.mobile.ui.components.UrbanFormDangerZone
import com.urbanblade.mobile.ui.components.UrbanFormSection
import com.urbanblade.mobile.ui.components.UrbanFormSheet
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.formBlockerText
import com.urbanblade.mobile.ui.components.rememberUrbanFormState

/** Valores con los que se abre el formulario de cliente al editar. */
data class ClientFormInitial(val name: String, val email: String, val telefono: String, val notas: String)

/** Lo que el formulario entrega al guardar; [password] solo viene al crear y [notas] solo al editar. */
data class ClientFormValues(val name: String, val email: String, val telefono: String?, val password: String?, val notas: String?)

/** Errores por campo del formulario de cliente (null = campo bien). */
internal data class ClientFormErrors(val name: String?, val email: String?, val password: String?) {
    /** Nombres de los campos con problema, en el orden en que aparecen. */
    val missing: List<String>
        get() = listOfNotNull("Nombre".takeIf { name != null }, "Correo".takeIf { email != null }, "Contraseña".takeIf { password != null })
}

private val EMAIL_SHAPE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** [password] es null al editar (no se pide); al crear exige 8 caracteres como el servidor. */
internal fun validateClientForm(name: String, email: String, password: String?): ClientFormErrors = ClientFormErrors(
    name = if (name.isBlank()) "Escribe el nombre." else null,
    email = when {
        email.isBlank() -> "Escribe el correo."
        !EMAIL_SHAPE.matches(email.trim()) -> "Escribe un correo válido, como nombre@correo.com."
        else -> null
    },
    password = when {
        password == null -> null
        password.isEmpty() -> "Escribe una contraseña."
        password.length < 8 -> "Usa al menos 8 caracteres."
        else -> null
    }
)

/**
 * Formulario de cliente, el mismo para crear ([initial] null) y para editar. Al editar no pide
 * contraseña y sí notas; al crear es al revés.
 */
@Composable
fun ClientFormSheet(
    initial: ClientFormInitial?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (ClientFormValues) -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteNote: String? = null
) {
    val editing = initial != null
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var telefono by remember { mutableStateOf(initial?.telefono.orEmpty()) }
    var notas by remember { mutableStateOf(initial?.notas.orEmpty()) }
    var password by remember { mutableStateOf("") }
    val form = rememberUrbanFormState()
    val errors = validateClientForm(name, email, if (editing) null else password)

    UrbanFormSheet(
        eyebrow = "Clientes",
        title = if (editing) "Editar cliente" else "Nuevo cliente",
        primaryLabel = if (editing) "Guardar cambios" else "Crear cliente",
        onSubmit = {
            onSubmit(
                ClientFormValues(
                    name = name.trim(),
                    email = email.trim(),
                    telefono = telefono.trim().takeIf { it.isNotEmpty() },
                    password = password.takeIf { !editing },
                    notas = if (editing) notas.trim() else null
                )
            )
        },
        onDismiss = onDismiss,
        saving = saving,
        blocker = formBlockerText(errors.missing),
        error = error
    ) {
        UrbanFormSection("Datos del cliente", icon = Icons.Default.Person) {
            UrbanTextField(
                name, { name = it.take(FormLimits.NAME_MAX) }, "Nombre completo", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Person, placeholder = "Ej. Carlos Mendoza",
                error = form.error("name", errors.name), onBlur = { form.touch("name") },
                capitalization = KeyboardCapitalization.Words
            )
            UrbanTextField(
                email, { email = it }, "Correo", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Email, placeholder = "carlos@correo.com",
                helper = "Aquí recibe sus confirmaciones.",
                error = form.error("email", errors.email), onBlur = { form.touch("email") },
                keyboardType = KeyboardType.Email
            )
            UrbanTextField(
                telefono, { telefono = it }, "Teléfono (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Phone, placeholder = "10 dígitos",
                keyboardType = KeyboardType.Phone
            )
            if (editing) {
                UrbanTextField(
                    notas, { notas = it }, "Notas (opcional)", Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Notes, placeholder = "Preferencias, alergias, lo que conviene recordar",
                    capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default, minLines = 3
                )
            }
        }
        if (!editing) {
            UrbanFormSection("Acceso", icon = Icons.Default.Lock) {
                UrbanTextField(
                    password, { password = it }, "Contraseña", Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Lock, placeholder = "Mínimo 8 caracteres",
                    helper = "Podrá cambiarla desde su cuenta.",
                    error = form.error("password", errors.password), onBlur = { form.touch("password") },
                    isPassword = true
                )
            }
        }
        if (editing && onDelete != null) {
            UrbanFormDangerZone("cliente", onDelete, enabled = !saving, note = deleteNote ?: "Se quita de la lista y no se puede deshacer.")
        }
    }
}
