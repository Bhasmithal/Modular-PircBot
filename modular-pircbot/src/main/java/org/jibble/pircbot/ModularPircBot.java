package org.jibble.pircbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.modules.AbstractPircModule;
import org.jibble.pircbot.modules.AbstractRunnablePircModule;
import org.jibble.pircbot.modules.AbstractStoppablePircModule;
import org.jibble.pircbot.modules.PrivatePircModule;
import org.jibble.pircbot.modules.PublicPircModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModularPircBot extends ExtendedPircBot {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModularPircBot.class);

	private ThreadGroup threadGroup = new ThreadGroup(getClass().getSimpleName());
	
	private String host;
	
	private List<Integer> ports;
	
	private String helpIntro;

	private Set<AbstractPircModule> modules = new HashSet<AbstractPircModule>();
	
	public ModularPircBot(String host, List<Integer> ports, String name) {
		this.host = host;
		this.ports = ports;
		setName(name);
	}

	public void addModule(AbstractPircModule module) {
		modules.add(module);
	}

	public void connect() {
		int i = 0;
		do {
			try {
				LOGGER.info("Connecting to {}:{}", host, ports.get(i));
				connect(host, ports.get(i));
			} catch (IrcException ie) {
				LOGGER.warn("IRC Exception while connecting to the server", ie);
			} catch (IOException ioe) {
				LOGGER.error("I/O Exception while connecting to the server", ioe);
			}
			i = ++i % ports.size();
		} while (!isConnected());
	}
	
	public void setHelpIntro(String helpIntro) {
		this.helpIntro = helpIntro;
	}

	@Override
	protected void onConnect() {
		LOGGER.info("Connected with name: {}", getNick());

		// First run onConnect() methods of all modules
		for (AbstractPircModule module : modules) {
			module.onConnect(this);
		}

		
		// Then launch the runnable modules
		for (AbstractPircModule module : modules) {
			if (module instanceof AbstractRunnablePircModule) {
				AbstractRunnablePircModule runnableModule = (AbstractRunnablePircModule) module;
				runnableModule.setBot(this);
				LOGGER.info("Launching module thread: {}", runnableModule);
				new Thread(threadGroup, runnableModule).start();
			}
		}
	}
	
	@Override
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		for (AbstractPircModule module : modules) {
			module.onTopic(this, channel, topic, setBy, date, changed);
		}
	}

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		for (AbstractPircModule module : modules) {
			module.onMessage(this, channel, sender, login, hostname, message);
			if (module instanceof PublicPircModule) {
				PublicPircModule publicModule = (PublicPircModule) module;
				String triggerMessage = "!" + publicModule.getTriggerMessage();
				if (message.equals(triggerMessage)) {
					publicModule.onTriggerMessage(this, channel, sender, login, hostname, message);
				}
			}
		}
	}
	
	@Override
	protected void onPrivateMessage(String sender, String login, String hostname, String message) {
		Boolean isSenderOp = null;

		for (AbstractPircModule module : modules) {
			module.onPrivateMessage(this, sender, login, hostname, message);

			if (module instanceof PrivatePircModule) {
				PrivatePircModule privateModule = (PrivatePircModule) module;
				
				// Is message a trigger?
				if (message.equals(privateModule.getPrivateTriggerMessage())) {
					// Is op required?
					if (privateModule.isOpRequired()) {
						// Check if user is op only the first time
						if (isSenderOp == null) {
							isSenderOp = isUserOp(sender);
						}

						if (!isSenderOp) {
							// Op required but user not op, skipping
							LOGGER.info("User {} cannot trigger {} module because he/she is not op", sender,
								module.getClass());
							continue;
						}
					}

					privateModule.onTriggerPrivateMessage(this, sender, login, hostname, message);
				}
			}
		}
	}
	
	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		for (AbstractPircModule module : modules) {
			module.onAction(this, sender, login, hostname, target, action);
		}
	}

	@Override
	protected void onServerResponse(int code, String response) {
		for (AbstractPircModule module : modules) {
			module.onServerResponse(this, code, response);
		}
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		for (AbstractPircModule module : modules) {
			module.onJoin(this, channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
		for (AbstractPircModule module : modules) {
			module.onNickChange(this, oldNick, login, hostname, newNick);
		}
	}

	@Override
	protected void onPart(String channel, String sender, String login, String hostname) {
		for (AbstractPircModule module : modules) {
			module.onPart(this, channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		for (AbstractPircModule module : modules) {
			module.onQuit(this, sourceNick, sourceLogin, sourceHostname, reason);
		}
	}
	
	@Override
	protected void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		for (AbstractPircModule module : modules) {
			module.onMode(this, channel, sourceNick, sourceLogin, sourceHostname, mode);
		}
	}
	
	@Override
	protected void
			onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		for (AbstractPircModule module : modules) {
			module.onUserMode(this, targetNick, sourceNick, sourceLogin, sourceHostname, mode);
		}
	}

	@Override
	protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname,
			String recipientNick, String reason) {
		for (AbstractPircModule module : modules) {
			module.onKick(this, channel, kickerNick, kickerLogin, kickerHostname, recipientNick, reason);
		}
	}

	@Override
	protected void onDisconnect() {
		LOGGER.info("Bot disconnected");

		if (!isQuitRequested()) {
			// Not a wanted quit, forcing reconnect
			LOGGER.info("Unexpected disconnection detected, reconnecting");
			connect();
		} else {
			// Quit requested, stopping threads and exiting
			for (AbstractPircModule module : modules) {
				if (module instanceof AbstractStoppablePircModule) {
					AbstractStoppablePircModule stoppableModule = (AbstractStoppablePircModule) module;
					stoppableModule.stop();
				}
			}
			
			int stopChecks = 0;
			do {
				stopChecks++;
				// Wait a total of 4.5s
				try {
					Thread.sleep(1500);
				} catch (InterruptedException ie) {
					LOGGER.error("Could not wait until threads were stopped, some might be killed", ie);
				}
			} while (stopChecks < 3 && threadGroup.activeCount() > 0);
			
			if (threadGroup.activeCount() > 0) {
				LOGGER.warn("One or more thread are still running, they will now be killed");
			}
			
			LOGGER.info("Exiting");

			System.exit(0);
		}
	}
	
	@Override
	public List<String> buildHelp(String nick, boolean inPrivate) {
		List<String> help = new ArrayList<String>();
		if (StringUtils.isNotBlank(helpIntro)) {
			help.add(helpIntro);
		}
		
		Map<String, String> helpMap = new TreeMap<String, String>();
		if (!inPrivate) {
			// Public
			for (AbstractPircModule module : modules) {
				if (module instanceof PublicPircModule) {
					PublicPircModule publicModule = (PublicPircModule) module;
					String trigger = "!" + publicModule.getTriggerMessage();
					String line = trigger;
					if (StringUtils.isNotBlank(publicModule.getHelp())) {
						// We suppose commands are never bigger than 20 characters
						line = StringUtils.rightPad(line, 20) + publicModule.getHelp();
					}
					helpMap.put(trigger, line);
				}
			}
		} else {
			// Private
			boolean isUserOp = isUserOp(nick);

			for (AbstractPircModule module : modules) {
				if (module instanceof PrivatePircModule) {
					PrivatePircModule privateModule = (PrivatePircModule) module;
					if (privateModule.isOpRequired() && !isUserOp) {
						// Module can only be displayed to ops
						continue;
					}
					
					String trigger = privateModule.getPrivateTriggerMessage();
					String line = trigger;
					// Adding public help if it is available
					if (module instanceof PublicPircModule) {
						PublicPircModule publicModule = (PublicPircModule) module;
						if (StringUtils.isNotBlank(publicModule.getHelp())) {
							// We suppose commands are never bigger than 20
							// characters
							line = StringUtils.rightPad(line, 20) + publicModule.getHelp();
						}
					}
					helpMap.put(trigger, line);
				}
			}
		}
		help.addAll(new TreeMap<String, String>(helpMap).values());

		return help;
	}
	
	// internal helpers
	
	private boolean isUserOp(String nick) {
		String[] channels = getChannels();
		// User may be found on any channel
		for (String channel : channels) {
			User[] users = getUsers(channel);
			for (User user : users) {
				// If the user is found
				if (user.getNick().equals(nick)) {
					// Check if OP
					if (user.isOp()) {
						return true;
					} else {
						// Break the loop for this channel, go for next one
						break;
					}
				}
			}
		}
		// User not found, considered as not op
		return false;
	}
}