package org.pircbotx.listeners;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.listeners.onaction.Answer;
import org.pircbotx.listeners.onaction.DynamicOnActionPattern;
import org.pircbotx.listeners.onaction.OnActionAnswers;
import org.pircbotx.listeners.onaction.OnActionPattern;
import org.pircbotx.listeners.onaction.OnActionPatternFactory;
import org.pircbotx.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends bot replies or actions in a public channel when someone does an action matching one of a
 * list of provided regular expression. These expressions and replies must be provided in a
 * properties file as described below.
 * <p>
 * You can define a trigger probability to make the bot reply only sometimes. This probability
 * should be between 0 (never triggered) and 100 (always triggered). For example, if you set a
 * probability of 50 for a specific set of regular expressions, the listener will be triggered only
 * half of the time.
 * <p>
 * You can use <b>{botname}</b> in your regular expressions to match the bot name.
 * <p>
 * Examples of valid regular expressions
 * <ul>
 * <li><tt>^uses .+$</tt></li>
 * <li><tt>^summons .+$</tt></li>
 * <li><tt>^pokes {botname}$</tt></li>
 * <li><tt>^says hello to {botname}$</tt></li>
 * </ul>
 *
 * When the listener is triggered, the bot replies with either a standard message (private) or an
 * action in the public channel the trigger was done.
 * <p>
 * Examples of private messages and actions:
 * <ul>
 * <li>(private) <tt>It's super effective!</tt></li>
 * <li>(private) <tt>It fails completely.</tt></li>
 * <li>(action) <tt>waves</tt></li>
 * <li>(action) <tt>giggles</tt></li>
 * </ul>
 *
 * <h2>Actions file format</h2>
 * The actions file should be formatted using one identifier for each set of actions this listener
 * should react to. Each identifier should then be suffixed as described below:
 * <p>
 * <dl>
 * <dt>&lt;identifier&gt;.triggermessages.&lt;number&gt;=</dt>
 * <dd>An action regular expression on which the listener can be triggered for this set. The number
 * should start at 1 and be increased for each regular expression.</dd>
 * <dt>&lt;identifier&gt;.probability=</dt>
 * <dd>The probability on 100 with which the listener will be triggered. For example, a probability
 * of 50 will trigger the listener only half of the times.</dd>
 * <dt>&lt;identifier&gt;.possibleanswers.private.&lt;number&gt;=</dt>
 * <dd>An answer with which the bot can reply to the trigger on a public chat. The number should
 * start at 1 and be increased for each possible answer. You can use <b>{sender}</b> to use the name
 * of the person who triggered the listener.</dd>
 * <dt>&lt;identifier&gt;.possibleanswers.action.&lt;number&gt;=</dt>
 * <dd>An action that the bot can use to reply to the trigger. The number should start at 1 and be
 * increased for each possible answer. You can use <b>{sender}</b> to use the name of the person who
 * triggered the listener.</dd>
 * </dl>
 *
 * Example of an action file:
 *
 * <pre>
 * poke.triggermessages.1=^pokes {botname}$
 * poke.triggermessages.2=^sends a poke to {botname}$
 * poke.probability=100
 * poke.possibleanswers.private.1=Hey!
 * poke.possibleanswers.action.1=giggles
 * poke.possibleanswers.action.2=pokes {sender} harder!
 * </pre>
 *
 * @author Emmanuel Cron
 */
public class OnActionListener extends ListenerAdapter<PircBotX> {
  private static final Logger LOGGER = LoggerFactory.getLogger(OnActionListener.class);

  private static final Random RANDOM = new Random();

  private static final Map<OnActionPattern, OnActionAnswers> TRIGGER_ACTIONS =
      new HashMap<OnActionPattern, OnActionAnswers>();

  /**
   * Creates a new on action listener.
   *
   * @param actionsPath path related to the context to the action file
   * @param encoding encoding in which the action file is saved
   */
  public OnActionListener(String actionsPath, String encoding) {
    loadActions(checkNotNull(actionsPath), checkNotNull(encoding));
  }

  @Override
  public void onConnect(ConnectEvent<PircBotX> event) {
    // We now know the name of the bot, update dynamic patterns
    updateDynamicOnActionPatterns(event.getBot().getNick());
  }

  @Override
  public void onNickChange(NickChangeEvent<PircBotX> event) {
    if (event.getNewNick().equals(event.getBot().getNick())) {
      // Bot just changed nick, updating all dynamic patterns
      updateDynamicOnActionPatterns(event.getBot().getNick());
    }
  }

  @Override
  public void onAction(ActionEvent<PircBotX> event) {
    // Check if action can trigger a reply
    for (Entry<OnActionPattern, OnActionAnswers> entry : TRIGGER_ACTIONS.entrySet()) {
      if (entry.getKey().matches(event.getAction())) {
        // Found one!
        OnActionAnswers onActionAnswers = entry.getValue();

        // Should we display it this time?
        if (RANDOM.nextInt(100) < onActionAnswers.getProbability()) {
          List<Answer> possibleAnswers = onActionAnswers.getPossibleAnswers();
          Answer answer = possibleAnswers.get(RANDOM.nextInt(possibleAnswers.size()));

          String line = answer.buildAnswer(event.getUser().getNick());

          if (answer.isAction()) {
            event.respond(line);
          } else {
            event.getChannel().send().message(line);
          }
        }
      }
    }
  }

  // internal helpers

  private void updateDynamicOnActionPatterns(String botname) {
    for (OnActionPattern onActionPattern : TRIGGER_ACTIONS.keySet()) {
      if (onActionPattern instanceof DynamicOnActionPattern) {
        ((DynamicOnActionPattern) onActionPattern).updatePattern(botname);
      }
    }
  }

  private void loadActions(String actionsPath, String encoding) {
    Properties properties = new Properties();
    try (InputStream input = ClassLoader.getSystemResourceAsStream(actionsPath)) {
      Reader reader = new InputStreamReader(input, encoding);
      properties.load(reader);
    } catch (IOException ioe) {
      LOGGER.error("Could not load actions file, they will not be available", ioe);
      return;
    }

    List<String> processedProperties = new ArrayList<String>();

    for (String rawProperty : properties.stringPropertyNames()) {
      String property = rawProperty.substring(0, rawProperty.indexOf('.'));
      if (!processedProperties.contains(property)) {
        processedProperties.add(property);

        List<String> triggerMessages =
            PropertiesUtils.getStringList(property + ".triggermessages", properties);
        int probability = PropertiesUtils.getInt(properties.getProperty(property + ".probability"));
        List<String> possiblePrivateAnswers =
            PropertiesUtils.getStringList(property + ".possibleanswers.private", properties);
        List<String> possibleActionAnswers =
            PropertiesUtils.getStringList(property + ".possibleanswers.action", properties);

        OnActionAnswers onActionAnswers =
            new OnActionAnswers(probability, possiblePrivateAnswers, possibleActionAnswers);

        for (String triggerMessage : triggerMessages) {
          OnActionPattern pattern = OnActionPatternFactory.build(triggerMessage);
          TRIGGER_ACTIONS.put(pattern, onActionAnswers);
        }
      }
    }
  }
}
