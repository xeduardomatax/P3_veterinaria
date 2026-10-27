package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import mx.tecnm.orizaba.tp.veterinaria.modelo.Propietario;
import mx.tecnm.orizaba.tp.veterinaria.modelo.CatalogoProcedimientos;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Atencion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Respaldo binario mediante serialización de objetos (complementa a SQLite).
 * Escribe y lee en el mismo orden (véase {@code java_4_modelo/t7_persistencia/a_serializacion/Gusano_07}).
 */
public final class Respaldo {

    public static final String ENCABEZADO = "Respaldo Clinica v1";

    private Respaldo() {
    }

    /** Contenido recuperado de un respaldo. */
    public record Contenido(String encabezado, LocalDateTime fecha, CatalogoProcedimientos catalogo,
                            List<Propietario> propietarios, List<Atencion> registros) {
    }

    /** Exporta catálogo, propietarios y atenciones en el orden del formato binario. */
    public static void exportar(Path archivo, CatalogoProcedimientos catalogo, Collection<Propietario> propietarios,
                                Collection<Atencion> registros) throws IOException {
        try (ObjectOutputStream s = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(archivo)))) {
            s.writeUTF(ENCABEZADO); s.writeObject(LocalDateTime.now()); s.writeObject(catalogo);
            s.writeObject(new ArrayList<>(propietarios)); s.writeObject(new ArrayList<>(registros));
        }
    }

    /** Lee un respaldo y valida su encabezado antes de reconstruir el contenido. */
    @SuppressWarnings("unchecked")
    public static Contenido importar(Path archivo) throws IOException, ClassNotFoundException {
        try (ObjectInputStream s = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(archivo)))) {
            String encabezado = s.readUTF();
            if (!ENCABEZADO.equals(encabezado)) throw new IOException("El archivo no es un respaldo de la clínica");
            LocalDateTime fecha = (LocalDateTime) s.readObject();
            CatalogoProcedimientos catalogo = (CatalogoProcedimientos) s.readObject();
            List<Propietario> propietarios = (List<Propietario>) s.readObject();
            List<Atencion> registros = (List<Atencion>) s.readObject();
            catalogo.forEach(p -> mx.tecnm.orizaba.tp.veterinaria.modelo.CodigoClinico.registrar(p.clave()));
            return new Contenido(encabezado,fecha,catalogo,List.copyOf(propietarios),List.copyOf(registros));
        }
    }
}
