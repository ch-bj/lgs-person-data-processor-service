package org.datarocks.lwgs.searchindex.client.util;

import java.io.*;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.service.exception.DeserializationFailedException;
import org.datarocks.lwgs.searchindex.client.service.exception.SerializationFailedException;

public class BinarySerializerUtil {
  private BinarySerializerUtil() {}

  public static <T> T convertByteArrayToObject(final byte[] payload, final Class<T> clazz)
      throws DeserializationFailedException {
    final ByteArrayInputStream bis = new ByteArrayInputStream(payload);
    try {
      final ObjectInput in = new ObjectInputStream(bis);
      return clazz.cast(in.readObject());
    } catch (ClassNotFoundException | IOException e) {
      throw new DeserializationFailedException();
    }
  }

  public static byte[] convertObjectToByteArray(@NonNull final Object o)
      throws SerializationFailedException {

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      final ObjectOutput out = new ObjectOutputStream(bos);
      out.writeObject(o);
      return bos.toByteArray();
    } catch (IOException e) {
      throw new SerializationFailedException();
    }
  }
}
