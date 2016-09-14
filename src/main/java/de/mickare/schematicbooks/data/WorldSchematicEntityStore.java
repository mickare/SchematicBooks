package de.mickare.schematicbooks.data;

import java.util.Collection;
import java.util.UUID;

public interface WorldSchematicEntityStore {

  long save(SchematicEntity entity) throws DataStoreException;

  Collection<SchematicEntity> load(ChunkPosition pos) throws DataStoreException;

  int remove(Collection<Long> entityIds) throws DataStoreException;

  SchematicEntity load(long id) throws DataStoreException;

  Collection<SchematicEntity> list(UUID owner) throws DataStoreException;

}
