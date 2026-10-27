package mx.tecnm.orizaba.tp.veterinaria.persistencia;

import mx.tecnm.orizaba.tp.veterinaria.modelo.Propietario;
import mx.tecnm.orizaba.tp.veterinaria.modelo.Nivel;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio JDBC de propietarios. */
public class RepositorioPropietarioSQLite implements Repositorio<Propietario, Integer> {

    private final ConexionSQLite bd;
    private final MapeadorFila<Propietario> mapeador = rs -> new Propietario(rs.getInt("id"),
            rs.getString("nombre"), rs.getString("correo"), Nivel.valueOf(rs.getString("nivel")));

    /** Construye una instancia con los datos recibidos y valida sus invariantes. */
    public RepositorioPropietarioSQLite(ConexionSQLite bd) {
        this.bd = bd;
    }

    /** Inserta o actualiza la entidad mediante sentencia parametrizada. */
    @Override
    public void guardar(Propietario a) {
        bd.enTransaccion(c -> {
            try (PreparedStatement s = c.prepareStatement("INSERT INTO propietario VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET nombre=excluded.nombre,correo=excluded.correo,nivel=excluded.nivel")) {
                s.setInt(1,a.id()); s.setString(2,a.nombre()); s.setString(3,a.correo()); s.setString(4,a.nivel().name()); s.executeUpdate();
            }
            return null;
        });
    }

    /** Busca entidades por identidad o criterio, según el contrato. */
    @Override
    public Optional<Propietario> buscar(Integer id) {
        synchronized (bd.conexion()) {
            try (PreparedStatement s = bd.conexion().prepareStatement("SELECT * FROM propietario WHERE id=?")) {
                s.setInt(1,id);
                try (ResultSet rs = s.executeQuery()) { return rs.next() ? Optional.of(mapeador.mapear(rs)) : Optional.empty(); }
            } catch (SQLException e) { throw new PersistenciaException("Error al consultar propietario",e); }
        }
    }

    /** Devuelve todas las entidades ordenadas por identidad. */
    @Override
    public List<Propietario> todos() {
        synchronized (bd.conexion()) {
            try (PreparedStatement s = bd.conexion().prepareStatement("SELECT * FROM propietario ORDER BY id"); ResultSet rs = s.executeQuery()) {
                List<Propietario> lista = new ArrayList<>();
                while (rs.next()) lista.add(mapeador.mapear(rs));
                return lista;
            } catch (SQLException e) { throw new PersistenciaException("Error al consultar propietarios",e); }
        }
    }

    /** Elimina la entidad identificada y devuelve si existía. */
    @Override
    public boolean eliminar(Integer id) {
        return bd.enTransaccion(c -> {
            try (PreparedStatement s = c.prepareStatement("DELETE FROM propietario WHERE id=?")) { s.setInt(1,id); return s.executeUpdate() > 0; }
        });
    }
}
