package de.mickare.schematicbooks.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.plugin.java.JavaPlugin;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.Rotation;
import lombok.Getter;

public class SqliteSchematicEntityDataStore implements WorldSchematicEntityStore {

  private static final String TABLE_ENTITIES = "`schematic_entities`";

  private @Getter final WorldSchematicEntityCache cache;
  private final SQLiteDataSource dataSource;

  public SqliteSchematicEntityDataStore(WorldSchematicEntityCache cache, File file)
      throws Exception {
    Preconditions.checkNotNull(cache);
    Preconditions.checkState(SQLiteJDBCLoader.initialize());

    this.cache = cache;

    JavaPlugin.getPlugin(SchematicBooksPlugin.class).getLogger()
        .info("jdbc:sqlite:" + file.getAbsolutePath().replace("/./", "/"));

    this.dataSource = new SQLiteDataSource();
    this.dataSource.setUrl("jdbc:sqlite:" + file.getAbsolutePath().replace("/./", "/"));
    // this.dataSource.setEncoding("UTF8");

    install();
  }

  private void executeUpdate(Connection con, String sql) throws SQLException {
    try (Statement stmt = con.createStatement()) {
      stmt.executeUpdate(sql);
    }
  }

  private void install() throws SQLException {

    try (Connection con = dataSource.getConnection()) {
      executeUpdate(con,
          "CREATE TABLE IF NOT EXISTS " + TABLE_ENTITIES + "(" //
              + " id INTEGER PRIMARY KEY AUTOINCREMENT," //
              + " name VARCHAR(255)," //
              + " rotation TINYINT," //
              + " entities SQLITE_BLOB," //
              + " posStart_x INT, posStart_y INT, posStart_z INT ,"//
              + " posEnd_x INT , posEnd_y INT , posEnd_z INT ," //
              + " timestamp INTEGER," //
              + " owner_msb INTEGER, owner_lsb INTEGER" //
              + ")");

      executeUpdate(con, "CREATE INDEX IF NOT EXISTS index_pos ON " + TABLE_ENTITIES
          + " (posStart_x, posStart_z, posEnd_x, posEnd_z)");

      executeUpdate(con, "CREATE INDEX IF NOT EXISTS index_owner ON " + TABLE_ENTITIES
          + " (owner_msb, owner_lsb)");

    }

  }

