import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat
import java.io.BufferedReader
import java.io.BufferedWriter

// Función para leer el archivo calificaciones.csv y devolver la lista de diccionarios ordenada por apellidos
fun leerCalificaciones(rutaEntrada: Path): List<Map<String, String>> {
    // Creamos una lista vacía mutable que se compondrá de diccionarios
    val alumnos = mutableListOf<Map<String, String>>()

    // Leemos el archivo usando Files.newBufferedReader
    val br: BufferedReader = Files.newBufferedReader(rutaEntrada)

    // Usamos .use para asegurarnos de que el BufferedReader se cierra correctamente
    br.use { reader ->
        // Leemos la cabecera del archivo y almacenamos los datos
        val headers = reader.readLine().split(";")

        // Iteramos cada línea y utilizamos headers y datos para crear el diccionario alumnos
        reader.forEachLine { line ->
            val datos = line.split(";")
            val alumno = headers.zip(datos).toMap() // Crear diccionario para cada alumno
            alumnos.add(alumno) // Crear lista de dicccionarios
        }
    }

    // Ordenar la lista de alumnos utilizando una lambda y el metodo sortedBy, utilizando el apellido como índice
    return alumnos.sortedBy { it["Apellidos"] }
}

// Función auxiliar para convertir el String en números Double con el formato adecuado
fun convertirANumeroSeguro(valor: String?): Double {
    return if (valor.isNullOrBlank()) {
        0.0  // Si el valor está vacío o es nulo, devolvemos 0 por defecto
    } else {
        valor.replace(",", ".").toDoubleOrNull() ?: 0.0 // Reemplazar coma por punto y convertir a Double
    }
}

// Función para calcular la nota final y agregarla al diccionario de cada alumno
fun calcularNotaFinal(alumnos: List<Map<String, String>>): List<MutableMap<String, String>> {
    val df = DecimalFormat("#.##") // Formateador que utilizamos para aseguranos de que se imprime correctamente

    return alumnos.map { alumno ->
        val alumnoMutable = alumno.toMutableMap()

        // Si las recuperaciones (Ordinario) existen, se sobreescribe en lugar de las notas originales
        val parcial1 = if (alumno["Ordinario1"].isNullOrBlank()) convertirANumeroSeguro(alumno["Parcial1"]) else convertirANumeroSeguro(alumno["Ordinario1"])
        val parcial2 = if (alumno["Ordinario2"].isNullOrBlank()) convertirANumeroSeguro(alumno["Parcial2"]) else convertirANumeroSeguro(alumno["Ordinario2"])
        val practicas = if (alumno["OrdinarioPracticas"].isNullOrBlank()) convertirANumeroSeguro(alumno["Practicas"]) else convertirANumeroSeguro(alumno["OrdinarioPracticas"])

        // Se calcula la nota final con el 30% de cada parcial y el 40% de las prácticas
        val notaFinal = parcial1 * 0.3 + parcial2 * 0.3 + practicas * 0.4

        // Añadir la nota final al diccionario del alumno
        alumnoMutable["NotaFinal"] = df.format(notaFinal) // Utilizamos el formateador

        alumnoMutable
    }
}

// Función para dividir la lista de diccionarios en dos, alumnos en aprobados y suspensos
fun dividirAprobadosYSuspensos(alumnos: List<Map<String, String>>): Pair<List<Map<String, String>>, List<Map<String, String>>> {
    val aprobados = mutableListOf<Map<String, String>>()
    val suspensos = mutableListOf<Map<String, String>>()

    alumnos.forEach { alumno ->
        val asistencia = convertirANumeroSeguro(alumno["Asistencia"]?.replace("%", ""))
        val parcial1 = if (alumno["Ordinario1"].isNullOrBlank()) convertirANumeroSeguro(alumno["Parcial1"]) else convertirANumeroSeguro(alumno["Ordinario1"])
        val parcial2 = if (alumno["Ordinario2"].isNullOrBlank()) convertirANumeroSeguro(alumno["Parcial2"]) else convertirANumeroSeguro(alumno["Ordinario2"])
        val practicas = if (alumno["OrdinarioPracticas"].isNullOrBlank()) convertirANumeroSeguro(alumno["Practicas"]) else convertirANumeroSeguro(alumno["OrdinarioPracticas"])
        val notaFinal = convertirANumeroSeguro(alumno["NotaFinal"])

        // Condiciones para aprobar:
        // - Asistencia >= 75%
        // - Cada parcial y práctica >= 4
        // - Nota final >= 5
        if (asistencia >= 75 && parcial1 >= 4 && parcial2 >= 4 && practicas >= 4 && notaFinal >= 5) {
            aprobados.add(alumno) // Si cumple las condiciones, va a la lista de aprobados
        } else {
            suspensos.add(alumno) // De lo contrario, va a la lista de suspensos
        }
    }

    return Pair(aprobados, suspensos)
}

fun main() {
    // Definimos ruta raíz con Path.of
    val rutaRaiz = Path.of("src")

    // Definimos la ruta de entrada del archivo calificacciones.csv
    val rutaEntrada = rutaRaiz.resolve("main")
        .resolve("resources")
        .resolve("ficheros")
        .resolve("calificaciones.csv")

    // Definimos la ruta de salida del archivo notasFinales.csv
    val rutaSalida = rutaRaiz.resolve("main")
        .resolve("resources")
        .resolve("ficheros")
        .resolve("notasFinales.csv")

    // Leemos calificaciones y ordenamos por apellidos
    val alumnos = leerCalificaciones(rutaEntrada)

    // Calculamos nota final para cada alumno
    val alumnosConNotaFinal = calcularNotaFinal(alumnos)

    // Se divide a los alumnos en aprobados y suspensos
    val (aprobados, suspensos) = dividirAprobadosYSuspensos(alumnosConNotaFinal)

    // Usamos BufferedWriter para escribir los resultados en el archivo de salida notasFinales.csv
    val bw: BufferedWriter = Files.newBufferedWriter(rutaSalida)

    bw.use { writer ->
        // Se escribe la lista de diccionarios de los alumnos aprobados
        writer.write("Aprobados:\n")
        aprobados.forEach { alumno ->
            writer.write("${alumno["Apellidos"]}, ${alumno["Nombre"]} - Nota Final: ${alumno["NotaFinal"]}\n")
        }

        // Se escribe la lista de diccionarios de los alumnos suspensos
        writer.write("\nSuspensos:\n")
        suspensos.forEach { alumno ->
            writer.write("${alumno["Apellidos"]}, ${alumno["Nombre"]} - Nota Final: ${alumno["NotaFinal"]}\n")
        }
    }

    println("Fichero de resultados generado: $rutaSalida")
}
