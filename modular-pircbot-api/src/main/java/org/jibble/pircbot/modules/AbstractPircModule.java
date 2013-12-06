package org.jibble.pircbot.modules;

import org.jibble.pircbot.ExtendedPircBot;
import org.jibble.pircbot.ReplyConstants;

/**
 * You can extend this class to create a Pirc module that can be added to the bot to enhance its
 * functionalities. A module can override any of the methods declared by this class to react to
 * particular events and do stuff. Typically, you can trigger actions when the bot connects to a
 * server, when a message is received, ...
 * <p>
 * You might want to give a look to {@link AbstractRunnablePircModule},
 * {@link AbstractStoppablePircModule}, {@link PrivatePircModule} and {@link PublicPircModule} as
 * well.
 * 
 * @author Emmanuel Cron
 */
public abstract class AbstractPircModule {
  /**
   * This method is called once the bot has successfully connected to the IRC server. This happens
   * before {@code AUTH}, {@code MODE} and joining channels.
   * 
   * @param bot the current instance of the bot
   */
  public void onConnect(ExtendedPircBot bot) {}

  /**
   * This method is called whenever a user sets the topic, or when the bot joins a new channel and
   * discovers its topic.
   * 
   * @param bot the current instance of the bot
   * @param channel the channel that the topic belongs to
   * @param topic the topic for the channel
   * @param setBy the nick of the user that set the topic
   * @param date when the topic was set (milliseconds since the epoch)
   * @param changed {@code true} if the topic has just been changed, {@code false} if the topic was
   *        discovered by the bot
   */
  public void onTopic(ExtendedPircBot bot, String channel, String topic, String setBy, long date,
      boolean changed) {}

  /**
   * Called when a message is received by the bot on a joined channel.
   * 
   * @param bot the current instance of the bot
   * @param channel the channel to which the message was sent
   * @param sender the nick of the person who sent the message
   * @param login the login of the person who sent the message
   * @param hostname the hostname of the person who sent the message
   * @param message the actual message sent to the channel
   */
  public void onMessage(ExtendedPircBot bot, String channel, String sender, String login,
      String hostname, String message) {}

  /**
   * This method is called whenever a private message is sent to the bot.
   * 
   * @param bot the current instance of the bot
   * @param sender the nick of the person who sent the private message
   * @param login the login of the person who sent the private message
   * @param hostname the hostname of the person who sent the private message
   */
  public void onPrivateMessage(ExtendedPircBot bot, String sender, String login, String hostname,
      String message) {}

  /**
   * This method is called whenever an ACTION is sent from a user. E.g. such events generated by
   * typing "{@code /me goes shopping}" in most IRC clients.
   * 
   * @param bot the current instance of the bot
   * @param sender the nick of the user that sent the action
   * @param login the login of the user that sent the action
   * @param hostname the hostname of the user that sent the action
   * @param target the target of the action, be it a channel or our nick
   * @param action the action carried out by the user
   */
  public void onAction(ExtendedPircBot bot, String sender, String login, String hostname,
      String target, String action) {}

  /**
   * This method is called when the bot receives a numeric response from the IRC server.
   * <p>
   * Numerics in the range from {@code 001} to {@code 099} are used for client-server connections
   * only and should never travel between servers. Replies generated in response to commands are
   * found in the range from {@code 200} to {@code 399}. Error replies are found in the range from
   * {@code 400} to {@code 599}.
   * <p>
   * You can use the values of {@link ReplyConstants} to identify the meaning of a particular code.
   * 
   * @param bot the current instance of the bot
   * @param code the three-digit numerical code for the response
   * @param response the full response from the IRC server
   */
  public void onServerResponse(ExtendedPircBot bot, int code, String response) {}

  /**
   * This method is called whenever someone (possibly us) joins a channel which we are on or
   * joining.
   * 
   * @param bot the current instance of the bot
   * @param channel the channel which somebody joined
   * @param sender the nick of the user who joined the channel
   * @param login the login of the user who joined the channel
   * @param hostname the hostname of the user who joined the channel
   */
  public void onJoin(ExtendedPircBot bot, String channel, String sender, String login,
      String hostname) {}