  private SchematicEntity from(ResultSet rs) throws SQLException, IOException {
    long id = rs.getLong("id");
    String name = rs.getString("name");
    Rotation rotation = Rotation.values()[rs.getByte("rotation") % Rotation.values().length];



    byte[] entitiesBytes = rs.getBytes("entities");
    int entitiesCount = (int) (entitiesBytes.length / 16);
    Set<UUID> entities = Sets.newHashSetWithExpectedSize(entitiesCount);
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(entitiesBytes))) {
      for (int i = 0; i < entitiesCount; ++i) {
        entities.add(new UUID(in.readLong(), in.readLong()));
      }
    }

    IntVector posStart =
        new IntVector(rs.getInt("posStart_x"), rs.getInt("posStart_y"), rs.getInt("posStart_z"));
    IntVector posEnd =
        new IntVector(rs.getInt("posEnd_x"), rs.getInt("posEnd_y"), rs.getInt("posEnd_z"));

    long timestamp = rs.getLong("timestamp");
    UUID owner = new UUID(rs.getLong("owner_msb"), rs.getLong("owner_lsb"));

    SchematicEntity entity = cache.computeIfAbsent(id);
    entity.set(name, rotation, posStart, posEnd, entities, timestamp, owner);
    return entity;
  }

  @Override
  public SchematicEntity load(long id) throws DataStoreException {
    try (Connection con = dataSource.getConnection()) {
      try (PreparedStatement ps =
          con.prepareStatement("SELECT * FROM " + TABLE_ENTITIES + " WHERE id = ?")) {
        ps.setLong(1, id);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return from(rs);
          }
        }
      }
    } catch (SQLException | IOException e) {
      throw new DataStoreException("Failed to load " + id, e);
    }
    return null;
  }

  @Override
  public Set<SchematicEntity> load(ChunkPosition pos) throws DataStoreException {
    final IntVector startPos = pos.getMinPoint();
    final IntVector endPos = pos.getMaxPoint();
    final Set<SchematicEntity> result = Sets.newHashSet();

    try (Connection con = dataSource.getConnection()) {
      try (PreparedStatement ps = con.prepareStatement("SELECT * FROM " + TABLE_ENTITIES
          + " WHERE posStart_x <= ? AND posStart_z <= ? AND posEnd_x >= ? AND posEnd_z >= ?")) {

        ps.setInt(1, endPos.getX());
        ps.setInt(2, endPos.getZ());
        ps.setInt(3, startPos.getX());
        ps.setInt(4, startPos.getZ());

        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            result.add(from(rs));
          }
        }
      }
    } catch (SQLException | IOException e) {
      throw new DataStoreException("Failed to load " + pos.toString(), e);
    }
    return result;
  }

  @Override
  public Set<SchematicEntity> list(UUID owner) throws DataStoreException {
    final UUID _owner = owner != null ? owner : new UUID(0, 0);

    final Set<SchematicEntity> result = Sets.newHashSet();
    try (Connection con = dataSource.getConnection()) {
      try (PreparedStatement ps = con.prepareStatement(
          "SELECT * FROM " + TABLE_ENTITIES + " WHERE owner_msb = ? AND owner_lsb = ?")) {

        ps.setLong(1, _owner.getMostSignificantBits());
        ps.setLong(2, _owner.getLeastSignificantBits());

        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            result.add(from(rs));
          }
        }
      }
    } catch (SQLException | IOException e) {
      throw new DataStoreException("Failed to list for owner " + _owner.toString(), e);
    }
    return result;
  }


  @Override
  public int remove(Collection<Long> entityIds) throws DataStoreException {
    if (entityIds.isEmpty()) {
      return 0;
    }
    try (Connection con = dataSource.getConnection()) {
      try (PreparedStatement ps = con.prepareStatement(//
          "DELETE FROM " + TABLE_ENTITIES + " WHERE id = ?")) {
        for (Long id : entityIds) {
          ps.setLong(1, id);
          ps.addBatch();
        }
        int[] res = ps.executeBatch();
        int sum = 0;
        for (int i = 0; i < res.length; ++i) {
          sum += res[i];
        }
        return sum;
      }
    } catch (SQLException e) {
      String ids =
          String.join(",", entityIds.stream().map(l -> l.toString()).collect(Collectors.toList()));
      throw new DataStoreException("Could not remove entity ids, " + ids, e);
    }
  }

  @Override
  public long save(SchematicEntity entity) throws DataStoreException {
    Preconditions.checkArgument(entity.isValid());
    try (Connection con = dataSource.getConnection()) {

      // Entities - start
      ByteBuffer entitiesBuffer = ByteBuffer.allocate(entity.getEntities().size() * 16);
      entity.getEntities().forEach(e -> {
        entitiesBuffer.putLong(e.getMostSignificantBits());
        entitiesBuffer.putLong(e.getLeastSignificantBits());
      });
      // Blob entities = con.createBlob();
      // entities.setBytes(0, entitiesBuffer.array());
      // Entities - end

      if (entity.hasId()) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE " + TABLE_ENTITIES + " SET" //
            + " name = ?, rotation = ?, entities = ?," //
            + " posStart_x = ?, posStart_y = ?, posStart_z = ?,"//
            + " posEnd_x = ?, posEnd_y = ?, posEnd_z = ?,"//
            + " timestamp = ?" //
            + " WHERE id = ?"//
        )) {

          ps.setString(1, entity.getName());
          ps.setByte(2, (byte) entity.getRotation().ordinal());
          ps.setBytes(3, entitiesBuffer.array());

          final IntVector min = entity.getHitBox().getMinPoint();
          ps.setInt(4, min.getX());
          ps.setInt(5, min.getY());
          ps.setInt(6, min.getZ());

          final IntVector max = entity.getHitBox().getMaxPoint();
          ps.setInt(7, max.getX());
          ps.setInt(8, max.getY());
          ps.setInt(9, max.getZ());

          ps.setLong(10, entity.getTimestamp());

          ps.setLong(11, entity.getId());

          // Check if anything was updated, if true then return
          if (ps.executeUpdate() != 0) {
            return entity.getId();
          }
        }
      }

      try (PreparedStatement ps = con.prepareStatement(//
          "INSERT INTO " + TABLE_ENTITIES + "(" //
              + " name, rotation, entities," //
              + " posStart_x, posStart_y, posStart_z,"//
              + " posEnd_x, posEnd_y, posEnd_z,"//
              + " timestamp,"//
              + " owner_msb, owner_lsb" //
              + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
          Statement.RETURN_GENERATED_KEYS)) {



        ps.setString(1, entity.getName());
        ps.setByte(2, (byte) entity.getRotation().ordinal());
        ps.setBytes(3, entitiesBuffer.array());

        final IntVector min = entity.getHitBox().getMinPoint();
        ps.setInt(4, min.getX());
        ps.setInt(5, min.getY());
        ps.setInt(6, min.getZ());

        final IntVector max = entity.getHitBox().getMaxPoint();
        ps.setInt(7, max.getX());
        ps.setInt(8, max.getY());
        ps.setInt(9, max.getZ());

        ps.setLong(10, entity.getTimestamp());
        UUID owner = entity.getOwner() != null ? entity.getOwner() : new UUID(0, 0);
        ps.setLong(11, owner.getMostSignificantBits());
        ps.setLong(12, owner.getLeastSignificantBits());

        if (ps.executeUpdate() == 0) {
          throw new SQLException("Creating entity failed, no rows affected.");
        }

        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            return generatedKeys.getLong(1);
          } else {
            throw new SQLException("Creating entity failed, no ID obtained.");
          }
        }

      }
    } catch (SQLException e) {
      throw new DataStoreException("Could not save entity, " + entity.getName(), e);
    }
  }

}
