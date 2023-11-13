package ch.ejpd.lgs.searchindex.client.util;

import ch.ejpd.lgs.searchindex.client.service.exception.DeserializationFailedException;
import ch.ejpd.lgs.searchindex.client.service.exception.SerializationFailedException;
import java.io.*;
import lombok.NonNull;

/**
 * Utility class for serializing and deserializing objects to and from byte arrays.
 * Provides methods to convert objects to byte arrays and vice versa using Java serialization.
 */
public class BinarySerializerUtil {
  private BinarySerializerUtil() {}

  /**
   * Converts a byte array to an object of the specified class using Java deserialization.
   *
   * @param payload The byte array to be deserialized.
   * @param clazz   The class type of the object to be deserialized.
   * @param <T>     The generic type of the object.
   * @return        The deserialized object.
   * @throws DeserializationFailedException If deserialization fails.
   */
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

  /**
   * Converts an object to a byte array using Java serialization.
   *
   * @param o The object to be serialized.
   * @return  The serialized byte array.
   * @throws SerializationFailedException If serialization fails.
   */
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
