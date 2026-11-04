package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;
import mx.tecnm.orizaba.tp.veterinaria.modelo.CodigoClinico;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositorio JDBC de procedimientos (tabla de herencia única: columna {@code tipo}).
 * <ul>
 *   <li>Escritura: {@code switch} con <em>pattern matching</em> exhaustivo sobre la jerarquía sellada.</li>
 *   <li>Lectura: mapa de reconstructores (lambdas) indexado por el nombre del subtipo.</li>
 * </ul>
 */
public class RepositorioProcedimientoSQLite implements Repositorio<Procedimiento, CodigoClinico> {

    /** Interfaz funcional privada anidada. */
    @FunctionalInterface
    private interface Reconstructor {
        Procedimiento crear(CodigoClinico clave, String nombre, double precio, String atributo);
    }

    private static final Map<String, Reconstructor> RECONSTRUCTORES = Map.of(
            "Vacuna", (c, n, p, a) -> new Procedimiento.Vacuna(c, n, p, a),
            "Consulta", (c, n, p, a) -> new Procedimiento.Consulta(c, n, p, Integer.parseInt(a)),
            "Cirugia", (c, n, p, a) -> new Procedimiento.Cirugia(c, n, p, Integer.parseInt(a))
    );

    private final ConexionSQLite bd;
    private final MapeadorFila<Procedimiento> mapeador = rs -> {
        Reconstructor r = RECONSTRUCTORES.get(rs.getString("tipo"));
        if (r == null) {
            throw new SQLException("Tipo desconocido: " + rs.getString("tipo"));
        }
        return r.crear(CodigoClinico.parse(rs.getString("clave")), rs.getString("nombre"),
                rs.getDouble("precio_base"), rs.getString("atributo"));
    };

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public RepositorioProcedimientoSQLite(ConexionSQLite bd) {
        this.bd = bd;
    }

    /** Atributo específico del subtipo, obtenido con pattern matching. */
    static String atributoDe(Procedimiento e) {
        return switch (e) {
            case Procedimiento.Vacuna x -> x.biologico();
            case Procedimiento.Consulta x -> Integer.toString(x.prioridad());
            case Procedimiento.Cirugia x -> Integer.toString(x.horasQuirofano());
        };
    }

    /** Inserta o actualiza la entidad mediante sentencia parametrizada. */
    @Override
    public void guardar(Procedimiento e) {
        guardarTodos(List.of(e));
    }

    /** Guarda el catálogo recibido en una sola transacción; un fallo revierte todas las filas. */
    public void guardarTodos(List<Procedimiento> elementos) {
        bd.enTransaccion(c -> {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO procedimiento VALUES(?,?,?,?,?) ON CONFLICT(clave) DO UPDATE SET tipo=excluded.tipo,nombre=excluded.nombre,precio_base=excluded.precio_base,atributo=excluded.atributo")) {
                for (Procedimiento e : elementos) {
                    s.setString(1,e.clave().toString()); s.setString(2,e.tipo()); s.setString(3,e.nombre()); s.setDouble(4,e.precioBase()); s.setString(5,atributoDe(e));
                    s.addBatch();
                }
                s.executeBatch();
            }
            return null;
        });
    }

    /** Busca entidades por identidad o criterio, según el contrato. */
    @Override
    public Optional<Procedimiento> buscar(CodigoClinico clave) {
        synchronized (bd.conexion()) {
            try (PreparedStatement s = bd.conexion().prepareStatement("SELECT * FROM procedimiento WHERE clave=?")) {
                s.setString(1,clave.toString());
                try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(mapeador.mapear(rs)) : Optional.empty(); }
            } catch (SQLException e) { throw new PersistenciaException("Error al consultar procedimiento",e); }
        }
    }

    /** Devuelve todas las entidades ordenadas por identidad. */
    @Override
    public List<Procedimiento> todos() {
        synchronized (bd.conexion()) {
            try (PreparedStatement s = bd.conexion().prepareStatement("SELECT * FROM procedimiento ORDER BY clave"); ResultSet rs = s.executeQuery()) {
                List<Procedimiento> lista = new ArrayList<>();
                while (rs.next()) lista.add(mapeador.mapear(rs));
                return lista;
            } catch (SQLException e) { throw new PersistenciaException("Error al consultar catálogo",e); }
        }
    }

    /** Elimina la entidad identificada y devuelve si existía. */
    @Override
    public boolean eliminar(CodigoClinico clave) {
        return bd.enTransaccion(c -> {
            try (PreparedStatement s = c.prepareStatement("DELETE FROM procedimiento WHERE clave=?")) { s.setString(1,clave.toString()); return s.executeUpdate() > 0; }
        });
    }
}
