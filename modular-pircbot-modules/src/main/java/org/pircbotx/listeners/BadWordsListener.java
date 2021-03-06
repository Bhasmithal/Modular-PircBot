package org.pircbotx.listeners;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Loads a list of words that are forbidden on any public chat on which the bot is connected. If any
 * of these words is said by a user connected to these channels, he/she is immediately kicked with a
 * custom reason.
 * <p>
 * This listener will not kick admins.
 *
 * <h2>Bad words file format</h2>
 * Bad words must be written in a file containing one bad word per line. If a line starts with '
 * <tt>#</tt>', the line is ignored.
 * <p>
 * Example:
 *
 * <pre>
 * # This is the bad words file
 * idiot
 * motherkisser
 *
 * # Will be ignored
 * #stupid
 * </pre>
 *
 * @author Emmanuel Cron
 */
public class BadWordsListener extends ListenerAdapter<PircBotX> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BadWordsListener.class);

  private static final List<String> BADWORDS = new ArrayList<String>();

  private String kickReason;

  /**
   * Creates a new badwords listener.
   *
   * @param badwordsPath path from the root of the classpath to the file containing the bad words;
   *        each word must be on a new line
   * @param encoding the encoding in which the file is stored
   * @param kickReason the custom reason that will be displayed when a user is kicked by the bot
   */
  public BadWordsListener(String badwordsPath, String encoding, String kickReason) {
    checkArgument(!Strings.isNullOrEmpty(badwordsPath), "No path to badwords file given");
    checkArgument(!Strings.isNullOrEmpty(encoding), "Badwords file encoding not specified");
    checkArgument(!Strings.isNullOrEmpty(kickReason), "No kick reason given");

    this.kickReason = kickReason;
    loadBadWords(badwordsPath, encoding);
  }

  @Override
  public void onMessage(MessageEvent<PircBotX> event) {
    if (event.getUser().getNick().equals(event.getBot().getNick())) {
      // Avoid self kick
      return;
    }

    if (event.getChannel().getOps().contains(event.getUser())) {
      // Avoid kicking ops
      return;
    }

    String lowerMessage = " " + event.getMessage().toLowerCase() + " ";
    for (String badword : BADWORDS) {
      if (lowerMessage.contains(badword)) {
        event.getChannel().send().kick(event.getUser(), kickReason);
      }
    }
  }

  // internal helpers

  private void loadBadWords(String badwordsPath, String encoding) {
    try (InputStream input = ClassLoader.getSystemResourceAsStream(badwordsPath)) {
      Reader reader = new InputStreamReader(input, encoding);
      BufferedReader bufferedReader = new BufferedReader(reader);

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        // trim afterwards to avoid NPE
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          // Put everything small case for easier comparison
          BADWORDS.add(" " + line.toLowerCase() + " ");
        }
      }
    } catch (IOException ioe) {
      LOGGER.error("Could not read badwords file, ignoring", ioe);
    }
  }
}
