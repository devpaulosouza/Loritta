package com.mrpowergamerbr.loritta.listeners;

import com.mrpowergamerbr.loritta.Loritta;
import com.mrpowergamerbr.loritta.LorittaLauncher;
import com.mrpowergamerbr.loritta.commands.CommandBase;
import com.mrpowergamerbr.loritta.commands.CommandContext;
import com.mrpowergamerbr.loritta.commands.CommandOptions;
import com.mrpowergamerbr.loritta.commands.custom.CustomCommand;
import com.mrpowergamerbr.loritta.userdata.LorittaProfile;
import com.mrpowergamerbr.loritta.userdata.ServerConfig;
import com.mrpowergamerbr.loritta.utils.LorittaUtils;
import com.mrpowergamerbr.loritta.utils.music.AudioTrackWrapper;
import com.mrpowergamerbr.loritta.whistlers.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter {
	Loritta loritta;

	public DiscordListener(Loritta loritta) {
		this.loritta = loritta;
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) { return; }
		if (event.isFromType(ChannelType.TEXT)) {
			loritta.getExecutor().execute(() -> {
				try {
					// cache.put(event.getMessage().getId(), event.getMessage());
					ServerConfig conf = loritta.getServerConfigForGuild(event.getGuild().getId());

					if (!event.getMessage().getContent().startsWith(conf.commandPrefix())) { // TODO: Filtrar links
						loritta.getHal().add(event.getMessage().getContent().toLowerCase());
					}

					for (Role r : event.getMember().getRoles()) {
						if (r.getName().equalsIgnoreCase("Inimigo da Loritta")) {
							return;
						}
					}

                    LorittaProfile lorittaProfile = loritta.getLorittaProfileForUser(event.getAuthor().getId());
                    lorittaProfile.setXp(lorittaProfile.getXp() + 1);
                    loritta.getDs().save(lorittaProfile);

					for (Whistler whistler : conf.whistlers()) {
						processCode(conf, event.getMessage(), whistler.codes);
					}

					// Primeiro os comandos customizados da Loritta(tm)
					for (CommandBase cmd : loritta.getCommandManager().getCommandMap()) {
						if (conf.debugOptions().enableAllModules() || !conf.disabledCommands().contains(cmd.getClass().getSimpleName())) {
							if (cmd.handle(event, conf)) {
								// event.getChannel().sendTyping().queue();
								CommandOptions cmdOpti = conf.getCommandOptionsFor(cmd);
								if (conf.deleteMessageAfterCommand() || cmdOpti.deleteMessageAfterCommand()) {
									event.getMessage().delete().complete();
								}
								return;
							}
						}
					}

					// E agora os comandos do servidor
					for (CustomCommand cmd : conf.customCommands()) {
						if (cmd.handle(event, conf)) {
							if (conf.deleteMessageAfterCommand()) {
								event.getMessage().delete().complete();
							}
						}
						return;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	@Override
	public void onGenericMessageReaction(GenericMessageReactionEvent e) {
		if (LorittaLauncher.getInstance().messageContextCache.containsKey(e.getMessageId())) {
			CommandContext context = (CommandContext) LorittaLauncher.getInstance().messageContextCache.get(e.getMessageId());
			Thread t = new Thread() {
			    public void run() {
                    Message msg = e.getTextChannel().getMessageById(e.getMessageId()).complete();
                    if (msg != null && !e.getMember().getUser().isBot()) {
                        context.cmd.onCommandReactionFeedback(context, e, msg);
                    }
                }
            };
            t.start();
		}
	    if (LorittaLauncher.getInstance().getMusicMessagesCache().containsKey(e.getMessageId())) {
            AudioTrackWrapper atw = (AudioTrackWrapper) LorittaLauncher.getInstance().getMusicMessagesCache().get(e.getMessageId());

            int count = e.getReaction().getUsers().complete().stream().filter((user) -> !user.isBot()).collect(Collectors.toList()).size();
            ServerConfig conf = LorittaLauncher.getInstance().getServerConfigForGuild(e.getGuild().getId());

            if (count > 0 && conf.musicConfig().isVoteToSkip() && LorittaLauncher.getInstance().getGuildAudioPlayer(e.getGuild()).scheduler.getCurrentTrack() == atw) {
                VoiceChannel vc = e.getGuild().getVoiceChannelById(conf.musicConfig().getMusicGuildId());

                if (vc != null) {
                    int inChannel = vc.getMembers().stream().filter((member) -> !member.getUser().isBot()).collect(Collectors.toList()).size();
                    long required = Math.round((double) inChannel * ((double) conf.musicConfig().getRequired() / 100));

                    if (count >= required) {
                        LorittaLauncher.getInstance().skipTrack(e.getGuild());
                        e.getTextChannel().sendMessage("🤹 Música pulada!").complete();
                        LorittaLauncher.getInstance().getMusicMessagesCache().remove(e.getMessageId());
                    }
                }
            }
        }
	}

	// TODO: Isto não deveria ficar aqui...
	public static void processCode(ServerConfig conf, Message message, List<ICode> codes) {
		try {
			wow:
				for (ICode code : codes) {
					if (code instanceof CodeBlock) {
						CodeBlock codeBlock = (CodeBlock) code;

						boolean valid = false;
						for (IPrecondition precondition : codeBlock.preconditions) {
							valid = precondition.isValid(conf, message);
							if (!valid) {
								break wow;
							}
						}

						processCode(conf, message, ((CodeBlock) code).codes);
					}
					if (code instanceof ReplyCode) {
						ReplyCode replyCode = (ReplyCode) code;

						replyCode.handle(message.getTextChannel());
					}
					if (code instanceof ReactionCode) {
						ReactionCode replyCode = (ReactionCode) code;

						replyCode.handle(message);
					}
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		loritta.getExecutor().execute(() -> {
			try {
				ServerConfig conf = loritta.getServerConfigForGuild(event.getGuild().getId());

				if (conf.joinLeaveConfig().isEnabled()) {
					if (conf.joinLeaveConfig().isTellOnJoin()) {
						Guild guild = event.getGuild();

						TextChannel textChannel = guild.getTextChannelById(conf.joinLeaveConfig().getCanalJoinId());

						if (textChannel != null) {
							if (textChannel.canTalk()) {
								String msg = conf.joinLeaveConfig().getJoinMessage().replace("%UserMention%", event.getMember().getAsMention());
								textChannel.sendMessage(msg).complete();
							} else {
								LorittaUtils.warnOwnerNoPermission(guild, textChannel, conf);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		loritta.getExecutor().execute(() -> {
			try {
				ServerConfig conf = loritta.getServerConfigForGuild(event.getGuild().getId());

				if (conf.joinLeaveConfig().isEnabled()) {
					if (conf.joinLeaveConfig().isTellOnLeave()) {
						Guild guild = event.getGuild();

						TextChannel textChannel = guild.getTextChannelById(conf.joinLeaveConfig().getCanalLeaveId());

						if (textChannel != null) {
							if (textChannel.canTalk()) {
								String msg = conf.joinLeaveConfig().getLeaveMessage().replace("%UserMention%", event.getMember().getAsMention());
								textChannel.sendMessage(msg).complete();
							} else {
								LorittaUtils.warnOwnerNoPermission(guild, textChannel, conf);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}