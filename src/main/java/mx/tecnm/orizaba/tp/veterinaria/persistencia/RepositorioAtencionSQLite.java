package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import mx.tecnm.orizaba.tp.veterinaria.modelo.Propietario;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Procedimiento;
import mx.tecnm.orizaba.tp.veterinaria.modelo.CodigoClinico;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Estado;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Atencion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JDBC de atenciones y sus líneas. Guardar es <strong>atómico</strong>:
 * encabezado y líneas se escriben en una sola transacción (commit/rollback).
 */
public class RepositorioAtencionSQLite implements Repositorio<Atencion, Integer> {

    private final ConexionSQLite bd;
    private final Repositorio<Propietario, Integer> propietarios;
    private final Repositorio<Procedimiento, CodigoClinico> elementos;

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public RepositorioAtencionSQLite(ConexionSQLite bd, Repositorio<Propietario, Integer> propietarios,
                         Repositorio<Procedimiento, CodigoClinico> elementos) {
        this.bd = bd;
        this.propietarios = propietarios;
        this.elementos = elementos;
    }

    /** Inserta o actualiza la entidad mediante sentencia parametrizada. */
    @Override
    public void guardar(Atencion t) {
        bd.enTransaccion(c -> {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO atencion VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET propietario_id=excluded.propietario_id,fecha=excluded.fecha,estado=excluded.estado")) {
                s.setInt(1,t.id()); s.setInt(2,t.propietario().id()); s.setString(3,t.fecha().toString()); s.setString(4,t.estado().name()); s.executeUpdate();
            }
            try (PreparedStatement s = c.prepareStatement("DELETE FROM atencion_linea WHERE atencion_id=?")) { s.setInt(1,t.id()); s.executeUpdate(); }
            try (PreparedStatement s = c.prepareStatement("INSERT INTO atencion_linea VALUES(?,?,?)")) {
                for (Atencion.Linea l : t) { s.setInt(1,t.id()); s.setString(2,l.elemento().clave().toString()); s.setInt(3,l.unidades()); s.addBatch(); }
                s.executeBatch();
            }
            return null;
        });
    }

    /** Busca entidades por identidad o criterio, según el contrato. */
    @Override
    public Optional<Atencion> buscar(Integer id) {
        synchronized (bd.conexion()) {
            try (PreparedStatement s = bd.conexion().prepareStatement("SELECT * FROM atencion WHERE id=?")) {
                s.setInt(1,id);
                try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(reconstruir(bd.conexion(),rs)) : Optional.empty(); }
            } catch (SQLException e) { throw new PersistenciaException("Error al consultar atención",e); }
        }
    }

    private Atencion reconstruir(Connection c, ResultSet rs) throws SQLException {
        Propietario propietario = propietarios.buscar(rs.getInt("propietario_id")).orElseThrow(() -> new SQLException("Propietario inexistente"));
        Atencion t = new Atencion(rs.getInt("id"),propietario,LocalDate.parse(rs.getString("fecha")));
        Estado estado = Estado.valueOf(rs.getString("estado"));
        try (PreparedStatement s = c.prepareStatement("SELECT * FROM atencion_linea WHERE atencion_id=? ORDER BY clave_procedimiento")) {
            s.setInt(1,t.id());
            try (ResultSet lineas = s.executeQuery()) {
                while (lineas.next()) {
                    Procedimiento p = elementos.buscar(CodigoClinico.parse(lineas.getString("clave_procedimiento"))).orElseThrow(() -> new SQLException("Procedimiento inexistente"));
                    t.agregar(p,lineas.getInt("unidades"));
                }
            }
        }
        t.restaurarEstado(estado);
        return t;
    }

    /** Devuelve todas las entidades ordenadas por identidad. */
    @Override
    public List<Atencion> todos() {
        synchronized (bd.conexion()) {
            try (PreparedStatement s = bd.conexion().prepareStatement("SELECT * FROM atencion ORDER BY id"); ResultSet rs = s.executeQuery()) {
                List<Atencion> lista = new ArrayList<>();
                while (rs.next()) lista.add(reconstruir(bd.conexion(),rs));
                return lista;
            } catch (SQLException e) { throw new PersistenciaException("Error al consultar atenciones",e); }
        }
    }

    /** Elimina la entidad identificada y devuelve si existía. */
    @Override
    public boolean eliminar(Integer id) {
        return bd.enTransaccion(c -> {
            try (PreparedStatement s = c.prepareStatement("DELETE FROM atencion WHERE id=?")) { s.setInt(1,id); return s.executeUpdate() > 0; }
        });
    }
}