  /**
   * This method is called whenever someone (possibly us) changes nick on any of the channels that
   * we are on.
   * 
   * @param bot the current instance of the bot
   * @param oldNick the old nick
   * @param login the login of the user
   * @param hostname the hostname of the user
   * @param newNick the new nick
   */
  public void onNickChange(ExtendedPircBot bot, String oldNick, String login, String hostname,
      String newNick) {}

  /**
   * This method is called whenever someone (possibly us) parts (leaves) a channel which we are on.
   * 
   * @param bot the current instance of the bot
   * @param channel the channel which somebody parted from
   * @param sender the nick of the user who parted from the channel
   * @param login the login of the user who parted from the channel
   * @param hostname the hostname of the user who parted from the channel
   */
  public void onPart(ExtendedPircBot bot, String channel, String sender, String login,
      String hostname) {}

  /**
   * This method carries out the actions to be performed when the bot gets disconnected. This may
   * happen if the bot quits from the server, or if the connection is unexpectedly lost.
   * <p>
   * Disconnection from the IRC server is detected immediately if either we or the server close the
   * connection normally. If the connection to the server is lost, but neither we nor the server
   * have explicitly closed the connection, then it may take a few minutes to detect (this is
   * commonly referred to as a "ping timeout").
   * <p>
   * If you wish to get your IRC bot to automatically rejoin a server after the connection has been
   * lost, then this is probably the ideal method to override to implement such functionality.
   * 
   * @param bot the current instance of the bot
   */
  public void onDisconnect(ExtendedPircBot bot) {}

  /**
   * This method is called whenever someone (possibly us) quits from the server. We will only
   * observe this if the user was in one of the channels to which we are connected.
   * 
   * @param bot the current instance of the bot
   * @param sourceNick the nick of the user that quit from the server
   * @param sourceLogin the login of the user that quit from the server
   * @param sourceHostname the hostname of the user that quit from the server
   * @param reason the reason given for quitting the server
   */
  public void onQuit(ExtendedPircBot bot, String sourceNick, String sourceLogin,
      String sourceHostname, String reason) {}

  /**
   * Called when the mode of a channel is set.
   * 
   * @param bot the current instance of the bot
   * @param channel the channel that the mode operation applies to
   * @param sourceNick the nick of the user that set the mode
   * @param sourceLogin the login of the user that set the mode
   * @param sourceHostname the hostname of the user that set the mode
   * @param mode the mode that has been set
   * 
   */
  public void onMode(ExtendedPircBot bot, String channel, String sourceNick, String sourceLogin,
      String sourceHostname, String mode) {}

  /**
   * Called when the mode of a user is set.
   * 
   * @param bot the current instance of the bot
   * @param targetNick the nick that the mode operation applies to
   * @param sourceNick the nick of the user that set the mode
   * @param sourceLogin the login of the user that set the mode
   * @param sourceHostname the hostname of the user that set the mode
   * @param mode the mode that has been set
   */
  public void onUserMode(ExtendedPircBot bot, String targetNick, String sourceNick,
      String sourceLogin, String sourceHostname, String mode) {}

  /**
   * This method is called whenever someone (possibly the bot itself) is kicked from any of the
   * channels that the bot is in.
   * 
   * @param bot the current instance of the bot
   * @param channel the channel from which the recipient was kicked
   * @param kickerNick the nick of the user who performed the kick
   * @param kickerLogin the login of the user who performed the kick
   * @param kickerHostname the hostname of the user who performed the kick
   * @param recipientNick the unfortunate recipient of the kick
   * @param reason the reason given by the user who performed the kick
   */
  public void onKick(ExtendedPircBot bot, String channel, String kickerNick, String kickerLogin,
      String kickerHostname, String recipientNick, String reason) {}
}
