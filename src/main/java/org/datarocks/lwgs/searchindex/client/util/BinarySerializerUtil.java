package org.datarocks.lwgs.searchindex.client.util;

import java.io.*;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.service.exception.DeserializationFailedException;
import org.datarocks.lwgs.searchindex.client.service.exception.SerializationFailedException;

public class BinarySerializerUtil {
  private BinarySerializerUtil() {}

  public static <T> T convertByteArrayToObject(byte[] payload, Class<T> clazz)
      throws DeserializationFailedException {
    ByteArrayInputStream bis = new ByteArrayInputStream(payload);
    try {
      ObjectInput in = new ObjectInputStream(bis);
      return clazz.cast(in.readObject());
    } catch (ClassNotFoundException | IOException e) {
      throw new DeserializationFailedException();
    }
  }

  public static byte[] convertObjectToByteArray(@NonNull final Object o)
      throws SerializationFailedException {

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      ObjectOutput out = new ObjectOutputStream(bos);
      out.writeObject(o);
      return bos.toByteArray();
    } catch (IOException e) {
      throw new SerializationFailedException();
    }
  }
}
