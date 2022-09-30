// Taken from https://github.com/apache/commons-io/blob/master/src/main/java/org/apache/commons/io/input/NullInputStream.java
package com.amazonaws.serverless.proxy.internal.servlet;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class NullInputStream extends InputStream {

  private final int EOF = -1;
  private final long size;
  private long position;
  private long mark = -1;
  private long readlimit;
  private boolean eof;
  private final boolean throwEofException;
  private final boolean markSupported;

  /**
   * Create an {@link InputStream} that emulates a size 0 stream which supports marking and does not
   * throw EOFException.
   *
   * @since 2.7
   */
  public NullInputStream() {
    this(0, true, false);
  }

  /**
   * Create an {@link InputStream} that emulates a specified size which supports marking and does
   * not throw EOFException.
   *
   * @param size The size of the input stream to emulate.
   */
  public NullInputStream(final long size) {
    this(size, true, false);
  }

  /**
   * Create an {@link InputStream} that emulates a specified size with option settings.
   *
   * @param size              The size of the input stream to emulate.
   * @param markSupported     Whether this instance will support the {@code mark()} functionality.
   * @param throwEofException Whether this implementation end of file is reached.
   */
  public NullInputStream(final long size, final boolean markSupported,
      final boolean throwEofException) {
    this.size = size;
    this.markSupported = markSupported;
    this.throwEofException = throwEofException;
  }

  /**
   * Read a byte.
   *
   * @return Either The byte value returned by {@code processByte()} or {@code -1} if the end of
   * file has been reached and {@code throwEofException} is set to {@code false}.
   * @throws IOException if trying to read past the end of file.
   * @throws_ EOFException if the end of file is reached and {@code throwEofException} is set to
   * {@code true}.
   */
  @Override
  public int read() throws IOException {
    if (eof) {
      throw new IOException("Read after end of file");
    }
    if (position == size) {
      return doEndOfFile();
    }
    position++;
    return processByte();
  }

  private int doEndOfFile() throws EOFException {
    eof = true;
    if (throwEofException) {
      throw new EOFException();
    }
    return EOF;
  }

  protected int processByte() {
    // do nothing - overridable by subclass
    return 0;
  }

}
