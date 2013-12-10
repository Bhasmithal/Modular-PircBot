package org.jibble.pircbot.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.jibble.pircbot.util.urlshorteners.SharasURLShortener;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Optional;

public class SharaURLShortenerTest {
  private static final String CACHE_FOLDER = "target/cache";

  @BeforeClass
  public static void createCacheFolder() {
    new File(CACHE_FOLDER).mkdir();
  }

  @Test
  public void shortenURL() throws IOException {
    SharasURLShortener sharas = new SharasURLShortener(Optional.of(Paths.get(CACHE_FOLDER)));
    String shortenedURL = sharas.shortenURL("http://www.judgehype.com");

    assertEquals("http://shar.as/Fg1e3", shortenedURL);
  }
}
